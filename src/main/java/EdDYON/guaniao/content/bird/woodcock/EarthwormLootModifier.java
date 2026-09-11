package EdDYON.guaniao.content.bird.woodcock;

import EdDYON.guaniao.content.bird.BirdTags;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Adds worms to soil dug by survival players without replacing the block's normal drops. */
public final class EarthwormLootModifier extends LootModifier {
    public static final Codec<EarthwormLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, EarthwormLootModifier::new));

    private EarthwormLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (state != null && state.is(BirdTags.WOODCOCK_FORAGE_GROUND)
                && tool != null && tool.getItem() instanceof ShovelItem
                && context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player
                && !player.isCreative() && !player.isSpectator()
                && context.getRandom().nextFloat() < 0.20F) {
            generatedLoot.add(new ItemStack(GuaniaoItems.EARTHWORM.get()));
        }
        return generatedLoot;
    }
}
