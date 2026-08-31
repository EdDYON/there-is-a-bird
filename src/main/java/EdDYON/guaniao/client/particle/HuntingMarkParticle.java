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

public final class HuntingMarkParticle extends TextureSheetParticle {
    private final float baseAlpha;
    private final float baseQuadSize;

    private HuntingMarkParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.82F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 8 + level.random.nextInt(4);
        this.baseQuadSize = 0.82F + level.random.nextFloat() * 0.16F;
        this.quadSize = this.baseQuadSize * 0.82F;
        this.baseAlpha = 0.70F + level.random.nextFloat() * 0.18F;
        this.alpha = 0.0F;
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

        this.roll += 0.055F;
        float progress = Mth.clamp(this.age / (float)this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(progress * 6.0F, 0.0F, 1.0F);
        float fadeOut = 1.0F - Mth.clamp((progress - 0.48F) / 0.52F, 0.0F, 1.0F);
        this.alpha = this.baseAlpha * fadeIn * fadeOut;
        this.quadSize = this.baseQuadSize * (0.82F + Mth.sin(progress * Mth.PI) * 0.24F);
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
            return new HuntingMarkParticle(level, x, y, z,
                    xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
