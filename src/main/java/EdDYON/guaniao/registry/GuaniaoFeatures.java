package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.world.CrowNestFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, GuaniaoMod.MOD_ID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> CROW_NEST = FEATURES.register("crow_nest", () ->
            new CrowNestFeature(NoneFeatureConfiguration.CODEC));

    private GuaniaoFeatures() {
    }
}
