package com.abel.wallpaper.world;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public interface IHasFaceOverride extends IBlockAccess {
    WallpaperTable getLocalWallpaperTable(int x, int y, int z);

    void setLocalWallpaperTable(int x, int y, int z, WallpaperTable wpt);

    default IIcon getWallpaper(int x, int y, int z, int face) {
        WallpaperTable wpt = getLocalWallpaperTable(x, y, z);
        if (wpt == null) {
            return null;
        }

        return wpt.getWallpaper(x & 15, y, z & 15, face);
    }

    default PaletteEntry getWallpaperEntry(int x, int y, int z, int face) {
        WallpaperTable wpt = getLocalWallpaperTable(x, y, z);
        if (wpt == null) {
            return null;
        }

        return wpt.getWallpaperEntry(x & 15, y, z & 15, face);
    }

    default boolean setWallpaper(int x, int y, int z, int face, Block sourceBlock, int sourceMeta, int sourceSide) {
        WallpaperTable wpt = getLocalWallpaperTable(x, y, z);
        if (wpt == null) {
            wpt = new WallpaperTable();
            setLocalWallpaperTable(x, y, z, wpt);
        }

        return wpt.setWallpaper(x & 15, y, z & 15, face, sourceBlock, sourceMeta, sourceSide);
    }

    default boolean setWallpaperEntry(int x, int y, int z, int face, PaletteEntry paletteEntry) {
        WallpaperTable wpt = getLocalWallpaperTable(x, y, z);
        if (wpt == null) {
            wpt = new WallpaperTable();
            setLocalWallpaperTable(x, y, z, wpt);
        }

        return wpt.setWallpaperEntry(x & 15, y, z & 15, face, paletteEntry);
    }

    default boolean resetWallpaper(int x, int y, int z, int face) {
        WallpaperTable wpt = getLocalWallpaperTable(x, y, z);
        if (wpt == null) {
            return false;
        }

        return wpt.resetWallpaper(x & 15, y, z & 15, face);
    }

    // TODO: refreshIcons?
    // TODO: Then also call refreshIcons somewhere!

    static IHasFaceOverride of(IBlockAccess blockAccess) {
        return (IHasFaceOverride)blockAccess;
    }
}
