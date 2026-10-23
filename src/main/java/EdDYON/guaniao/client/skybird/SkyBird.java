package EdDYON.guaniao.client.skybird;

import net.minecraft.world.phys.Vec3;

public final class SkyBird {
    private final int index;
    private final Vec3 formationOffset;
    private final float wingPhase;
    private final float size;
    private final float rollOffset;

    public SkyBird(int index, Vec3 formationOffset, float wingPhase, float size, float rollOffset) {
        this.index = index;
        this.formationOffset = formationOffset;
        this.wingPhase = wingPhase;
        this.size = size;
        this.rollOffset = rollOffset;
    }

    public int index() {
        return this.index;
    }

    public Vec3 formationOffset() {
        return this.formationOffset;
    }

    public float wingPhase() {
        return this.wingPhase;
    }

    public float size() {
        return this.size;
    }

    public float rollOffset() {
        return this.rollOffset;
    }
}
