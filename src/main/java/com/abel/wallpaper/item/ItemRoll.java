package com.abel.wallpaper.item;

import static com.abel.wallpaper.WallpaperMod.LOG;

import java.util.List;

import com.abel.wallpaper.handler.WallpaperHandler;
import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.PaletteEntry;
import com.abel.wallpaper.world.WallpaperTable;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

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
        // TODO: Do I need this check?
        // if (world.isRemote) {
        //     return true;
        // }

        Block block = world.getBlock(x, y, z);
        if (block == null) {
            return false;
        }

        PaletteEntry paletteEntry = getPalette(stack);
        if (paletteEntry == null) {
            if (!WallpaperHandler.canBeWallpaper(block)) {
                return false;
            }

            WallpaperTable wpt = IHasFaceOverride.of(world).getWallpaperTableAt(x, y, z);
            if (wpt != null) {
                paletteEntry = wpt.getWallpaperEntry(x & 15, y, z & 15, side);  // Long-term: wrappers that do & 15 in IHasFaceOverride for all relevant methods
                if (paletteEntry != null) {
                    setPalette(stack, new PaletteEntry(paletteEntry));
                    return true;
                }
            }

            int meta = world.getBlockMetadata(x, y, z);

            paletteEntry = new PaletteEntry(block, meta, side);
            setPalette(stack, paletteEntry);
            // TODO: ?
            return true;
        }

        WallpaperTable wpt = IHasFaceOverride.of(world).getWallpaperTableAt(x, y, z);
        if (wpt == null) {
            return false;
        }
        // LOG.warn("Roll wpt={} icon={}", wpt, wpt.getWallpaper(x & 15, y, z & 15, side));

        if (!WallpaperHandler.supportsWallpaper(block)) {
            return false;
        }
        // LOG.warn("Roll supports wallpaper");

        boolean changed = wpt.setWallpaperEntry(x & 15, y, z & 15, side, paletteEntry);
        // LOG.warn("Roll changed={} icon={}", changed, wpt.getWallpaper(x & 15, y, z & 15, side));
        if (!changed) {
            return false;
        }

        // TODO: Sound?
        // TODO: Mark chunk as dirty? Or I guess fire an "onWallpaperUpdate" somewhere?
        WallpaperHandler.onWallpaperUpdate(world, x, y, z);
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

        if (
            tag == null ||
            !tag.hasKey("paletteBlock") ||
            !tag.hasKey("paletteMeta") ||
            !tag.hasKey("paletteSide")
        ) {
            return null;
        }

        String blockName = tag.getString("paletteBlock");
        int blockMeta = tag.getInteger("paletteMeta");
        int side = tag.getInteger("paletteSide");

        Block block = Block.getBlockFromName(blockName);
        if (block == null) {
            return null;
        }

        return new PaletteEntry(block, blockMeta, side);
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
        if (tag == null) tag = new NBTTagCompound();
        tag.setString("paletteBlock", GameRegistry.findUniqueIdentifierFor(paletteEntry.block).toString());
        tag.setInteger("paletteMeta", paletteEntry.blockMeta);
        tag.setInteger("paletteSide", paletteEntry.blockSide);
        stack.setTagCompound(tag);
    }

    public static void resetPalette(ItemStack stack) {
        if (stack == null || stack.getItem() != ModItems.roll) {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        tag.removeTag("paletteBlock");
        tag.removeTag("paletteMeta");
        tag.removeTag("paletteSide");
        stack.setTagCompound(tag);
    }
}
