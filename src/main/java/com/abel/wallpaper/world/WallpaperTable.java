package com.abel.wallpaper.world;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/**
 * A lookup table of wallpapers for a chunk (16x256x16).
 */
public class WallpaperTable {
    private static final int FACE_BITS = 3;
    private static final int LOCAL_X_BITS = 4;
    private static final int LOCAL_Z_BITS = 4;
    private static final int Y_BITS = 8;
    private static final int IDX_BITS = FACE_BITS + LOCAL_X_BITS + LOCAL_Z_BITS + Y_BITS;

    // An open-addressing hash table
    // An entry is (value << IDX_BITS | key).
    // Value is the index into the texture palette.
    // Key is the local index, containing the face number, local x and z coordinates and the global y coordinate.
    private int[] hashtable;

    // TODO: Palette.
    // - Palette entries should have a ref-counter. An unused entry will be replace on next insert.
    private PaletteEntry[] palette;

    public WallpaperTable() {
        this.hashtable = null;
        this.palette = null;
    }

    public WallpaperTable(WallpaperTable other) {
        this.hashtable = other.hashtable.clone();
        this.palette = other.palette.clone();
    }

    public IIcon getWallpaper(int localX, int y, int localZ, int face) {
        int idx = packIndex(localX, y, localZ, face);
        // TODO
        return null;
    }

    private int packIndex(int localX, int y, int localZ, int face) {
        // Note: optimal order for cache friedliness (render loops on y -> z -> x -> face)
        // Though I guess it doesn't matter since we're hashing it anyway
        return ((y << LOCAL_Z_BITS | localZ) << LOCAL_X_BITS | localX) << FACE_BITS | face;
    }
}
