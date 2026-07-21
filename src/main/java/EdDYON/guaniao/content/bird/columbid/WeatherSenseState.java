package EdDYON.guaniao.content.bird.columbid;

public enum WeatherSenseState {
    NORMAL,
    PRE_RAIN,
    RAIN,
    THUNDER;

    public static WeatherSenseState byId(int id) {
        WeatherSenseState[] values = values();
        if (id < 0 || id >= values.length) {
            return NORMAL;
        }
        return values[id];
    }
}
