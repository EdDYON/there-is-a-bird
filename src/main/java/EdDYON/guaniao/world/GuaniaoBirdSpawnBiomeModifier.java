package EdDYON.guaniao.world;

import EdDYON.guaniao.registry.GuaniaoBiomeModifierSerializers;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public final class GuaniaoBirdSpawnBiomeModifier implements BiomeModifier {
    public GuaniaoBirdSpawnBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD) {
            GuaniaoBirdSpawnRegistry.addBiomeSpawns(biome, builder);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return GuaniaoBiomeModifierSerializers.BIRD_SPAWNS.get();
    }

    public static MapCodec<GuaniaoBirdSpawnBiomeModifier> makeCodec() {
        return MapCodec.unit(GuaniaoBirdSpawnBiomeModifier::new);
    }
}
