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
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BirdBathBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BirdBathBlockEntity birdBath, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        BirdBathBoneVisibility.apply(birdBath.getContentType(), birdBath.getContentLevel(), birdBath.getCleanliness(), birdBath.getRenderContentType(), bone);
        if (BirdBathBoneVisibility.isContentBoneVisible(birdBath.getContentType(), birdBath.getContentLevel(), birdBath.getRenderContentType(), bone.getName())) {
            float[] tint = BirdBathBoneVisibility.tintFor(birdBath.getContentType(), birdBath.getRenderContentType(), birdBath.getCleanliness());
            renderColor = tintColor(renderColor, tint);
        } else if (BirdBathBoneVisibility.isDirtBone(bone.getName())) {
            float[] tint = BirdBathBoneVisibility.dirtTintFor(birdBath.getContentType(), birdBath.getCleanliness(), bone.getName());
            renderColor = tintColor(renderColor, tint);
        }
        super.renderRecursively(poseStack, birdBath, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }

    private static int tintColor(int color, float[] tint) {
        return net.minecraft.util.FastColor.ARGB32.color(
                net.minecraft.util.FastColor.ARGB32.alpha(color),
                (int)(net.minecraft.util.FastColor.ARGB32.red(color) * tint[0]),
                (int)(net.minecraft.util.FastColor.ARGB32.green(color) * tint[1]),
                (int)(net.minecraft.util.FastColor.ARGB32.blue(color) * tint[2]));
    }

    @Override
    public boolean shouldRenderOffScreen(BirdBathBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
