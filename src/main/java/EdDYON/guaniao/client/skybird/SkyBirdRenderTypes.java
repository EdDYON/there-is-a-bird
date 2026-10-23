package EdDYON.guaniao.client.skybird;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Render types used by the client-only sky ecology layer. */
final class SkyBirdRenderTypes extends RenderType {
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT = Util.memoize(texture -> {
        CompositeState state = CompositeState.builder()
                // These vertices already include the event's camera rotation.
                // Use the entity pipeline, not the chunk/water pipeline. Keep
                // a vanilla shader so shader loaders can provide their entity
                // equivalent instead of suppressing an unknown custom shader.
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                // Sky-bird strips are tiny pixel-art atlases. Mipmaps average a
                // whole 32x32 frame into a translucent rectangle at long range.
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                // Sky birds are drawn before clouds. Keep them on the main
                // target and never write the quad's transparent pixels into
                // depth, otherwise Fabulous cloud composition sees a 32x32
                // occluder and leaves a sky-coloured hole around the bird.
                .setOutputState(MAIN_TARGET)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create(
                "guaniao_sky_bird_translucent",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                2_097_152,
                true,
                true,
                state
        );
    });

    private SkyBirdRenderTypes() {
        super(
                "guaniao_sky_bird_internal",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                () -> { },
                () -> { }
        );
        throw new IllegalStateException("Static utility class");
    }

    static RenderType translucent(ResourceLocation texture) {
        return TRANSLUCENT.apply(texture);
    }
}
