package com.abel.wallpaper.world;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/**
 * A lookup table of wallpapers for a chunk (16x256x16).
 */
public class WallpaperTable {
    public IIcon getWallpaper(int localX, int y, int localZ, int face) {
        // TODO: Temp placeholder, remove!
        if (localX % 4 == 0 & localZ % 4 == 0) {
            return Blocks.tnt.getIcon(0, face);
        }

        return null;

        // // Note: optimal order for cache friedliness (render loops on y -> z -> x -> face)
        // // Should fit in 19 bits
        // // TODO: constants?
        // int idx = y << 11 | localZ << 7 | localX << 3 | face;
        // // TODO
        // return null;
    }
}
