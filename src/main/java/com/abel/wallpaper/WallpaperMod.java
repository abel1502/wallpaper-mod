package com.abel.wallpaper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = WallpaperMod.MODID,
    version = WallpaperMod.VERSION,
    name = WallpaperMod.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:unimixins;required-after:chunkapi"
)
public class WallpaperMod {
    public static final String NAME = "Abel's Wallpaper Mod";
    public static final String MODID = "abel_wallpaper";
    public static final String VERSION = "1.1";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy (clientSide="com.abel.wallpaper.client.ClientProxy", serverSide="com.abel.wallpaper.CommonProxy")
    public static CommonProxy proxy;
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
