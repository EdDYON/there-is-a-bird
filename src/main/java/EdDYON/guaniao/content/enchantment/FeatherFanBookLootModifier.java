package EdDYON.guaniao.content.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Rarely adds one of the three feather-fan enchantment books to chests or monster drops. */
public final class FeatherFanBookLootModifier extends LootModifier {
    public static final MapCodec<FeatherFanBookLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, FeatherFanBookLootModifier::new));

    private static final float CHEST_CHANCE = 0.04F;
    private static final float MONSTER_CHANCE = 0.0025F;

    protected FeatherFanBookLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        boolean chestLoot = context.getQueriedLootTableId().getPath().startsWith("chests/");
        boolean monsterLoot = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Monster;
        float chance = chestLoot ? CHEST_CHANCE : monsterLoot ? MONSTER_CHANCE : 0.0F;
        if (chance > 0.0F && context.getRandom().nextFloat() < chance) {
            generatedLoot.add(FeatherFanEnchantmentBooks.randomBook(context.getRandom()));
        }
        return generatedLoot;
    }
}
