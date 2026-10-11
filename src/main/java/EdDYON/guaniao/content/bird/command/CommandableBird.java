package EdDYON.guaniao.content.bird.command;

public interface CommandableBird {
    String COMMAND_MODE_NBT_KEY = "CommandMode";

    BirdCommandMode getBirdCommandMode();

    void setBirdCommandMode(BirdCommandMode mode);

    default boolean isBirdEmergencyOverrideActive() {
        return false;
    }

    default boolean isBirdCommandMode(BirdCommandMode mode) {
        return this.getBirdCommandMode() == mode;
    }
}
