package com.abel.wallpaper.mixin.hbm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import com.hbm.render.block.ct.CTContext;
import com.hbm.render.block.ct.IBlockCT;

import com.abel.wallpaper.world.WallpaperCompat;

/**
 * Makes HBM's connected-texture blocks also connect to adjacent painted faces.
 *
 * <p>Surgically redirects the {@link IBlockCT#canConnect} call inside {@link CTContext#loadContext}
 * so a neighbor is treated as connected when it either connects normally or carries a wallpaper
 * matching the block being rendered. Only loaded when HBM is present (see {@code LateMixins}).</p>
 */
@Pseudo
@Mixin(CTContext.class)
public abstract class MixinCTContext {

    /**
     * @author Abel's Wallpaper Mod
     * @reason Also treat neighbors carrying a matching wallpaper as connected.
     */
    @Redirect(
        method = "loadContext",
        at = @At(value = "INVOKE", target = "Lcom/hbm/render/block/ct/IBlockCT;canConnect(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraft/block/Block;)Z"),
        remap = false
    )
    private static boolean abel$canConnect(
        IBlockCT owner,
        IBlockAccess world,
        int nx,
        int ny,
        int nz,
        Block neighbor,
        @Local(name = "i") int i,
        @Local(name = "x") int x,
        @Local(name = "y") int y,
        @Local(name = "z") int z
    ) {
        int meta = world.getBlockMetadata(x, y, z);
        return owner.canConnect(world, nx, ny, nz, neighbor)
            || WallpaperCompat.hasMatchingWallpaper(world, nx, ny, nz, i, (Block) owner, meta);
    }
}
