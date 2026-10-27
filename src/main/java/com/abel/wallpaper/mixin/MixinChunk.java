package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.WallpaperTable;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IHasWallpaperTable {
    @Shadow
    public World worldObj;

    @Unique
    public WallpaperTable wallpaperData = new WallpaperTable();

    @Override
    public WallpaperTable getWallpaperTable() {
        return this.wallpaperData;
    }

    @Override
    public void setWallpaperTable(WallpaperTable wpt) {
        this.wallpaperData = wpt;
    }

    @Inject(method="func_150807_a(IIILnet/minecraft/block/Block;I)Z", at=@At("RETURN"), remap = false)
    private void onSetBlock(int x, int y, int z, Block block, int meta, CallbackInfoReturnable<Boolean> info) {
        if (!info.getReturnValueZ()) {
            return;
        }

        // TODO: Maybe run on client-side too?
        if (worldObj.isRemote) {
            return;
        }

        if (!WallpaperHandler.supportsWallpaper(block)) {
            boolean changed = false;
            for (int face = 0; face < 6; face++) {
                changed |= this.wallpaperData.resetWallpaper(x, y, z, face);
            }

            if (changed) {
                WallpaperHandler.onWallpaperUpdate(worldObj, x, y, z);
            }
        }
    }
}
