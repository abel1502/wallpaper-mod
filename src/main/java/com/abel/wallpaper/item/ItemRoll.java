package com.abel.wallpaper.item;

import static com.abel.wallpaper.WallpaperMod.LOG;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.PaletteEntry;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

// TODO: Two different base icons -- white for blank, colored for imprinted
public class ItemRoll extends Item {
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
        Block block = world.getBlock(x, y, z);
        if (block == null) {
            return false;
        }

        PaletteEntry paletteEntry = getPalette(stack);
        if (paletteEntry == null) {
            if (!WallpaperHandler.canBeWallpaper(block)) {
                return false;
            }

            if (world.isRemote) {
                return true;
            }

            paletteEntry = IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side);
            if (paletteEntry != null) {
                setPalette(stack, new PaletteEntry(paletteEntry));
                return true;
            }

            int meta = world.getBlockMetadata(x, y, z);

            paletteEntry = new PaletteEntry(block, meta, side);
            setPalette(stack, paletteEntry);
            return true;
        }

        if (!WallpaperHandler.supportsWallpaper(block)) {
            return false;
        }

        if (world.isRemote) {
            return !paletteEntry.matches(IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side));
        }

        boolean changed = IHasFaceOverride.of(world).setWallpaperEntry(x, y, z, side, paletteEntry);
        if (!changed) {
            return false;
        }

        // TODO: Sound?
        LOG.debug("[ABEL] ItemRoll.onItemUse x={} y={} z={} side={}", x, y, z, side);
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

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        PaletteEntry paletteEntry = getPalette(stack);
        if (paletteEntry != null) {
            return super.getItemStackDisplayName(stack) + " (" + paletteEntry.block.getLocalizedName() + ")";
        }
        return super.getItemStackDisplayName(stack);
    }

    public static boolean hasPalette(ItemStack stack) {
        return getPalette(stack) != null;
    }

    public static PaletteEntry getPalette(ItemStack stack) {
        if (stack == null || stack.getItem() != ModItems.roll) {
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("palette", 10)) {
            return null;
        }

        tag = tag.getCompoundTag("palette");

        PaletteEntry paletteEntry = new PaletteEntry();
        paletteEntry.readFromNBT(tag);
        return paletteEntry;
    }

    public static void setPalette(ItemStack stack, PaletteEntry paletteEntry) {
        if (stack == null || stack.getItem() != ModItems.roll) {
            return;
        }

        if (paletteEntry == null) {
            resetPalette(stack);
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        NBTTagCompound paletteTag = new NBTTagCompound();
        paletteEntry.writeToNBT(paletteTag);
        tag.setTag("palette", paletteTag);

        stack.setTagCompound(tag);
    }

    public static void resetPalette(ItemStack stack) {
        if (stack == null || stack.getItem() != ModItems.roll) {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        tag.removeTag("palette");
        stack.setTagCompound(tag);
    }
}
