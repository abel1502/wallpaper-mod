package com.abel.wallpaper.mixin;

import static com.abel.wallpaper.WallpaperMod.LOG;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abel.wallpaper.world.IExtendedBlockAccess;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Inject(method="getBlockIcon(Lnet/minecraft/block/Block;Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;", at=@At("HEAD"))
    private void getBlockIcon(Block block, IBlockAccess blockAccess, int x, int y, int z, int side, CallbackInfoReturnable<IIcon> info) {
        IExtendedBlockAccess extendedBlockAccess;
        try {
            extendedBlockAccess = IExtendedBlockAccess.of(blockAccess);
        } catch (ClassCastException e) {
            LOG.error("IBlockAccess " + blockAccess.getClass().getName() + " is not supported by Abel's Wallpaper mod!");
            return;
        }

        LOG.info("Hooked getBlockIcon, chunk=" + extendedBlockAccess.getChunkFromBlockCoords(x, z));
    }
}
