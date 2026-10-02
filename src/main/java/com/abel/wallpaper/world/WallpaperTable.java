package com.abel.wallpaper.world;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

/**
 * A sparse lookup table of wallpapers for a chunk (16x256x16).
 *
 * The maximum number of distinct palette entries is 2^13 = 8192.
 */
public class WallpaperTable {
    private static final int FACE_BITS = 3;
    private static final int LOCAL_X_BITS = 4;
    private static final int LOCAL_Z_BITS = 4;
    private static final int Y_BITS = 8;

    // 19 bits
    private static final int IDX_BITS = FACE_BITS + LOCAL_X_BITS + LOCAL_Z_BITS + Y_BITS;
    private static final int KEY_MASK = (1 << IDX_BITS) - 1;

    // Remaining 13 bits
    private static final int PALETTE_BITS = 32 - IDX_BITS;
    private static final int MAX_PALETTE_SIZE = 1 << PALETTE_BITS;

    private static final int EMPTY = 0;
    private static final int TOMBSTONE = -1;

    // 0 -> 4 -> double every time
    private static final int MIN_TABLE_SIZE = 4;

    // Grow/rebuild at 2/3 capacity, including tombstones
    private static final int LOAD_NUMERATOR = 2;
    private static final int LOAD_DENOMINATOR = 3;

    // Shrink at 1/4 capacity
    private static final int SHRINK_NUMERATOR = 1;
    private static final int SHRINK_DENOMINATOR = 4;

    // An open-addressing hash table
    // An entry is (value << IDX_BITS | key).
    // Value is the index into the texture palette.
    // Key is the local index, containing the face number, local x and z coordinates and the global y coordinate, +1; or EMPTY or TOMBSTONE.
    private int[] hashtable;

    // Number of live entries in the hash table.
    private int size;

    // Number of tombstones in the hash table.
    private int tombstones;

    // Palette. Entries [0, paletteSize) are meaningful.
    private PaletteEntry[] palette;

    private int paletteSize;

    public WallpaperTable() {
        hashtable = null;
        size = 0;
        tombstones = 0;

        palette = null;
        paletteSize = 0;
    }

    public WallpaperTable(WallpaperTable other) {
        this.hashtable = other.hashtable != null
            ? other.hashtable.clone()
            : null;

        this.size = other.size;
        this.tombstones = other.tombstones;

        if (other.palette != null) {
            this.palette = new PaletteEntry[other.palette.length];

            for (int i = 0; i < other.paletteSize; ++i) {
                PaletteEntry entry = other.palette[i];
                this.palette[i] = entry != null
                    ? new PaletteEntry(entry)
                    : null;
            }
        } else {
            this.palette = null;
        }

        this.paletteSize = other.paletteSize;
    }

    // Note: this is called in the rendering pipeline. Should be fast and avoid exceptions.
    // Returns null if there is no wallpaper on the face.
    public IIcon getWallpaper(int localX, int y, int localZ, int face) {
        if (hashtable == null) {
            return null;
        }
        
        int key = packIndex(localX, y, localZ, face);

        int result = findSlot(hashtable, key);
        if (result < 0) {
            return null;
        }

        int entry = hashtable[result];
        int paletteIndex = entry >>> IDX_BITS;

        PaletteEntry paletteEntry = palette[paletteIndex];
        return paletteEntry != null ? paletteEntry.icon : null;
    }

    // Note: this is called relatively infrequently, so the performance doesn't matter as much.
    // Returns true if the target face changed.
    public boolean setWallpaper(
        int localX,
        int y,
        int localZ,
        int face,
        Block sourceBlock,
        int sourceMeta,
        int sourceSide
    ) {
        if (sourceBlock == null) {
            throw new IllegalArgumentException("sourceBlock == null");
        }

        int key = packIndex(localX, y, localZ, face);
        
        int slotResult = findSlot(hashtable, key);

        int oldPaletteIndex = -1;

        if (slotResult >= 0) {
            int oldEntry = hashtable[slotResult];
            oldPaletteIndex = oldEntry >>> IDX_BITS;
        }

        int newPaletteIndex = findOrAllocatePalette(sourceBlock, sourceMeta, sourceSide);
        if (oldPaletteIndex == newPaletteIndex) {
            return false;
        }

        if (slotResult < 0) {
            ensureCapacityForInsert();

            slotResult = findSlot(hashtable, key);
            if (slotResult >= 0) {
                // Shouldn't happen because the key wasn't in the table pre-resize
                throw new IllegalStateException("Wallpaper hash table unexpectedly found an existing key");
            }

            slotResult = ~slotResult;
        }

        if (oldPaletteIndex >= 0) {
            --palette[oldPaletteIndex].refCount;
        }

        ++palette[newPaletteIndex].refCount;

        hashtable[slotResult] = makeEntry(key, newPaletteIndex);

        if (oldPaletteIndex < 0) {
            ++size;
        }

        return true;
    }

    // Note: this is also called relatively infrequently, so the performance doesn't matter as much.
    // Returns true if the target face changed.
    public boolean resetWallpaper(
        int localX,
        int y,
        int localZ,
        int face
    ) {
        if (hashtable == null) {
            return false;
        }

        int slot = findSlot(hashtable, packIndex(localX, y, localZ, face));

        if (slot < 0) {
            return false;
        }

        int entry = hashtable[slot];
        int paletteIndex = entry >>> IDX_BITS;

        hashtable[slot] = TOMBSTONE;

        --size;
        ++tombstones;

        --palette[paletteIndex].refCount;

        if (size == 0) {
            // Discard the whole table if empty to save memory

            hashtable = null;
            tombstones = 0;

            palette = null;
            paletteSize = 0;

            return true;
        }

        maybeShrinkOrCompact();

        return true;
    }

