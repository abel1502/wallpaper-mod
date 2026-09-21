package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;

import net.minecraft.world.chunk.Chunk;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IHasWallpaperTable {
    @Unique
    public WallpaperTable wallpaperData = new WallpaperTable();

    @Override
    public WallpaperTable getWallpaperTable() {
        return this.wallpaperData;
    }
}
