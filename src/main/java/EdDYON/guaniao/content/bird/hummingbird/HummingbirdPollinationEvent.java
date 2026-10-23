package EdDYON.guaniao.content.bird.hummingbird;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

/** Automation permission hook, not a player bone-meal or natural random-growth event. */
public final class HummingbirdPollinationEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos target;
    private final BlockState state;
    private final long round;
    private final List<HummingbirdPollinationService.Source> sources;

    public HummingbirdPollinationEvent(ServerLevel level, BlockPos target, BlockState state, long round,
                                      List<HummingbirdPollinationService.Source> sources) {
        this.level = level; this.target = target.immutable(); this.state = state; this.round = round;
        this.sources = List.copyOf(sources);
    }
    public ServerLevel getLevel() { return level; }
    public BlockPos getTarget() { return target; }
    public BlockState getState() { return state; }
    public long getRound() { return round; }
    public List<HummingbirdPollinationService.Source> getSources() { return sources; }
    public Set<UUID> getOwners() {
        return sources.stream().map(HummingbirdPollinationService.Source::owner)
                .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
}
