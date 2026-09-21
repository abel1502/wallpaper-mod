package com.abel.wallpaper.world;

import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public interface IHasFaceOverride extends IBlockAccess {
    IIcon getFaceOverride(int x, int y, int z, int face);

    static IHasFaceOverride of(IBlockAccess blockAccess) {
        return (IHasFaceOverride)blockAccess;
    }
}
