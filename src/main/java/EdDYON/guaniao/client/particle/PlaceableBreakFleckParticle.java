package EdDYON.guaniao.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class PlaceableBreakFleckParticle extends TextureSheetParticle {
    private float spinSpeed;

    private PlaceableBreakFleckParticle(ClientLevel level, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed,
                                        SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.hasPhysics = true;
        this.gravity = 0.62F;
        this.friction = 0.76F;
        this.lifetime = 11 + level.random.nextInt(9);
        this.quadSize = 0.045F + level.random.nextFloat() * 0.045F;
        this.roll = level.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spinSpeed = (level.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.14F + level.random.nextFloat() * 0.20F);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) {
            return;
        }

        this.roll += this.spinSpeed;
        if (this.onGround) {
            this.xd *= 0.58D;
            this.zd *= 0.58D;
            this.spinSpeed *= 0.72F;
        }
        float fadeStart = this.lifetime * 0.65F;
        if (this.age > fadeStart) {
            this.alpha = Mth.clamp((this.lifetime - this.age) / (this.lifetime - fadeStart), 0.0F, 1.0F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                 double x, double y, double z,
                                                 double xSpeed, double ySpeed, double zSpeed) {
            return new PlaceableBreakFleckParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
