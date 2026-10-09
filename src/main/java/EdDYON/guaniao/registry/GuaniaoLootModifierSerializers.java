package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.enchantment.FeatherFanBookLootModifier;
import EdDYON.guaniao.content.note.BirdNoteLootModifier;
import EdDYON.guaniao.content.bird.woodcock.EarthwormLootModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class GuaniaoLootModifierSerializers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
            GuaniaoMod.MOD_ID
    );

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> BIRD_NOTE = LOOT_MODIFIER_SERIALIZERS.register(
            "bird_note",
            () -> BirdNoteLootModifier.CODEC
    );
    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> FEATHER_FAN_BOOK = LOOT_MODIFIER_SERIALIZERS.register(
            "feather_fan_book",
            () -> FeatherFanBookLootModifier.CODEC
    );

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> EARTHWORM = LOOT_MODIFIER_SERIALIZERS.register(
            "earthworm", () -> EarthwormLootModifier.CODEC);

    private GuaniaoLootModifierSerializers() {
    }
}
