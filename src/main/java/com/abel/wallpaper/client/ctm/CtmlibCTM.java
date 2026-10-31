package com.abel.wallpaper.client.ctm;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import team.chisel.ctmlib.ICTMBlock;
import team.chisel.ctmlib.ISubmapManager;
import team.chisel.ctmlib.RenderBlocksCTM;
import team.chisel.ctmlib.TextureSubmap;

import com.abel.wallpaper.world.PaletteEntry;

/**
 * Chisel / CTMLib connected-texture provider (compact CTM, i.e. the 4x4 {@code -ctm} texture
 * plus the 2x2 base texture).
 *
 * <p>All references to {@code team.chisel.ctmlib.*} live inside methods annotated with
 * {@code @Optional.Method(modid = "chisel")}, so this class can be loaded even when Chisel isn't
 * present (CTMLib is bundled with Chisel at runtime). Callers must guard entry with
 * {@code Loader.isModLoaded("chisel")}.</p>
 */
@SideOnly(Side.CLIENT)
final class CtmlibCTM {

    // Offsets for the four quadrants in counter-clockwise order: bottom-left, bottom-right,
    // top-right, top-left. Ported from team.chisel.ctmlib.CTM#submapOffsets.
    private static final int[] SUBMAP_OFFSETS = {4, 5, 1, 0};

    private CtmlibCTM() {
    }

    @Optional.Method(modid = "chisel")
    static IIcon[] tryGetQuadrants(IBlockAccess world, int x, int y, int z, int side, Block source, int sourceMeta) {
        if (!(source instanceof ICTMBlock)) {
            return null;
        }

        try {
            ISubmapManager manager = ((ICTMBlock<?>) source).getManager(world, x, y, z, sourceMeta);
            if (manager == null) {
                return null;
            }

            RenderBlocks context = manager.createRenderContext(null, source, world);
            if (!(context instanceof RenderBlocksCTM)) {
                // Not compact CTM.
                return null;
            }

            TextureSubmap submap = ((RenderBlocksCTM) context).submap;
            TextureSubmap small = ((RenderBlocksCTM) context).submapSmall;
            if (submap == null || small == null) {
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
                    cons[j] = world.getBlock(nx, ny, nz) == source && world.getBlockMetadata(nx, ny, nz) == sourceMeta;
                }
            }

            // Ported from team.chisel.ctmlib.CTM#getSubmapIndices / #fillSubmaps.
            int[] ret = {18, 19, 17, 16}; // bottom-left, bottom-right, top-right, top-left

            for (int idx = 0; idx < 4; idx++) {
                boolean a;
                boolean b;
                boolean c;

                switch (idx) {
                    case 0: // bottom-left: {BOTTOM, LEFT, BOTTOM_LEFT}
                        a = cons[6];
                        b = cons[3];
                        c = cons[5];
                        break;
                    case 1: // bottom-right: {BOTTOM, RIGHT, BOTTOM_RIGHT}
                        a = cons[6];
                        b = cons[4];
                        c = cons[7];
                        break;
                    case 2: // top-right: {TOP, RIGHT, TOP_RIGHT}
                        a = cons[1];
                        b = cons[4];
                        c = cons[2];
                        break;
                    default: // top-left: {TOP, LEFT, TOP_LEFT}
                        a = cons[1];
                        b = cons[3];
                        c = cons[0];
                        break;
                }

                if (a || b) {
                    if (a && b && c) {
                        ret[idx] = SUBMAP_OFFSETS[idx];
                    } else {
                        ret[idx] = SUBMAP_OFFSETS[idx] + (a ? 2 : 0) + (b ? 8 : 0);
                    }
                }
            }

            // ret is counter-clockwise [BL, BR, TR, TL]; convert to lexical [TL, TR, BL, BR].
            return new IIcon[] {
                getIcon(submap, small, ret[3]),
                getIcon(submap, small, ret[2]),
                getIcon(submap, small, ret[0]),
                getIcon(submap, small, ret[1]),
            };
        } catch (Throwable t) {
            return null;
        }
    }

    // Ported from team.chisel.ctmlib.RenderBlocksCTM#side.
    private static IIcon getIcon(TextureSubmap submap, TextureSubmap small, int index) {
        TextureSubmap map;
        if (index >= 16) {
            index -= 16;
            map = small;
        } else {
            map = submap;
        }

        return map.getSubIcon(index % map.getWidth(), index / map.getHeight());
    }
}
