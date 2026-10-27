package com.abel.wallpaper.handler;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

// Long-term: Singleton?
public class WallpaperHandler {
    // Checks if the block can have a wallpaper applied to it
    public static boolean supportsWallpaper(Block block) {
        return isRenderedAsNormalCube(block);
    }

    // Checks if the block's texture can be used as a wallpaper
    public static boolean canBeWallpaper(Block block) {
        // Transparent wallpapers make an x-ray
        return block.isOpaqueCube() && isRenderedAsNormalCube(block);
    }

    private static boolean isRenderedAsNormalCube(Block block) {
        switch (block.getRenderType()) {
            case 0:   // Normal
            case 31:  // Log
            case 39:  // Quartz
                return true;
            default:
                return false;
        }
    }

    // TODO: Separate visual update (should trigger a re-render) and source-of-truth update (should propagate the change to clients by marking the block for update, and the chunk as dirty)?
    public static void onWallpaperUpdate(World world, int x, int y, int z) {
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        if (chunk != null) {
            // Should always be the case, but just to be safe
            world.getChunkFromBlockCoords(x, z).setChunkModified();
        }
        
        // world.markBlockForUpdate(x, y, z);
        // Minecraft.getMinecraft().renderGlobal.markBlockForRenderUpdate(x, y, z);
        // world.markBlockRangeForRenderUpdate(x, y, z, x + 1, y + 1, z + 1);
        int sectionX = x >> 4 << 4;
        int sectionY = y >> 4 << 4;
        int sectionZ = z >> 4 << 4;
        world.markBlockRangeForRenderUpdate(
            sectionX, sectionY, sectionZ,
            sectionX + 15, sectionY + 15, sectionZ + 15
        );
    }
}
