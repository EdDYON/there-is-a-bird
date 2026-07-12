package EdDYON.guaniao.client.bath;

import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class BirdBathRenderer extends GeoBlockRenderer<BirdBathBlockEntity> {
    public BirdBathRenderer(BlockEntityRendererProvider.Context context) {
        super(new BirdBathModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BirdBathBlockEntity birdBath, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        BirdBathBoneVisibility.apply(birdBath.getContentType(), birdBath.getContentLevel(), birdBath.getCleanliness(), birdBath.getRenderContentType(), bone);
        if (BirdBathBoneVisibility.isContentBoneVisible(birdBath.getContentType(), birdBath.getContentLevel(), birdBath.getRenderContentType(), bone.getName())) {
            float[] tint = BirdBathBoneVisibility.tintFor(birdBath.getContentType(), birdBath.getRenderContentType(), birdBath.getCleanliness());
            red *= tint[0];
            green *= tint[1];
            blue *= tint[2];
        } else if (BirdBathBoneVisibility.isDirtBone(bone.getName())) {
            float[] tint = BirdBathBoneVisibility.dirtTintFor(birdBath.getContentType(), birdBath.getCleanliness(), bone.getName());
            red *= tint[0];
            green *= tint[1];
            blue *= tint[2];
        }
        super.renderRecursively(poseStack, birdBath, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public boolean shouldRenderOffScreen(BirdBathBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
