package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuaniaoParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GuaniaoMod.MOD_ID);
    public static final RegistryObject<SimpleParticleType> KILL_FEATHER =
            PARTICLE_TYPES.register("kill_feather", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BURIAL_WIND =
            PARTICLE_TYPES.register("burial_wind", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BURIAL_CYCLONE =
            PARTICLE_TYPES.register("burial_cyclone", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> RIVEN_SPLIT =
            PARTICLE_TYPES.register("riven_split", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> RIVEN_STREAK =
            PARTICLE_TYPES.register("riven_streak", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> HUNTING_MARK =
            PARTICLE_TYPES.register("hunting_mark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> HUNTING_STREAK =
            PARTICLE_TYPES.register("hunting_streak", () -> new SimpleParticleType(false));
    private GuaniaoParticleTypes() {
    }
}
