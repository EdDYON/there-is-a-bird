package EdDYON.guaniao.client.nest;

import EdDYON.guaniao.content.nest.CrowNestBlock;
import EdDYON.guaniao.content.nest.CrowNestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CrowNestRenderer extends GeoBlockRenderer<CrowNestBlockEntity> {
    public CrowNestRenderer(BlockEntityRendererProvider.Context context) {
        super(new CrowNestModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CrowNestBlockEntity nest, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        int eggs = nest.getBlockState().hasProperty(CrowNestBlock.EGGS)
                ? nest.getBlockState().getValue(CrowNestBlock.EGGS)
                : 0;
        applyEggVisibility(bone, eggs);
        super.renderRecursively(poseStack, nest, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public boolean shouldRenderOffScreen(CrowNestBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    static void applyEggVisibility(GeoBone bone, int eggs) {
        int eggIndex = switch (bone.getName()) {
            case "egg1" -> 1;
            case "egg2" -> 2;
            case "egg3" -> 3;
            default -> 0;
        };
        if (eggIndex > 0) {
            boolean visible = eggs >= eggIndex;
            bone.setHidden(!visible);
            bone.setChildrenHidden(!visible);
        }
    }
}
