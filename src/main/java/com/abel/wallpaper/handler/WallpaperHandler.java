package com.abel.wallpaper.handler;

import static com.abel.wallpaper.WallpaperMod.LOG;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.hbm.render.block.ct.IBlockCT;
import team.chisel.ctmlib.ICTMBlock;

// Long-term: Singleton?
public class WallpaperHandler {
    // Checks if the block can have a wallpaper applied to it
    public static boolean supportsWallpaper(Block block) {
        return isRenderedAsNormalCube(block) || isConnectedTextureBlock(block);
    }

    // Checks if the block's texture can be used as a wallpaper
    public static boolean canBeWallpaper(Block block) {
        // Transparent wallpapers make an x-ray
        return block.isOpaqueCube() && (isRenderedAsNormalCube(block) || isConnectedTextureBlock(block));
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

    // Connected-texture blocks (HBM's IBlockCT and Chisel's ICTMBlock) are full cubes but render
    // through a custom renderer instead of the vanilla normal-cube path. Detect them so they can
    // be used as wallpaper sources (and so wallpaper data can be stored on them).
    private static boolean isConnectedTextureBlock(Block block) {
        if (Loader.isModLoaded("hbm") && isHbmCTBlock(block)) {
            return true;
        }
        if (Loader.isModLoaded("chisel") && isCtmlibBlock(block)) {
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "hbm")
    private static boolean isHbmCTBlock(Block block) {
        return block instanceof IBlockCT;
    }

    @Optional.Method(modid = "chisel")
    private static boolean isCtmlibBlock(Block block) {
        return block instanceof ICTMBlock;
    }

    // public static void onWallpaperUpdate(World world, int x, int y, int z) {
    //     LOG.warn("[ABEL] WallpaperHandler.onWallpaperUpdate x={} y={} z={} world.isRemote={}", x, y, z, world.isRemote);

    //     // Chunk chunk = world.getChunkFromBlockCoords(x, z);
    //     // if (chunk != null) {
    //     //     // Should always be the case, but just to be safe
    //     //     chunk.setChunkModified();
    //     // }

    //     // world.markBlockForUpdate(x, y, z);
    //     // Minecraft.getMinecraft().renderGlobal.markBlockForRenderUpdate(x, y, z);
    //     // world.markBlockRangeForRenderUpdate(x, y, z, x + 1, y + 1, z + 1);
    //     int sectionX = x >> 4 << 4;
    //     int sectionY = y >> 4 << 4;
    //     int sectionZ = z >> 4 << 4;
    //     world.markBlockRangeForRenderUpdate(
    //         sectionX, sectionY, sectionZ,
    //         sectionX + 15, sectionY + 15, sectionZ + 15
    //     );
    // }
}
