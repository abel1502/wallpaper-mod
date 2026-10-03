package com.abel.wallpaper.handler;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

// TODO: Singleton?
public class WallpaperHandler {
    public static boolean supportsWallpaper(Block block) {
        // TODO: ?
        if (!block.renderAsNormalBlock()) {
            return false;
        }

        switch (block.getRenderType()) {
            case 0:   // Normal
            case 31:  // Log
            case 39:  // Quartz
                return true;
            default:
                return false;
        }
    }

    public static void onWallpaperUpdate(World world, int x, int y, int z) {
        // TODO: Null-safety, remove unnecessary calls
        world.getChunkFromBlockCoords(x, z).setChunkModified();
        world.markBlockForUpdate(x, y, z);
        // Minecraft.getMinecraft().renderGlobal.markBlockForRenderUpdate(x, y, z);
        int sectionX = x >> 4;
        int sectionY = y >> 4;
        int sectionZ = z >> 4;
        world.markBlockRangeForRenderUpdate(
            sectionX * 16, sectionY * 16, sectionZ * 16,
            sectionX * 16 + 15, sectionY * 16 + 15, sectionZ * 16 + 15
        );
    }
}
