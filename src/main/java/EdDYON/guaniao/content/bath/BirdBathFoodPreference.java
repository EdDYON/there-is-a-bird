package EdDYON.guaniao.content.bath;

public enum BirdBathFoodPreference {
    FISH,
    MEAT,
    BREAD,
    WATER,
    ANY_FOOD;

    public boolean matches(BirdBathContentType type) {
        if (type == null) {
            return false;
        }
        return switch (this) {
            case FISH -> type == BirdBathContentType.FISH;
            case MEAT -> type == BirdBathContentType.MEAT;
            case BREAD -> type == BirdBathContentType.BREAD;
            case WATER -> type == BirdBathContentType.WATER;
            case ANY_FOOD -> type.isFood();
        };
    }
}
