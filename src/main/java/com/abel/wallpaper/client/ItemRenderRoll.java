package com.abel.wallpaper.client;

import org.lwjgl.opengl.GL11;

import com.abel.wallpaper.item.ItemRoll;
import com.abel.wallpaper.world.PaletteEntry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

@SideOnly(Side.CLIENT)
public class ItemRenderRoll implements IItemRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderItem renderItem = new RenderItem();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        switch (type) {
            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
            case ENTITY:
            case INVENTORY:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type == ItemRenderType.ENTITY && (helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object ... data) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        if (type == ItemRenderType.INVENTORY) {
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glTranslated(8, 10, 0);
            GL11.glRotated(-30, 1, 0, 0);
            GL11.glRotated(45, 0, 1, 0);
            GL11.glScaled(-1, -1, -1);
        } else {
            if (type != ItemRenderType.ENTITY) {
                GL11.glTranslated(0.5, 0.25, 0);
            } else {
                GL11.glScaled(1.5, 1.5, 1.5);
            }

            GL11.glScaled(0.25, 0.25, 0.25);

            if (type != ItemRenderType.EQUIPPED) {
                GL11.glRotated(90, 0, 1, 0);
            }
        }

        IIcon baseIcon = stack.getIconIndex();

        mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);
        renderItem.renderIcon(0, 0, baseIcon, 16, 16);

        PaletteEntry paletteEntry = ItemRoll.getPalette(stack);
        if (paletteEntry != null) {
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            renderItem.renderIcon(1, 1, paletteEntry.icon, 6, 6);
        }

        GL11.glPopMatrix();
    }
}
