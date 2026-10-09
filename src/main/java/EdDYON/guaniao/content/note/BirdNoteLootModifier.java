package EdDYON.guaniao.content.note;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Adds a bird note to eligible structure-chest loot. */
public final class BirdNoteLootModifier extends LootModifier {
    public static final Codec<BirdNoteLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, BirdNoteLootModifier::new));

    protected BirdNoteLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ItemStack note = BirdNoteContent.roll(context);
        if (!note.isEmpty()) {
            generatedLoot.add(note);
        }
        return generatedLoot;
    }
}
