package EdDYON.guaniao.client.gui;

import com.mojang.math.Divisor;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Keeps the guide's pixel borders and fixed preview framing across game versions. */
public final class BirdGuiRendering {
    private BirdGuiRendering() {}

    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int width, int height, int border, int sourceWidth, int sourceHeight, int u, int v) {
        blitNineSliced(graphics, texture, x, y, width, height, border, sourceWidth, sourceHeight, u, v, 256, 256);
    }

    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int width, int height, int border, int sourceWidth, int sourceHeight,
            int u, int v, int textureWidth, int textureHeight) {
        int bx = Math.min(border, width / 2);
        int by = Math.min(border, height / 2);
        int[] widths = {bx, width - bx * 2, bx};
        int[] heights = {by, height - by * 2, by};
        int[] sourceWidths = {bx, sourceWidth - bx * 2, bx};
        int[] sourceHeights = {by, sourceHeight - by * 2, by};
        int py = y, sy = v;
        for (int row = 0; row < 3; row++) {
            int px = x, sx = u;
            for (int col = 0; col < 3; col++) {
                tile(graphics, texture, px, py, widths[col], heights[row], sx, sy,
                        sourceWidths[col], sourceHeights[row], textureWidth, textureHeight);
                px += widths[col];
                sx += sourceWidths[col];
            }
            py += heights[row];
            sy += sourceHeights[row];
        }
    }

    private static void tile(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
            int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0) return;
        IntIterator columns = new Divisor(width, Mth.positiveCeilDiv(width, sourceWidth));
        while (columns.hasNext()) {
            int w = columns.nextInt();
            int py = y;
            IntIterator rows = new Divisor(height, Mth.positiveCeilDiv(height, sourceHeight));
            while (rows.hasNext()) {
                int h = rows.nextInt();
                graphics.blit(texture, x, py, u + (sourceWidth - w) / 2, v + (sourceHeight - h) / 2, w, h, textureWidth, textureHeight);
                py += h;
            }
            x += w;
        }
    }

    public static void renderEntity(GuiGraphics graphics, int x, int y, int scale,
            float mouseX, float mouseY, LivingEntity entity) {
        float yaw = (float)Math.atan(mouseX / 40.0F);
        float pitch = (float)Math.atan(mouseY / 40.0F);
        Quaternionf camera = new Quaternionf().rotateX(pitch * 20.0F * ((float)Math.PI / 180.0F));
        Quaternionf pose = new Quaternionf().rotateZ((float)Math.PI).mul(camera);
        float body = entity.yBodyRot, oldYaw = entity.getYRot(), oldPitch = entity.getXRot();
        float head = entity.yHeadRot, oldHead = entity.yHeadRotO;
        try {
            entity.yBodyRot = 180.0F + yaw * 20.0F;
            entity.setYRot(180.0F + yaw * 40.0F);
            entity.setXRot(-pitch * 20.0F);
            entity.yHeadRot = entity.yHeadRotO = entity.getYRot();
            InventoryScreen.renderEntityInInventory(graphics, x, y, scale, new Vector3f(), pose, camera, entity);
        } finally {
            entity.yBodyRot = body;
            entity.setYRot(oldYaw);
            entity.setXRot(oldPitch);
            entity.yHeadRot = head;
            entity.yHeadRotO = oldHead;
        }
    }
}
