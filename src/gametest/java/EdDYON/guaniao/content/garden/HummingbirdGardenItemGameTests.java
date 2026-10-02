package EdDYON.guaniao.content.garden;

import EdDYON.guaniao.content.bath.BirdBathAttraction;
import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathFoodPreference;
import EdDYON.guaniao.registry.GuaniaoBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("guaniao_hummingbird_garden_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdGardenItemGameTests {
    private static void check(boolean value, String message) {
        if (!value) throw new GameTestAssertException(message);
    }

    private static BirdBathBlockEntity bath() {
        return new BirdBathBlockEntity(BlockPos.ZERO, GuaniaoBlocks.BIRD_BATH.get().defaultBlockState());
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sugarWaterPreservesLegacySavesAndPortions(GameTestHelper helper) {
        String[] legacy = {"EMPTY", "WATER", "FISH", "MEAT", "BREAD", "FROZEN_WATER", "SPOILED"};
        for (int i = 0; i < legacy.length; i++) {
            check(BirdBathContentType.fromOrdinal(i).name().equals(legacy[i]), "legacy bath ordinal " + i);
            CompoundTag old = new CompoundTag();
            old.putInt("ContentType", i); old.putInt("ContentLevel", i == 0 ? 0 : 3);
            BirdBathBlockEntity restored = bath(); restored.load(old);
            check(restored.getContentType().name().equals(legacy[i]), "load legacy bath ordinal " + i);
            if (i == 5) check(!restored.containsSugarWater(), "old ice never acquires sugar");
        }
        check(BirdBathContentType.SUGAR_WATER.ordinal() == 7, "sugar appended after all legacy values");
        BirdBathBlockEntity bath = bath(); bath.setContent(BirdBathContentType.WATER, 3);
        check(bath.sweetenWater() && bath.getContentLevel() == 3, "sweetening preserves three portions");
        check(!bath.hasUsableWater() && !bath.hasUsableFood(), "sugar is not ordinary bird water or food");
        for (BirdBathFoodPreference preference : BirdBathFoodPreference.values())
            check(!bath.hasFoodForBird(preference), "ordinary food preference rejects sugar: " + preference);
        check(!BirdBathAttraction.consumeServingForBird(bath), "ordinary warmup cannot consume freshly sweetened water");
        check(!bath.canAccept(BirdBathContentType.WATER), "water cannot dilute sugar");
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        check(bath.tryClaimUse(first, 80) && !bath.tryClaimUse(second, 80), "one nectar visitor at a time");
        check(bath.hasUsableSugarWater() && bath.consumeOneServing() && bath.getContentLevel() == 2, "one completed visit costs one portion");
        bath.releaseUse(first); check(bath.tryClaimUse(second, 80), "release permits next visitor");
        BirdBathBlockEntity restored = bath(); restored.load(bath.saveWithoutMetadata());
        check(restored.getContentType() == BirdBathContentType.SUGAR_WATER && restored.getContentLevel() == 2,
                "sugar contents and portions survive saving");
        CompoundTag frozen = restored.saveWithoutMetadata();
        frozen.putInt("ContentType", BirdBathContentType.FROZEN_WATER.ordinal());
        frozen.putBoolean("FrozenSugarWater", true); restored.load(frozen);
        check(restored.isFrozen() && restored.containsSugarWater() && !restored.hasUsableSugarWater()
                && !restored.consumeOneServing() && !restored.canAccept(BirdBathContentType.WATER), "frozen sugar cannot be consumed or diluted");
        check(restored.saveWithoutMetadata().getBoolean("FrozenSugarWater"), "frozen sugar marker round trips");
        frozen.putInt("ContentType", BirdBathContentType.SPOILED.ordinal());
        frozen.putInt("SpoiledContentType", BirdBathContentType.SUGAR_WATER.ordinal()); restored.load(frozen);
        check(restored.isSpoiled() && restored.getRenderContentType() == BirdBathContentType.SUGAR_WATER,
                "spoiled sugar retains liquid render identity");
        check(restored.cleanByHand() && restored.isEmpty(), "clearing spoiled sugar empties the basin");
        helper.succeed();
    }

}
