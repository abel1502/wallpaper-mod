package com.abel.wallpaper.client.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.abel.wallpaper.world.PaletteEntry;

/**
 * Renders a painted block face, either as a single flat quad or as four connected-texture
 * sub-faces.
 *
 * <p>The geometry and ambient-occlusion interpolation are adapted from
 * {@code com.hbm.render.block.ct.RenderBlocksCT}, generalized to read the render bounds and the
 * per-corner light/color values off the live {@link RenderBlocks} instance.</p>
 */
@SideOnly(Side.CLIENT)
public final class WallpaperCTMRenderer {

    public static final WallpaperCTMRenderer instance = new WallpaperCTMRenderer();

    private RenderBlocks rb;
    private boolean enableAO;

    // Pre-allocated to avoid per-face allocations.
    private final VertInfo tl = new VertInfo(0, 0, 0, 0);
    private final VertInfo tc = new VertInfo(0, 0, 0, 0);
    private final VertInfo tr = new VertInfo(0, 0, 0, 0);
    private final VertInfo cl = new VertInfo(0, 0, 0, 0);
    private final VertInfo cc = new VertInfo(0, 0, 0, 0);
    private final VertInfo cr = new VertInfo(0, 0, 0, 0);
    private final VertInfo bl = new VertInfo(0, 0, 0, 0);
    private final VertInfo bc = new VertInfo(0, 0, 0, 0);
    private final VertInfo br = new VertInfo(0, 0, 0, 0);

    private WallpaperCTMRenderer() {
    }

    /**
     * Intercepts a normal-cube face (the vanilla {@code RenderBlocks.renderFace*} path). Only
     * connected-texture wallpapers are rendered here; flat wallpapers fall through so vanilla can
     * handle them (including log/quartz UV rotation).
     *
     * @return {@code true} if the face was rendered.
     */
    public static boolean tryRender(RenderBlocks rb, Block block, double x, double y, double z, int side) {
        return tryRenderInternal(rb, block, x, y, z, x, y, z, side, false);
    }

    /**
     * Intercepts a connected-texture host face (HBM's {@code RenderBlocksCT} / Chisel's
     * {@code RenderBlocksCTM}, which ignore per-face texture overrides). Both flat and
     * connected-texture wallpapers are rendered here.
     *
     * @param lx/ly/lz world position used to look up the wallpaper
     * @param rx/ry/rz position to draw at (world for HBM, block-local {@code 0,0,0} for Chisel,
     *                 whose tessellator is already translated)
     * @return {@code true} if the face was rendered.
     */
    public static boolean tryRenderHost(RenderBlocks rb, Block block, double lx, double ly, double lz, double rx, double ry, double rz, int side) {
        return tryRenderInternal(rb, block, lx, ly, lz, rx, ry, rz, side, true);
    }

    private static boolean tryRenderInternal(
        RenderBlocks rb, Block block,
        double lx, double ly, double lz,
        double rx, double ry, double rz,
        int side, boolean renderFlat
    ) {
        IBlockAccess world = rb.blockAccess;
        if (world == null) {
            return false;
        }

        int bx = (int) lx;
        int by = (int) ly;
        int bz = (int) lz;

        // Only handle whole-block positions.
        if (lx != bx || ly != by || lz != bz) {
            return false;
        }

        PaletteEntry entry = WallpaperCTM.getEntry(world, bx, by, bz, side);
        if (entry == null) {
            return false;
        }

        IIcon[] quadrants = WallpaperCTM.getQuadrants(world, bx, by, bz, side, entry);
        if (quadrants != null) {
            instance.render(rb, rx, ry, rz, side, quadrants);
            return true;
        }

        if (renderFlat && entry.icon != null) {
            instance.renderFlat(rb, rx, ry, rz, side, entry.icon);
            return true;
        }

        return false;
    }

    private void render(RenderBlocks rb, double x, double y, double z, int side, IIcon[] quadrants) {
        this.rb = rb;
        this.enableAO = rb.enableAO;
        initSideInfo(side);

        double[][] c = corners(side, x, y, z);
        drawFace(c[0], c[1], c[2], c[3], quadrants[0], quadrants[1], quadrants[2], quadrants[3]);
    }

    private void renderFlat(RenderBlocks rb, double x, double y, double z, int side, IIcon icon) {
        this.rb = rb;
        this.enableAO = rb.enableAO;
        initSideInfo(side);

        double[][] c = corners(side, x, y, z);
        drawSubFace(c[0], tl, c[1], tr, c[2], bl, c[3], br, icon);
    }

    private double[][] corners(int side, double x, double y, double z) {
        // Wallpapers only ever apply to full cubes, so the render bounds are always 0..1.
        // (HBM's RenderBlocksCT never calls setRenderBoundsFromBlock, leaving the inherited
        // render bounds at 0.0, so relying on rb.renderMaxX here would produce degenerate faces.)
        switch (side) {
            case 0: // DOWN
                return new double[][] {
                    {x, y, z + 1},
                    {x + 1, y, z + 1},
                    {x, y, z},
                    {x + 1, y, z},
                };
            case 1: // UP
                return new double[][] {
                    {x, y + 1, z},
                    {x + 1, y + 1, z},
                    {x, y + 1, z + 1},
                    {x + 1, y + 1, z + 1},
                };
            case 2: // NORTH
                return new double[][] {
                    {x + 1, y + 1, z},
                    {x, y + 1, z},
                    {x + 1, y, z},
                    {x, y, z},
                };
            case 3: // SOUTH
                return new double[][] {
                    {x, y + 1, z + 1},
                    {x + 1, y + 1, z + 1},
                    {x, y, z + 1},
                    {x + 1, y, z + 1},
                };
            case 4: // WEST
                return new double[][] {
                    {x, y + 1, z},
                    {x, y + 1, z + 1},
                    {x, y, z},
                    {x, y, z + 1},
                };
            default: // EAST
                return new double[][] {
                    {x + 1, y + 1, z + 1},
                    {x + 1, y + 1, z},
                    {x + 1, y, z + 1},
                    {x + 1, y, z},
                };
        }
    }

