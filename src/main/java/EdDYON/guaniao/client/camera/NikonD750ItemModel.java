package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.NikonD750Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NikonD750ItemModel extends GeoModel<NikonD750Item> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "geo/nikon_d750.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "textures/item/nikon_d750.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "animations/nikon_d750.animation.json");

    @Override
    public ResourceLocation getModelResource(NikonD750Item animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NikonD750Item animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NikonD750Item animatable) {
        return ANIMATION;
    }
}
