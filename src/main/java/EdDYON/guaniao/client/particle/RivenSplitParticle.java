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

/** A small cluster of fan-shaped afterimages used only by Riven Plume. */
public final class RivenSplitParticle extends TextureSheetParticle {
    private final float baseQuadSize;
    private final float baseAlpha;
    private final float spinSpeed;

    private RivenSplitParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.93F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 12 + level.random.nextInt(7);
        this.baseQuadSize = 0.54F + level.random.nextFloat() * 0.22F;
        this.quadSize = this.baseQuadSize * 0.72F;
        this.baseAlpha = 0.62F + level.random.nextFloat() * 0.20F;
        this.alpha = 0.0F;
        this.spinSpeed = (level.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.045F + level.random.nextFloat() * 0.055F);
        this.roll = level.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) {
            return;
        }

        this.roll += this.spinSpeed;
        float progress = Mth.clamp(this.age / (float)this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(progress * 7.0F, 0.0F, 1.0F);
        float fadeOut = 1.0F - Mth.clamp((progress - 0.48F) / 0.52F, 0.0F, 1.0F);
        this.alpha = this.baseAlpha * fadeIn * fadeOut;
        this.quadSize = this.baseQuadSize * (0.72F + Mth.sin(progress * Mth.PI) * 0.42F);
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
            return new RivenSplitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
