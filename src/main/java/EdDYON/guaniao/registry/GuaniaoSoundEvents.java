package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GuaniaoMod.MOD_ID);

    public static final RegistryObject<SoundEvent> NIGHT_HERON_AMBIENT = register("entity.night_heron.ambient");
    public static final RegistryObject<SoundEvent> NIGHT_HERON_HURT = register("entity.night_heron.hurt");
    public static final RegistryObject<SoundEvent> NIGHT_HERON_DEATH = register("entity.night_heron.death");
    public static final RegistryObject<SoundEvent> NIGHT_HERON_ATTACK = register("entity.night_heron.attack");
    public static final RegistryObject<SoundEvent> CROW_AMBIENT = register("entity.crow.ambient");
    public static final RegistryObject<SoundEvent> CROW_HURT = register("entity.crow.hurt");
    public static final RegistryObject<SoundEvent> SPARROW_AMBIENT = register("entity.sparrow.ambient");
    public static final RegistryObject<SoundEvent> SPARROW_HURT = register("entity.sparrow.hurt");
    public static final RegistryObject<SoundEvent> SPARROW_DEATH = register("entity.sparrow.death");
    public static final RegistryObject<SoundEvent> LONG_TAILED_TIT_AMBIENT = register("entity.long_tailed_tit.ambient");
    public static final RegistryObject<SoundEvent> LONG_TAILED_TIT_HURT = register("entity.long_tailed_tit.hurt");
    public static final RegistryObject<SoundEvent> LONG_TAILED_TIT_DEATH = register("entity.long_tailed_tit.death");
    public static final RegistryObject<SoundEvent> BUDGERIGAR_AMBIENT = register("entity.budgerigar.ambient");
    public static final RegistryObject<SoundEvent> BUDGERIGAR_HURT = register("entity.budgerigar.hurt");
    public static final RegistryObject<SoundEvent> BUDGERIGAR_DEATH = register("entity.budgerigar.death");
    public static final RegistryObject<SoundEvent> BUDGERIGAR_INTERACT = register("entity.budgerigar.interact");
    public static final RegistryObject<SoundEvent> COCKATIEL_AMBIENT = register("entity.cockatiel.ambient");
    public static final RegistryObject<SoundEvent> COCKATIEL_HURT = register("entity.cockatiel.hurt");
    public static final RegistryObject<SoundEvent> COCKATIEL_DEATH = register("entity.cockatiel.death");
    public static final RegistryObject<SoundEvent> MACAW_AMBIENT = register("entity.macaw.ambient");
    public static final RegistryObject<SoundEvent> MACAW_HURT = register("entity.macaw.hurt");
    public static final RegistryObject<SoundEvent> MACAW_DEATH = register("entity.macaw.death");
    public static final RegistryObject<SoundEvent> SPOTTED_DOVE_AMBIENT = register("entity.spotted_dove.ambient");
    public static final RegistryObject<SoundEvent> SPOTTED_DOVE_HURT = register("entity.spotted_dove.hurt");
    public static final RegistryObject<SoundEvent> SPOTTED_DOVE_DEATH = register("entity.spotted_dove.death");
    public static final RegistryObject<SoundEvent> SPOTTED_DOVE_MATE = register("entity.spotted_dove.mate");
    public static final RegistryObject<SoundEvent> PIGEON_AMBIENT = register("entity.pigeon.ambient");
    public static final RegistryObject<SoundEvent> SEAGULL_AMBIENT = register("entity.seagull.ambient");

    private GuaniaoSoundEvents() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GuaniaoMod.MOD_ID, id)));
    }
}
