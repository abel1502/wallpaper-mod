package com.abel.wallpaper.crafting;

import com.abel.wallpaper.item.ModItems;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class ModRecipes {
    public static void init() {
        GameRegistry.addRecipe(
            new ItemStack(ModItems.scraper),
            "I ",
            " S",
            'I', Items.iron_ingot,
            'S', Items.stick
        );

        GameRegistry.addRecipe(
            new ItemStack(ModItems.roll),
            " P ",
            "PSP",
            " P ",
            'P', Items.paper,
            'S', Items.string
        );

        GameRegistry.addRecipe(
            new ClearNBTRecipe(ModItems.roll)
        );
    }
}
