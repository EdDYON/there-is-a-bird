package EdDYON.guaniao.registry;

import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create((ResourceKey)Registries.CREATIVE_MODE_TAB, (String)"guaniao");
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder().title((Component)Component.translatable((String)"itemGroup.guaniao.main")).icon(() -> new ItemStack((ItemLike)GuaniaoItems.BIRD_GUIDE.get())).displayItems((parameters, output) -> {
        output.accept((ItemLike)GuaniaoItems.BIRD_GUIDE.get());
        output.accept((ItemLike)GuaniaoItems.NIKON_D750.get());
        output.accept((ItemLike)GuaniaoItems.FILM.get());
        output.accept((ItemLike)GuaniaoItems.PHOTOGRAPH.get());
        output.accept((ItemLike)GuaniaoItems.BREADCRUMBS.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_DROPPING_1.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_DROPPING_2.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_DROPPING_3.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_DROPPING_4.get());
        output.accept((ItemLike)GuaniaoItems.SMALL_BIRD_CAGE.get());
        output.accept((ItemLike)GuaniaoItems.MEDIUM_BIRD_CAGE.get());
        output.accept((ItemLike)GuaniaoItems.LARGE_BIRD_CAGE.get());
        output.accept((ItemLike)GuaniaoItems.WOODEN_BIRD_BATH.get());
        output.accept((ItemLike)GuaniaoItems.STONE_BIRD_BATH.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_BATH.get());
        output.accept((ItemLike)GuaniaoItems.WOODEN_BIRD_BATH_2.get());
        output.accept((ItemLike)GuaniaoItems.STONE_BIRD_BATH_2.get());
        output.accept((ItemLike)GuaniaoItems.BIRD_BATH_2.get());
        output.accept((ItemLike)GuaniaoItems.CROW_NEST.get());
        output.accept((ItemLike)GuaniaoItems.NIGHT_HERON_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.SPARROW_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.LONG_TAILED_TIT_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.COCKATIEL_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.MACAW_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.BUDGERIGAR_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.SPOTTED_DOVE_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.PIGEON_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.CROW_SPAWN_EGG.get());
        output.accept((ItemLike)GuaniaoItems.SEAGULL_SPAWN_EGG.get());
    }).build());

    private GuaniaoCreativeTabs() {
    }
}
