package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.content.camera.PhotographEntity;
import EdDYON.guaniao.client.camera.frame.FrameDesignRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class PhotographEntityRenderer extends EntityRenderer<PhotographEntity> {
    public PhotographEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PhotographEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(@NotNull PhotographEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poses, @NotNull MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poses, buffers, packedLight);
        poses.pushPose();
        poses.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poses.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        if (entity.getFrameDesign() != null) {
            FrameDesignRenderer.render(entity.getItem(), entity.getFrameDesign(), entity.getRotation(), poses, buffers, packedLight);
        } else {
            PhotographFrameRenderer.render(entity.getItem(), entity.getFrameStyle(), entity.getRotation(),
                    poses, buffers, packedLight);
        }
        poses.popPose();
    }
}
