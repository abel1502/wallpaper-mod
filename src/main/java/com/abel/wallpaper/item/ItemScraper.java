package com.abel.wallpaper.item;

import static com.abel.wallpaper.WallpaperMod.LOG;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.WallpaperTable;

import net.minecraft.client.Minecraft;
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
        // if (world.isRemote) {
        //     return true;
        // }

        WallpaperTable wpt = IHasFaceOverride.of(world).getWallpaperTableAt(x, y, z);
        if (wpt == null) {
            return false;
        }
        // LOG.warn("Scraper wpt={} icon={}", wpt, wpt.getWallpaper(x & 15, y, z & 15, side));

        boolean removed = wpt.resetWallpaper(x & 15, y, z & 15, side);
        // LOG.warn("Scraper removed={} icon={}", removed, wpt.getWallpaper(x & 15, y, z & 15, side));
        if (!removed) {
            return false;
        }

        // TODO: Sound?
        // TODO: Mark chunk as dirty? Or I guess fire an "onWallpaperUpdate" somewhere?
        WallpaperHandler.onWallpaperUpdate(world, x, y, z);
        stack.damageItem(1, player);
        return true;
    }
}
