package EdDYON.guaniao.content.bird.kiwi;

public enum KiwiConflictState {
    NONE,
    WARNING,
    APPROACH,
    FIGHTING,
    CHASING,
    FLEEING;

    public static KiwiConflictState byId(int id) {
        KiwiConflictState[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
