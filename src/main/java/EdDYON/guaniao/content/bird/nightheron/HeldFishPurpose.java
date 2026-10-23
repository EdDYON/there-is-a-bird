package EdDYON.guaniao.content.bird.nightheron;

/** Stable save/sync IDs, independent of the current movement or work state. */
public enum HeldFishPurpose {
    NONE(0), SELF_FOOD(1), OWNER_DELIVERY(2);

    private final int id;
    HeldFishPurpose(int id) { this.id = id; }
    public int id() { return this.id; }
    public static HeldFishPurpose byId(int id) {
        return switch (id) { case 1 -> SELF_FOOD; case 2 -> OWNER_DELIVERY; default -> NONE; };
    }
}
