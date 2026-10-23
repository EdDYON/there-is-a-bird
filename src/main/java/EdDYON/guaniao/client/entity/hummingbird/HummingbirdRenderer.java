package EdDYON.guaniao.client.entity.hummingbird;

import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The authored model is already at its intended small size; no species-wide scaling. */
public final class HummingbirdRenderer extends GeoEntityRenderer<HummingbirdEntity> {
    public HummingbirdRenderer(EntityRendererProvider.Context context) {
        super(context, new HummingbirdModel());
        this.shadowRadius = 0.06F;
        this.addRenderLayer(new HummingbirdHeldItemLayer(this));
    }

    @Override
    public Vec3 getRenderOffset(HummingbirdEntity bird, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            Vec3 ownerPosition = new Vec3(Mth.lerp(partialTick, owner.xOld, owner.getX()),
                    Mth.lerp(partialTick, owner.yOld, owner.getY()),
                    Mth.lerp(partialTick, owner.zOld, owner.getZ()));
            Vec3 birdPosition = new Vec3(Mth.lerp(partialTick, bird.xOld, bird.getX()),
                    Mth.lerp(partialTick, bird.yOld, bird.getY()),
                    Mth.lerp(partialTick, bird.zOld, bird.getZ()));
            float yaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
            float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
            return ownerPosition.add(bird.ownerHeadOffset(yaw, pitch)).subtract(birdPosition);
        }
        return super.getRenderOffset(bird, partialTick);
    }

    @Override
    protected void applyRotations(HummingbirdEntity bird, PoseStack pose, float age, float bodyYaw, float partialTick) {
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            bodyYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
        }
        super.applyRotations(bird, pose, age, bodyYaw, partialTick);
        if (bird.getVehicle() instanceof AbstractClientPlayer owner && bird.isOwnedBy(owner)) {
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, owner.xRotO, owner.getXRot())));
        }
    }
}
