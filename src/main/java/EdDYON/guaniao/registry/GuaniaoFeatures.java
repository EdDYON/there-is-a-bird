package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.world.CrowNestFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, GuaniaoMod.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CROW_NEST = FEATURES.register("crow_nest", () ->
            new CrowNestFeature(NoneFeatureConfiguration.CODEC));

    private GuaniaoFeatures() {
    }
}
