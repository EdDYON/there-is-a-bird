package EdDYON.guaniao.content.bird;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class BirdSoundVolume {
    private BirdSoundVolume() {
    }

    public static float apply(Entity bird, float originalVolume) {
        double multiplier = BirdConfigManager.soundMultiplier(BirdSpecies.from(bird));
        return Mth.clamp((float)(originalVolume * multiplier), 0.0F, 16.0F);
    }
}
