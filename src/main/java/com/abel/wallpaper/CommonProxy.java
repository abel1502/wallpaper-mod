package com.abel.wallpaper;

import com.abel.wallpaper.crafting.ModRecipes;
import com.abel.wallpaper.item.ModItems;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class CommonProxy {
    public void init(FMLInitializationEvent event) {
        ModItems.init();
        ModRecipes.init();
    }
}
