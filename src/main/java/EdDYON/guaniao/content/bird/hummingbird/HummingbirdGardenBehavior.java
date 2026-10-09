package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.config.HummingbirdConfig;
import EdDYON.guaniao.content.bath.BirdBathBlockEntity;
import EdDYON.guaniao.content.bird.BirdFoodSafety;
import EdDYON.guaniao.content.bird.BirdScanBudget;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.UUID;
import static EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity.Activity;

/** Nectar transactions, owner commands and territorial behavior share a single flight motor. */
final class HummingbirdGardenBehavior {
    private final HummingbirdEntity bird;
    @Nullable private BlockPos garden, linkedBath;
    @Nullable private HummingbirdSites.Perch anchor, landing;
    @Nullable private HummingbirdSites.Perch cachedTree;
    @Nullable private HummingbirdSites.Perch departedTree;
    @Nullable private Vec3 treeSearchCenter;
    @Nullable private BlockPos failedTree;
    @Nullable private FlowerHabitatIndex.NectarTarget flower;
    @Nullable private BlockPos drinkingBath;
    @Nullable private Vec3 destination, aim, retreat, panicTarget;
    @Nullable private Vec3 nectarApproach;
    @Nullable private Vec3 browseCenter;
    @Nullable private Vec3 nightSearchCenter, nightSearchTarget;
    private int nightSearchTicks, nightTargetTicks, nightSearchStep;
    private boolean waitingForTree, waitingAligned, nightLiftPending;
    private Vec3 ownerOffset = new Vec3(1.8, 1.7, 0);
    private int ownerOffsetTicks;
    @Nullable private HummingbirdEntity rival;
    @Nullable private Vec3 territoryCenter;
    private double territoryPhase;
    @Nullable private ResourceLocation lastFlower;
    @Nullable private ResourceLocation gardenDimension;
    @Nullable private BlockPos restoredPerch, deliveryBath;
    @Nullable private BlockPos blockedDeliveryBath;
    @Nullable private BlockPos approachingDeliveryBath;
    @Nullable private Vec3 deliveryApproach;
    @Nullable private Vec3 progressTarget;
    private double closestTargetDistance;
    private int stalledTravelTicks, blockedDeliveryTicks, deliverySearchDelay;
    private int ticks, scanDelay, nectarAge, visitCount, seedCooldown = 12000, territoryCooldown, territoryTicks,
            panicTicks, perchTicks, ownerStillTicks, ownerPerchCooldown, bathCooldown,
            treeSearchCooldown, failedTreeTicks, nectarRestTicks, treeDepartureTicks, freeFlightTicks;
    private boolean asleepRestored;

