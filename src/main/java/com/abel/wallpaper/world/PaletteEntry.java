package com.abel.wallpaper.world;

import java.nio.ByteBuffer;

import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

public final class PaletteEntry {
    // Cached icon
    public IIcon icon;

    // Texture source of truth
    public Block block;
    public short blockMeta;
    public short blockSide;

    // Long-term: Rotation? Would need special support

    public int refCount = 0;

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

    public PaletteEntry() {
        this.icon = null;
        this.block = null;
        this.blockMeta = 0;
        this.blockSide = 0;
        this.refCount = 0;
    }

    // Note: assumes refCount == 0
    public void set(
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

    public boolean matches(
        Block block,
        int blockMeta,
        int blockSide
    ) {
        return this.block == block
            && this.blockMeta == (short)blockMeta
            && this.blockSide == (short)blockSide;
    }

    public boolean matches(PaletteEntry other) {
        return other != null
            && this.block == other.block
            && this.blockMeta == other.blockMeta
            && this.blockSide == other.blockSide;
    }

    public void refreshIcon() {
        this.icon = block.getIcon(blockSide, blockMeta);
    }

    // Note: doesn't save icon or refCount
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("block", GameRegistry.findUniqueIdentifierFor(block).toString());
        nbt.setShort("meta", blockMeta);
        nbt.setShort("side", blockSide);
    }

    // Note: refCount is always 0; restore manually if needed
    public void readFromNBT(NBTTagCompound nbt) {
        Block block = Block.getBlockFromName(nbt.getString("block"));
        int meta = nbt.getShort("meta");
        int side = nbt.getShort("side");

        if (block == null) {
            block = Blocks.stone;
        }

        this.refCount = 0;
        set(block, meta, side);
    }

    public static final int MAX_BUFFER_SIZE = 4;

    // Note: doesn't save icon or refCount
    public void writeToBuffer(ByteBuffer buffer) {
        int blockId = Block.getIdFromBlock(block);
        int packed = blockId << 7 | blockMeta << 3 | blockSide;

        buffer.putInt(packed);
    }

    // Note: refCount is always 0; restore manually if needed
    public void readFromBuffer(ByteBuffer buffer) {
        int packed = buffer.getInt();
        int blockSide = packed & 7;
        int blockMeta = packed >>> 3 & 15;
        int blockId = packed >>> 7;
        Block block = Block.getBlockById(blockId);

        if (block == null) {
            block = Blocks.stone;
        }

        this.refCount = 0;
        set(block, blockMeta, blockSide);
    }

    // Duplication because ChunkAPI uses two different kinds of buffers like a moron

    // Note: doesn't save icon or refCount
    public void writeToBuffer(ByteBuf buffer) {
        int blockId = Block.getIdFromBlock(block);
        int packed = blockId << 7 | blockMeta << 3 | blockSide;

        buffer.writeInt(packed);
    }

    // Note: refCount is always 0; restore manually if needed
    public void readFromBuffer(ByteBuf buffer) {
        int packed = buffer.readInt();
        int blockSide = packed & 7;
        int blockMeta = packed >>> 3 & 15;
        int blockId = packed >>> 7;
        Block block = Block.getBlockById(blockId);

        if (block == null) {
            block = Blocks.stone;
        }

        this.refCount = 0;
        set(block, blockMeta, blockSide);
    }
}
