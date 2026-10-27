package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.abel.wallpaper.network.IBlockPacket;
import com.abel.wallpaper.world.PaletteEntry;

import net.minecraft.network.play.server.S23PacketBlockChange;

@Mixin(S23PacketBlockChange.class)
public abstract class MixinS23PacketBlockChange implements IBlockPacket {
    @Unique
    PaletteEntry[] faceWallpapers = new PaletteEntry[6];

    @Override
    public PaletteEntry[] abel$getFaceWallpapers() {
        return this.faceWallpapers;
    }

    // @Override
    // public void abel$setFaceWallpapers(PaletteEntry[] wallpapers) {
    //     this.faceWallpapers = wallpapers;
    // }
}
