package com.abel.wallpaper.mixin.chisel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;

import team.chisel.ctmlib.RenderBlocksCTM;

import com.abel.wallpaper.client.ctm.WallpaperCTMRenderer;

/**
 * Renders wallpapers painted onto Chisel / CTMLib connected-texture blocks, whose
 * {@link RenderBlocksCTM} otherwise ignores per-face texture overrides. Only loaded when Chisel is
 * present (see {@code LateMixins}).
 *
 * <p>Chisel's renderer translates the tessellator by the block position and draws at block-local
 * coordinates, so we look up the wallpaper at the world coordinates but draw at {@code 0,0,0}.</p>
 */
@Pseudo
@Mixin(RenderBlocksCTM.class)
public abstract class MixinRenderBlocksCTM {

    @Inject(method = "func_147764_f", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 5)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147798_e", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 4)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147806_b", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 1)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147768_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 0)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147734_d", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 3)) {
            info.cancel();
        }
    }

    @Inject(method = "func_147761_c", at = @At("HEAD"), cancellable = true, remap = false)
    private void abel$renderFaceZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo info) {
        if (WallpaperCTMRenderer.tryRenderHost((RenderBlocks)(Object)this, block, x, y, z, 0, 0, 0, 2)) {
            info.cancel();
        }
    }
}
