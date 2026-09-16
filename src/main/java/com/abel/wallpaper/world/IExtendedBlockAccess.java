package com.abel.wallpaper.world;

import javax.annotation.Nullable;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;

public interface IExtendedBlockAccess extends IBlockAccess {
    @Nullable
    Chunk getChunkFromChunkCoords(int chunkX, int chunkZ);

    @Nullable
    default Chunk getChunkFromBlockCoords(int blockX, int blockZ) {
        return getChunkFromChunkCoords(blockX >> 4, blockZ >> 4);
    }

    static IExtendedBlockAccess of(IBlockAccess blockAccess) {
        return (IExtendedBlockAccess)blockAccess;
    }
}
