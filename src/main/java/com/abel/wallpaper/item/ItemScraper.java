package com.abel.wallpaper.item;

import static com.abel.wallpaper.WallpaperMod.LOG;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasFaceOverride;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

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
        LOG.debug("[ABEL] ItemScraper.onItemUse x={} y={} z={} side={}", x, y, z, side);
        world.markBlockForUpdate(x, y, z);

        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        if (chunk != null) {
            // Should always be the case, but just to be safe
            chunk.setChunkModified();
        }
        // WallpaperHandler.onWallpaperUpdate(world, x, y, z);

        stack.damageItem(1, player);
        return true;
    }
}
