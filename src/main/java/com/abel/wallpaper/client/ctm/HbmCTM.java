package com.abel.wallpaper.client.ctm;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.hbm.blocks.generic.BlockEnumMultiCT;
import com.hbm.render.block.ct.CTStitchReceiver;
import com.hbm.render.block.ct.IBlockCT;

import com.abel.wallpaper.world.PaletteEntry;

/**
 * HBM Nuclear Tech Mod connected-texture provider.
 *
 * <p>All references to {@link IBlockCT} live inside methods annotated with
 * {@code @Optional.Method(modid = "hbm")}, so this class can be loaded even when HBM isn't
 * present. Callers must guard entry with {@code Loader.isModLoaded("hbm")}.</p>
 */
@SideOnly(Side.CLIENT)
final class HbmCTM {

    // Mirrors com.hbm.render.block.ct.CT bit layout.
    private static final int T = 0;
    private static final int B = 2;
    private static final int L = 0;
    private static final int R = 1;

    private static final int F = 0;
    private static final int C = 4;
    private static final int J = 8;
    private static final int H = 12;
    private static final int V = 16;

    private HbmCTM() {
    }

    @Optional.Method(modid = "hbm")
    static IIcon[] tryGetQuadrants(IBlockAccess world, int x, int y, int z, int side, Block source, int sourceMeta) {
        if (!(source instanceof IBlockCT)) {
            return null;
        }

        try {
            IBlockCT ct = (IBlockCT) source;
            IIcon[] fragments = getFragments(ct, world, x, y, z, side, sourceMeta);
            if (fragments == null || fragments.length < 20) {
                return null;
            }

            boolean[] cons = new boolean[8];
            int[][] dirs = WallpaperCTM.ACCESS[side];

            for (int j = 0; j < 8; j++) {
                int[] off = dirs[j];
                int nx = x + off[0];
                int ny = y + off[1];
                int nz = z + off[2];

                PaletteEntry neighbor = WallpaperCTM.getEntry(world, nx, ny, nz, side);
                if (neighbor != null) {
                    cons[j] = neighbor.block == source && neighbor.blockMeta == (short) sourceMeta;
                } else {
                    cons[j] = ct.canConnect(world, nx, ny, nz, world.getBlock(nx, ny, nz));
                }
            }

            int itl = T | L | cornerType(cons[3], cons[0], cons[1]);
            int itr = T | R | cornerType(cons[4], cons[2], cons[1]);
            int ibl = B | L | cornerType(cons[3], cons[5], cons[6]);
            int ibr = B | R | cornerType(cons[4], cons[7], cons[6]);

            return new IIcon[] {fragments[itl], fragments[itr], fragments[ibl], fragments[ibr]};
        } catch (Throwable t) {
            return null;
        }
    }

    // Returns the fragment cache for a CT block, using the *source* metadata. Handles
    // BlockEnumMultiCT (whose getFragments otherwise reads the host block's metadata) explicitly.
    @Optional.Method(modid = "hbm")
    private static IIcon[] getFragments(IBlockCT ct, IBlockAccess world, int x, int y, int z, int side, int sourceMeta) {
        if (ct instanceof BlockEnumMultiCT) {
            CTStitchReceiver[] receivers = ((BlockEnumMultiCT) ct).receivers;
            if (receivers == null || receivers.length == 0) {
                return null;
            }
            CTStitchReceiver receiver = receivers[sourceMeta % receivers.length];
            return receiver != null ? receiver.fragCache : null;
        }
        return ct.getFragments(world, x, y, z, side);
    }

    // Ported from com.hbm.render.block.ct.CTContext#cornerType.
    private static int cornerType(boolean hor, boolean corner, boolean vert) {
        if (vert && hor && corner) {
            return C;
        } else if (vert && hor) {
            return J;
        } else if (vert) {
            return V;
        } else if (hor) {
            return H;
        } else {
            return F;
        }
    }
}
