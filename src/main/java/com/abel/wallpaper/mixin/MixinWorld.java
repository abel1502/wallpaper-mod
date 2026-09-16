package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.abel.wallpaper.world.IExtendedBlockAccess;

import net.minecraft.world.World;

@Mixin(World.class)
public abstract class MixinWorld implements IExtendedBlockAccess {
}
