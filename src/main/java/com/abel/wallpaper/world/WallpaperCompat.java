package com.abel.wallpaper.world;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

/**
 * Small shared helper for bridging wallpapers with third-party connected-texture renderers.
 *
 * <p>Kept in the common (side-independent) package so it can be referenced from mixins targeting
 * HBM's {@code CTContext} and Chisel's {@code CTM} without pulling in client-only code.</p>
 */
public final class WallpaperCompat {

    private WallpaperCompat() {
    }

    /**
     * Returns {@code true} if the face {@code side} of the block at {@code (x, y, z)} carries a
     * wallpaper whose source block and metadata match the given {@code block}/{@code meta}.
     */
    public static boolean hasMatchingWallpaper(IBlockAccess world, int x, int y, int z, int side, Block block, int meta) {
        try {
            PaletteEntry entry = IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side);
            return entry != null && entry.block == block && entry.blockMeta == (short) meta;
        } catch (ClassCastException e) {
            // Unsupported IBlockAccess implementation (already reported by MixinRenderBlocks).
            return false;
        }
    }
}
