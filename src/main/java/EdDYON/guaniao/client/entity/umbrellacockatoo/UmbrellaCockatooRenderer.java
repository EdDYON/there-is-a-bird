package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UmbrellaCockatooRenderer extends GeoEntityRenderer<UmbrellaCockatooEntity> {
    private final CockatooPoseComposer composer = new CockatooPoseComposer();
    private CockatooPoseComposer.RenderPose framePose;

    public UmbrellaCockatooRenderer(EntityRendererProvider.Context context) {
        super(context, new UmbrellaCockatooModel());
        this.shadowRadius = 0.20F;
    }

    @Override
    public void preRender(PoseStack poseStack, UmbrellaCockatooEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          int renderColor) {
        this.withScale(animatable.getModelRenderScale());
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, UmbrellaCockatooEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               int renderColor) {
        var previousPose = framePose;
        framePose = null;
        try {
            // super evaluates the base controller before calling renderRecursively.
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, renderColor);
        } finally {
            framePose = previousPose;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, UmbrellaCockatooEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int renderColor) {
        if (framePose == null) framePose = ((UmbrellaCockatooModel) getGeoModel()).samplePose(animatable);
        try (var scope = composer.apply(bone, framePose.bone(bone.getName()))) {
            // Re-render passes also need the delta; only active recursion is suppressed.
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, renderColor);
        }
    }
}
