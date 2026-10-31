package com.abel.wallpaper.world;

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
     * Returns the wallpaper entry for the face {@code side} of the block at {@code (x, y, z)},
     * or {@code null} if there is none (or the block access isn't supported).
     */
    public static PaletteEntry getEntry(IBlockAccess world, int x, int y, int z, int side) {
        try {
            return IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side);
        } catch (ClassCastException e) {
            // Unsupported IBlockAccess implementation (already reported by MixinRenderBlocks).
            return null;
        }
    }
}
