package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;

import net.minecraft.util.IIcon;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCache implements IHasFaceOverride {
    @Shadow
    private int chunkX;
    @Shadow
    private int chunkZ;
    @Shadow
    private Chunk[][] chunkArray;

    @Override
    public IIcon getFaceOverride(int x, int y, int z, int face) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int idxX = chunkX - this.chunkX;
        int idxZ = chunkZ - this.chunkZ;

        if (idxX < 0 || idxX >= this.chunkArray.length) {
            return null;
        }
        if (idxZ < 0 || idxZ >= this.chunkArray[idxX].length) {
            return null;
        }

        Chunk chunk = chunkArray[idxX][idxZ];
        if (chunk == null) {
            return null;
        }

        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            return null;
        }

        return wpt.getWallpaper(x & 15, y, z & 15, face);
    }
}
