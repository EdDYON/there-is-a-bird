package EdDYON.guaniao.world;

import EdDYON.guaniao.registry.GuaniaoBiomeModifierSerializers;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

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
    public Codec<? extends BiomeModifier> codec() {
        return GuaniaoBiomeModifierSerializers.BIRD_SPAWNS.get();
    }

    public static Codec<GuaniaoBirdSpawnBiomeModifier> makeCodec() {
        return Codec.unit(GuaniaoBirdSpawnBiomeModifier::new);
    }
}
