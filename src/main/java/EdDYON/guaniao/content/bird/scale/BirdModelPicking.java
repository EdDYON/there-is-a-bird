package EdDYON.guaniao.content.bird.scale;

import net.minecraft.world.entity.Entity;

/** Extends native ray picking to the normal visual body without changing physical collisions. */
public final class BirdModelPicking {
    private static final float PICK_TOLERANCE = 0.025F;

    private BirdModelPicking() { }

    public static float pickRadius(Entity bird, ScalableBirdModel model, float inheritedRadius) {
        // An inflated head passenger can contain its owner's eyes and intercept
        // every ground click. Keep its original selectable box while mounted.
        if (bird.isPassenger()) return inheritedRadius;
        BirdModelScaleProfile profile = model.modelScaleProfile();
        float visibleHeight = profile.targetHeightBlocks()
                * BirdModelScale.sanitize(model.getIndividualModelScale(), profile);
        // Use the normal authored size, not the temporary April Fools multiplier.
        float heightDifference = Math.max(0.0F, visibleHeight - bird.getBbHeight());
        return Math.max(inheritedRadius, heightDifference + PICK_TOLERANCE);
    }
}
