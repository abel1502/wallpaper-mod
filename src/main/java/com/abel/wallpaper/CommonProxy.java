package com.abel.wallpaper;

import com.abel.wallpaper.crafting.ModRecipes;
import com.abel.wallpaper.handler.WallpaperDataManager;
import com.abel.wallpaper.item.ModItems;
import com.falsepattern.chunk.api.DataRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class CommonProxy {
    public void init(FMLInitializationEvent event) {
        ModItems.init();
        ModRecipes.init();
        DataRegistry.registerDataManager(new WallpaperDataManager(), 0);
    }
}
