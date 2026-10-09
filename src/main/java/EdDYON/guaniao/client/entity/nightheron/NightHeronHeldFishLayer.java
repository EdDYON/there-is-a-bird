package EdDYON.guaniao.client.entity.nightheron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class NightHeronHeldFishLayer extends GeoRenderLayer<NightHeronEntity> {
    private static final String MOUTH_BONE = "fish_anchor";
    private static final double MOUTH_ANCHOR_Y = 14.35D / 16.0D;
    private static final double MOUTH_ANCHOR_Z = -12.85D / 16.0D;
    private static final float BASE_X_ROTATION = 90.0F;
    private static final float BASE_Z_ROTATION = 145.0F;
    private static final float X_ROTATION_VARIATION = 4.0F;
    private static final float Z_ROTATION_VARIATION = 22.0F;
    private static final float ITEM_SCALE = 0.55F;
    // The bill ends at local Z = -13.10648; keep the carried sprite just beyond its tip.
    private static final double DELIVERY_ANCHOR_Y = 14.05D / 16.0D;
    private static final double DELIVERY_ANCHOR_Z = -13.35D / 16.0D;
    private static final float DELIVERY_SCALE = 0.34F;

    public NightHeronHeldFishLayer(GeoRenderer<NightHeronEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, NightHeronEntity nightHeron, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!MOUTH_BONE.equals(bone.getName())) return;
        ItemStack stack = getStackForRendering(nightHeron);
        if (stack.isEmpty()) {
            return;
        }
        int poseSeed = getPoseSeed(nightHeron, stack);
        boolean delivery = nightHeron.hasDeliveryFish();
        poseStack.pushPose();
        try {
            poseStack.translate(0.0D, delivery ? DELIVERY_ANCHOR_Y : MOUTH_ANCHOR_Y,
                    delivery ? DELIVERY_ANCHOR_Z : MOUTH_ANCHOR_Z);
            // Keep the fish's depth fixed in front of the bill; tilting it can cut back through the beak.
            poseStack.mulPose(Axis.XP.rotationDegrees(delivery ? 0 : BASE_X_ROTATION + seededOffset(poseSeed, 11, X_ROTATION_VARIATION)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(delivery ? -20 : BASE_Z_ROTATION + seededOffset(poseSeed, 37, Z_ROTATION_VARIATION)));
            float scale = delivery ? DELIVERY_SCALE : ITEM_SCALE;
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                    nightHeron,
                    stack,
                    delivery ? ItemDisplayContext.NONE : ItemDisplayContext.GROUND,
                    false,
                    poseStack,
                    bufferSource,
                    packedLight);
        } finally {
            poseStack.popPose();
            bufferSource.getBuffer(renderType);
        }
    }

    private static int getPoseSeed(NightHeronEntity nightHeron, ItemStack stack) {
        int seed = nightHeron.getHeldFishPoseSeed();
        if (!nightHeron.getHeldFishForRendering().isEmpty() && seed != 0) {
            return seed;
        }
        return 31 * nightHeron.getId() + stack.getDescriptionId().hashCode();
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

    private static ItemStack getStackForRendering(NightHeronEntity nightHeron) {
        ItemStack stack = nightHeron.getHeldFishForRendering();
        if (NightHeronEntity.isEdibleFishItem(stack)) {
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
        if (!debugTool.is(Items.DEBUG_STICK) || !NightHeronEntity.isEdibleFishItem(previewStack)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = previewStack.copy();
        stack.setCount(1);
        return stack;
    }
}
