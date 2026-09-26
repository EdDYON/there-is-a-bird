package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.CameraState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Full-screen optical pass: depth of field plus the selected lens distortion. */
public final class CameraOpticsShader {
    private static final float NEAR_PLANE = 0.05F;
    private static ShaderInstance shader;

    private CameraOpticsShader() {
    }

    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "camera_optics"),
                        DefaultVertexFormat.POSITION_TEX
                ),
                loaded -> shader = loaded
        );
    }

    public static boolean process(RenderTarget source, RenderTarget destination, CameraState state) {
        if (shader == null || source.getDepthTextureId() < 0 || source.width <= 0 || source.height <= 0) {
            return false;
        }

        RenderSystem.assertOnRenderThread();
        destination.bindWrite(true);
        shader.setSampler("DiffuseSampler", source.getColorTextureId());
        shader.setSampler("DepthSampler", source.getDepthTextureId());
        setUniform("OutSize", destination.width, destination.height);
        setUniform("NearPlane", NEAR_PLANE);
        float farPlane = Math.max(128.0F, Minecraft.getInstance().options.renderDistance().get() * 16.0F);
        setUniform("FarPlane", farPlane);
        setUniform("FocusDistance", (float)state.focusDistance());
        setUniform("Aperture", state.aperture().fStop());
        setUniform("FocalLength", (float)state.focalLength());
        setUniform("DofMultiplier", state.lens().depthOfFieldMultiplier());
        setUniform("LensDistortion", state.lens().distortion());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F);
        builder.addVertex(1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F);
        builder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
        builder.addVertex(-1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        return true;
    }

    private static void setUniform(String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(String name, float first, float second) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(first, second);
        }
    }
}
