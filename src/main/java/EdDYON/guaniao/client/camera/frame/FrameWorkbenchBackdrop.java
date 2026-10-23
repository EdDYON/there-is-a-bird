package EdDYON.guaniao.client.camera.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Muted vanilla wood surfaces in the frame workbench. */
final class FrameWorkbenchBackdrop {
    private static final int TILE_SIZE=38;
    private static final float SHADE=0.48F;
    private static final ResourceLocation WOOD=new ResourceLocation("minecraft","textures/block/birch_planks.png");

    private FrameWorkbenchBackdrop() { }

    static void render(GuiGraphics graphics) {
        render(graphics,79,41,152,150);
    }

    static void render(GuiGraphics graphics,int x,int y,int width,int height) {
        // One continuous quad repeats the vanilla wood texture. Separate atlas-sprite
        // tiles expose their sampling boundaries when the workbench uses a fractional scale.
        graphics.setColor(SHADE,SHADE,SHADE,1);
        graphics.blit(WOOD,x,y,0,0,width,height,TILE_SIZE,TILE_SIZE);
        graphics.setColor(1,1,1,1);
        graphics.fillGradient(x,y,x+width,y+height,0x08000000,0x28000000);
        graphics.fill(x,y,x+width,y+1,0x48000000);
        graphics.fill(x,y,x+1,y+height,0x48000000);
    }
}
