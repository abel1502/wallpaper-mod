package com.abel.wallpaper.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.abel.wallpaper.world.IExtendedBlockAccess;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCache implements IExtendedBlockAccess {
    @Shadow
    private int chunkX;
    @Shadow
    private int chunkZ;
    @Shadow
    private Chunk[][] chunkArray;

    @Override
    @Nullable
    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        int idxX = chunkX - this.chunkX;
        int idxZ = chunkZ - this.chunkZ;

        if (idxX < 0 || idxX >= this.chunkArray.length) {
            return null;
        }

        if (idxZ < 0 || idxZ >= this.chunkArray[idxX].length) {
            return null;
        }

        return this.chunkArray[idxX][idxZ];
    }
}
