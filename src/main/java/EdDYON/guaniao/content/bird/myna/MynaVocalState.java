package EdDYON.guaniao.content.bird.myna;

public enum MynaVocalState {
    NONE,
    STARTING,
    SPEAKING,
    ENDING;

    public static MynaVocalState byId(int id) {
        MynaVocalState[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
