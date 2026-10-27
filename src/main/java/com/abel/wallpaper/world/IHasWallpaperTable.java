package com.abel.wallpaper.world;

import cpw.mods.fml.common.Optional;
import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;

import net.minecraft.world.chunk.Chunk;

public interface IHasWallpaperTable {
    WallpaperTable getWallpaperTable();

    void setWallpaperTable(WallpaperTable wpt);

    static IHasWallpaperTable of(Chunk chunk) {
        return (IHasWallpaperTable)chunk;
    }

    @Optional.Method(modid = "angelica")
    static IHasWallpaperTable of(WorldSlice worldSlice) {
        return (IHasWallpaperTable)worldSlice;
    }

    @Optional.Method(modid = "angelica")
    static IHasWallpaperTable of(ClonedChunkSection section) {
        return (IHasWallpaperTable)section;
    }
}
