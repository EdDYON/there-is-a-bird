package EdDYON.guaniao.config;

public enum BirdConfigScope {
    GLOBAL,
    WORLD;

    public static BirdConfigScope sanitize(BirdConfigScope scope) {
        return scope == null ? GLOBAL : scope;
    }
}
