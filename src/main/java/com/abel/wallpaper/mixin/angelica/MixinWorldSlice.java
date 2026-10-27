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

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;
import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ChunkRenderContext;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.multiplayer.WorldClient;

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

    @Inject(
        method = "copyData",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/rendering/celeritas/world/WorldSlice;unpackBlockData([Lnet/minecraft/block/Block;[ILcom/gtnewhorizons/angelica/rendering/celeritas/world/cloned/ClonedChunkSection;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)V"
        ),
        remap = false
    )
    private void copyData_inner(
        ChunkRenderContext context,
        CallbackInfo ci,
        @Local(name = "x") int x,
        @Local(name = "y") int y,
        @Local(name = "z") int z,
        @Local(name = "idx") int idx,
        @Local ClonedChunkSection section
    ) {
        if (y != 0) {
            return;
        }

        this.wptArrays[getLocalWPTIndex(x, z)] = IHasWallpaperTable.of(section).getWallpaperTable();
    }

    @Override
    public WallpaperTable getLocalWallpaperTable(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            return null;
        }

        int relX = x - this.baseX;
        int relZ = z - this.baseZ;

        return this.wptArrays[getLocalWPTIndex(relX >> 4, relZ >> 4)];
    }

    @Unique
    private static int getLocalWPTIndex(int sectionX, int sectionZ) {
        return sectionZ << TABLE_BITS | sectionX;
    }

    @Override
    public void setLocalWallpaperTable(int x, int y, int z, WallpaperTable wpt) {
        LOG.error("[ABEL] MixinWorldSlice.setLocalWallpaperTable called unexpectedly");
        throw new UnsupportedOperationException("MixinWorldSlice.setLocalWallpaperTable");
    }
}
