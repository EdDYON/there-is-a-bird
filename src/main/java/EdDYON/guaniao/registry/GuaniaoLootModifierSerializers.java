package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.enchantment.FeatherFanBookLootModifier;
import EdDYON.guaniao.content.note.BirdNoteLootModifier;
import EdDYON.guaniao.content.bird.woodcock.EarthwormLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoLootModifierSerializers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
            GuaniaoMod.MOD_ID
    );

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> BIRD_NOTE = LOOT_MODIFIER_SERIALIZERS.register(
            "bird_note",
            () -> BirdNoteLootModifier.CODEC
    );
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> FEATHER_FAN_BOOK = LOOT_MODIFIER_SERIALIZERS.register(
            "feather_fan_book",
            () -> FeatherFanBookLootModifier.CODEC
    );

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> EARTHWORM = LOOT_MODIFIER_SERIALIZERS.register(
            "earthworm", () -> EarthwormLootModifier.CODEC);

    private GuaniaoLootModifierSerializers() {
    }
}
