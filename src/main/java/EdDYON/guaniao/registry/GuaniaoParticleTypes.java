package EdDYON.guaniao.registry;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class GuaniaoParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, GuaniaoMod.MOD_ID);
    public static final Supplier<SimpleParticleType> KILL_FEATHER =
            PARTICLE_TYPES.register("kill_feather", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> BURIAL_WIND =
            PARTICLE_TYPES.register("burial_wind", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> BURIAL_CYCLONE =
            PARTICLE_TYPES.register("burial_cyclone", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> RIVEN_SPLIT =
            PARTICLE_TYPES.register("riven_split", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> RIVEN_STREAK =
            PARTICLE_TYPES.register("riven_streak", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> HUNTING_MARK =
            PARTICLE_TYPES.register("hunting_mark", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> HUNTING_STREAK =
            PARTICLE_TYPES.register("hunting_streak", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> PLACEABLE_FLECK =
            PARTICLE_TYPES.register("placeable_fleck", () -> new SimpleParticleType(false));
    private GuaniaoParticleTypes() {
    }
}
