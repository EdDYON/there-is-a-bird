package EdDYON.guaniao.content.bird.kiwi;

public enum KiwiBehaviorState {
    AWAKE,
    LISTENING,
    FORAGING,
    IDLE_VARIATION,
    PECKING,
    RETURNING_HOME,
    SEEKING_SHELTER,
    ENTERING_SLEEP,
    SLEEPING,
    GROUND_ESCAPE;

    public static KiwiBehaviorState byId(int id) {
        KiwiBehaviorState[] values = values();
        return id >= 0 && id < values.length ? values[id] : AWAKE;
    }
}
