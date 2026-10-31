package com.abel.wallpaper.mixin.hbm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;

import com.hbm.render.block.ct.RenderBlocksCT;

import com.abel.wallpaper.client.ctm.WallpaperCTMRenderer;

/**
 * Renders wallpapers painted onto HBM connected-texture blocks, whose {@link RenderBlocksCT}
 * otherwise ignores per-face texture overrides. Only loaded when HBM is present
 * (see {@code LateMixins}).
 */
@Pseudo
@Mixin(RenderBlocksCT.class)
public abstract class MixinRenderBlocksCT {

    @Inject(method = "func_147764_f", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 5)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147798_e", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 4)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147806_b", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 1)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147768_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 0)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147734_d", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 3)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147761_c", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, x, y, z, 2)) {
            info.cancel();
        }
    }
}
