package com.abel.wallpaper.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class ClearNBTRecipe extends ShapelessOreRecipe {
    public ClearNBTRecipe(Item item) {
        super(new ItemStack(item), new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        int size = inv.getSizeInventory();
        for (int i = 0; i < size; ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null) {
                ItemStack result = stack.copy();
                result.setTagCompound(null);;
                return result;
            }
        }

        return null;
    }
}
