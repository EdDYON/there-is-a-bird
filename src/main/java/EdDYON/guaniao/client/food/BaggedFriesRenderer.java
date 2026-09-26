package EdDYON.guaniao.client.food;

import EdDYON.guaniao.content.food.BaggedFriesBlockEntity;
import EdDYON.guaniao.content.food.BaggedFriesBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class BaggedFriesRenderer extends GeoBlockRenderer<BaggedFriesBlockEntity> {
    public BaggedFriesRenderer(BlockEntityRendererProvider.Context context) {
        super(new BaggedFriesModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BaggedFriesBlockEntity fries, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int renderColor) {
        int index = fryIndex(bone.getName());
        if (index > 0) {
            boolean visible = fries.isFryVisible(index);
            bone.setHidden(!visible);
            bone.setChildrenHidden(!visible);
        }
        super.renderRecursively(poseStack, fries, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor);
    }

    private static int fryIndex(String name) {
        if (!name.startsWith("fry_") || name.length() != 6) {
            return 0;
        }
        try {
            int index = Integer.parseInt(name.substring(4));
            return index >= 1 && index <= BaggedFriesBlock.TOTAL_FRIES ? index : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
