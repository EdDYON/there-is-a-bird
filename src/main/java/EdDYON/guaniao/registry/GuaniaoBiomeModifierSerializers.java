package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.world.GuaniaoBirdSpawnBiomeModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoBiomeModifierSerializers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            GuaniaoMod.MOD_ID
    );

    public static final RegistryObject<Codec<? extends BiomeModifier>> BIRD_SPAWNS = BIOME_MODIFIER_SERIALIZERS.register(
            "bird_spawns",
            GuaniaoBirdSpawnBiomeModifier::makeCodec
    );

    private GuaniaoBiomeModifierSerializers() {
    }
}
