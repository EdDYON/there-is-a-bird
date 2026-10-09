package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoBlocks;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum BirdDroppingVariant {
    ONE(0),
    TWO(1),
    THREE(2),
    FOUR(3);

    private static final BirdDroppingVariant[] VALUES = values();

    private final int id;

    BirdDroppingVariant(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    public Item item() {
        return switch (this) {
            case ONE -> GuaniaoItems.BIRD_DROPPING_1.get();
            case TWO -> GuaniaoItems.BIRD_DROPPING_2.get();
            case THREE -> GuaniaoItems.BIRD_DROPPING_3.get();
            case FOUR -> GuaniaoItems.BIRD_DROPPING_4.get();
        };
    }

    public Block stainBlock() {
        return switch (this) {
            case ONE, TWO -> GuaniaoBlocks.BIRD_DROPPING_STAIN_LIGHT.get();
            case THREE, FOUR -> GuaniaoBlocks.BIRD_DROPPING_STAIN_DARK.get();
        };
    }

    public static BirdDroppingVariant byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return ONE;
        }
        return VALUES[id];
    }

    public static BirdDroppingVariant random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }
}
