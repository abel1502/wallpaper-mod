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
 * Splits a painted block face into four sub-faces and renders each with its own quadrant icon.
 *
 * <p>The geometry and ambient-occlusion interpolation are adapted from
 * {@code com.hbm.render.block.ct.RenderBlocksCT}, generalized to read the render bounds and the
 * per-corner light/color values off the live {@link RenderBlocks} instance. This lets us only
 * replace the faces that actually carry a CTM wallpaper, while every other face keeps rendering
 * through vanilla.</p>
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
     * Attempts to render the given face as a connected-texture wallpaper.
     *
     * @return {@code true} if the face was rendered, in which case the caller should cancel the
     *         original {@code renderFace*} call.
     */
    public static boolean tryRender(RenderBlocks rb, Block block, double x, double y, double z, int side) {
        IBlockAccess world = rb.blockAccess;
        if (world == null) {
            return false;
        }

        int bx = (int) x;
        int by = (int) y;
        int bz = (int) z;

        // Only handle whole-block positions (normal-cube world rendering).
        if (x != bx || y != by || z != bz) {
            return false;
        }

        PaletteEntry entry = WallpaperCTM.getEntry(world, bx, by, bz, side);
        if (entry == null) {
            return false;
        }

        IIcon[] quadrants = WallpaperCTM.getQuadrants(world, bx, by, bz, side, entry);
        if (quadrants == null) {
            return false;
        }

        return instance.render(rb, x, y, z, side, quadrants);
    }

    private boolean render(RenderBlocks rb, double x, double y, double z, int side, IIcon[] quadrants) {
        this.rb = rb;
        this.enableAO = rb.enableAO;

        switch (side) {
            case 0: renderFaceYNeg(x, y, z, quadrants); break;
            case 1: renderFaceYPos(x, y, z, quadrants); break;
            case 2: renderFaceZNeg(x, y, z, quadrants); break;
            case 3: renderFaceZPos(x, y, z, quadrants); break;
            case 4: renderFaceXNeg(x, y, z, quadrants); break;
            case 5: renderFaceXPos(x, y, z, quadrants); break;
            default: return false;
        }

        return true;
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

    private void renderFaceXPos(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.EAST.ordinal());
        drawFace(
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMinZ},
            q[0], q[1], q[2], q[3]);
    }

    private void renderFaceXNeg(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.WEST.ordinal());
        drawFace(
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMinZ},
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMaxZ},
            q[0], q[1], q[2], q[3]);
    }

    private void renderFaceYPos(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.UP.ordinal());
        drawFace(
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMaxZ},
            q[0], q[1], q[2], q[3]);
    }

    private void renderFaceYNeg(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.DOWN.ordinal());
        drawFace(
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMinZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMinZ},
            q[0], q[1], q[2], q[3]);
    }

    private void renderFaceZPos(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.SOUTH.ordinal());
        drawFace(
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMaxZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMaxZ},
            q[0], q[1], q[2], q[3]);
    }

    private void renderFaceZNeg(double x, double y, double z, IIcon[] q) {
        initSideInfo(ForgeDirection.NORTH.ordinal());
        drawFace(
            new double[] {x + rb.renderMaxX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMinX, y + rb.renderMaxY, z + rb.renderMinZ},
            new double[] {x + rb.renderMaxX, y + rb.renderMinY, z + rb.renderMinZ},
            new double[] {x + rb.renderMinX, y + rb.renderMinY, z + rb.renderMinZ},
            q[0], q[1], q[2], q[3]);
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
