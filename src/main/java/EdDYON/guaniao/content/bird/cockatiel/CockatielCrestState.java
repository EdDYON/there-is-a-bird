package EdDYON.guaniao.content.bird.cockatiel;

public enum CockatielCrestState {
    RELAXED,
    CURIOUS,
    ALERT,
    AFRAID,
    HAPPY;

    private static final CockatielCrestState[] VALUES = values();

    public static CockatielCrestState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : RELAXED;
    }
}
