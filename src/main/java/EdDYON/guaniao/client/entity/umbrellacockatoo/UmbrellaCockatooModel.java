package EdDYON.guaniao.client.entity.umbrellacockatoo;

import EdDYON.guaniao.client.entity.mutation.BirdMutationTextureFactory;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooDefinition;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.model.GeoModel;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.WeakHashMap;

/** Resource binding and per-entity expression history; never writes the shared base bones. */
public class UmbrellaCockatooModel extends GeoModel<UmbrellaCockatooEntity> {
    private final Map<UmbrellaCockatooEntity, CockatooVisualState> expressions = new WeakHashMap<>();
    private Animation resourceVersion;
    private Animation displayResidual;
    private CockatooExpressionSampler curves;

    @Override
    public ResourceLocation getModelResource(UmbrellaCockatooEntity animatable) {
        return UmbrellaCockatooDefinition.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(UmbrellaCockatooEntity animatable) {
        return BirdMutationTextureFactory.textureFor(animatable.getTextureResource(), animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(UmbrellaCockatooEntity animatable) {
        return UmbrellaCockatooDefinition.ANIMATION;
    }

    @Override
    public Animation getAnimation(UmbrellaCockatooEntity animatable, String name) {
        if (!name.equals(CockatooDisplayResidual.NAME)) return super.getAnimation(animatable, name);
        loadExpressionData(animatable);
        return displayResidual;
    }

    CockatooPoseComposer.RenderPose samplePose(UmbrellaCockatooEntity animatable) {
        loadExpressionData(animatable);
        // A reload/first visible draw during the residual must restore its reference,
        // not replay a closed-to-open curve underneath an already active body display.
        var state = expressions.computeIfAbsent(animatable,
                bird -> new CockatooVisualState(bird.needsDisplayReference() ? 2 : 0));
        var pose = state.sample(curves, animatable.getExpressionLevel(), animatable.getCockatooAnimationTick());
        float roll = animatable.getHeadRoll();
        return new CockatooPoseComposer.RenderPose(pose, Math.abs(roll) < 0.05F ? 0 : roll);
    }

    private void loadExpressionData(UmbrellaCockatooEntity animatable) {
        // GeckoLib replaces baked Animation objects on resource reload. Use identity,
        // so resource packs rebuild both the reference and its derived residual together.
        Animation version = super.getAnimation(animatable, "animation.emotion2");
        if (curves != null && version == resourceVersion) return;
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(getAnimationResource(animatable))) {
            var loaded = new CockatooExpressionSampler(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations"));
            var residual = CockatooDisplayResidual.create(super.getAnimation(animatable, "animation.idle_diff_2"), loaded.hold(2));
            curves = loaded;
            displayResidual = residual;
            resourceVersion = version;
            expressions.clear();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load umbrella cockatoo expression curves", exception);
        }
    }
}
