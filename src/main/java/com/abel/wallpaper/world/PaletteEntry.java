package com.abel.wallpaper.world;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

public final class PaletteEntry {
    // Cached icon
    IIcon icon;

    // Texture source of truth
    Block block;
    short blockMeta;
    short blockSide;

    // TODO: Rotation? Would need special support

    int refCount = 0;

    public PaletteEntry(
        Block block,
        int blockMeta,
        int blockSide
    ) {
        set(block, blockMeta, blockSide);
    }

    public PaletteEntry(PaletteEntry other) {
        this.icon = other.icon;
        this.block = other.block;
        this.blockMeta = other.blockMeta;
        this.blockSide = other.blockSide;
        this.refCount = other.refCount;
    }

    // Note: assumes refCount == 0
    void set(
        Block block,
        int blockMeta,
        int blockSide
    ) {
        if (this.refCount != 0) {
            throw new IllegalStateException("Cannot mutate a referenced PaletteEntry");
        }

        this.block = block;
        this.blockMeta = (short)blockMeta;
        this.blockSide = (short)blockSide;

        this.icon = block.getIcon(blockSide, blockMeta);
    }

    boolean matches(
        Block block,
        int blockMeta,
        int blockSide
    ) {
        return this.block == block
            && this.blockMeta == (short)blockMeta
            && this.blockSide == (short)blockSide;
    }

    void refreshIcon() {
        this.icon = block.getIcon(blockSide, blockMeta);
    }
}
