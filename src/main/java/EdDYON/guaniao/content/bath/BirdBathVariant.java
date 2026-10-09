package EdDYON.guaniao.content.bath;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum BirdBathVariant {
    WOODEN_BIRD_BATH("wooden_bird_bath", "geo/wooden_bird_bath.geo.json", "textures/block/wooden_bird_bath.png", "animations/bird_bath.animation.json", SoundType.WOOD,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D)),
    STONE_BIRD_BATH("stone_bird_bath", "geo/stone_bird_bath.geo.json", "textures/block/stone_bird_bath.png", "animations/bird_bath.animation.json", SoundType.STONE,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D)),
    BIRD_BATH("bird_bath", "geo/iron_bird_bath.geo.json", "textures/block/iron_bird_bath.png", "animations/bird_bath.animation.json", SoundType.METAL,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D)),
    WOODEN_BIRD_BATH_2("wooden_bird_bath_2", "geo/wooden_bird_bath_2.geo.json", "textures/block/wooden_bird_bath_2.png", "animations/bird_bath_2.animation.json", SoundType.WOOD,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D)),
    STONE_BIRD_BATH_2("stone_bird_bath_2", "geo/stone_bird_bath_2.geo.json", "textures/block/stone_bird_bath_2.png", "animations/bird_bath_2.animation.json", SoundType.STONE,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D)),
    BIRD_BATH_2("bird_bath_2", "geo/iron_bird_bath_2.geo.json", "textures/block/iron_bird_bath_2.png", "animations/bird_bath_2.animation.json", SoundType.METAL,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D));

    private final String id;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;
    private final SoundType soundType;
    private final VoxelShape shape;

    BirdBathVariant(String id, String modelPath, String texturePath, String animationPath, SoundType soundType, VoxelShape shape) {
        this.id = id;
        this.model = new ResourceLocation(GuaniaoMod.MOD_ID, modelPath);
        this.texture = new ResourceLocation(GuaniaoMod.MOD_ID, texturePath);
        this.animation = new ResourceLocation(GuaniaoMod.MOD_ID, animationPath);
        this.soundType = soundType;
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

    public ResourceLocation animation() {
        return this.animation;
    }

    public SoundType soundType() {
        return this.soundType;
    }

    public VoxelShape shape() {
        return this.shape;
    }
}
