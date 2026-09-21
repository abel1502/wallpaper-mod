package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;

import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@Mixin(World.class)
public abstract class MixinWorld implements IHasFaceOverride {
    @Shadow
    public abstract Chunk getChunkFromBlockCoords(int x, int z);

    @Override
    public IIcon getFaceOverride(int x, int y, int z, int face) {
        Chunk chunk = this.getChunkFromBlockCoords(x, z);
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
