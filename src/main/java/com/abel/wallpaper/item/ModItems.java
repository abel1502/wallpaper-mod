package com.abel.wallpaper.item;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ModItems {
    public static Item scraper;
    public static Item roll;

    public static void init() {
        scraper = new ItemScraper()
            .setUnlocalizedName("scraper")
            .setTextureName("abel_wallpaper:scraper")
            .setCreativeTab(CreativeTabs.tabTools)
            .setMaxStackSize(1)
            .setMaxDamage(1000);

        roll = new ItemRoll()
            .setUnlocalizedName("roll")
            .setTextureName("abel_wallpaper:roll")
            .setCreativeTab(CreativeTabs.tabTools)
            .setMaxStackSize(1)
            .setMaxDamage(1000);

        GameRegistry.registerItem(scraper, "scraper");
        GameRegistry.registerItem(roll, "roll");
    }
}
