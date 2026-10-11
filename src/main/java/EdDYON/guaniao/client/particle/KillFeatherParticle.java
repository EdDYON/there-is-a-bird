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

public final class KillFeatherParticle extends TextureSheetParticle {
    private final double driftX;
    private final double driftZ;
    private final double initialYSpeed;
    private final float swayPhase;
    private final float swaySpeed;
    private final float swayAmount;
    private final float spinDirection;
    private final float fallGravity;
    private final float terminalFallSpeed;
    private final float baseAlpha;
    private final float baseQuadSize;
    private final int startDelay;
    private final int fadeTicks;
    private final int groundHoldTicks;
    private final boolean fadesAfterLanding;
    private int fadeStartAge;
    private boolean started;
    private boolean landed;

    private KillFeatherParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.hasPhysics = true;
        this.startDelay = level.random.nextInt(11);
        this.fadeTicks = 10 + level.random.nextInt(13);
        this.groundHoldTicks = 4 + level.random.nextInt(13);
        this.fadesAfterLanding = level.random.nextBoolean();
        this.fadeStartAge = this.startDelay + (this.fadesAfterLanding
                ? 180
                : 24 + level.random.nextInt(25));
        this.lifetime = this.fadeStartAge + this.fadeTicks;
        this.fallGravity = 0.025F + level.random.nextFloat() * 0.035F;
        this.gravity = 0.0F;
        this.terminalFallSpeed = 0.020F + level.random.nextFloat() * 0.025F;
        this.baseQuadSize = 0.17F + level.random.nextFloat() * 0.09F;
        this.quadSize = this.baseQuadSize;
        this.baseAlpha = 0.78F + level.random.nextFloat() * 0.18F;
        this.alpha = 0.0F;

        this.driftX = (level.random.nextDouble() - 0.5D) * 0.014D;
        this.driftZ = (level.random.nextDouble() - 0.5D) * 0.014D;
        this.initialYSpeed = 0.010D + level.random.nextDouble() * 0.040D;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.swayPhase = level.random.nextFloat() * Mth.TWO_PI;
        this.swaySpeed = 0.13F + level.random.nextFloat() * 0.16F;
        this.swayAmount = 0.007F + level.random.nextFloat() * 0.017F;
        this.spinDirection = level.random.nextBoolean() ? 1.0F : -1.0F;
        this.roll = level.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.started = this.startDelay == 0;
        if (this.started) {
            this.beginFalling();
        }
    }

    @Override
    public void tick() {
        if (!this.started && this.age >= this.startDelay) {
            this.started = true;
            this.beginFalling();
        }

        super.tick();
        if (this.removed) {
            return;
        }
        if (!this.started) {
            this.alpha = 0.0F;
            return;
        }

        float swayTime = this.age * this.swaySpeed + this.swayPhase;
        this.oRoll = this.roll;
        if (this.onGround || this.landed) {
            if (!this.landed) {
                this.landed = true;
                if (this.fadesAfterLanding) {
                    this.fadeStartAge = this.age + this.groundHoldTicks;
                    this.lifetime = this.fadeStartAge + this.fadeTicks;
                }
            }
            this.xd *= 0.18D;
            this.yd = 0.0D;
            this.zd *= 0.18D;
            this.roll += this.spinDirection * 0.012F;
        } else {
            this.xd = this.driftX + Mth.sin(swayTime) * this.swayAmount;
            this.zd = this.driftZ + Mth.cos(swayTime * 0.82F) * this.swayAmount * 0.75F;
            this.yd = Math.max(this.yd, -this.terminalFallSpeed);
            this.roll += this.spinDirection * (0.035F + Mth.sin(swayTime) * 0.030F);
        }

        int visibleAge = this.age - this.startDelay;
        float fadeIn = Mth.clamp(visibleAge / 4.0F, 0.0F, 1.0F);
        if (this.age >= this.fadeStartAge) {
            float fade = Mth.clamp((this.age - this.fadeStartAge) / (float)this.fadeTicks, 0.0F, 1.0F);
            this.alpha = this.baseAlpha * fadeIn * (1.0F - smootherStep(fade));
            this.quadSize = this.baseQuadSize * (1.0F - fade * 0.12F);
        } else {
            this.alpha = this.baseAlpha * fadeIn;
        }
    }

    private void beginFalling() {
        this.gravity = this.fallGravity;
        this.xd = this.driftX;
        this.yd = this.initialYSpeed;
        this.zd = this.driftZ;
    }

    private static float smootherStep(float value) {
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
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
            return new KillFeatherParticle(level, x, y, z, this.sprites);
        }
    }
}
