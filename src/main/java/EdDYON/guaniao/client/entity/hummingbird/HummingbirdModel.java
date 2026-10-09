package EdDYON.guaniao.client.entity.hummingbird;

import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.model.GeoModel;

/** The entity owns time; model queries are rebound for the bird currently being rendered. */
public final class HummingbirdModel extends GeoModel<HummingbirdEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("guaniao", "geo/hummingbird.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guaniao", "textures/entity/hummingbird.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("guaniao", "animations/hummingbird_runtime.animation.json");
    private static final String[] HEAD_PERCH_SUPPORT_BONES = {
            "root", "all", "waist", "leg_left", "leg_right", "foot_left", "foot_right"
    };

    @Override public ResourceLocation getModelResource(HummingbirdEntity bird) { return MODEL; }
    @Override public ResourceLocation getTextureResource(HummingbirdEntity bird) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(HummingbirdEntity bird) { return ANIMATION; }

    @Override
    public void setCustomAnimations(HummingbirdEntity bird, long instanceId, AnimationState<HummingbirdEntity> state) {
        super.setCustomAnimations(bird, instanceId, state);
        if (!(bird.getVehicle() instanceof Player owner) || !bird.isOwnedBy(owner)) return;
        restoreHeadPerchSupportPose();
    }

    void restoreHeadPerchSupportPose() {
        // Keep the authored standing soles on the shared crown contact during fly-to-idle blending.
        // Head, eyes and hip/body details remain animated; ordinary flight is untouched.
        for (String name : HEAD_PERCH_SUPPORT_BONES) {
            getBone(name).ifPresent(bone -> {
                var bind = bone.getInitialSnapshot();
                bone.setRotX(bind.getRotX()); bone.setRotY(bind.getRotY()); bone.setRotZ(bind.getRotZ());
                bone.setPosX(bind.getOffsetX()); bone.setPosY(bind.getOffsetY()); bone.setPosZ(bind.getOffsetZ());
                bone.setScaleX(bind.getScaleX()); bone.setScaleY(bind.getScaleY()); bone.setScaleZ(bind.getScaleZ());
            });
        }
    }

    @Override
    public void applyMolangQueries(AnimationState<HummingbirdEntity> state, double animTime) {
        super.applyMolangQueries(state, animTime);
        HummingbirdEntity bird = state.getAnimatable();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double flightTime = bird.getFlightAnimationTime() + (bird.isAirborne() ? partialTick / 20.0 : 0);
        double forwardBlend = bird.getForwardFlightBlend(partialTick);
        double backwardBlend = bird.getBackwardFlightBlend(partialTick);
        double lateralBlend = bird.getLateralFlightBlend(partialTick);
        double wingStrokeTime = bird.getWingStrokeTime(partialTick);
        double nectar = Mth.clamp(bird.getNectarProgress(), 0, 1);
        double wingPitch = Mth.lerp(nectar, 40 - forwardBlend * 7.5 + backwardBlend * 7.5, 40);
        double bank = bird.isAirborne() && !bird.isSleeping() ? Mth.clamp(bird.getVisualBankAngle(partialTick), -15, 15) : 0;
        double wingYawBias = Mth.clamp(bank * .3, -4, 4);
        // GeckoLib evaluates each entity synchronously after applyMolangQueries. Capture values,
        // not an entity reference or an accumulated global clock shared by every hummingbird.
        MathParser.setVariable("variable.flight_time", () -> flightTime);
        MathParser.setVariable("variable.wing_stroke_time", () -> wingStrokeTime);
        MathParser.setVariable("variable.forward_blend", () -> forwardBlend);
        MathParser.setVariable("variable.backward_blend", () -> backwardBlend);
        MathParser.setVariable("variable.lateral_blend", () -> lateralBlend);
        MathParser.setVariable("variable.wing_pitch", () -> wingPitch);
        MathParser.setVariable("variable.wing_yaw_bias", () -> wingYawBias);
        MathParser.setVariable("variable.bank_angle", () -> bank);
    }
}