    private void initSideInfo(int side) {
        if (!enableAO) {
            return;
        }

        // Vanilla's per-corner values are stored per-face; remap them into the lexical (tl, tr,
        // bl, br) layout we use for the four quadrants. Ported from RenderBlocksCT#initSideInfo.
        if (side == ForgeDirection.SOUTH.ordinal()) {
            tl.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            tr.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            bl.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            br.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        } else if (side == ForgeDirection.NORTH.ordinal()) {
            tr.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            br.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            tl.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            bl.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        } else if (side == ForgeDirection.EAST.ordinal()) {
            bl.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            tl.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            br.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            tr.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        } else if (side == ForgeDirection.WEST.ordinal()) {
            tr.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            br.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            tl.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            bl.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        } else if (side == ForgeDirection.UP.ordinal()) {
            br.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            bl.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            tr.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            tl.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        } else {
            tl.setup(rb.colorRedTopLeft, rb.colorGreenTopLeft, rb.colorBlueTopLeft, rb.brightnessTopLeft);
            tr.setup(rb.colorRedTopRight, rb.colorGreenTopRight, rb.colorBlueTopRight, rb.brightnessTopRight);
            bl.setup(rb.colorRedBottomLeft, rb.colorGreenBottomLeft, rb.colorBlueBottomLeft, rb.brightnessBottomLeft);
            br.setup(rb.colorRedBottomRight, rb.colorGreenBottomRight, rb.colorBlueBottomRight, rb.brightnessBottomRight);
        }

        VertInfo.avg(tc, tl, tr);
        VertInfo.avg(bc, bl, br);
        VertInfo.avg(cl, tl, bl);
        VertInfo.avg(cr, tr, br);
        VertInfo.avg(cc, tl, tr, bl, br);
    }

    private void drawFace(
        double[] ftl, double[] ftr, double[] fbl, double[] fbr,
        IIcon itl, IIcon itr, IIcon ibl, IIcon ibr
    ) {
        double[] ftc = avgCoords(ftl, ftr);
        double[] fbc = avgCoords(fbl, fbr);
        double[] fcl = avgCoords(ftl, fbl);
        double[] fcr = avgCoords(ftr, fbr);
        double[] fcc = avgCoords(ftc, fbc);

        drawSubFace(ftl, tl, ftc, tc, fcl, cl, fcc, cc, itl);
        drawSubFace(ftc, tc, ftr, tr, fcc, cc, fcr, cr, itr);
        drawSubFace(fcl, cl, fcc, cc, fbl, bl, fbc, bc, ibl);
        drawSubFace(fcc, cc, fcr, cr, fbc, bc, fbr, br, ibr);
    }

    private void drawSubFace(
        double[] ftl, VertInfo ntl,
        double[] ftr, VertInfo ntr,
        double[] fbl, VertInfo nbl,
        double[] fbr, VertInfo nbr,
        IIcon icon
    ) {
        drawVert(ftr, icon.getMaxU(), icon.getMinV(), ntr);
        drawVert(ftl, icon.getMinU(), icon.getMinV(), ntl);
        drawVert(fbl, icon.getMinU(), icon.getMaxV(), nbl);
        drawVert(fbr, icon.getMaxU(), icon.getMaxV(), nbr);
    }

    private void drawVert(double[] coord, double u, double v, VertInfo info) {
        drawVert(coord[0], coord[1], coord[2], u, v, info);
    }

    private void drawVert(double x, double y, double z, double u, double v, VertInfo info) {
        if (enableAO) {
            Tessellator.instance.setColorOpaque_F(info.red, info.green, info.blue);
            Tessellator.instance.setBrightness(info.brightness);
        }

        Tessellator.instance.addVertexWithUV(x, y, z, u, v);
    }

    private double[] avgCoords(double[] first, double[] second) {
        return new double[] {
            (first[0] + second[0]) / 2D,
            (first[1] + second[1]) / 2D,
            (first[2] + second[2]) / 2D,
        };
    }

    private static final class VertInfo {
        float red;
        float green;
        float blue;
        int brightness;

        VertInfo(float red, float green, float blue, int brightness) {
            setup(red, green, blue, brightness);
        }

        VertInfo setup(float red, float green, float blue, int brightness) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.brightness = brightness;
            return this;
        }

        static VertInfo avg(VertInfo writeTo, VertInfo... infos) {
            float r = 0F;
            float g = 0F;
            float b = 0F;
            int l = 0;

            for (VertInfo vert : infos) {
                r += vert.red;
                g += vert.green;
                b += vert.blue;
                l += vert.brightness;
            }

            r /= infos.length;
            g /= infos.length;
            b /= infos.length;
            l /= infos.length;

            writeTo.setup(r, g, b, l);
            return writeTo;
        }
    }
}
