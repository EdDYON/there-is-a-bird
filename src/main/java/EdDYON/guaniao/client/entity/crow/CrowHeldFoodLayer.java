package EdDYON.guaniao.client.entity.crow;

import EdDYON.guaniao.content.bird.crow.CrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CrowHeldFoodLayer extends GeoRenderLayer<CrowEntity> {
    private static final String MOUTH_BONE = "mouth_anchor";
    private static final double MOUTH_ANCHOR_Y = 12.475D / 16.0D;
    private static final double MOUTH_ANCHOR_Z = -8.95D / 16.0D;
    private static final float BASE_X_ROTATION = 90.0F;
    private static final float BASE_Z_ROTATION = 150.0F;
    private static final float X_ROTATION_VARIATION = 5.0F;
    private static final float Z_ROTATION_VARIATION = 35.0F;
    private static final float ITEM_SCALE = 0.80F;

    public CrowHeldFoodLayer(GeoRenderer<CrowEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, CrowEntity crow, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        ItemStack stack = getStackForRendering(crow);
        if (!MOUTH_BONE.equals(bone.getName()) || stack.isEmpty()) {
            return;
        }
        int poseSeed = getPoseSeed(crow, stack);
        poseStack.pushPose();
        try {
            poseStack.translate(0.0D, MOUTH_ANCHOR_Y - 0.015D, MOUTH_ANCHOR_Z - 0.025D);
            poseStack.mulPose(Axis.XP.rotationDegrees(BASE_X_ROTATION + seededOffset(poseSeed, 11, X_ROTATION_VARIATION)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(BASE_Z_ROTATION + seededOffset(poseSeed, 37, Z_ROTATION_VARIATION)));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    crow,
                    stack,
                    ItemDisplayContext.GROUND,
                    false,
                    poseStack,
                    bufferSource,
                    packedLight);
        } finally {
            poseStack.popPose();
            bufferSource.getBuffer(renderType);
        }
    }

    private static int getPoseSeed(CrowEntity crow, ItemStack stack) {
        ItemStack heldFood = crow.getHeldFoodForRendering();
        int seed = crow.getHeldFoodPoseSeed();
        if (!heldFood.isEmpty() && seed != 0) {
            return seed;
        }
        return 31 * crow.getId() + stack.getDescriptionId().hashCode();
    }

    private static float seededOffset(int seed, int salt, float range) {
        return (seededUnit(seed, salt) * 2.0F - 1.0F) * range;
    }

    private static float seededUnit(int seed, int salt) {
        int value = seed ^ salt * 0x9E3779B9;
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        value ^= value >>> 16;
        return (value & 0xFFFFFF) / 16777216.0F;
    }

    private static ItemStack getStackForRendering(CrowEntity crow) {
        ItemStack stack = crow.getHeldFoodForRendering();
        if (!stack.isEmpty()) {
            return stack;
        }
        return getDebugPreviewStack();
    }

    private static ItemStack getDebugPreviewStack() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isCreative()) {
            return ItemStack.EMPTY;
        }
        ItemStack debugTool = minecraft.player.getMainHandItem();
        ItemStack previewStack = minecraft.player.getOffhandItem();
        if (!debugTool.is(Items.DEBUG_STICK) || previewStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = previewStack.copy();
        stack.setCount(1);
        return stack;
    }
}
