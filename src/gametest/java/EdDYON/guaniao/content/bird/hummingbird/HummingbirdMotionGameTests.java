package EdDYON.guaniao.content.bird.hummingbird;

import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Observe real displacement and facing, independently of the motor's internal mode selection. */
@GameTestHolder("guaniao_hummingbird_qa")
@PrefixGameTestTemplate(false)
public final class HummingbirdMotionGameTests {
    private HummingbirdMotionGameTests() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static double forwardAlignment(HummingbirdEntity bird) {
        Vec3 velocity = bird.getDeltaMovement();
        double horizontalSpeed = velocity.horizontalDistance();
        if (horizontalSpeed < 1.0E-6) return 1;
        double yaw = Math.toRadians(bird.yBodyRot);
        return (-Math.sin(yaw) * velocity.x + Math.cos(yaw) * velocity.z) / horizontalSpeed;
    }

    private static double lookingToward(HummingbirdEntity bird, Vec3 point) {
        Vec3 direction = point.subtract(bird.position());
        if (direction.horizontalDistanceSqr() < 1.0E-6) return 1;
        double yaw = Math.toRadians(bird.yBodyRot);
        return (-Math.sin(yaw) * direction.x + Math.cos(yaw) * direction.z) / direction.horizontalDistance();
    }

    private static String trace(HummingbirdEntity bird) {
        return " activity=" + bird.activity() + " pos=" + bird.position()
                + " velocity=" + bird.getDeltaMovement() + " yaw=" + bird.yBodyRot
                + " forwardDot=" + forwardAlignment(bird);
    }

    @GameTest(template = "garden", batch = "hummingbird_motion_body_heading", timeoutTicks = 40)
    public static void bodyHeadingUsesNetworkYawDuringForwardAndSidewaysMovement(GameTestHelper helper) throws Exception {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        HummingbirdEntity bird = f.actualBird(Vec3.atCenterOf(f.origin).add(0, 10, 0));
        bird.setNoAi(true);
        // Mob invokes this same callback after client movement interpolation. Movement
        // packets carry entity yaw, not yBodyRot; a passive body control leaves it stale.
        var callback = net.minecraft.world.entity.Mob.class.getDeclaredMethod("tickHeadTurn", float.class, float.class);
        callback.setAccessible(true);
        for (float yaw : new float[] {0, 90, 179, -179, -90, 0}) {
            bird.yBodyRot = 37;
            bird.setYRot(yaw);
            bird.setDeltaMovement(new Vec3(.10, 0, -.10));
            callback.invoke(bird, yaw + 90, 1F);
            check(Math.abs(Mth.wrapDegrees(bird.yBodyRot - yaw)) < .001,
                    "body follows the received heading even when velocity deliberately points sideways; yaw=" + yaw);
        }
        helper.succeed();
    }

    @GameTest(template = "garden", batch = "hummingbird_motion_garden", timeoutTicks = 220)
    public static void distantGardenTravelFacesItsActualMovement(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        BlockPos home = f.origin.offset(24, 10, 0);
        Vec3 start = Vec3.atCenterOf(f.origin.offset(-8, 10, 0));
        f.loadAround(BlockPos.containing(start)); f.loadAround(home);
        HummingbirdEntity bird = f.actualBird(start);
        // Start facing away from home. A real turn may retain lateral momentum before settling.
        bird.setYRot(90); bird.yBodyRot = 90; bird.setYHeadRot(90);
        bird.behavior.bindGarden(f.level, home);
        int[] samples = {0}, aligned = {0};
        double[] closest = {Double.MAX_VALUE};
        f.run(180, tick -> {
            check(!bird.isNoAi() && !bird.noPhysics && bird.isAlive(), "garden return uses real AI and collision");
            double distance = bird.position().distanceTo(Vec3.atCenterOf(home));
            closest[0] = Math.min(closest[0], distance);
            if (tick > 15 && distance > 18 && bird.getDeltaMovement().horizontalDistance() > .04) {
                samples[0]++;
                if (forwardAlignment(bird) > .90) aligned[0]++;
            }
            if (tick == 180) {
                check(samples[0] >= 25 && aligned[0] >= samples[0] * .80,
                        "garden return has enough real forward-facing travel samples: " + aligned[0] + "/" + samples[0]);
                check(closest[0] < 17 && bird.position().distanceTo(start) > 10,
                        "heading correction still makes substantial progress to the distant garden" + trace(bird));
            }
        });
    }

