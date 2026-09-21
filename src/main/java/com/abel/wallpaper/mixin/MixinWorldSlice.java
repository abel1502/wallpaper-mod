package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.WallpaperTable;
import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ChunkRenderContext;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.IIcon;

@Pseudo
@Mixin(WorldSlice.class)
public abstract class MixinWorldSlice implements IHasFaceOverride {
    @Shadow(remap = false)
    @Final
    private static int SECTION_LENGTH;

    @Shadow(remap = false)
    @Final
    private static int TABLE_LENGTH;

    @Shadow(remap = false)
    @Final
    private static int TABLE_BITS;

    @Shadow(remap = false)
    @Final
    private static int SECTION_TABLE_ARRAY_SIZE;

    @Shadow(remap = false)
    @Final
    private WorldClient world;

    @Shadow(remap = false)
    private int baseX;

    @Shadow(remap = false)
    private int baseY;

    @Shadow(remap = false)
    private int baseZ;

    @Shadow(remap = false)
    public abstract boolean containsBlock(int x, int y, int z);

    // (x, z) -> table
    @Unique
    private WallpaperTable[] wptArrays = new WallpaperTable[TABLE_LENGTH * TABLE_LENGTH];

    @Inject(method = "copyData(Lcom.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ChunkRenderContext;)V", at = @At("TAIL"))
    public void copyData(ChunkRenderContext context, CallbackInfoReturnable<Void> info) {
        // TODO: Populate wptArrays!! Here or elsewhere? Probably also in ClonedChunkSection
    }

    @Override
    public IIcon getFaceOverride(int x, int y, int z, int face) {
        WallpaperTable wpt = getWPT(x, y, z);
        if (wpt == null) {
            return null;
        }

        return wpt.getWallpaper(x & 15, y, z & 15, face);
    }

    @Unique
    private static int getLocalWPTIndex(int sectionX, int sectionZ) {
        return sectionZ << TABLE_BITS | sectionX;
    }

    @Unique
    private WallpaperTable getWPT(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            return null;
        }

        int relX = x - this.baseX;
        int relZ = z - this.baseZ;

        return this.wptArrays[getLocalWPTIndex(relX >> 4, relZ >> 4)];
    }
}
