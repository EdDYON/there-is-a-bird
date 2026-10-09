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

public final class HuntingStreakParticle extends TextureSheetParticle {
    private final float baseAlpha;
    private final float baseQuadSize;
    private final float spinSpeed;

    private HuntingStreakParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.88F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 8 + level.random.nextInt(5);
        this.baseQuadSize = 0.46F + level.random.nextFloat() * 0.18F;
        this.quadSize = this.baseQuadSize;
        this.baseAlpha = 0.65F + level.random.nextFloat() * 0.22F;
        this.alpha = 0.0F;
        this.spinSpeed = (level.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.018F + level.random.nextFloat() * 0.022F);
        this.roll = level.random.nextInt(8) * (Mth.PI / 4.0F);
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
        float fadeIn = Mth.clamp(progress * 8.0F, 0.0F, 1.0F);
        float fadeOut = 1.0F - Mth.clamp((progress - 0.38F) / 0.62F, 0.0F, 1.0F);
        this.alpha = this.baseAlpha * fadeIn * fadeOut;
        this.quadSize = this.baseQuadSize * (1.0F - progress * 0.28F);
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
            return new HuntingStreakParticle(level, x, y, z,
                    xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
