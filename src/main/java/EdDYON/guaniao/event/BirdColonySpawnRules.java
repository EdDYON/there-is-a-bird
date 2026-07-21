package EdDYON.guaniao.event;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Optional large flocks are limited to deterministic, habitat-specific colony
 * cells. Ordinary spawning is intentionally left unchanged outside those cells.
 */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdColonySpawnRules {
    private static final int COLONY_CELL_SIZE = 64;
    private static final int COLONY_CELL_DIVISOR = 12;
    private static final int MAX_CACHED_CELLS_PER_LEVEL = 1024;
    private static final Map<ServerLevel, LinkedHashMap<ColonyCell, HabitatSnapshot>> HABITAT_CACHE = new WeakHashMap<>();

    private BirdColonySpawnRules() {
    }

    static GroupSize groupAt(ServerLevel level, BlockPos pos, BirdSpecies species) {
        if (!BirdConfigManager.colonialMode() || !isColonialSpecies(species)) {
            return null;
        }
        ColonyCell cell = ColonyCell.from(pos);
        if (!isColonyCell(level, cell)) {
            return null;
        }
        if (!habitatAt(level, pos, cell).supports(species)) {
            return null;
        }
        return switch (species) {
            case SPARROW -> new GroupSize(9, 16);
            case LONG_TAILED_TIT -> new GroupSize(10, 16);
            case BUDGERIGAR -> new GroupSize(8, 14);
            case COCKATIEL -> new GroupSize(6, 10);
            case PIGEON -> new GroupSize(8, 14);
            case SEAGULL -> new GroupSize(8, 16);
            default -> null;
        };
    }

    private static boolean isColonialSpecies(BirdSpecies species) {
        return switch (species) {
            case SPARROW, LONG_TAILED_TIT, BUDGERIGAR, COCKATIEL, PIGEON, SEAGULL -> true;
            default -> false;
        };
    }

    private static boolean isColonyCell(ServerLevel level, ColonyCell cell) {
        long mixed = level.getSeed() ^ cell.x * 341873128712L ^ cell.z * 132897987541L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return Math.floorMod(mixed, COLONY_CELL_DIVISOR) == 0L;
    }

    private static HabitatSnapshot habitatAt(ServerLevel level, BlockPos pos, ColonyCell cell) {
        LinkedHashMap<ColonyCell, HabitatSnapshot> cachedCells = HABITAT_CACHE.computeIfAbsent(level,
                ignored -> new LinkedHashMap<>(16, 0.75F, true));
        HabitatSnapshot cached = cachedCells.get(cell);
        long gameTime = level.getGameTime();
        if (cached != null && gameTime - cached.checkedAt < BirdConfigManager.habitatCacheTicks()) {
            return cached;
        }

        HabitatSnapshot scanned = new HabitatSnapshot(gameTime,
                hasTreeCover(level, pos, 6), hasNearbyWater(level, pos, 10),
                hasSettlementMarker(level, pos, 5), isDryOpenGround(level, pos));
        cachedCells.put(cell, scanned);
        trimCache(cachedCells);
        return scanned;
    }

    private static void trimCache(LinkedHashMap<ColonyCell, HabitatSnapshot> cachedCells) {
        Iterator<ColonyCell> iterator = cachedCells.keySet().iterator();
        while (cachedCells.size() > MAX_CACHED_CELLS_PER_LEVEL && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static boolean hasTreeCover(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 8, radius))) {
            if (!isLoaded(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyWater(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (!isLoaded(level, pos)) {
                continue;
            }
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSettlementMarker(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
            if (!isLoaded(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND)
                    || state.is(Blocks.HAY_BLOCK)
                    || state.getBlock() instanceof CropBlock
                    || state.getBlock() instanceof ComposterBlock
                    || state.getBlock() instanceof FenceBlock
                    || state.getBlock() instanceof FenceGateBlock) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDryOpenGround(ServerLevel level, BlockPos pos) {
        return !level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    public static int settlementScore(ServerLevel level, BlockPos pos) {
        HabitatSnapshot snapshot = habitatAt(level, pos, ColonyCell.from(pos));
        if (snapshot.settlementScore < 0) {
            snapshot.settlementScore = scanSettlementScore(level, pos);
        }
        return snapshot.settlementScore;
    }

    public static int columbidHabitatScore(ServerLevel level, BlockPos pos, boolean urbanBias) {
        HabitatSnapshot snapshot = habitatAt(level, pos, ColonyCell.from(pos));
        if (urbanBias) {
            if (snapshot.urbanColumbidScore < 0) {
                snapshot.urbanColumbidScore = scanColumbidScore(level, pos, true);
            }
            return snapshot.urbanColumbidScore;
        }
        if (snapshot.ruralColumbidScore < 0) {
            snapshot.ruralColumbidScore = scanColumbidScore(level, pos, false);
        }
        return snapshot.ruralColumbidScore;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HABITAT_CACHE.remove(level);
        }
    }

    private static int scanSettlementScore(ServerLevel level, BlockPos origin) {
        int score = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -14; x <= 14; ++x) {
            for (int z = -14; z <= 14; ++z) {
                if (x * x + z * z > 196) {
                    continue;
                }
                for (int y = -4; y <= 4; ++y) {
                    mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!isLoaded(level, mutable)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(Blocks.FARMLAND)) score += 4;
                    else if (state.getBlock() instanceof CropBlock) score += 3;
                    else if (state.is(Blocks.HAY_BLOCK) || state.is(Blocks.COMPOSTER)) score += 5;
                    else if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof DoorBlock) score += 6;
                    else if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof FenceGateBlock) score += 3;
                    if (score >= 42) {
                        return score;
                    }
                }
            }
        }
        return score;
    }

    private static int scanColumbidScore(ServerLevel level, BlockPos origin, boolean urbanBias) {
        int score = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, -2, -7), origin.offset(7, 5, 7))) {
            if (!isLoaded(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND) || state.is(Blocks.WHEAT) || state.getBlock() instanceof CropBlock) {
                score += urbanBias ? 2 : 4;
            } else if (state.is(Blocks.SUNFLOWER) || state.is(Blocks.HAY_BLOCK) || state.getBlock() instanceof ComposterBlock) {
                score += 3;
            } else if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                score += urbanBias ? 1 : 3;
            } else if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof FenceGateBlock) {
                score += 2;
            } else if (urbanBias && (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BedBlock)) {
                score += 4;
            }
            if (score >= 24) {
                return score;
            }
        }
        return score;
    }

    private static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    record GroupSize(int min, int max) {
    }

    private record ColonyCell(long x, long z) {
        private static ColonyCell from(BlockPos pos) {
            return new ColonyCell(Math.floorDiv(pos.getX(), COLONY_CELL_SIZE), Math.floorDiv(pos.getZ(), COLONY_CELL_SIZE));
        }
    }

    private static final class HabitatSnapshot {
        private final long checkedAt;
        private final boolean treeCover;
        private final boolean nearbyWater;
        private final boolean settlement;
        private final boolean dryOpenGround;
        private int settlementScore = -1;
        private int urbanColumbidScore = -1;
        private int ruralColumbidScore = -1;

        private HabitatSnapshot(long checkedAt, boolean treeCover, boolean nearbyWater, boolean settlement, boolean dryOpenGround) {
            this.checkedAt = checkedAt;
            this.treeCover = treeCover;
            this.nearbyWater = nearbyWater;
            this.settlement = settlement;
            this.dryOpenGround = dryOpenGround;
        }

        private boolean supports(BirdSpecies species) {
            return switch (species) {
                case LONG_TAILED_TIT -> this.treeCover;
                case SEAGULL -> this.nearbyWater;
                case SPARROW, PIGEON -> this.settlement;
                case BUDGERIGAR, COCKATIEL -> this.dryOpenGround;
                default -> false;
            };
        }
    }
}
