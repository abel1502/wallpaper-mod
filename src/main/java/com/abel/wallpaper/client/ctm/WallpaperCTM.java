package com.abel.wallpaper.client.ctm;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.abel.wallpaper.world.IHasFaceOverride;
import com.abel.wallpaper.world.PaletteEntry;

/**
 * Resolves connected-texture fragments for a painted face.
 *
 * <p>The source block recorded in a {@link PaletteEntry} is inspected for a CTM-capable
 * "fragment provider". Two providers are supported, both guarded with Forge's {@code @Optional}
 * so that the references only resolve when the corresponding mod is actually present:</p>
 * <ul>
 *   <li>HBM's Nuclear Tech Mod ({@code com.hbm.render.block.ct.IBlockCT})</li>
 *   <li>Chisel / CTMLib ({@code team.chisel.ctmlib.ICTMBlock})</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class WallpaperCTM {

    private WallpaperCTM() {
    }

    // Neighbor offsets for each face, in lexical order:
    //   0 1 2
    //   3   4
    //   5 6 7
    // dim1: 6 faces (ForgeDirection ordinal), dim2: 8 neighbors, dim3: x/y/z offset.
    // Ported from com.hbm.render.block.ct.CTContext#access.
    static final int[][][] ACCESS = buildAccess();

    private static int[][][] buildAccess() {
        return new int[][][] {
            lcfs(ForgeDirection.SOUTH, ForgeDirection.WEST),  // DOWN
            lcfs(ForgeDirection.NORTH, ForgeDirection.WEST),  // UP
            lcfs(ForgeDirection.UP, ForgeDirection.EAST),     // NORTH
            lcfs(ForgeDirection.UP, ForgeDirection.WEST),     // SOUTH
            lcfs(ForgeDirection.UP, ForgeDirection.NORTH),    // WEST
            lcfs(ForgeDirection.UP, ForgeDirection.SOUTH)     // EAST
        };
    }

    // Lexical coordinates from side.
    private static int[][] lcfs(ForgeDirection up, ForgeDirection left) {
        ForgeDirection down = up.getOpposite();
        ForgeDirection right = left.getOpposite();

        return new int[][] {
            cfs(up, left),
            cfs(up),
            cfs(up, right),
            cfs(left),
            cfs(right),
            cfs(down, left),
            cfs(down),
            cfs(down, right),
        };
    }

    // Coordinates from sides.
    private static int[] cfs(ForgeDirection... dirs) {
        int x = 0;
        int y = 0;
        int z = 0;

        for (ForgeDirection dir : dirs) {
            x += dir.offsetX;
            y += dir.offsetY;
            z += dir.offsetZ;
        }

        return new int[] {x, y, z};
    }

    /**
     * Returns the four quadrant icons for a painted face in lexical order
     * {@code [top-left, top-right, bottom-left, bottom-right]}, or {@code null} if the source
     * block isn't CTM-capable.
     */
    public static IIcon[] getQuadrants(IBlockAccess world, int x, int y, int z, int side, PaletteEntry entry) {
        Block source = entry.block;
        if (source == null) {
            return null;
        }

        int sourceMeta = entry.blockMeta;

        if (Loader.isModLoaded("hbm")) {
            IIcon[] quadrants = HbmCTM.tryGetQuadrants(world, x, y, z, side, source, sourceMeta);
            if (quadrants != null) {
                return quadrants;
            }
        }

        if (Loader.isModLoaded("chisel")) {
            IIcon[] quadrants = CtmlibCTM.tryGetQuadrants(world, x, y, z, side, source, sourceMeta);
            if (quadrants != null) {
                return quadrants;
            }
        }

        return null;
    }

    /**
     * Safely looks up the wallpaper entry at a neighbor position for the given face.
     * Returns {@code null} if there's no wallpaper there or the block access isn't supported.
     */
    public static PaletteEntry getEntry(IBlockAccess world, int x, int y, int z, int side) {
        try {
            return IHasFaceOverride.of(world).getWallpaperEntry(x, y, z, side);
        } catch (ClassCastException e) {
            // Unsupported IBlockAccess implementation; already reported by MixinRenderBlocks.
            return null;
        }
    }
}
