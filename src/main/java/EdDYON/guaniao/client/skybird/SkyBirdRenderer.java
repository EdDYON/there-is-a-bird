package EdDYON.guaniao.client.skybird;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SkyBirdRenderer {
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private SkyBirdRenderer() {
    }

    public static void render(RenderLevelStageEvent event, SkyBirdManager manager) {
        manager.markRenderStageSeen();
        manager.setLastRenderedBirdCount(0);
        if (!manager.textureReady() || manager.flocks().isEmpty()) {
            manager.setRenderDiagnostics(0, 0, 0, 0, 0);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick();
        int renderedBirds = 0;
        int candidates = 0;
        int invalidMotion = 0;
        int distanceRejected = 0;
        int viewRejected = 0;
        int lifecycleRejected = 0;

        for (SkyBirdSpecies species : SkyBirdSpecies.values()) {
            if (!manager.textureReady(species)) {
                continue;
            }
            // Draw on the sky layer before clouds. The custom type uses no
            // mipmaps and no depth writes, so a transparent sprite cell cannot
            // punch a rectangular hole through the later cloud pass.
            RenderType renderType = SkyBirdRenderTypes.translucent(species.texture());
            VertexConsumer vertices = buffers.getBuffer(renderType);
            int renderedForSpecies = 0;

            for (SkyFlock flock : manager.flocks()) {
                if (flock.species() != species) {
                    continue;
                }
                for (SkyBird bird : flock.birds()) {
                    candidates++;
                    Vec3 forward = flock.birdRenderVelocity(bird, partialTick).normalize();
                    if (forward.lengthSqr() < 1.0E-6D) {
                        invalidMotion++;
                        continue;
                    }
                    Vec3 right = WORLD_UP.cross(forward).normalize();
                    if (right.lengthSqr() < 1.0E-6D) {
                        invalidMotion++;
                        continue;
                    }
                    Vec3 up = forward.cross(right).normalize();
                    Vec3 birdPosition = flock.birdPosition(bird, partialTick);
                    double distance = cameraPosition.distanceTo(birdPosition);
                    float distanceAlpha = distanceAlpha(
                            distance,
                            manager.farFadeStart(minecraft),
                            manager.farFadeEnd(minecraft)
                    );
                    if (distanceAlpha <= 0.0F) {
                        distanceRejected++;
                        continue;
                    }
                    float viewAlpha = viewAlpha(cameraPosition, birdPosition);
                    if (viewAlpha <= 0.0F) {
                        viewRejected++;
                        continue;
                    }
                    float lifecycleAlpha = flock.lifecycleAlpha(partialTick);
                    if (lifecycleAlpha <= 0.0F) {
                        lifecycleRejected++;
                        continue;
                    }
                    float alpha = distanceAlpha * viewAlpha * lifecycleAlpha;

                    renderBird(
                            poseStack,
                            vertices,
                            cameraPosition,
                            birdPosition,
                            forward,
                            right,
                            up,
                            animationFrame(flock, bird, partialTick),
                            flock.renderRoll(bird, partialTick),
                            alpha,
                            bird.size(),
                            species
                    );
                    renderedBirds++;
                    renderedForSpecies++;
                }
            }
            buffers.endBatch(renderType);
            if (renderedForSpecies > 0) {
                manager.recordRenderedSpecies(species, renderedForSpecies);
            }
        }
        manager.setLastRenderedBirdCount(renderedBirds);
        manager.setRenderDiagnostics(
                candidates,
                invalidMotion,
                distanceRejected,
                viewRejected,
                lifecycleRejected
        );
    }

    private static float distanceAlpha(double distance, double farStart, double farEnd) {
        float nearAlpha;
        if (distance <= SkyBirdManager.NEAR_FADE_START) {
            nearAlpha = 0.0F;
        } else if (distance < SkyBirdManager.NEAR_FADE_END) {
            nearAlpha = (float)((distance - SkyBirdManager.NEAR_FADE_START)
                    / (SkyBirdManager.NEAR_FADE_END - SkyBirdManager.NEAR_FADE_START));
        } else {
            nearAlpha = 1.0F;
        }

        float farAlpha;
        if (distance <= farStart) {
            farAlpha = 1.0F;
        } else if (distance >= farEnd) {
            farAlpha = 0.0F;
        } else {
            farAlpha = (float)((farEnd - distance) / (farEnd - farStart));
        }
        return nearAlpha * farAlpha;
    }

    /**
     * These flat atmosphere sprites are meant to be observed from below. They
     * fade only at render time when the camera reaches their altitude or sees
     * the flight plane at a very shallow angle; natural ecology keeps ticking.
     */
    private static float viewAlpha(Vec3 cameraPosition, Vec3 birdPosition) {
        double heightAboveCamera = birdPosition.y - cameraPosition.y;
        float altitudeAlpha;
        if (heightAboveCamera <= 4.0D) {
            altitudeAlpha = 0.0F;
        } else if (heightAboveCamera < 18.0D) {
            altitudeAlpha = (float)((heightAboveCamera - 4.0D) / 14.0D);
        } else {
            altitudeAlpha = 1.0F;
        }

        Vec3 toCamera = cameraPosition.subtract(birdPosition);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            return 0.0F;
        }
        double underside = -toCamera.normalize().y;
        float angleAlpha;
        if (underside <= 0.15D) {
            angleAlpha = 0.0F;
        } else if (underside < 0.30D) {
            angleAlpha = (float)((underside - 0.15D) / 0.15D);
        } else {
            angleAlpha = 1.0F;
        }
        return altitudeAlpha * angleAlpha;
    }

    private static void renderBird(PoseStack poseStack, VertexConsumer vertices,
                                   Vec3 cameraPosition, Vec3 birdPosition,
                                   Vec3 forward, Vec3 right, Vec3 up,
                                   int frame, float rollDegrees, float alpha,
                                   float individualSize, SkyBirdSpecies species) {
        double roll = Math.toRadians(rollDegrees);
        Vec3 bankedRight = right.scale(Math.cos(roll)).add(up.scale(Math.sin(roll))).normalize();
        // Forge's particles-target type culls back faces. These atmosphere birds
        // use both windings below so they remain visible from above and below.
        Vec3 surfaceNormal = forward.cross(bankedRight).scale(-1.0D).normalize();
        float halfSize = species.renderSize() * individualSize * 0.5F;

        Vec3 topLeft = birdPosition.add(forward.scale(halfSize)).subtract(bankedRight.scale(halfSize));
        Vec3 topRight = birdPosition.add(forward.scale(halfSize)).add(bankedRight.scale(halfSize));
        Vec3 bottomRight = birdPosition.subtract(forward.scale(halfSize)).add(bankedRight.scale(halfSize));
        Vec3 bottomLeft = birdPosition.subtract(forward.scale(halfSize)).subtract(bankedRight.scale(halfSize));

        // Sample texel centres instead of atlas borders. Sampling an exact frame
        // boundary can pull a beak, wing tip or tail from the neighbouring frame.
        float atlasWidth = species.textureWidthPixels();
        float u0 = (frame * SkyBirdSpecies.FRAME_PIXELS + 0.5F) / atlasWidth;
        float u1 = ((frame + 1) * SkyBirdSpecies.FRAME_PIXELS - 0.5F) / atlasWidth;
        float v0 = 0.5F / SkyBirdSpecies.FRAME_PIXELS;
        float v1 = (SkyBirdSpecies.FRAME_PIXELS - 0.5F) / SkyBirdSpecies.FRAME_PIXELS;
        int alphaByte = Mth.clamp((int)(alpha * 255.0F), 0, 255);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(vertices, matrix, normal, topLeft.subtract(cameraPosition), u0, v0, alphaByte, surfaceNormal);
        vertex(vertices, matrix, normal, bottomLeft.subtract(cameraPosition), u0, v1, alphaByte, surfaceNormal);
        vertex(vertices, matrix, normal, bottomRight.subtract(cameraPosition), u1, v1, alphaByte, surfaceNormal);
        vertex(vertices, matrix, normal, topRight.subtract(cameraPosition), u1, v0, alphaByte, surfaceNormal);

        // Submit the reverse face as well. The atmosphere sprite must remain
        // visible from every observer height even when the selected RenderType
        // performs back-face culling.
        Vec3 reverseNormal = surfaceNormal.scale(-1.0D);
        vertex(vertices, matrix, normal, topRight.subtract(cameraPosition), u1, v0, alphaByte, reverseNormal);
        vertex(vertices, matrix, normal, bottomRight.subtract(cameraPosition), u1, v1, alphaByte, reverseNormal);
        vertex(vertices, matrix, normal, bottomLeft.subtract(cameraPosition), u0, v1, alphaByte, reverseNormal);
        vertex(vertices, matrix, normal, topLeft.subtract(cameraPosition), u0, v0, alphaByte, reverseNormal);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, float u, float v, int alpha, Vec3 surfaceNormal) {
        vertices.vertex(matrix, (float)position.x, (float)position.y, (float)position.z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, (float)surfaceNormal.x, (float)surfaceNormal.y, (float)surfaceNormal.z)
                .endVertex();
    }

    private static int animationFrame(SkyFlock flock, SkyBird bird, float partialTick) {
        SkyBirdSpecies species = flock.species();
        if (species.animationStyle() == SkyBirdSpecies.AnimationStyle.COORDINATED_SLOW_FLAP) {
            float phaseTicks = bird.wingPhase()
                    * species.frameCount() * species.frameInterval() * 0.35F;
            float animationTick = flock.age() + partialTick + phaseTicks;
            return Mth.floor(animationTick / species.frameInterval()) % species.frameCount();
        }
        if (species.animationStyle() == SkyBirdSpecies.AnimationStyle.BURST_FLAP_WITH_SHORT_GLIDE) {
            int cycle = species.flapCycle(flock.seed(), bird.index());
            if (cycle <= 0) {
                return 0;
            }
            int phaseOffset = Mth.floor(bird.wingPhase() * cycle);
            int cycleTick = Math.floorMod(flock.age() + phaseOffset, cycle);
            int flapDuration = species.frameCount() * species.frameInterval() * 3;
            if (cycleTick >= flapDuration) {
                return 0;
            }
            return (cycleTick / species.frameInterval()) % species.frameCount();
        }
        if (species.animationStyle() == SkyBirdSpecies.AnimationStyle.GLIDE_WITH_OCCASIONAL_FLAP) {
            int cycle = species.flapCycle(flock.seed(), bird.index());
            if (cycle <= 0) {
                return 0;
            }
            int phaseOffset = Mth.floor(bird.wingPhase() * cycle);
            int cycleTick = Math.floorMod(flock.age() + phaseOffset, cycle);
            int flapDuration = species.frameCount() * species.frameInterval();
            if (cycleTick >= flapDuration) {
                return 0;
            }
            return Math.min(cycleTick / species.frameInterval(), species.frameCount() - 1);
        }

        float phaseTicks = bird.wingPhase() * species.frameCount() * species.frameInterval();
        float animationTick = flock.age() + partialTick + phaseTicks;
        return Mth.floor(animationTick / species.frameInterval()) % species.frameCount();
    }

}
