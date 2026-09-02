package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.kiwi.KiwiConflictState;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class KiwiFightCommands {
    private static final String AUDIENCE_TAG = "guaniao.kiwi_fight_audience";
    private static final int MIN_CELEBRATION_TICKS = 40;
    private static final int MAX_CELEBRATION_TICKS = 60;
    private static final List<AudienceScene> ACTIVE_AUDIENCE_SCENES = new ArrayList<>();

    private KiwiFightCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("kiwiFight")
                        .executes(context -> spawnKiwiFight(context.getSource())))
                .then(Commands.literal("kiwiFightAudience")
                        .executes(context -> spawnKiwiFightAudience(context.getSource()))));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_AUDIENCE_SCENES.isEmpty()) {
            return;
        }
        Iterator<AudienceScene> iterator = ACTIVE_AUDIENCE_SCENES.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE_AUDIENCE_SCENES.clear();
    }

    private static int spawnKiwiFight(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 forward = horizontalForward(player);
        Vec3 center = player.position().add(forward.scale(4.0D));
        return spawnKiwiFightAt(level, center, new Vec3(-forward.z, 0.0D, forward.x), player.getYRot()) == null
                ? 0 : 2;
    }

    private static int spawnKiwiFightAudience(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 forward = horizontalForward(player);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 center = player.position().add(forward.scale(13.0D));

        FightPair fight = spawnKiwiFightAt(level, center, right, player.getYRot());
        if (fight == null) {
            return 0;
        }
        List<UUID> audience = spawnAudience(level, center, forward, right);
        ACTIVE_AUDIENCE_SCENES.add(new AudienceScene(
                level, fight.first().getUUID(), fight.second().getUUID(), audience));
        return 2 + audience.size();
    }

    private static Vec3 horizontalForward(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }

    private static FightPair spawnKiwiFightAt(ServerLevel level, Vec3 center, Vec3 right, float baseYaw) {
        Vec3 desiredA = center.add(right.scale(1.5D));
        Vec3 desiredB = center.subtract(right.scale(1.5D));
        BlockPos standA = findStandPosition(level, desiredA);
        BlockPos standB = findStandPosition(level, desiredB);
        if (standA == null || standB == null || standA.equals(standB)) {
            return null;
        }

        KiwiEntity first = createKiwi(level, desiredA, standA, baseYaw);
        KiwiEntity second = createKiwi(level, desiredB, standB, baseYaw + 180.0F);
        if (first == null || second == null) {
            return null;
        }

        if (!level.addFreshEntity(first)) {
            return null;
        }
        if (!level.addFreshEntity(second)) {
            first.discard();
            return null;
        }

        BlockPos homeA = BlockPos.containing(center.add(right.scale(8.0D)));
        BlockPos homeB = BlockPos.containing(center.subtract(right.scale(8.0D)));
        if (!first.startCommandConflict(second, homeA, homeB)) {
            first.discard();
            second.discard();
            return null;
        }

        return new FightPair(first, second);
    }

    private static List<UUID> spawnAudience(ServerLevel level, Vec3 center, Vec3 forward, Vec3 right) {
        List<EntityType<? extends Mob>> audienceTypes = List.of(
                GuaniaoEntityTypes.SPARROW.get(), EntityType.COW,
                GuaniaoEntityTypes.LONG_TAILED_TIT.get(), EntityType.SHEEP,
                GuaniaoEntityTypes.BUDGERIGAR.get(), EntityType.PIG,
                GuaniaoEntityTypes.COCKATIEL.get(), EntityType.CHICKEN,
                GuaniaoEntityTypes.SPOTTED_DOVE.get(), EntityType.RABBIT,
                GuaniaoEntityTypes.PIGEON.get(), EntityType.HORSE,
                GuaniaoEntityTypes.CROW.get(), EntityType.GOAT,
                GuaniaoEntityTypes.SEAGULL.get(), EntityType.LLAMA,
                GuaniaoEntityTypes.MYNA.get(), GuaniaoEntityTypes.NIGHT_HERON.get(),
                GuaniaoEntityTypes.MACAW.get()
        );
        List<UUID> spawned = new ArrayList<>();
        double angleStep = Math.PI * 2.0D / audienceTypes.size();
        double angleOffset = -Math.PI * 0.5D + angleStep * 0.5D;
        for (int index = 0; index < audienceTypes.size(); ++index) {
            double angle = angleOffset + angleStep * index;
            Vec3 radial = right.scale(Math.cos(angle)).add(forward.scale(Math.sin(angle)));
            Vec3 desired = center.add(radial.scale(10.0D));
            BlockPos stand = findStandPosition(level, desired);
            if (stand == null) {
                continue;
            }
            Mob audienceMember = spawnAudienceMob(level, audienceTypes.get(index), desired, stand, center);
            if (audienceMember != null) {
                spawned.add(audienceMember.getUUID());
            }
        }
        return spawned;
    }

    private static Mob spawnAudienceMob(ServerLevel level, EntityType<? extends Mob> type,
                                       Vec3 desired, BlockPos stand, Vec3 center) {
        Mob mob = type.create(level);
        if (mob == null) {
            return null;
        }
        Vec3 towardCenter = center.subtract(desired);
        float yaw = (float)(Math.atan2(towardCenter.z, towardCenter.x) * 180.0D / Math.PI) - 90.0F;
        mob.moveTo(desired.x, stand.getY(), desired.z, yaw, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.COMMAND, null, null);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setNoAi(true);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.addTag(AUDIENCE_TAG);
        if (!level.addFreshEntity(mob)) {
            return null;
        }
        return mob;
    }

    private static void faceAudienceMember(Mob mob, Vec3 focus) {
        Vec3 offset = focus.subtract(mob.getEyePosition());
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (horizontalDistance < 1.0E-4D) {
            return;
        }
        float yaw = (float)(Math.atan2(offset.z, offset.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float)(-Math.atan2(offset.y, horizontalDistance) * 180.0D / Math.PI);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.setXRot(Math.max(-45.0F, Math.min(45.0F, pitch)));
        Vec3 movement = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, movement.y, 0.0D);
    }

    private static void playAudienceCall(Mob mob) {
        if (mob instanceof MynaEntity myna) {
            myna.startAudienceCheerCall();
        } else {
            mob.playAmbientSound();
        }
    }

    private static boolean isResolvedState(KiwiConflictState state) {
        return state == KiwiConflictState.CHASING || state == KiwiConflictState.FLEEING;
    }

    private record FightPair(KiwiEntity first, KiwiEntity second) {
    }

    private static final class AudienceMember {
        private final UUID uuid;
        private int callCooldown;
        private int jumpCooldown;

        private AudienceMember(UUID uuid) {
            this.uuid = uuid;
        }
    }

    private static final class AudienceScene {
        private static final int MAX_SCENE_TICKS = 1200;
        private final ServerLevel level;
        private final UUID firstKiwi;
        private final UUID secondKiwi;
        private final List<AudienceMember> audience;
        private int sceneTicks;
        private int celebrationTicks;
        private boolean celebrationStarted;
        private boolean celebrationFinished;

        private AudienceScene(ServerLevel level, UUID firstKiwi, UUID secondKiwi, List<UUID> audience) {
            this.level = level;
            this.firstKiwi = firstKiwi;
            this.secondKiwi = secondKiwi;
            this.audience = audience.stream().map(AudienceMember::new).toList();
        }

        private boolean tick() {
            if (++this.sceneTicks > MAX_SCENE_TICKS) {
                this.silenceAudience();
                return false;
            }
            Entity firstEntity = this.level.getEntity(this.firstKiwi);
            Entity secondEntity = this.level.getEntity(this.secondKiwi);
            if (!(firstEntity instanceof KiwiEntity first) || !(secondEntity instanceof KiwiEntity second)
                    || !first.isAlive() || !second.isAlive()) {
                this.silenceAudience();
                return false;
            }

            Vec3 focus = new Vec3(
                    (first.getX() + second.getX()) * 0.5D,
                    (first.getEyeY() + second.getEyeY()) * 0.5D,
                    (first.getZ() + second.getZ()) * 0.5D);
            for (AudienceMember member : this.audience) {
                Mob mob = this.getAudienceMob(member);
                if (mob != null) {
                    faceAudienceMember(mob, focus);
                }
            }

            KiwiConflictState firstState = first.getConflictState();
            KiwiConflictState secondState = second.getConflictState();
            boolean conflictActive = firstState != KiwiConflictState.NONE
                    || secondState != KiwiConflictState.NONE;
            if (!this.celebrationStarted
                    && (isResolvedState(firstState) || isResolvedState(secondState))) {
                this.startCelebration();
            }
            if (this.celebrationStarted && !this.celebrationFinished) {
                this.tickCelebration();
                if (--this.celebrationTicks <= 0) {
                    this.finishCelebration();
                }
            }
            if (!this.celebrationStarted && !conflictActive) {
                this.silenceAudience();
                return false;
            }
            return !this.celebrationFinished || conflictActive;
        }

        private void startCelebration() {
            this.celebrationStarted = true;
            this.celebrationTicks = MIN_CELEBRATION_TICKS
                    + this.level.random.nextInt(MAX_CELEBRATION_TICKS - MIN_CELEBRATION_TICKS + 1);
            for (AudienceMember member : this.audience) {
                Mob mob = this.getAudienceMob(member);
                if (mob == null) {
                    continue;
                }
                mob.setSilent(false);
                member.callCooldown = 3 + mob.getRandom().nextInt(Math.max(1, this.celebrationTicks - 5));
                member.jumpCooldown = mob.getRandom().nextInt(9);
            }
        }

        private void tickCelebration() {
            for (AudienceMember member : this.audience) {
                Mob mob = this.getAudienceMob(member);
                if (mob == null) {
                    continue;
                }
                if (--member.callCooldown <= 0) {
                    playAudienceCall(mob);
                    member.callCooldown = 60 + mob.getRandom().nextInt(41);
                }
                if (--member.jumpCooldown <= 0 && mob.onGround()) {
                    mob.setDeltaMovement(0.0D, 0.32D + mob.getRandom().nextDouble() * 0.09D, 0.0D);
                    mob.hasImpulse = true;
                    mob.fallDistance = 0.0F;
                    member.jumpCooldown = 14 + mob.getRandom().nextInt(12);
                }
            }
        }

        private void finishCelebration() {
            this.celebrationFinished = true;
            this.silenceAudience();
        }

        private void silenceAudience() {
            for (AudienceMember member : this.audience) {
                Mob mob = this.getAudienceMob(member);
                if (mob != null) {
                    mob.setSilent(true);
                }
            }
        }

        private Mob getAudienceMob(AudienceMember member) {
            Entity entity = this.level.getEntity(member.uuid);
            return entity instanceof Mob mob && mob.isAlive() && mob.getTags().contains(AUDIENCE_TAG)
                    ? mob : null;
        }
    }

    private static KiwiEntity createKiwi(ServerLevel level, Vec3 desired, BlockPos stand, float yaw) {
        KiwiEntity kiwi = GuaniaoEntityTypes.KIWI.get().create(level);
        if (kiwi == null) {
            return null;
        }
        kiwi.moveTo(desired.x, stand.getY(), desired.z, yaw, 0.0F);
        kiwi.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.COMMAND, null, null);
        kiwi.setPersistenceRequired();
        return kiwi;
    }

    private static BlockPos findStandPosition(ServerLevel level, Vec3 desired) {
        BlockPos origin = BlockPos.containing(desired);
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
        for (int offset : offsets) {
            BlockPos feet = origin.offset(0, offset, 0);
            if (isOpen(level, feet)
                    && isOpen(level, feet.above())
                    && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
