package com.abel.wallpaper.world;

import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public interface IHasFaceOverride extends IBlockAccess {
    WallpaperTable getWallpaperTableAt(int x, int y, int z);

    default IIcon getFaceOverride(int x, int y, int z, int face) {
        WallpaperTable wpt = getWallpaperTableAt(x, y, z);
        if (wpt == null) {
            return null;
        }

        return wpt.getWallpaper(x & 15, y, z & 15, face);
    }

    static IHasFaceOverride of(IBlockAccess blockAccess) {
        return (IHasFaceOverride)blockAccess;
    }
}
