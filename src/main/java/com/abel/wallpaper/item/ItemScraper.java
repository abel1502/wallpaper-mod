package com.abel.wallpaper.item;

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.WallpaperTable;

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
        // TODO: Do I need this check?
        if (world.isRemote) {
            return true;
        }

        WallpaperTable wpt = IHasFaceOverride.of(world).getWallpaperTableAt(x, y, z);
        if (wpt == null) {
            return true;
        }

        // TODO: Also get chunk and check the block?

        boolean removed = wpt.resetWallpaper(x, y, z, side);
        if (removed) {
            // TODO: Sound?
            // TODO: Mark chunk as dirty? Or I guess fire an "onWallpaperUpdate" somewhere?
            stack.damageItem(1, player);
        }

        return true;
    }
}
