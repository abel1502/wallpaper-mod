package com.abel.wallpaper.mixin.chisel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import team.chisel.ctmlib.CTM;

import com.abel.wallpaper.world.WallpaperCompat;

/**
 * Makes Chisel / CTMLib connected-texture blocks also connect to adjacent painted faces.
 *
 * <p>Hooks {@link CTM#isConnected(IBlockAccess, int, int, int, ForgeDirection, Block, int)} so a
 * neighbor carrying a matching wallpaper is treated as connected. Only loaded when Chisel is
 * present (see {@code LateMixins}).</p>
 */
@Pseudo
@Mixin(CTM.class)
public abstract class MixinCTM {

    @Inject(
        method = "isConnected(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraftforge/common/util/ForgeDirection;Lnet/minecraft/block/Block;I)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void abel$isConnected(IBlockAccess world, int x, int y, int z, ForgeDirection dir, Block block, int meta, CallbackInfoReturnable<Boolean> info) {
        if (WallpaperCompat.hasMatchingWallpaper(world, x, y, z, dir.ordinal(), block, meta)) {
            info.setReturnValue(true);
        }
    }
}
