package com.abel.wallpaper.client;

import com.abel.wallpaper.CommonProxy;
import com.abel.wallpaper.item.ModItems;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.MinecraftForgeClient;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        MinecraftForgeClient.registerItemRenderer(ModItems.roll, new ItemRenderRoll());
    }
}