    // Refreshes the cached IIcon from the authoritative block + face information
    public void refreshIcons() {
        if (palette == null) {
            return;
        }

        for (int i = 0; i < paletteSize; ++i) {
            PaletteEntry entry = palette[i];

            if (entry != null) {
                entry.refreshIcon();
            }
        }
    }

    private static int packIndex(int localX, int y, int localZ, int face) {
        // Note: optimal order for cache friedliness (render loops on y -> z -> x -> face)
        // Though I guess it doesn't matter since we're hashing it anyway
        return ((y << LOCAL_Z_BITS | localZ) << LOCAL_X_BITS | localX) << FACE_BITS | face;
    }

    private static int makeEntry(int key, int paletteIndex) {
        return (paletteIndex << IDX_BITS) | (key + 1);
    }

    private static int getKey(int entry) {
        return (entry & KEY_MASK) - 1;
    }

    // Returns field index (>=0) if found; otherwise ~index (<0) of the preferred insertion slot.
    // Note: for insertion, preface with ensureCapacityForInsert().
    // Note: Will throw if the table is completely full, which should never happen.
    private static int findSlot(int[] table, int key) {
        if (table == null) {
            return -1;
        }

        int mask = table.length - 1;
        int firstSlot = mix(key) & mask;
        int slot = firstSlot;
        int firstTombstone = -1;

        while (true) {
            int entry = table[slot];

            if (entry == EMPTY) {
                break;
            }

            if (entry == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = slot;
                }
            } else if (getKey(entry) == key) {
                return slot;
            }

            slot = (slot + 1) & mask;
            if (slot == firstSlot) {
                break;
            }
        }

        if (firstTombstone >= 0) {
            return ~firstTombstone;
        }

        if (slot == firstSlot) {
            throw new IllegalStateException("Wallpaper hash table is completely full");
        }

        return ~slot;
    }

    private void ensureCapacityForInsert() {
        if (hashtable == null) {
            hashtable = new int[MIN_TABLE_SIZE];
            return;
        }

        int capacity = hashtable.length;
        int threshold = capacity * LOAD_NUMERATOR / LOAD_DENOMINATOR;
        int used = size + tombstones;

        if (used + 1 <= threshold) {
            return;
        }

        if (size + 1 <= threshold) {
            rehash(capacity);
            return;
        }

        rehash(capacity << 1);
    }

    // Note: may shring several times at once
    private void maybeShrinkOrCompact() {
        int capacity = hashtable.length;
        int newCapacity = capacity;

        while (newCapacity > MIN_TABLE_SIZE && size <= newCapacity * SHRINK_NUMERATOR / SHRINK_DENOMINATOR) {
            newCapacity >>= 1;
        }

        if (newCapacity != capacity) {
            rehash(newCapacity);
            return;
        }

        // TODO: Do I need this here?
        if (tombstones > capacity / 4) {
            rehash(capacity);
        }
    }

    // Note: expects newCapacity to be a power of 2
    private void rehash(int newCapacity) {
        int[] oldTable = hashtable;

        if (oldTable == null) {
            hashtable = new int[newCapacity];
            return;
        }

        int[] newTable = new int[newCapacity];
        int newMask = newCapacity - 1;

        for (int i = 0; i < oldTable.length; ++i) {
            int entry = oldTable[i];

            if (entry == EMPTY || entry == TOMBSTONE) {
                continue;
            }

            int key = getKey(entry);
            int slot = mix(key) & newMask;

            while (newTable[slot] != EMPTY) {
                slot = (slot + 1) & newMask;
            }

            newTable[slot] = entry;
        }

        hashtable = newTable;
        tombstones = 0;
    }

    // MurmurHash3's 32-bit finalizer for bit-shuffling
    private static int mix(int x) {
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;
        return x;
    }

    /**
     * Searches the palette for an existing matching source texture.
     *
     * We scan backwards because insertion is rare and temporal locality
     * is useful: if the player just started painting with texture X,
     * another request for X is likely to occur shortly afterward, and X
     * will usually be near the end of the palette.
     *
     * During the same scan we remember the first unused entry so it can
     * be recycled without growing the palette.
     */
    // TODO: Cache the last result and start from it?
    private int findOrAllocatePalette(Block block, int meta, int side) {
        if (palette == null) {
            palette = new PaletteEntry[MIN_TABLE_SIZE];
            return 0;
        }

        int recyclable = -1;

        for (int i = paletteSize - 1; i >= 0; --i) {
            PaletteEntry entry = palette[i];

            if (entry == null) {
                continue;
            }

            if (entry.matches(block, meta, side)) {
                return i;
            }

            if (entry.refCount == 0) {
                recyclable = i;
            }
        }

        if (recyclable >= 0) {
            palette[recyclable].set(block, meta, side);
            return recyclable;
        }

        if (paletteSize >= MAX_PALETTE_SIZE) {
            throw new IllegalStateException("Wallpaper palette exhausted (" + MAX_PALETTE_SIZE + " entries)");
        }

        int newPaletteSize = paletteSize + 1;
        int capacity = palette.length;

        if (capacity <= newPaletteSize) {
            while (capacity < newPaletteSize) {
                capacity <<= 1;
            }

            PaletteEntry[] newPalette = new PaletteEntry[capacity];
            System.arraycopy(palette, 0, newPalette, 0, paletteSize);
            palette = newPalette;
        }

        int index = paletteSize;
        paletteSize = newPaletteSize;

        palette[index] = new PaletteEntry(block, meta, side);
        return index;
    }
}
