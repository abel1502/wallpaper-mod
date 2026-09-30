package com.abel.wallpaper.world;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

public final class PaletteEntry {
    IIcon icon;

    Block block;
    short blockMeta;
    short blockSide;

    // TODO: Rotation? Would need special support

    int refCount;
}
