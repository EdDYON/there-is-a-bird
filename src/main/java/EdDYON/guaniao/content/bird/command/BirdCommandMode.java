package EdDYON.guaniao.content.bird.command;

public enum BirdCommandMode {
    FREE,
    FOLLOW,
    STAY,
    ROOST;

    private static final BirdCommandMode[] VALUES = values();

    public BirdCommandMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public String translationKey() {
        return "message.guaniao.bird_command." + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static BirdCommandMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : FREE;
    }
}
