package EdDYON.guaniao.content.bird.columbid;

import EdDYON.guaniao.GuaniaoMod;
import net.minecraft.resources.ResourceLocation;

public enum ColumbidVariant {
    SPOTTED_DOVE("spotted_dove", "textures/entity/spotted_dove.png", 0x7D5E4C, 0xD6C4B4),
    GRAY_PIGEON("gray_pigeon", "textures/entity/pigeon_gray.png", 0x6D7077, 0x2F3138),
    WHITE_PIGEON("white_pigeon", "textures/entity/pigeon_white.png", 0xE8E5DD, 0xC4C0B8);

    private final String id;
    private final ResourceLocation texture;
    private final int baseColor;
    private final int spotColor;

    ColumbidVariant(String id, String texturePath, int baseColor, int spotColor) {
        this.id = id;
        this.texture = new ResourceLocation(GuaniaoMod.MOD_ID, texturePath);
        this.baseColor = baseColor;
        this.spotColor = spotColor;
    }

    public String id() {
        return this.id;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public int baseColor() {
        return this.baseColor;
    }

    public int spotColor() {
        return this.spotColor;
    }

    public static ColumbidVariant byOrdinal(int ordinal, ColumbidVariant fallback) {
        ColumbidVariant[] values = ColumbidVariant.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return fallback;
        }
        return values[ordinal];
    }

    public static ColumbidVariant pigeonByOrdinal(int ordinal) {
        ColumbidVariant variant = byOrdinal(ordinal, GRAY_PIGEON);
        return variant == WHITE_PIGEON ? WHITE_PIGEON : GRAY_PIGEON;
    }
}
