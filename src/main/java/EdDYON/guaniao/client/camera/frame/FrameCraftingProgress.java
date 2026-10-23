package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/** A second, independent preview bird walks the authoritative crafting progress. */
final class FrameCraftingProgress {
    private static final float START_X=260,END_X=307,FEET_Y=264,SIZE=32;
    private static final ResourceLocation GRASS_SIDE=ResourceLocation.fromNamespaceAndPath("minecraft","block/grass_block_side");
    private static final ResourceLocation GRASS_OVERLAY=ResourceLocation.fromNamespaceAndPath("minecraft","block/grass_block_side_overlay");
    private static final ResourceLocation CORNFLOWER=ResourceLocation.fromNamespaceAndPath("minecraft","block/cornflower");
    private static final ResourceLocation DANDELION=ResourceLocation.fromNamespaceAndPath("minecraft","block/dandelion");
    private static final ResourceLocation GRASS=ResourceLocation.fromNamespaceAndPath("minecraft","block/grass");

    private FrameCraftingProgress() { }

    static void render(GuiGraphics graphics,float ticks,float eatingTicks,long seed) {
        float progress=Mth.clamp(ticks/FrameWorkbenchMenu.CRAFT_DURATION_TICKS,0,1);
        float x=Mth.lerp(progress,START_X,END_X);
        renderMeadow(graphics);
        float foodScale=eatingTicks<0?1:1-Mth.clamp((eatingTicks-8)/16,0,1);
        if(foodScale>0) {
            graphics.pose().pushPose();
            graphics.pose().translate(315,261,100);
            graphics.pose().scale(0.7F*foodScale,0.7F*foodScale,1);
            graphics.renderItem(FrameWorkshopBird.progressFood(seed),-8,-12);
            graphics.pose().popPose();
        }
        if(eatingTicks>=6 && eatingTicks<22) {
            float bite=(eatingTicks-6)/16;
            int spread=Math.round(bite*6),lift=Math.round((float)Math.sin(bite*Math.PI)*4);
            graphics.fill(312-spread,258-lift,314-spread,260-lift,0xFFC7B783);
            graphics.fill(316+spread,259-lift,317+spread,260-lift,0xFFE2D5AE);
        }
        // Shorten the full native peck clip for this UI-only finish, without cutting off its bite.
        float animationTicks=ticks*0.45F+Math.max(0,eatingTicks)*4;
        FrameWorkshopBird.renderProgress(graphics,x,FEET_Y,SIZE,-90,0,animationTicks,seed,
                eatingTicks<0?FrameWorkshopBird.Action.WALK:FrameWorkshopBird.Action.EAT);
    }

    private static void renderMeadow(GuiGraphics graphics) {
        Minecraft mc=Minecraft.getInstance();
        var atlas=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
        var side=atlas.apply(GRASS_SIDE);
        var overlay=atlas.apply(GRASS_OVERLAY);
        // Use the native side texture and inventory tint, including resource-pack overrides.
        int tint=mc.getBlockColors().getColor(Blocks.GRASS_BLOCK.defaultBlockState(),null,null,0);
        float red=((tint>>16)&255)/255.0F,green=((tint>>8)&255)/255.0F,blue=(tint&255)/255.0F;
        for(int x=247;x<324;x+=7) {
            graphics.blit(x,264,0,7,7,side);
            graphics.blit(x,264,0,7,7,overlay,red,green,blue,1);
        }
        // Small plants sit behind the bird; leave the food at the right-hand end clear.
        plant(graphics,CORNFLOWER,251,7,0xFFFFFF);
        plant(graphics,GRASS,270,5,tint);
        plant(graphics,DANDELION,284,6,0xFFFFFF);
        plant(graphics,GRASS,299,5,tint);
    }

    private static void plant(GuiGraphics graphics,ResourceLocation texture,int centerX,int size,int tint) {
        var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.blit(centerX-size/2,(int)FEET_Y-size,0,size,size,sprite,
                ((tint>>16)&255)/255.0F,((tint>>8)&255)/255.0F,(tint&255)/255.0F,1);
    }
}