    @GameTest(template = "garden", batch = "hummingbird_motion_follow", timeoutTicks = 370)
    public static void followingMovingOwnerAllowsNearbySidewaysFlight(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        Vec3 ownerStart = Vec3.atCenterOf(f.origin).add(0, 10, 0);
        f.loadAround(BlockPos.containing(ownerStart.add(-22, 0, 0)));
        f.loadAround(BlockPos.containing(ownerStart.add(14, 0, 14)));
        var owner = f.player("HummerMotionOwner", ownerStart);
        owner.setNoGravity(true);
        // Keep this test about flight rather than letting a stationary owner become a head perch.
        owner.setShiftKeyDown(true);
        HummingbirdEntity bird = f.actualBird(ownerStart.add(-20, 1.6, 0));
        bird.tame(owner); bird.setBirdCommandMode(BirdCommandMode.FOLLOW);
        int[] farSamples = {0}, closeSamples = {0}, aligned = {0}, nearSideways = {0}, moving = {0};
        double[] closest = {Double.MAX_VALUE};
        Vec3[] previous = {bird.position()};
        f.run(330, tick -> {
            double x = Math.max(0, Math.min(80, tick - 40)) * .14;
            double z = Math.max(0, Math.min(80, tick - 120)) * .14;
            Vec3 at = ownerStart.add(x, 0, z);
            owner.setPos(at.x, at.y, at.z);
            owner.setDeltaMovement(tick > 40 && tick <= 120 ? new Vec3(.14, 0, 0)
                    : tick > 120 && tick <= 200 ? new Vec3(0, 0, .14) : Vec3.ZERO);
            check(!bird.isNoAi() && !bird.noPhysics && !bird.isPassenger(), "FOLLOW remains real autonomous flight");
            check(bird.position().distanceTo(previous[0]) < .65, "FOLLOW catches up by continuous flight rather than a position jump" + trace(bird));
            previous[0] = bird.position();
            double distance = bird.distanceTo(owner);
            if (tick > 200) closest[0] = Math.min(closest[0], distance);
            if (tick > 15 && bird.getDeltaMovement().horizontalDistance() > .04) {
                moving[0]++;
                if (distance > 6) {
                    farSamples[0]++;
                    if (forwardAlignment(bird) > .75) aligned[0]++;
                }
                if (distance < 4) closeSamples[0]++;
                if (distance < 6 && lookingToward(bird, owner.position()) > .75 && forwardAlignment(bird) < .55)
                    nearSideways[0]++;
            }
            if (tick == 330) {
                check(farSamples[0] >= 20 && closeSamples[0] >= 35,
                        "observe both catch-up and nearby movement: far=" + farSamples[0] + ", close=" + closeSamples[0]);
                check(moving[0] > 100 && aligned[0] >= farSamples[0] * .70,
                        "distant catch-up mainly flies forward after turning, while allowing transitional momentum: " + aligned[0] + "/" + farSamples[0]);
                check(nearSideways[0] >= 5, "nearby flight can keep watching the owner during side/back motion: " + nearSideways[0]);
                check(closest[0] < 4 && distance < 6, "bird catches up after the owner turns and stops" + trace(bird));
            }
        });
    }

    /** Isolate requested targets but still run the production motor, travel and world collision. */
    private static void drive(HummingbirdEntity bird, HummingbirdEcologyGameTests.Fixture f,
                              Vec3 target, Vec3 facing, boolean transit, double speed) {
        bird.setNoAi(false);
        try {
            if (transit) bird.motor.travel(target, speed);
            else bird.motor.aim(target, facing, speed);
            bird.motor.tick(f.level);
            bird.travel(Vec3.ZERO);
        } finally {
            // The automatic world tick must not choose another target between these deterministic stages.
            bird.setNoAi(true);
        }
    }

