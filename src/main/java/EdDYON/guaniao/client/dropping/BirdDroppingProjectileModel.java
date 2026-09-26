package EdDYON.guaniao.client.dropping;

import EdDYON.guaniao.content.dropping.BirdDroppingProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BirdDroppingProjectileModel extends GeoModel<BirdDroppingProjectileEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("guaniao", "geo/bird_dropping_projectile.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("guaniao", "textures/entity/bird_dropping_projectile.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("guaniao", "animations/bird_dropping.animation.json");

    @Override
    public ResourceLocation getModelResource(BirdDroppingProjectileEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BirdDroppingProjectileEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BirdDroppingProjectileEntity animatable) {
        return ANIMATION;
    }
}
