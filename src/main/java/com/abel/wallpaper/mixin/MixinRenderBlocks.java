package com.abel.wallpaper.mixin;

import static com.abel.wallpaper.WallpaperMod.LOG;

import java.util.HashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abel.wallpaper.client.ctm.WallpaperCTMRenderer;
import com.abel.wallpaper.world.IHasFaceOverride;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Unique
    private static HashSet<String> reportedBAClasses = new HashSet<>();

    @Shadow
    public abstract boolean hasOverrideBlockTexture();

    @Inject(method="getBlockIcon(Lnet/minecraft/block/Block;Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;", at=@At("HEAD"), cancellable = true)
    private void getBlockIcon(Block block, IBlockAccess blockAccess, int x, int y, int z, int side, CallbackInfoReturnable<IIcon> info) {
        if (hasOverrideBlockTexture()) {
            // Don't apply wallpapers over explicit texture overrides
            return;
        }

        IHasFaceOverride worldish;
        try {
            worldish = IHasFaceOverride.of(blockAccess);
        } catch (ClassCastException e) {
            String className = blockAccess.getClass().getName();
            if (!reportedBAClasses.contains(className)) {
                LOG.error("IBlockAccess " + blockAccess.getClass().getName() + " is not supported by Abel's Wallpaper mod!");
                reportedBAClasses.add(className);
            }
            return;
        }

        IIcon iconOverride = worldish.getWallpaper(x, y, z, side);

        if (iconOverride != null) {
            info.setReturnValue(iconOverride);
        }
    }

    @Inject(method = "renderFaceYNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 0)) {
            info.cancel();
        }
    }

    @Inject(method = "renderFaceYPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 1)) {
            info.cancel();
        }
    }

    @Inject(method = "renderFaceZNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 2)) {
            info.cancel();
        }
    }

    @Inject(method = "renderFaceZPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 3)) {
            info.cancel();
        }
    }

    @Inject(method = "renderFaceXNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 4)) {
            info.cancel();
        }
    }

    @Inject(method = "renderFaceXPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"), cancellable = true)
    private void abel$renderFaceXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRender((RenderBlocks)(Object)this, block, x, y, z, 5)) {
            info.cancel();
        }
    }
}
