package com.example.examplemod;

import net.minecraft.init.Blocks;

import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = WallpaperMod.MODID,
    version = WallpaperMod.VERSION,
    name = WallpaperMod.NAME,
    acceptedMinecraftVersions = "[1.7.10]"
)
public class WallpaperMod {
    public static final String NAME = "Abel's Wallpaper Mod";
    public static final String MODID = "abel_wallpaper";
    public static final String VERSION = "1.0";
    public static final Logger LOG = LogManager.getLogger(MODID);
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
    }
}
