package EdDYON.guaniao.content.bird.myna;

public enum MynaActionState {
    NONE,
    IDLE_1,
    IDLE_2,
    ENTERING_SLEEP,
    SLEEPING;

    public static MynaActionState byId(int id) {
        MynaActionState[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
