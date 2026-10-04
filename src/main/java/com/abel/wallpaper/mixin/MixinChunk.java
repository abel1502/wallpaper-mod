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

    @Inject(method="func_150807_a(IIILnet/minecraft/block/Block;I)Z", at=@At("RETURN"), remap = false)
    private void onSetBlock(int x, int y, int z, Block block, int meta, CallbackInfoReturnable<Boolean> info) {
        if (!info.getReturnValueZ()) {
            return;
        }

        // TODO: Maybe only if !world.isRemote?

        if (!WallpaperHandler.supportsWallpaper(block)) {
            for (int face = 0; face < 6; face++) {
                this.wallpaperData.resetWallpaper(x, y, z, face);
                WallpaperHandler.onWallpaperUpdate(worldObj, x, y, z);
            }
        }
    }
}
