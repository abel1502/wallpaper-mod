package com.abel.wallpaper.item;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasFaceOverride;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemScraper extends Item {
    @Override
    public boolean onItemUse(
        ItemStack stack,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z,
        int side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (world.isRemote) {
            return IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side) != null;
        }

        boolean removed = IHasFaceOverride.of(world).resetWallpaper(x, y, z, side);
        if (!removed) {
            return false;
        }

        // TODO: Sound?
        world.markBlockForUpdate(x, y, z);
        WallpaperHandler.onWallpaperUpdate(world, x, y, z);
        stack.damageItem(1, player);
        return true;
    }
}