    @GameTest(template = "garden", batch = "hummingbird_motion_precision", timeoutTicks = 390)
    public static void flowerSideStepRetreatTurningAndHoverHaveDistinctMotion(GameTestHelper helper) {
        var f = new HummingbirdEcologyGameTests.Fixture(helper);
        f.day(6000);
        Vec3 start = Vec3.atCenterOf(f.origin).add(0, 10, 0);
        f.loadAround(BlockPos.containing(start.add(0, 0, 48)));
        f.loadAround(BlockPos.containing(start.add(0, 0, -48)));
        HummingbirdEntity bird = f.actualBird(start);
        bird.setNoAi(true); bird.setYRot(0); bird.yBodyRot = 0; bird.setYHeadRot(0);
        Vec3[] target = {start.add(3, 0, 0)}, face = {start.add(0, 0, 16)};
        Vec3[] beforeRetreat = {null}, stoppedAt = {null};
        Vec3[] previousPosition = {bird.position()}, previousVelocity = {bird.getDeltaMovement()};
        float[] oldYaw = {0};
        double[] peakSpeed = {0}, positioningPeak = {0}, turnMinSpeed = {Double.MAX_VALUE}, stopDrift = {0};
        double[] previousStrokeTime = {bird.getWingStrokeTime(1)};
        int[] sideways = {0}, backwards = {0}, longForward = {0};
        int[] sidePoseSamples = {0}, backwardPoseSamples = {0}, forwardPoseSamples = {0};
        int[] fasterBackwardStroke = {0}, steadyForwardStroke = {0};
        f.run(350, tick -> {
            // Entity.tick consumes the preceding motor step before this callback. Inspect its
            // public animation values before asking the motor for this tick's next movement.
            double forwardPose = bird.getForwardFlightBlend(1), backwardPose = bird.getBackwardFlightBlend(1);
            double lateralPose = bird.getLateralFlightBlend(1);
            check(forwardPose >= 0 && backwardPose >= 0 && forwardPose + backwardPose <= 1.00001
                            && Math.abs(lateralPose) <= 1.00001,
                    "smoothed direction poses remain bounded and never combine full forward and backward leans");
            double strokeTime = bird.getWingStrokeTime(1), strokeStep = strokeTime - previousStrokeTime[0];
            if (tick > 1) check(strokeStep > .049 && strokeStep < .058,
                    "each world tick advances the wing clock continuously through changes of flight direction: step=" + strokeStep);
            previousStrokeTime[0] = strokeTime;
            if (tick >= 8 && tick <= 50 && bird.getDeltaMovement().horizontalDistance() > .10
                    && Math.abs(forwardAlignment(bird)) < .45) {
                check(lateralPose > .4 && forwardPose < .4,
                        "sustained rightward flight selects the lateral pose rather than the full forward pose" + trace(bird));
                sidePoseSamples[0]++;
            }
            if (tick >= 79 && tick <= 125 && bird.getDeltaMovement().horizontalDistance() > .10
                    && forwardAlignment(bird) < -.8) {
                check(backwardPose > .5 && forwardPose < .15,
                        "sustained backwards movement selects the backwards pose after interpolation" + trace(bird));
                backwardPoseSamples[0]++;
                if (strokeStep > .052) fasterBackwardStroke[0]++;
            }
            if (tick >= 195 && tick <= 235) {
                check(forwardPose > .8 && backwardPose < .1,
                        "stable long travel settles into its forward pose" + trace(bird));
                forwardPoseSamples[0]++;
                if (Math.abs(strokeStep - .05) < .0008) steadyForwardStroke[0]++;
            }
            if (tick == 71) {
                beforeRetreat[0] = bird.position(); target[0] = bird.position().add(0, 0, -3);
                face[0] = bird.position().add(0, 0, 16);
            }
            if (tick == 141) { target[0] = bird.position(); face[0] = target[0].add(0, 0, 16); }
            if (tick == 171) { target[0] = bird.position().add(0, 0, 40); bird.activity(HummingbirdEntity.Activity.TRAVEL); }
            if (tick == 236) target[0] = bird.position().add(0, 0, -40);
            if (tick == 301) { target[0] = bird.position(); face[0] = bird.position().add(bird.getLookAngle()); }
            drive(bird, f, target[0], face[0], tick >= 171 && tick <= 300, .30);
            double speed = bird.getDeltaMovement().horizontalDistance();
            double angleStep = Math.abs(Mth.wrapDegrees(bird.yBodyRot - oldYaw[0]));
            check(bird.position().distanceTo(previousPosition[0]) < .50,
                    "all movement modes advance through real travel without jumping position" + trace(bird));
            check(bird.getDeltaMovement().subtract(previousVelocity[0]).length() < .08,
                    "a new target changes velocity continuously, including sideways/backwards transitions" + trace(bird));
            if (tick <= 140) positioningPeak[0] = Math.max(positioningPeak[0], speed);
            if (tick <= 70 && speed > .035 && Math.abs(forwardAlignment(bird)) < .45) sideways[0]++;
            if (tick > 70 && tick <= 140 && speed > .035 && forwardAlignment(bird) < -.8) backwards[0]++;
            if (tick == 70) check(bird.getX() - start.x > 2.5 && lookingToward(bird, face[0]) > .98,
                    "a three-block side flight keeps facing its observation target beyond close flower positioning" + trace(bird));
            if (tick == 140) check(beforeRetreat[0].z - bird.getZ() > 2.5 && lookingToward(bird, face[0]) > .98,
                    "a three-block backwards flight keeps watching the target without turning away" + trace(bird));
            if (tick == 170) check(speed < .015 && bird.position().distanceTo(target[0]) < .12,
                    "precision movement settles into a stationary hover" + trace(bird));
            if (tick > 190 && tick <= 235) {
                peakSpeed[0] = Math.max(peakSpeed[0], speed);
                if (speed > .15 && forwardAlignment(bird) > .90) longForward[0]++;
            }
            if (tick >= 236 && tick <= 260) turnMinSpeed[0] = Math.min(turnMinSpeed[0], speed);
            if (tick >= 236 && tick <= 275) {
                check(angleStep < 60, "a sharp course change turns over multiple ticks rather than snapping" + trace(bird));
            }
            if (tick == 300) check(bird.getDeltaMovement().z < -.15 && forwardAlignment(bird) > .90,
                    "after a half-turn, real movement follows the new forward course" + trace(bird));
            if (tick == 335) stoppedAt[0] = bird.position();
            if (tick > 335) stopDrift[0] = Math.max(stopDrift[0], bird.position().distanceTo(stoppedAt[0]));
            if (tick == 350) {
                check(sideways[0] >= 10 && backwards[0] >= 10,
                        "sustained side flight and backwards retreat both occurred over several blocks");
                check(longForward[0] >= 20 && peakSpeed[0] > .20 && turnMinSpeed[0] < peakSpeed[0] * .65,
                        "long travel accelerates, then slows down for a sharp turn; peak=" + peakSpeed[0] + ", turn=" + turnMinSpeed[0]);
                check(positioningPeak[0] < peakSpeed[0], "side/back observation flight stays slower than ordinary catch-up travel");
                check(sidePoseSamples[0] >= 8 && backwardPoseSamples[0] >= 8 && forwardPoseSamples[0] >= 20,
                        "actual entity animation getters were observed during sustained lateral, backwards and forward flight");
                check(fasterBackwardStroke[0] >= 8 && steadyForwardStroke[0] >= 20,
                        "backwards flight modestly speeds the continuous wing clock; ordinary flight returns to its normal rate");
                check(speed < .015 && stopDrift[0] < .06 && bird.isAirborne(),
                        "ending travel keeps a stable airborne hover without residual drift" + trace(bird));
            }
            oldYaw[0] = bird.yBodyRot;
            previousPosition[0] = bird.position(); previousVelocity[0] = bird.getDeltaMovement();
        });
    }
}
