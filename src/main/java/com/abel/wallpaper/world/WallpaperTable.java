package com.abel.wallpaper.world;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;

// A sparse lookup table of wallpapers for a chunk (16x256x16).
// The maximum number of distinct palette entries is 2^13 = 8192.
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

    // Higher than the actual max number of faces, used for max packet size
    private static final int MAX_HASHTABLE_ENTRIES = 1 << IDX_BITS;

    // An open-addressing hash table
    // An entry is (value << IDX_BITS | key).
    // Value is the index into the texture palette.
    // Key is the local index (containing the face number, local x and z coordinates and the global y coordinate) +1; or EMPTY or TOMBSTONE.
    private int[] hashtable;

    // Number of live entries in the hash table.
    private int size;

    // Number of tombstones in the hash table.
    private int tombstones;

    // Palette. Entries [0, paletteSize) are meaningful.
    private PaletteEntry[] palette;

    private int paletteSize;

    public WallpaperTable() {
        clear();
    }

    public WallpaperTable(WallpaperTable other) {
        this.hashtable = other.hashtable != null ? other.hashtable.clone() : null;

        this.size = other.size;
        this.tombstones = other.tombstones;

        if (other.palette != null) {
            this.palette = new PaletteEntry[other.palette.length];

            for (int i = 0; i < other.paletteSize; ++i) {
                PaletteEntry entry = other.palette[i];
                this.palette[i] = entry != null ? new PaletteEntry(entry) : null;
            }
        } else {
            this.palette = null;
        }

        this.paletteSize = other.paletteSize;
    }

    public void clear() {
        hashtable = null;
        size = 0;
        tombstones = 0;

        palette = null;
        paletteSize = 0;
    }

    public boolean empty() {
        return size == 0;
    }

    // Note: this is called in the rendering pipeline. Should be fast and avoid exceptions.
    // Returns null if there is no wallpaper on the face.
    public IIcon getWallpaper(int localX, int y, int localZ, int face) {
        PaletteEntry paletteEntry = getWallpaperEntry(localX, y, localZ, face);
        return paletteEntry != null ? paletteEntry.icon : null;
    }

    // Note: returns a reference, not a copy. Can be mutated at any time
    public PaletteEntry getWallpaperEntry(int localX, int y, int localZ, int face) {
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

        return palette[paletteIndex];
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

    public boolean setWallpaperEntry(
        int localX,
        int y,
        int localZ,
        int face,
        PaletteEntry paletteEntry
    ) {
        if (paletteEntry == null) {
            return resetWallpaper(localX, y, localZ, face);
        }
        return setWallpaper(localX, y, localZ, face, paletteEntry.block, paletteEntry.blockMeta, paletteEntry.blockSide);
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
        return (((y & 0xFF) << LOCAL_Z_BITS | localZ) << LOCAL_X_BITS | localX) << FACE_BITS | face;
    }

    private static int makeEntry(int key, int paletteIndex) {
        // Note: key + 1 is always okay because `face` (the lowest 3 bits of the key) is at most 5, while the bits can hold up to 7 before overflowing
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

        if (slot == firstSlot && table[slot] != EMPTY) {
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

    // Note: may shrink several times at once
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

    // Long-term: Cache the last result and start from it?
    private int findOrAllocatePalette(Block block, int meta, int side) {
        if (palette == null) {
            palette = new PaletteEntry[MIN_TABLE_SIZE];
            palette[0] = new PaletteEntry(block, meta, side);
            paletteSize = 1;
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

    private class Serialized {
        public int[] faces;
        public PaletteEntry[] palette;

        public Serialized(int[] faces, PaletteEntry[] palette) {
            this.faces = faces;
            this.palette = palette;
        }
    }

    private Serialized serialize() {
        if (size == 0 || paletteSize == 0) {
            return new Serialized(null, null);
        }

        int[] faces = new int[size];
        for (int i = 0, j = 0; i < hashtable.length; ++i) {
            int entry = hashtable[i];
            if (entry != EMPTY && entry != TOMBSTONE) {
                faces[j++] = entry;
            }
        }

        PaletteEntry[] outPalette = new PaletteEntry[this.paletteSize];
        for (int i = 0; i < paletteSize; ++i) {
            outPalette[i] = new PaletteEntry(palette[i]);
        }

        return new Serialized(faces, outPalette);
    }

    private void deserialize(Serialized serialized) {
        if (serialized.faces == null || serialized.palette == null) {
            clear();
            return;
        }

        int size = serialized.faces.length;
        int paletteSize = serialized.palette.length;

        if (paletteSize >= MAX_PALETTE_SIZE) {
            throw new IllegalStateException("Serialized wallpaper palette exceeds maximum size");
        }

        int capacity = MIN_TABLE_SIZE;
        while (capacity * LOAD_NUMERATOR / LOAD_DENOMINATOR < size) {
            capacity <<= 1;
        }

        int paletteCapacity = MIN_TABLE_SIZE;
        while (paletteCapacity < paletteSize) {
            paletteCapacity <<= 1;
        }

        int[] hashtable = new int[capacity];
        for (int i = 0; i < size; ++i) {
            int entry = serialized.faces[i];
            if (entry == EMPTY || entry == TOMBSTONE) {
                throw new IllegalStateException("Unexpected special entry: " + entry);
            }

            int key = getKey(entry);
            if (key < 0) {
                throw new IllegalStateException("Unexpected entry with invalid key: " + entry);
            }

            int slot = findSlot(hashtable, key);
            if (slot >= 0) {
                throw new IllegalStateException("Unexpected entry with duplicate key: " + entry);
            }
            slot = ~slot;
            hashtable[slot] = entry;
        }

        PaletteEntry[] palette = new PaletteEntry[paletteCapacity];
        for (int i = 0; i < paletteSize; ++i) {
            palette[i] = new PaletteEntry(serialized.palette[i]);
        }

        // Restore refCounts
        for (int i = 0; i < size; ++i) {
            int entry = serialized.faces[i];
            int paletteIndex = entry >>> IDX_BITS;

            if (paletteIndex < 0 || paletteIndex >= paletteSize) {
                throw new IllegalStateException("Unexpected entry with invalid palette index: " + entry);
            }

            ++palette[paletteIndex].refCount;
        }

        this.hashtable = hashtable;
        this.size = size;
        this.tombstones = 0;
        this.palette = palette;
        this.paletteSize = paletteSize;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        if (size == 0 || paletteSize == 0) {
            // Shortcut to save a heap allocation in trivial cases
            nbt.removeTag("faces");
            nbt.removeTag("palette");
            return;
        }

        Serialized serialized = serialize();

        if (serialized.faces == null) {
            nbt.removeTag("faces");
        } else {
            nbt.setIntArray("faces", serialized.faces);
        }

        if (serialized.palette == null) {
            nbt.removeTag("palette");
        } else {
            NBTTagList nbtPalette = new NBTTagList();

            for (int i = 0; i < serialized.palette.length; ++i) {
                PaletteEntry entry = serialized.palette[i];
                NBTTagCompound nbtPaletteEntry = new NBTTagCompound();
                entry.writeToNBT(nbtPaletteEntry);
                
                nbtPalette.appendTag(nbtPaletteEntry);
            }

            nbt.setTag("palette", nbtPalette);
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt == null) {
            // Shortcut to save a heap allocation in trivial cases
            clear();
            return;
        }

        Serialized serialized = new Serialized(null, null);

        if (nbt.hasKey("faces")) {
            serialized.faces = nbt.getIntArray("faces");
        }

        if (nbt.hasKey("palette")) {
            NBTTagList nbtPalette = nbt.getTagList("palette", 10);
            serialized.palette = new PaletteEntry[nbtPalette.tagCount()];

            for (int i = 0; i < serialized.palette.length; ++i) {
                NBTTagCompound nbtPaletteEntry = nbtPalette.getCompoundTagAt(i);
                PaletteEntry paletteEntry = new PaletteEntry();
                paletteEntry.readFromNBT(nbtPaletteEntry);
                serialized.palette[i] = paletteEntry;
            }
        }

        deserialize(serialized);
    }

    public static final int MAX_BUFFER_SIZE = 4 + MAX_HASHTABLE_ENTRIES * 4 + 4 + MAX_PALETTE_SIZE * PaletteEntry.MAX_BUFFER_SIZE;

    public void writeToBuffer(ByteBuffer buf) {
        Serialized serialized = serialize();

        if (serialized.faces == null) {
            buf.putInt(0);
        } else {
            buf.putInt(serialized.faces.length);
            for (int i = 0; i < serialized.faces.length; ++i) {
                buf.putInt(serialized.faces[i]);
            }
        }

        if (serialized.palette == null) {
            buf.putInt(0);
        } else {
            buf.putInt(serialized.palette.length);
            for (int i = 0; i < serialized.palette.length; ++i) {
                serialized.palette[i].writeToBuffer(buf);
            }
        }
    }

    public void readFromBuffer(ByteBuffer buf) {
        Serialized serialized = new Serialized(null, null);

        int facesSize = buf.getInt();
        if (facesSize > 0) {
            serialized.faces = new int[facesSize];
            for (int i = 0; i < facesSize; ++i) {
                serialized.faces[i] = buf.getInt();
            }
        }

        int paletteSize = buf.getInt();
        if (paletteSize > 0) {
            serialized.palette = new PaletteEntry[paletteSize];
            for (int i = 0; i < paletteSize; ++i) {
                PaletteEntry paletteEntry = new PaletteEntry();
                paletteEntry.readFromBuffer(buf);
                serialized.palette[i] = paletteEntry;
            }
        }

        deserialize(serialized);
    }

    public void writeToBuffer(ByteBuf buf) {
        Serialized serialized = serialize();

        if (serialized.faces == null) {
            buf.writeInt(0);
        } else {
            buf.writeInt(serialized.faces.length);
            for (int i = 0; i < serialized.faces.length; ++i) {
                buf.writeInt(serialized.faces[i]);
            }
        }

        if (serialized.palette == null) {
            buf.writeInt(0);
        } else {
            buf.writeInt(serialized.palette.length);
            for (int i = 0; i < serialized.palette.length; ++i) {
                serialized.palette[i].writeToBuffer(buf);
            }
        }
    }

    // Duplication because ChunkAPI uses two different kinds of buffers like a moron

    public void readFromBuffer(ByteBuf buf) {
        Serialized serialized = new Serialized(null, null);

        int facesSize = buf.readInt();
        if (facesSize > 0) {
            serialized.faces = new int[facesSize];
            for (int i = 0; i < facesSize; ++i) {
                serialized.faces[i] = buf.readInt();
            }
        }

        int paletteSize = buf.readInt();
        if (paletteSize > 0) {
            serialized.palette = new PaletteEntry[paletteSize];
            for (int i = 0; i < paletteSize; ++i) {
                PaletteEntry paletteEntry = new PaletteEntry();
                paletteEntry.readFromBuffer(buf);
                serialized.palette[i] = paletteEntry;
            }
        }

        deserialize(serialized);
    }
}
