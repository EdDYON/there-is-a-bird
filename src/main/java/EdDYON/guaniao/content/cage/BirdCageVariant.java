package EdDYON.guaniao.content.cage;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

public enum BirdCageVariant {
    SMALL("small_bird_cage", "geo/small_bird_cage.geo.json", "textures/block/small_bird_cage.png",
            Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0)),
    MEDIUM("medium_bird_cage", "geo/medium_bird_cage.geo.json", "textures/block/medium_bird_cage.png",
            Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0)),
    LARGE("large_bird_cage", "geo/large_bird_cage.geo.json", "textures/block/large_bird_cage.png",
            Block.box(0.0, 0.0, 0.0, 16.0, 48.0, 16.0));

    public static final ResourceLocation ANIMATION = new ResourceLocation(GuaniaoMod.MOD_ID, "animations/bird_cage.animation.json");

    private final String id;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final VoxelShape shape;

    BirdCageVariant(String id, String modelPath, String texturePath, VoxelShape shape) {
        this.id = id;
        this.model = new ResourceLocation(GuaniaoMod.MOD_ID, modelPath);
        this.texture = new ResourceLocation(GuaniaoMod.MOD_ID, texturePath);
        this.shape = shape;
    }

    public String id() {
        return this.id;
    }

    public ResourceLocation model() {
        return this.model;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public VoxelShape shape() {
        return this.shape;
    }
}
