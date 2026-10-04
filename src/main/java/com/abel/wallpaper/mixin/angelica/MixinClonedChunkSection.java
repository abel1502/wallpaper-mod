package com.abel.wallpaper.mixin.angelica;

import static com.abel.wallpaper.WallpaperMod.LOG;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;
import com.gtnewhorizons.angelica.compat.mojang.ChunkSectionPos;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@Pseudo
@Mixin(ClonedChunkSection.class)
public abstract class MixinClonedChunkSection implements IHasWallpaperTable {
    @Shadow(remap = false)
    @Final
    private World world;
    
    @Unique
    private WallpaperTable wpt = null;
    
    @Inject(method = "copyBlockEntities", at = @At("TAIL"), remap = false)
    public void copyBlockEntities(Chunk chunk, ChunkSectionPos pos, CallbackInfo info) {
        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        // LOG.warn("ClonedChunkSection copyBlockEntities wpt={}", wpt);
        if (wpt == null) {
            this.wpt = null;
            return;
        }

        // Long-term: Maybe trim the unnecessary verticals?
        this.wpt = new WallpaperTable(wpt);
    }

    @Override
    public WallpaperTable getWallpaperTable() {
        return this.wpt;
    }
}