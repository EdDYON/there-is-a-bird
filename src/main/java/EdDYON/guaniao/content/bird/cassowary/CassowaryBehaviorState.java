package EdDYON.guaniao.content.bird.cassowary;

public enum CassowaryBehaviorState {
    CALM,
    WATCHING,
    ALERT,
    WARNING,
    CHARGING,
    ATTACKING,
    WITHDRAWING,
    FORAGING,
    EATING,
    PECKING,
    LOOKING,
    REST_ENTER,
    RESTING,
    REST_EXIT,
    SLEEP_ENTER,
    SLEEPING,
    SLEEP_EXIT;

    public static CassowaryBehaviorState byId(int id) {
        CassowaryBehaviorState[] values = values();
        return id >= 0 && id < values.length ? values[id] : CALM;
    }
}
