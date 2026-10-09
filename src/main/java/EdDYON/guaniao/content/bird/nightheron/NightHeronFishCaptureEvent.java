package EdDYON.guaniao.content.bird.nightheron;

import net.minecraft.world.entity.animal.AbstractFish;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

/** Server-side permission hook before direct fish conversion; this is not a death/loot event. */
public final class NightHeronFishCaptureEvent extends Event implements ICancellableEvent {
    private final NightHeronEntity bird;
    private final AbstractFish fish;
    private final HeldFishPurpose purpose;

    public NightHeronFishCaptureEvent(NightHeronEntity bird, AbstractFish fish, HeldFishPurpose purpose) {
        this.bird = bird;
        this.fish = fish;
        this.purpose = purpose;
    }

    public NightHeronEntity getBird() { return this.bird; }
    public AbstractFish getFish() { return this.fish; }
    public HeldFishPurpose getPurpose() { return this.purpose; }
}
