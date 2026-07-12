package EdDYON.guaniao.content.bath;

import java.util.Locale;

public enum BirdBathContentType {
    EMPTY,
    WATER,
    FISH,
    MEAT,
    BREAD,
    FROZEN_WATER,
    SPOILED;

    public String serializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean isWaterLike() {
        return this == WATER || this == FROZEN_WATER;
    }

    public boolean isFood() {
        return this == FISH || this == MEAT || this == BREAD;
    }

    public static BirdBathContentType fromOrdinal(int ordinal) {
        BirdBathContentType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return EMPTY;
        }
        return values[ordinal];
    }
}
