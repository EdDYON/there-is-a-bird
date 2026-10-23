package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.world.GuaniaoBirdSpawnBiomeModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class GuaniaoBiomeModifierSerializers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            GuaniaoMod.MOD_ID
    );

    public static final Supplier<MapCodec<? extends BiomeModifier>> BIRD_SPAWNS = BIOME_MODIFIER_SERIALIZERS.register(
            "bird_spawns",
            GuaniaoBirdSpawnBiomeModifier::makeCodec
    );

    private GuaniaoBiomeModifierSerializers() {
    }
}
