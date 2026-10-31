package com.abel.wallpaper.mixin.hbm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import com.hbm.render.block.ct.CT;
import com.hbm.render.block.ct.CTContext;
import com.hbm.render.block.ct.IBlockCT;

import com.abel.wallpaper.world.WallpaperCompat;

/**
 * Makes HBM's connected-texture blocks also connect to adjacent painted faces.
 *
 * <p>Replaces {@link CTContext#loadContext} with an equivalent that additionally treats a neighbor
 * as connected when it carries a wallpaper matching the block being rendered. Only loaded when HBM
 * is present (see {@code LateMixins}).</p>
 */
@Pseudo
@Mixin(CTContext.class)
public abstract class MixinCTContext {

    /**
     * @author Abel's Wallpaper Mod
     * @reason Treat painted faces of a matching source as connected neighbors in addition to real
     *         blocks of the same type.
     */
    @Overwrite(remap = false)
    public static void loadContext(IBlockAccess world, int x, int y, int z, Block block) {
        int meta = world.getBlockMetadata(x, y, z);
        CTContext.faces = new CTContext.CTFace[6];

        for (int i = 0; i < 6; i++) {
            boolean[] cons = new boolean[8];
            int[][] dirs = CTContext.access[i];

            for (int j = 0; j < 8; j++) {
                int[] coord = dirs[j];
                int nx = x + coord[0];
                int ny = y + coord[1];
                int nz = z + coord[2];
                Block neighbor = world.getBlock(nx, ny, nz);

                if (((IBlockCT) block).canConnect(world, nx, ny, nz, neighbor)
                        || WallpaperCompat.hasMatchingWallpaper(world, nx, ny, nz, i, block, meta)) {
                    cons[j] = true;
                }
            }

            int itl = CT.t | CT.l | CTContext.cornerType(cons[3], cons[0], cons[1]);
            int itr = CT.t | CT.r | CTContext.cornerType(cons[4], cons[2], cons[1]);
            int ibl = CT.b | CT.l | CTContext.cornerType(cons[3], cons[5], cons[6]);
            int ibr = CT.b | CT.r | CTContext.cornerType(cons[4], cons[7], cons[6]);

            CTContext.faces[i] = new CTContext.CTFace(world, x, y, z, i, (IBlockCT) block, itl, itr, ibl, ibr);
        }
    }
}