    HummingbirdGardenBehavior(HummingbirdEntity bird) {
        this.bird = bird;
        this.seedCooldown = HummingbirdConfig.seedMinTicks();
    }
    boolean hasAnchor() { return anchor != null; }
    void placeOnGround(HummingbirdSites.Perch perch) {
        release(); anchor = perch; landing = null; destination = aim = null;
        panicTicks = ownerStillTicks = 0; ownerPerchCooldown = 100; perchTicks = 100;
        state(Activity.PERCH);
    }
    private void state(Activity state) {
        if (bird.activity() != state) { bird.activity(state); ticks = 0; }
    }
    void bindGarden(ServerLevel level, BlockPos at) {
        BirdBathBlockEntity bath = HummingbirdSites.bath(level, Vec3.atCenterOf(at), false);
        garden = bath == null ? at.immutable() : bath.getBlockPos().immutable();
        linkedBath = bath == null ? null : bath.getBlockPos().immutable();
        gardenDimension = level.dimension().location();
        bird.setBirdCommandMode(BirdCommandMode.FREE);
    }
    void commandChanged() {
        release(); anchor = landing = null; destination = aim = null; ownerStillTicks = 0; scanDelay = 0;
        if (bird.isPassenger()) bird.stopRiding();
        state(Activity.TAKEOFF);
    }
    void panic(@Nullable Entity attacker) {
        commandChanged(); panicTicks = 80;
        Vec3 away = attacker == null ? new Vec3(1, 0, 0) : bird.position().subtract(attacker.position());
        if (away.horizontalDistanceSqr() < 0.001) away = new Vec3(1, 0, 0);
        panicTarget = bird.position().add(away.normalize().scale(6)).add(0, 2, 0);
        state(Activity.PANIC);
    }
    void tick(ServerLevel level) {
        ticks++;
        if (scanDelay > 0) scanDelay--;
        if (territoryCooldown > 0) territoryCooldown--;
        if (bathCooldown > 0) bathCooldown--;
        if (ownerPerchCooldown > 0) ownerPerchCooldown--;
        if (deliverySearchDelay > 0) deliverySearchDelay--;
        if (blockedDeliveryTicks > 0 && --blockedDeliveryTicks == 0) blockedDeliveryBath = null;
        if (treeSearchCooldown > 0) treeSearchCooldown--;
        if (nectarRestTicks > 0) nectarRestTicks--;
        if (freeFlightTicks > 0 && --freeFlightTicks == 0) scanDelay = 0;
        if (treeDepartureTicks > 0 && --treeDepartureTicks == 0) departedTree = null;
        if (failedTreeTicks > 0 && --failedTreeTicks == 0) failedTree = null;
        if (gardenDimension != null && !gardenDimension.equals(level.dimension().location())) {
            garden = linkedBath = null; gardenDimension = null;
        }
        boolean night = level.dimensionType().hasSkyLight() && level.isNight();
        if (!night) {
            resetNightSearch();
            if (waitingForTree) {
                waitingForTree = false;
                if (bird.getBirdCommandMode() == BirdCommandMode.FREE || bird.getBirdCommandMode() == BirdCommandMode.FOLLOW) {
                    anchor = landing = null; state(Activity.TAKEOFF);
                }
            }
        }
        if (browseCenter == null) browseCenter = bird.position();
        if (restoredPerch != null) {
            HummingbirdSites.Perch saved = HummingbirdSites.at(level, bird, restoredPerch, night);
            restoredPerch = null;
            if (saved != null && (bird.getBirdCommandMode() != BirdCommandMode.FREE
                    || HummingbirdSites.isTreeSupport(level, saved.block()))
                    && bird.position().distanceToSqr(saved.feet()) < .01) {
                anchor = saved; perchTicks = 120;
                state(night && asleepRestored ? Activity.SLEEP : Activity.PERCH);
            }
        }
        Player owner = bird.getOwner() instanceof Player p && p.isAlive() && !p.isSpectator() ? p : null;
        if (bird.isPassenger()) {
            if (!(bird.getVehicle() instanceof Player p) || p != owner || night || !mayRemainHeadPerch(p)
                    || bird.getBirdCommandMode() != BirdCommandMode.FOLLOW) {
                bird.stopRiding(); anchor = null; state(Activity.TAKEOFF);
            } else { state(Activity.HEAD_PERCH); bird.motor.clear(); return; }
        }
        if (panicTicks > 0) {
            HummingbirdPollinationService.remove(bird);
            panicTicks--; anchor = landing = null; state(Activity.PANIC);
            bird.motor.travel(panicTarget == null ? bird.position().add(0, 2, 0) : panicTarget, 0.50); return;
        }
        if (anchor != null && !HummingbirdSites.valid(level, bird, anchor, night && !waitingForTree)) {
            anchor = null; landing = null; waitingForTree = false; state(Activity.TAKEOFF);
        }
        if (night || bird.getBirdCommandMode() == BirdCommandMode.STAY || bird.getBirdCommandMode() == BirdCommandMode.ROOST) {
            if (freeFlightTicks > 0) { freeFlightTicks = 0; scanDelay = 0; }
            cancelNectar(); endTerritory(); HummingbirdPollinationService.remove(bird);
            resting(level, night); return;
        }
        if (bird.isSleeping()) { state(Activity.WAKE); bird.motor.clear(); return; }
        if (bird.activity() == Activity.WAKE) {
            bird.motor.clear(); if (ticks >= 10) { anchor = null; state(Activity.TAKEOFF); } return;
        }
        if (lastFlower != null && bird.getBirdCommandMode() == BirdCommandMode.FREE
                && bird.getHeldGardenItem().isEmpty() && seedCooldown > 0) seedCooldown--;
        if (anchor != null) {
            HummingbirdPollinationService.remove(bird);
            bird.motor.clear(); state(Activity.PERCH);
            if (--perchTicks <= 0) {
                if (HummingbirdSites.isTreeSupport(level, anchor.block())) {
                    departedTree = anchor; treeDepartureTicks = 120;
                    if (bird.getBirdCommandMode() == BirdCommandMode.FREE) freeFlightTicks = 80 + bird.getRandom().nextInt(21);
                }
                anchor = null; state(Activity.TAKEOFF);
            } return;
        }
        if (bird.activity() == Activity.TAKEOFF) {
            bird.motor.aim(bird.position().add(0, 0.4, 0), bird.position().add(bird.getLookAngle()), 0.16);
            if (ticks >= 6) { state(Activity.HOVER); destination = null; } return;
        }
        if (territoryTicks > 0) { territory(level); return; }
        if (flower != null || drinkingBath != null || bird.activity() == Activity.NECTAR_EXIT) {
            if (bird.getBirdCommandMode() == BirdCommandMode.FOLLOW && owner != null && bird.distanceToSqr(owner) > 100) {
                cancelNectar(); state(Activity.HOVER); destination = null;
            } else {
            nectar(level); return;
            }
        }
        if (bird.getBirdCommandMode() == BirdCommandMode.FOLLOW && owner != null) {
            if (ownerStillTicks > 40 && scanDelay == 0 && BirdScanBudget.tryAcquire(level, bird)) {
                scanDelay = 40;
                flower = FlowerHabitatIndex.find(level, bird.position(), 8, bird.getUUID())
                        .filter(t -> t.contact().distanceToSqr(owner.position()) <= 36)
                        .filter(t -> HummingbirdFlightMotor.visible(level, bird.getEyePosition(), t.contact(), bird))
                        .orElse(null);
                if (flower != null && FlowerHabitatIndex.claim(level, flower, bird.getUUID())) {
                    destination = flower.hover(); aim = flower.contact(); nectarAge = 0; state(Activity.TRAVEL); ticks = 0;
                    bird.motor.aim(destination, aim, .28); return;
                }
                flower = null;
            }
            follow(owner); return;
        }
        if (bird.getBirdCommandMode() == BirdCommandMode.FOLLOW && owner == null) {
            // An unloaded/offline owner is not a request to force-load or teleport.
            bird.motor.aim(bird.position(), bird.position().add(bird.getLookAngle()), 0.12); state(Activity.HOVER); return;
        }
        if (landing != null) {
            if (!HummingbirdSites.valid(level, bird, landing, false)
                    || !HummingbirdSites.isTreeSupport(level, landing.block())) {
                landing = null; destination = aim = null;
            } else { land(level, false); return; }
        }
        if (!bird.getHeldGardenItem().isEmpty() && deliver(level)) return;
        if (scanDelay == 0 && BirdScanBudget.tryAcquire(level, bird)) {
            scanDelay = 40 + bird.getRandom().nextInt(20);
            if (tryTerritory(level)) return;
            if (tempt(level)) return;
            Vec3 center = garden == null ? browseCenter : Vec3.atCenterOf(garden);
            if (bird.position().distanceToSqr(center) > 16 * 16) {
                destination = center.add(0, 1.5, 0); aim = null; state(Activity.TRAVEL);
            } else {
                nectarApproach = null;
                flower = nectarRestTicks == 0 ? FlowerHabitatIndex.find(level, bird.position(), 12, bird.getUUID())
                        .filter(target -> garden == null || target.contact().distanceToSqr(center) <= 16 * 16)
                        .orElse(null) : null;
                if (flower != null && !HummingbirdFlightMotor.visible(level, bird.getEyePosition(), flower.contact(), bird)) {
                    nectarApproach = treeNectarApproach(level, flower);
                    if (nectarApproach == null) flower = null;
                }
                if (flower != null && FlowerHabitatIndex.claim(level, flower, bird.getUUID())) {
                    destination = flower.hover(); aim = flower.contact(); nectarAge = 0; state(Activity.TRAVEL);
                    ticks = 0;
                } else {
                    flower = null; nectarApproach = null;
                    BirdBathBlockEntity bath = bathCooldown == 0 && nectarRestTicks == 0
                            ? HummingbirdSites.bath(level, center, true) : null;
                    if (bath != null && bath.tryClaimUse(bird.getUUID(), HummingbirdConfig.nectarTicks() + 140)) {
                        drinkingBath = bath.getBlockPos().immutable();
                        destination = HummingbirdSites.bathHover(bath, bird.position());
                        aim = Vec3.atCenterOf(drinkingBath).add(0, 1.05, 0);
                        nectarAge = 0; state(Activity.TRAVEL);
                        ticks = 0;
                    } else {
                        if (freeFlightTicks > 0) {
                            browseTreeCrown(level);
                            landing = null;
                        } else landing = findTree(level, garden == null ? bird.position() : center);
                        if (landing != null) { destination = landing.feet(); aim = landing.facing(); state(Activity.LAND); }
                        else if (freeFlightTicks == 0 && (destination == null || bird.position().distanceToSqr(destination) < 0.04)) {
                        if (bird.getRandom().nextInt(5) == 0) {
                            // Watch a fixed point while browsing sideways/backwards around the current hover.
                            destination = bird.position().add((bird.getRandom().nextDouble() - .5) * 2.4,
                                    (bird.getRandom().nextDouble() - .5) * .7,
                                    (bird.getRandom().nextDouble() - .5) * 2.4);
                            aim = bird.getEyePosition().add(bird.getLookAngle().scale(4)); state(Activity.HOVER);
                        } else {
                            double morning = level.getDayTime() % 24000 < 3000 ? 1.4 : 1;
                            destination = center.add((bird.getRandom().nextDouble() - .5) * 8 * morning,
                                    1 + bird.getRandom().nextDouble() * 2,
                                    (bird.getRandom().nextDouble() - .5) * 8 * morning);
                            aim = null; state(Activity.TRAVEL);
                        }
                        }
                    }
                }
            }
        }
        if (landing != null) {
            land(level, false); return;
        }
        if (destination != null && travelStalled(destination, .04)) {
            // An obstructed browsing point must not remain the sole target indefinitely.
            destination = aim = null; scanDelay = 0; state(Activity.HOVER);
            resetTravelProgress();
        }
        if (destination == null) destination = bird.position().add(0, bird.onGround() ? .6 : 0, 0);
        if (aim == null) bird.motor.travel(destination, .28);
        else bird.motor.aim(destination, aim, .28);
    }
    private void browseTreeCrown(ServerLevel level) {
        if (destination != null && bird.position().distanceToSqr(destination) > .04) return;
        Vec3 crown = departedTree == null ? bird.position() : departedTree.feet();
        destination = bird.position(); aim = bird.getEyePosition().add(bird.getLookAngle().scale(4));
        // Short, bounded flights stay above the tree we actually left; the garden's
        // ground-level browse center must not pull this rest break down to the floor.
        for (int attempt = 0; attempt < 4; attempt++) {
            double phase = bird.getRandom().nextDouble() * Math.PI * 2;
            double radius = 1 + bird.getRandom().nextDouble() * 1.2;
            Vec3 candidate = crown.add(Math.cos(phase) * radius, .8 + bird.getRandom().nextDouble() * .45,
                    Math.sin(phase) * radius);
            if (!HummingbirdFlightMotor.visible(level, bird.getEyePosition(), candidate.add(0, bird.getEyeHeight(), 0), bird)
                    || !level.noCollision(bird, bird.getBoundingBox().move(candidate.subtract(bird.position())))) continue;
            destination = candidate;
            if (bird.getRandom().nextBoolean()) aim = null;
            state(Activity.TRAVEL); return;
        }
        state(Activity.HOVER);
    }
    private void resting(ServerLevel level, boolean night) {
        if (anchor != null) {
            bird.motor.clear(); bird.setNectarProgress(0);
            if (night && waitingForTree) {
                // A safe support is a place to wait awake, never a substitute sleeping tree.
                state(Activity.PERCH);
                if (scanDelay == 0 && BirdScanBudget.tryAcquire(level, bird)) {
                    scanDelay = 100;
                    landing = findTree(level, bird.position());
                    if (landing != null) {
                        anchor = null; waitingForTree = false; resetNightSearch();
                        land(level, true); return;
                    }
                }
                if (ticks >= 200) {
                    anchor = null; waitingForTree = false; nightSearchTicks = nightTargetTicks = 0;
                    nightSearchTarget = null; nightLiftPending = true; scanDelay = 0; state(Activity.TAKEOFF);
                }
                return;
            }
            resetNightSearch();
            if (night) {
                if (bird.activity() != Activity.SLEEP_ENTER && bird.activity() != Activity.SLEEP) state(Activity.SLEEP_ENTER);
                if (ticks >= 20) state(Activity.SLEEP);
            } else if (bird.activity() == Activity.SLEEP || bird.activity() == Activity.SLEEP_ENTER) state(Activity.WAKE);
            else if (bird.activity() != Activity.WAKE || ticks >= 10) state(Activity.PERCH);
            return;
        }
        if (night) nightSearchTicks++;
        if (landing == null && scanDelay == 0 && BirdScanBudget.tryAcquire(level, bird)) {
            scanDelay = 20;
            Vec3 center = bird.getBirdCommandMode() == BirdCommandMode.ROOST && garden != null
                    ? Vec3.atCenterOf(garden) : bird.position();
            if (night && nightSearchCenter != null) center = bird.position();
            landing = night ? findTree(level, center) : HummingbirdSites.perch(level, bird, center, false);
            if (night && landing == null && nightSearchTicks >= 180) {
                landing = awakeWaitingPerch(level);
                waitingForTree = landing != null && !HummingbirdSites.isTreeSupport(level, landing.block());
                waitingAligned = false;
            }
        }
        if (landing != null) {
            if (!HummingbirdSites.valid(level, bird, landing, night && !waitingForTree)) {
                landing = null; waitingForTree = false; bird.motor.clear(); return;
            }
            land(level, night && !waitingForTree);
        } else if (night) {
            // High-flying birds may start above the ten-block support scan. Descend
            // in small checked steps after an unsuccessful search instead of orbiting forever.
            Vec3 below = bird.position().add(0, -1.5, 0);
            if (nightSearchTicks >= 180 && nightWaypointClear(level, below)) {
                nightSearchTarget = null; nightTargetTicks = 0;
                state(Activity.TRAVEL); bird.motor.travel(below, .14); return;
            }
            searchForSleepingTree(level);
        } else { state(Activity.HOVER); bird.motor.aim(bird.position(), bird.position().add(bird.getLookAngle()), .12); }
    }
    @Nullable private HummingbirdSites.Perch awakeWaitingPerch(ServerLevel level) {
        // Prefer the reachable support beneath us over a decorative perch behind a wall.
        for (int depth = 0; depth < 10; depth++) {
            var perch = HummingbirdSites.at(level, bird, bird.blockPosition().below(depth), false);
            if (perch != null && !perch.delicate() && nightWaypointClear(level, perch.feet())) return perch;
        }
        var perch = HummingbirdSites.perch(level, bird, bird.position(), false);
        return perch != null && !perch.delicate() && nightWaypointClear(level, perch.feet()) ? perch : null;
    }
    private void searchForSleepingTree(ServerLevel level) {
        if (nightSearchCenter == null) { nightSearchCenter = bird.position(); nightLiftPending = true; }
        if (nightSearchTarget != null && (++nightTargetTicks >= 80
                || bird.position().distanceToSqr(nightSearchTarget) < .04
                || !nightWaypointClear(level, nightSearchTarget))) nightSearchTarget = null;
        if (nightSearchTarget == null && nightTargetTicks % 20 == 0) {
            // Lift off first. Afterwards rotate short search legs around a fixed origin,
            // so a treeless night neither pins us to the floor nor sends us wandering away.
            Vec3 lift = bird.position().add(0, 1.2, 0);
            if (nightLiftPending) {
                nightLiftPending = false;
                if (nightWaypointClear(level, lift)) nightSearchTarget = lift;
            }
            if (nightSearchTarget == null) for (int attempt = 0; attempt < 4; attempt++) {
                double angle = (bird.getId() % 8 + nightSearchStep++) * Math.PI / 4;
                double radius = 4 + ((nightSearchStep / 8) & 1) * 4;
                Vec3 candidate = nightSearchCenter.add(Math.cos(angle) * radius, 1.2, Math.sin(angle) * radius);
                if (bird.position().distanceToSqr(candidate) < .16 || !nightWaypointClear(level, candidate)) continue;
                nightSearchTarget = candidate; break;
            }
            if (nightSearchTarget != null) { nightTargetTicks = 0; nightSearchStep = Math.max(1, nightSearchStep); }
        }
        if (nightSearchTarget == null) {
            nightTargetTicks++; state(Activity.HOVER); bird.motor.clear();
        } else { state(Activity.TRAVEL); bird.motor.travel(nightSearchTarget, .18); }
    }
    private boolean nightWaypointClear(ServerLevel level, Vec3 candidate) {
        var feet = FlowerHabitatIndex.readyState(level, BlockPos.containing(candidate));
        var head = FlowerHabitatIndex.readyState(level, BlockPos.containing(candidate.add(0, bird.getBbHeight(), 0)));
        if (feet == null || head == null || !feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        // The ray checks every affected chunk before inspecting collision shapes.
        if (!HummingbirdFlightMotor.visible(level, bird.getEyePosition(), candidate.add(0, bird.getEyeHeight(), 0), bird)) return false;
        var box = bird.getBoundingBox().move(candidate.subtract(bird.position()));
        return level.getWorldBorder().isWithinBounds(box) && level.noCollision(bird, box);
    }
    private void resetNightSearch() {
        nightSearchCenter = nightSearchTarget = null;
        nightSearchTicks = nightTargetTicks = nightSearchStep = 0;
        nightLiftPending = false;
    }
    @Nullable private HummingbirdSites.Perch findTree(ServerLevel level, Vec3 center) {
        boolean sameArea = treeSearchCenter != null && center.distanceToSqr(treeSearchCenter) <= 16;
        if (sameArea && cachedTree != null && HummingbirdSites.valid(level, bird, cachedTree, true)) return cachedTree;
        if (sameArea && treeSearchCooldown > 0) return null;
        // Both callers already hold the shared scan token. Requiring another token here
        // would permanently exclude tree searches at the lowest supported budget.
        treeSearchCenter = center; treeSearchCooldown = 60;
        cachedTree = HummingbirdSites.treePerch(level, bird, center, failedTree);
        return cachedTree;
    }
    private void land(ServerLevel level, boolean night) {
        if (bird.position().distanceToSqr(landing.feet()) < .0025) {
            anchor = landing; landing = null; destination = aim = null;
            if (waitingForTree && nightSearchCenter != null) {
                nightSearchCenter = new Vec3(nightSearchCenter.x, anchor.feet().y, nightSearchCenter.z);
                nightSearchTarget = null; nightTargetTicks = 0;
            }
            perchTicks = 240 + bird.getRandom().nextInt(240);
            state(night ? Activity.SLEEP_ENTER : Activity.PERCH); bird.motor.clear(); return;
        }
        boolean tree = HummingbirdSites.isTreeSupport(level, landing.block());
        if ((tree || waitingForTree) && bird.activity() == Activity.LAND && ticks > 240) {
            if (tree) {
                failedTree = landing.block(); failedTreeTicks = 600; cachedTree = null; treeSearchCooldown = 0;
            }
            waitingForTree = false;
            landing = null; destination = aim = null; state(Activity.HOVER); bird.motor.clear(); return;
        }
        state(Activity.LAND);
        Vec3 feet = landing.feet();
        double horizontal = bird.position().subtract(feet).horizontalDistanceSqr();
        if (waitingForTree) {
            // Align above the support before descending. Diagonal arrival can hit
            // the floor early and keep invoking the motor's upward collision detour.
            if (horizontal > .04) waitingAligned = false;
            if (horizontal < .0025 && bird.getDeltaMovement().horizontalDistanceSqr() < .0004) waitingAligned = true;
            Vec3 approach = new Vec3(feet.x, Math.max(bird.getY(), feet.y + .6), feet.z);
            bird.motor.aim(!waitingAligned && nightWaypointClear(level, approach) ? approach : feet,
                    landing.facing(), .12);
        } else if (tree && horizontal > .16) {
            // Climb outside the crown before crossing it, rather than driving through leaves from below.
            Vec3 approach = bird.getY() < feet.y + .65
                    ? new Vec3(bird.getX(), feet.y + .8, bird.getZ()) : feet.add(0, .8, 0);
            bird.motor.travel(approach, .24);
        } else bird.motor.aim(feet, landing.facing(), .20);
    }
    private boolean feedingValid(ServerLevel level) {
        if (flower != null) return FlowerHabitatIndex.claim(level, flower, bird.getUUID())
                && (nectarApproach == null
                    ? HummingbirdFlightMotor.visible(level, bird.getEyePosition(), flower.contact(), bird)
                    : nectarApproachClear(level, flower, nectarApproach));
        BirdBathBlockEntity bath = HummingbirdSites.bathAt(level, drinkingBath);
        return bath != null && bath.hasUsableSugarWater() && bath.isOccupiedBy(bird.getUUID());
    }
    @Nullable private Vec3 treeNectarApproach(ServerLevel level, FlowerHabitatIndex.NectarTarget target) {
        if (treeDepartureTicks == 0 || departedTree == null
                || !HummingbirdSites.valid(level, bird, departedTree, true)
                || bird.position().distanceToSqr(departedTree.feet()) > 16) return null;
        // Only our recently departed crown may obscure the direct ray. A wall is
        // still a rejected target, rather than permission to fly over a building.
        ClipContext ray = new ClipContext(bird.getEyePosition(), target.contact(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bird);
        // Collision shapes can inspect their neighbors, not just the traversed
        // block. Check the same loaded rectangle as the normal nectar rays first.
        int minX = Mth.floor(Math.min(ray.getFrom().x, ray.getTo().x) - 1) >> 4;
        int maxX = Mth.floor(Math.max(ray.getFrom().x, ray.getTo().x) + 1) >> 4;
        int minZ = Mth.floor(Math.min(ray.getFrom().z, ray.getTo().z) - 1) >> 4;
        int maxZ = Mth.floor(Math.max(ray.getFrom().z, ray.getTo().z) + 1) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            if (level.getChunkSource().getChunkNow(x, z) == null) return null;
        boolean blockedElsewhere = BlockGetter.traverseBlocks(ray.getFrom(), ray.getTo(), ray, (context, pos) -> {
            var block = FlowerHabitatIndex.readyState(level, pos);
            if (block == null) return Boolean.TRUE;
            if (context.getBlockShape(block, level, pos).clip(context.getFrom(), context.getTo(), pos) == null) return null;
            boolean ownCrown = pos.distSqr(departedTree.block()) <= 25
                    && (block.is(BlockTags.LEAVES) || block.is(BlockTags.LOGS))
                    && HummingbirdSites.isTreeSupport(level, pos);
            return ownCrown ? null : Boolean.TRUE;
        }, context -> Boolean.FALSE);
        if (blockedElsewhere) return null;
        Vec3 aboveFlower = new Vec3(target.hover().x,
                Math.max(bird.getY() + .75, departedTree.feet().y + .8), target.hover().z);
        return nectarApproachClear(level, target, aboveFlower) ? aboveFlower : null;
    }
    private boolean nectarApproachClear(ServerLevel level, FlowerHabitatIndex.NectarTarget target, Vec3 approach) {
        Vec3 eye = approach.add(0, bird.getEyeHeight(), 0);
        return HummingbirdFlightMotor.visible(level, bird.getEyePosition(), eye, bird)
                && HummingbirdFlightMotor.visible(level, eye, target.contact(), bird)
                && level.noCollision(bird, bird.getBoundingBox().move(approach.subtract(bird.position())));
    }
    private void nectar(ServerLevel level) {
        if (bird.activity() == Activity.NECTAR_EXIT) {
            bird.setNectarProgress(Math.max(0, 1 - ticks / 8F));
            bird.motor.aim(retreat == null ? bird.position() : retreat, aim, .15);
            if (ticks >= 8) { state(Activity.HOVER); bird.setNectarProgress(0); destination = null; scanDelay = 10; }
            return;
        }
        if (!feedingValid(level) || destination == null || ticks > HummingbirdConfig.nectarTicks() + 160) {
            cancelNectar(); state(Activity.HOVER); destination = null; return;
        }
        if (nectarApproach != null) {
            bird.motor.travel(nectarApproach, .28);
            if (bird.position().distanceToSqr(nectarApproach) > .0036 || bird.getDeltaMovement().lengthSqr() > .0016) return;
            nectarApproach = null;
        }
        bird.motor.aim(destination, aim, .20);
        if (flower != null && ticks % 40 == 0 && !FlowerHabitatIndex.claim(level, flower, bird.getUUID())) {
            cancelNectar(); state(Activity.HOVER); destination = null; return;
        }
        if (bird.position().distanceToSqr(destination) > .0036 || bird.getDeltaMovement().lengthSqr() > .0016) {
            if (bird.activity() == Activity.NECTAR_ENTER || bird.activity() == Activity.NECTAR_LOOP) {
                cancelNectar(); state(Activity.HOVER); destination = null;
            }
            return;
        }
        Vec3 look = aim.subtract(bird.position());
        float desiredYaw = (float)(Mth.atan2(look.z, look.x) * 180 / Math.PI) - 90;
        if (Math.abs(Mth.wrapDegrees(desiredYaw - bird.getYRot())) > 12) return;
        if (bird.activity() == Activity.TRAVEL) state(Activity.NECTAR_ENTER);
        if (bird.activity() == Activity.NECTAR_ENTER) {
            bird.setNectarProgress(Math.min(1, ticks / 6F));
            if (ticks >= 6) { state(Activity.NECTAR_LOOP); nectarAge = 0; }
        }
        if (bird.activity() == Activity.NECTAR_LOOP) {
            bird.setNectarProgress(1);
            if (flower != null) HummingbirdPollinationService.activate(bird);
            if (++nectarAge < HummingbirdConfig.nectarTicks()) return;
            boolean completed;
            if (flower != null) {
                completed = FlowerHabitatIndex.complete(level, flower, bird.getUUID(), bird.getRandom());
                if (completed) {
                    browseCenter = Vec3.atCenterOf(flower.anchor());
                    visitCount = Math.min(1000, visitCount + 1);
                    lastFlower = BuiltInRegistries.BLOCK.getKey(flower.flower());
                    collectAfterFlower();
                }
            } else {
                BirdBathBlockEntity bath = HummingbirdSites.bathAt(level, drinkingBath);
                completed = bath != null && bath.hasUsableSugarWater() && bath.isOccupiedBy(bird.getUUID()) && bath.consumeOneServing();
                bathCooldown = 1200;
            }
            if (!completed) HummingbirdPollinationService.remove(bird);
            else {
                HummingbirdPollinationService.activate(bird);
                nectarRestTicks = 160 + bird.getRandom().nextInt(160);
            }
            Vec3 away = bird.position().subtract(aim);
            retreat = bird.position().add(new Vec3(away.x, 0, away.z).normalize().scale(.40));
            releaseNectarLease(); state(Activity.NECTAR_EXIT);
        }
    }
    private void collectAfterFlower() {
        // A completed real visit is required even after the saved daytime work timer expires.
        if (!bird.getHeldGardenItem().isEmpty() || visitCount < 3 || seedCooldown > 0) return;
        seedCooldown = HummingbirdConfig.seedMinTicks() + bird.getRandom().nextInt(
                HummingbirdConfig.seedMaxTicks() - HummingbirdConfig.seedMinTicks() + 1);
        if (bird.getRandom().nextDouble() >= HummingbirdConfig.seedChance()) return;
        Block last = lastFlower == null ? null : BuiltInRegistries.BLOCK.get(lastFlower);
        if (last != null && FlowerHabitatIndex.isVanillaFlower(last) && last.asItem() != Items.AIR) {
            bird.setHeldGardenItem(new ItemStack(last.asItem()));
        }
    }
    private boolean deliver(ServerLevel level) {
        if (bird.activity() == Activity.DROP_SEED) {
            bird.motor.aim(bird.position(), aim, .10);
            if (ticks < 8) return true;
            ItemEntity item = new ItemEntity(level, bird.getX(), bird.getY() + .20, bird.getZ(), bird.getHeldGardenItem().copy());
            item.setDeltaMovement(0, .03, 0); item.setDefaultPickUpDelay();
            if (level.addFreshEntity(item)) bird.setHeldGardenItem(ItemStack.EMPTY);
            destination = null; approachingDeliveryBath = null; deliveryApproach = null;
            resetTravelProgress(); state(Activity.HOVER); return true;
        }
        BirdBathBlockEntity bath = HummingbirdSites.bathAt(level, linkedBath);
        if (bath != null && bath.getBlockPos().equals(blockedDeliveryBath)) bath = null;
        if (bath == null) bath = HummingbirdSites.bathAt(level, deliveryBath);
        if (bath != null && bath.getBlockPos().equals(blockedDeliveryBath)) bath = null;
        if (bath == null && deliverySearchDelay == 0 && BirdScanBudget.tryAcquire(level, bird)) {
            deliverySearchDelay = 100;
            bath = HummingbirdSites.bath(level, bird.position(), false);
            if (bath != null && bath.getBlockPos().equals(blockedDeliveryBath)) bath = null;
            deliveryBath = bath == null ? null : bath.getBlockPos().immutable();
        }
        if (bath == null) { approachingDeliveryBath = null; deliveryApproach = null; return false; }
        if (!bath.getBlockPos().equals(approachingDeliveryBath) || deliveryApproach == null) {
            approachingDeliveryBath = bath.getBlockPos().immutable();
            deliveryApproach = HummingbirdSites.bathHover(bath, bird.position());
            resetTravelProgress();
        }
        Vec3 at = deliveryApproach;
        if (!HummingbirdFlightMotor.visible(level, bird.getEyePosition(), at.add(0, bird.getEyeHeight(), 0), bird)
                || !level.noCollision(bird, bird.getBoundingBox().move(at.subtract(bird.position())))
                || travelStalled(at, .01)) {
            // Keep the carried flower and resume normal activity while this bath is inaccessible.
            blockedDeliveryBath = bath.getBlockPos().immutable(); blockedDeliveryTicks = 200;
            deliveryBath = approachingDeliveryBath = null; deliveryApproach = null;
            destination = aim = null; scanDelay = 0;
            resetTravelProgress(); state(Activity.HOVER); return false;
        }
        aim = Vec3.atCenterOf(bath.getBlockPos()).add(0, 1, 0);
        bird.motor.aim(at, aim, .28);
        state(Activity.TRAVEL);
        if (bird.position().distanceToSqr(at) < .01) {
            state(Activity.DROP_SEED);
        }
        return true;
    }
    private boolean travelStalled(Vec3 target, double arrivalDistanceSquared) {
        double distance = bird.position().distanceToSqr(target);
        if (!target.equals(progressTarget) || distance < arrivalDistanceSquared || distance < closestTargetDistance - .01) {
            progressTarget = target; closestTargetDistance = distance; stalledTravelTicks = 0;
        } else stalledTravelTicks++;
        return stalledTravelTicks >= 100;
    }
    private void resetTravelProgress() {
        progressTarget = null; stalledTravelTicks = 0;
    }
    private boolean tempt(ServerLevel level) {
        if (bird.isTame()) return false;
        Player tempting = level.getEntitiesOfClass(Player.class, bird.getBoundingBox().inflate(8),
                p -> !p.isSpectator() && (BirdFoodSafety.matchesClean(HummingbirdEntity.TEMPT, p.getMainHandItem())
                        || BirdFoodSafety.matchesClean(HummingbirdEntity.TEMPT, p.getOffhandItem())))
                .stream().min(java.util.Comparator.comparingDouble(bird::distanceToSqr)).orElse(null);
        if (tempting == null) return false;
        destination = tempting.getEyePosition().add(tempting.getLookAngle().scale(1.1)).add(0, -.27, 0);
        aim = tempting.getEyePosition(); state(bird.position().distanceToSqr(destination) <= .64 ? Activity.HOVER : Activity.TRAVEL);
        bird.motor.aim(destination, aim, .22); return true;
    }
    private void follow(Player owner) {
        if (owner.getDeltaMovement().horizontalDistanceSqr() < .0025 && !owner.isShiftKeyDown()) ownerStillTicks++;
        else ownerStillTicks = 0;
        if (ownerStillTicks > 200 && mayHeadPerch(owner) && owner.getPassengers().isEmpty()) {
            Vec3 head = owner.position().add(bird.ownerHeadOffset(owner.getYHeadRot(), owner.getXRot()));
            bird.motor.aim(head, owner.getEyePosition(), .20);
            if (bird.position().distanceToSqr(head) < .01 && bird.startRiding(owner, true)) {
                state(Activity.HEAD_PERCH); bird.syncHeadPassenger(owner);
            }
            return;
        }
        if (--ownerOffsetTicks <= 0) {
            double phase = bird.getRandom().nextDouble() * Math.PI * 2;
            double radius = 1.3 + bird.getRandom().nextDouble() * .9;
            ownerOffset = new Vec3(Math.cos(phase) * radius, owner.getEyeHeight() + .1 + bird.getRandom().nextDouble() * .25,
                    Math.sin(phase) * radius);
            ownerOffsetTicks = 50 + bird.getRandom().nextInt(60);
        }
        Vec3 at = owner.position().add(ownerOffset);
        double speed = bird.distanceToSqr(owner) > 100 ? .45 : .28;
        state(bird.position().distanceToSqr(at) <= .64 ? Activity.HOVER : Activity.TRAVEL);
        if (bird.distanceToSqr(owner) > 36) bird.motor.travel(at, speed);
        else bird.motor.aim(at, owner.getEyePosition(), speed);
    }
    private boolean mayHeadPerch(Player owner) {
        return ownerPerchCooldown == 0 && !owner.isShiftKeyDown() && !owner.isPassenger()
                && owner.getDeltaMovement().lengthSqr() < .02 && mayRemainHeadPerch(owner);
    }
    private boolean mayRemainHeadPerch(Player owner) {
        if (!(bird.level() instanceof ServerLevel level)) return false;
        Vec3 head = owner.position().add(bird.ownerHeadOffset(owner.getYHeadRot(), owner.getXRot()));
        BlockPos at = BlockPos.containing(head);
        for (int x = (at.getX() - 1) >> 4; x <= (at.getX() + 1) >> 4; x++)
            for (int z = (at.getZ() - 1) >> 4; z <= (at.getZ() + 1) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return level.noCollision(bird, bird.getBoundingBox().move(head.subtract(bird.position())))
                && owner.isAlive() && !owner.isSpectator() && !owner.isSleeping()
                && !owner.isFallFlying() && !owner.isInWaterOrBubble();
    }
    private boolean tryTerritory(ServerLevel level) {
        if (territoryCooldown > 0 || bird.getBirdCommandMode() != BirdCommandMode.FREE) return false;
        var other = level.getEntitiesOfClass(HummingbirdEntity.class, bird.getBoundingBox().inflate(6),
                b -> b != bird && b.isAlive() && !b.isNoAi() && !b.isBirdEmergencyOverrideActive() && b.isAirborne() && b.behavior.territoryCooldown == 0
                        && b.getBirdCommandMode() == BirdCommandMode.FREE && !b.isNectarFeeding())
                .stream().filter(b -> bird.getUUID().compareTo(b.getUUID()) < 0).findFirst().orElse(null);
        if (other == null) return false;
        double chance = bird.isMale() && other.isMale() ? HummingbirdConfig.territoryMaleChance() : HummingbirdConfig.territoryFemaleChance();
        territoryCooldown = other.behavior.territoryCooldown = HummingbirdConfig.territoryCooldownTicks();
        if (bird.getRandom().nextDouble() >= chance) return false;
        cancelNectar(); other.behavior.cancelNectar();
        rival = other; other.behavior.rival = bird;
        territoryCenter = other.behavior.territoryCenter = bird.position().add(other.position()).scale(.5);
        territoryPhase = Math.atan2(bird.getZ() - territoryCenter.z, bird.getX() - territoryCenter.x);
        other.behavior.territoryPhase = Math.atan2(other.getZ() - territoryCenter.z, other.getX() - territoryCenter.x);
        territoryTicks = other.behavior.territoryTicks = 60 + bird.getRandom().nextInt(21);
        state(Activity.TERRITORY); other.behavior.state(Activity.TERRITORY);
        HummingbirdPollinationService.remove(bird); HummingbirdPollinationService.remove(other);
        return true;
    }
    private void territory(ServerLevel level) {
        if (rival == null || !rival.isAlive() || rival.level() != level || bird.distanceToSqr(rival) > 20 * 20
                || rival.getBirdCommandMode() != BirdCommandMode.FREE || territoryCenter == null) { endTerritory(); return; }
        if (--territoryTicks <= 0) { endTerritory(); return; }
        territoryPhase += .10;
        Vec3 at = territoryCenter.add(Math.cos(territoryPhase) * 3,
                .5 + Math.sin(territoryPhase * .5) * .35, Math.sin(territoryPhase) * 3);
        state(Activity.TERRITORY); bird.motor.travel(at, .55);
    }
    private void endTerritory() {
        HummingbirdEntity other = rival; rival = null; territoryTicks = 0; territoryCenter = null;
        if (other != null && other.behavior.rival == bird) {
            other.behavior.rival = null; other.behavior.territoryTicks = 0; other.behavior.territoryCenter = null;
            other.behavior.state(Activity.HOVER);
        }
        if (bird.activity() == Activity.TERRITORY) state(Activity.HOVER);
    }
    private void releaseNectarLease() {
        if (bird.level() instanceof ServerLevel level) {
            if (flower != null) FlowerHabitatIndex.release(level, flower, bird.getUUID());
            BirdBathBlockEntity bath = HummingbirdSites.bathAt(level, drinkingBath);
            if (bath != null) bath.releaseUse(bird.getUUID());
        }
        flower = null; drinkingBath = null; nectarApproach = null;
    }
    private void cancelNectar() {
        releaseNectarLease(); bird.setNectarProgress(0); HummingbirdPollinationService.remove(bird);
        if (bird.activity() == Activity.NECTAR_ENTER || bird.activity() == Activity.NECTAR_LOOP || bird.activity() == Activity.NECTAR_EXIT) state(Activity.HOVER);
    }
    void release() {
        cancelNectar(); endTerritory(); bird.motor.clear(); freeFlightTicks = 0;
        approachingDeliveryBath = null; deliveryApproach = null;
        resetTravelProgress();
        waitingForTree = waitingAligned = false; resetNightSearch();
    }
    void save(CompoundTag tag) {
        if (garden != null) tag.putLong("Garden", garden.asLong());
        if (linkedBath != null) tag.putLong("LinkedBath", linkedBath.asLong());
        if (gardenDimension != null) tag.putString("GardenDimension", gardenDimension.toString());
        if (anchor != null && !waitingForTree) tag.putLong("Perch", anchor.block().asLong());
        tag.putInt("SeedCooldown", seedCooldown); tag.putInt("TerritoryCooldown", territoryCooldown);
        tag.putInt("BathCooldown", bathCooldown); tag.putInt("FlowerVisits", visitCount);
        if (lastFlower != null) tag.putString("LastFlower", lastFlower.toString());
        tag.putBoolean("HummingbirdSleeping", bird.isSleeping());
    }
    void load(CompoundTag tag) {
        garden = tag.contains("Garden") ? BlockPos.of(tag.getLong("Garden")) : null;
        linkedBath = tag.contains("LinkedBath") ? BlockPos.of(tag.getLong("LinkedBath")) : null;
        gardenDimension = ResourceLocation.tryParse(tag.getString("GardenDimension"));
        restoredPerch = tag.contains("Perch") ? BlockPos.of(tag.getLong("Perch")) : null;
        asleepRestored = tag.getBoolean("HummingbirdSleeping");
        seedCooldown = Math.max(0, Math.min(HummingbirdConfig.seedMaxTicks(), tag.contains("SeedCooldown") ? tag.getInt("SeedCooldown") : HummingbirdConfig.seedMinTicks()));
        territoryCooldown = Math.max(0, Math.min(HummingbirdConfig.territoryCooldownTicks(), tag.getInt("TerritoryCooldown")));
        bathCooldown = Math.max(0, Math.min(1200, tag.getInt("BathCooldown")));
        visitCount = Math.max(0, Math.min(1000, tag.getInt("FlowerVisits")));
        lastFlower = ResourceLocation.tryParse(tag.getString("LastFlower"));
        browseCenter = null;
        // Targets/leases are never restored; a real block must be found and validated after reload.
        anchor = landing = null; flower = null; drinkingBath = null; state(Activity.HOVER);
        cachedTree = departedTree = null; treeSearchCenter = null; failedTree = null; nectarApproach = null;
        deliveryBath = blockedDeliveryBath = null; deliverySearchDelay = blockedDeliveryTicks = 0;
        approachingDeliveryBath = null; deliveryApproach = null;
        resetTravelProgress();
        treeSearchCooldown = failedTreeTicks = nectarRestTicks = treeDepartureTicks = freeFlightTicks = 0;
        waitingForTree = waitingAligned = false; resetNightSearch();
    }
}
