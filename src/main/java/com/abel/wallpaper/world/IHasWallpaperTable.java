package com.abel.wallpaper.world;

import cpw.mods.fml.common.Optional;
import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;

import net.minecraft.world.chunk.Chunk;

public interface IHasWallpaperTable {
    WallpaperTable getWallpaperTable();

    static IHasWallpaperTable of(Chunk chunk) {
        return (IHasWallpaperTable)chunk;
    }

    @Optional.Method(modid = "angelica")
    static IHasWallpaperTable of(WorldSlice worldSlice) {
        return (IHasWallpaperTable)worldSlice;
    }
}
