package EdDYON.guaniao.content.bird.nightheron;

import java.util.EnumSet;
import java.util.List;
import EdDYON.guaniao.content.bird.nightheron.NightHeronBehaviorState;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronFlightController;
import EdDYON.guaniao.content.bird.nightheron.NightHeronLandingSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class NightHeronFrightGoal
extends Goal {
    private final NightHeronEntity nightHeron;
    private Player threat;
    private Vec3 externalThreatPosition;
    private Vec3 escapeDirection = Vec3.ZERO;
    private Response response = Response.NONE;
    private int remainingTicks;
    private BlockPos landingTarget;

    public NightHeronFrightGoal(NightHeronEntity nightHeron) {
        this.nightHeron = nightHeron;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        this.threat = this.findNearestRelevantPlayer();
        this.externalThreatPosition = this.nightHeron.getExternalFrightSource();
        this.response = this.chooseResponse();
        return this.response != Response.NONE;
    }

    public boolean canContinueToUse() {
        this.threat = this.findNearestRelevantPlayer();
        if (this.nightHeron.hasExternalFright()) {
            this.externalThreatPosition = this.nightHeron.getExternalFrightSource();
        }
        if (this.response.isFlight()) {
            return this.nightHeron.isControlledFlightActive() && (this.remainingTicks > 0 || !this.nightHeron.onGround());
        }
        return this.remainingTicks > 0 && this.chooseResponse() != Response.NONE;
    }

    public void start() {
        this.nightHeron.rememberFright(this.response == Response.LONG_FLIGHT);
        this.notifyNearbyNightHerons(this.response.isFlight());
        this.beginResponse(this.response);
    }

    public void stop() {
        this.response = Response.NONE;
        this.remainingTicks = 0;
        this.threat = null;
        this.externalThreatPosition = null;
        this.landingTarget = null;
        this.escapeDirection = Vec3.ZERO;
        this.nightHeron.clearExternalFright();
        this.nightHeron.getNavigation().stop();
        if (this.nightHeron.getBehaviorState().isAirborne()) {
            this.nightHeron.settleInterruptedFlight(NightHeronBehaviorState.IDLE);
        } else if (this.nightHeron.getBehaviorState().isEscape()) {
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.IDLE);
        }
    }

    public void tick() {
        Response currentPressure = this.chooseResponse();
        if (currentPressure.ordinal() > this.response.ordinal()) {
            this.response = currentPressure;
            this.beginResponse(this.response);
        }
        --this.remainingTicks;
        if (this.response == Response.ALERT) {
            this.tickAlert();
            return;
        }
        if (this.response == Response.WALK) {
            this.tickGroundEscape(NightHeronBehaviorState.WALK_ESCAPE, 0.36);
            return;
        }
        if (this.response == Response.RUN) {
            this.tickGroundEscape(NightHeronBehaviorState.RUN_ESCAPE, 1.35);
            return;
        }
        this.tickFlightEscape(this.response == Response.LONG_FLIGHT);
    }

    private void beginResponse(Response response) {
        this.nightHeron.getNavigation().stop();
        this.nightHeron.birdBrain().onFrightened(this.frightAmount(response));
        this.escapeDirection = this.computeEscapeDirection();
        if (response == Response.ALERT) {
            this.remainingTicks = this.randomBetween(18, 36);
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.ALERT_FREEZE);
            return;
        }
        if (response == Response.WALK) {
            this.remainingTicks = this.randomBetween(30, 58);
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.WALK_ESCAPE);
            this.moveAwayOnGround(0.36);
            return;
        }
        if (response == Response.RUN) {
            this.remainingTicks = this.randomBetween(30, 58);
            this.nightHeron.setBehaviorState(NightHeronBehaviorState.RUN_ESCAPE);
            this.moveAwayOnGround(1.35);
            return;
        }
        this.remainingTicks = response == Response.LONG_FLIGHT ? this.randomBetween(150, 260) : this.randomBetween(40, 72);
        this.landingTarget = NightHeronLandingSelector.findEscapeLanding(this.nightHeron, this.getThreatPosition(), response == Response.LONG_FLIGHT ? 36 : 14, response == Response.LONG_FLIGHT ? 96 : 34);
        NightHeronFlightController.takeOff(this.nightHeron, this.escapeDirection, 0.48, 0.72);
    }

    private Response chooseResponse() {
        Player player = this.threat != null ? this.threat : this.findNearestRelevantPlayer();
        if (player == null && !this.nightHeron.hasExternalFright()) {
            return Response.NONE;
        }
        float risk = this.nightHeron.birdBrain().computeRiskScore();
        float fatigue = this.nightHeron.birdBrain().motivation().fatigue();
        int recentFrightCount = this.nightHeron.getRecentFrightCount();
        if (this.nightHeron.hasExternalFright()) {
            risk = Math.max(risk, this.nightHeron.hasSevereExternalFright() ? NightHeronDefinition.RISK_LOW_FLIGHT_THRESHOLD : NightHeronDefinition.RISK_WALK_THRESHOLD);
        }
        if (recentFrightCount > 0) {
            risk += Math.min(NightHeronDefinition.RECENT_FRIGHT_RISK_BONUS_MAX, (float)recentFrightCount * NightHeronDefinition.RECENT_FRIGHT_RISK_BONUS_PER_COUNT);
        }
        if (this.nightHeron.horizontalCollision && risk >= NightHeronDefinition.RISK_ALERT_THRESHOLD) {
            risk = Math.max(risk, NightHeronDefinition.RISK_WALK_THRESHOLD);
        }
        if (this.nightHeron.getBehaviorState() == NightHeronBehaviorState.ROOSTING && risk >= NightHeronDefinition.RISK_ALERT_THRESHOLD) {
            risk = Math.max(risk, NightHeronDefinition.RISK_RUN_THRESHOLD);
        }
        risk = Mth.clamp(risk, 0.0F, 1.0F);
        if (recentFrightCount >= NightHeronDefinition.RECENT_FRIGHT_LONG_FLIGHT_COUNT && risk >= NightHeronDefinition.RISK_RUN_THRESHOLD && fatigue <= NightHeronDefinition.LONG_FLIGHT_FATIGUE_LIMIT) {
            return Response.LONG_FLIGHT;
        }
        if (risk < NightHeronDefinition.RISK_NONE_THRESHOLD) {
            return Response.NONE;
        }
        if (risk < NightHeronDefinition.RISK_ALERT_THRESHOLD) {
            return Response.ALERT;
        }
        if (risk < NightHeronDefinition.RISK_WALK_THRESHOLD) {
            return Response.WALK;
        }
        if (risk < NightHeronDefinition.RISK_RUN_THRESHOLD) {
            return Response.RUN;
        }
        if (risk < NightHeronDefinition.RISK_LOW_FLIGHT_THRESHOLD) {
            return Response.LOW_FLIGHT;
        }
        return fatigue > NightHeronDefinition.LONG_FLIGHT_FATIGUE_LIMIT ? Response.LOW_FLIGHT : Response.LONG_FLIGHT;
    }

    private Player findNearestRelevantPlayer() {
        Player player = this.nightHeron.level().getNearestPlayer((Entity)this.nightHeron, 17.0);
        return player != null && !player.isSpectator() ? player : null;
    }

    private void tickAlert() {
        Vec3 threatPosition = this.getThreatPosition();
        if (threatPosition != null) {
            this.nightHeron.getLookControl().setLookAt(threatPosition.x, threatPosition.y + 1.0, threatPosition.z);
        }
        this.nightHeron.getNavigation().stop();
    }

    private void tickGroundEscape(NightHeronBehaviorState state, double speed) {
        if (this.remainingTicks % 12 == 0 || this.nightHeron.getNavigation().isDone()) {
            this.escapeDirection = this.computeEscapeDirection();
            this.moveAwayOnGround(speed);
        }
        this.nightHeron.setBehaviorState(state);
        if (this.threat != null) {
            this.nightHeron.getLookControl().setLookAt((Entity)this.threat, 30.0f, 30.0f);
        }
    }

    private void tickFlightEscape(boolean longEscape) {
        if (this.remainingTicks <= 0 && this.landingTarget == null) {
            this.landingTarget = NightHeronLandingSelector.findEscapeLanding(this.nightHeron, this.getThreatPosition(), 4, 28);
            if (this.landingTarget == null) {
                NightHeronFlightController.tickOpenLanding(this.nightHeron, this.escapeDirection);
                return;
            }
        }
        if (this.landingTarget != null && NightHeronFlightController.shouldBeginLandingApproach(this.nightHeron, this.landingTarget, this.remainingTicks, longEscape ? 34.0 : 14.0)) {
            if (NightHeronFlightController.tickLandingApproach(this.nightHeron, this.landingTarget)) {
                this.remainingTicks = 0;
            }
            return;
        }
        if (this.escapeDirection.lengthSqr() <= 1.0E-4 || this.nightHeron.horizontalCollision || this.remainingTicks % 16 == 0) {
            Vec3 vec3 = this.escapeDirection = this.landingTarget != null ? NightHeronLandingSelector.directionTo(this.landingTarget, this.nightHeron) : this.computeEscapeDirection();
        }
        if (longEscape) {
            NightHeronFlightController.tickLongEscapeFlight(this.nightHeron, this.escapeDirection, 0.55, 20.0, 32.0);
        } else {
            NightHeronFlightController.tickLowEscapeFlight(this.nightHeron, this.escapeDirection, 0.36, 4.0, 8.0);
        }
    }

    private void moveAwayOnGround(double speed) {
        Vec3 threatPosition = this.getThreatPosition();
        if (threatPosition == null) {
            return;
        }
        Vec3 target = LandRandomPos.getPosAway((PathfinderMob)this.nightHeron, (int)12, (int)5, (Vec3)threatPosition);
        if (target == null) {
            target = this.nightHeron.position().add(this.escapeDirection.scale(8.0));
        }
        this.nightHeron.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    private Vec3 computeEscapeDirection() {
        Vec3 threatPosition = this.getThreatPosition();
        if (threatPosition == null) {
            float angle = this.nightHeron.getRandom().nextFloat() * ((float)Math.PI * 2);
            return new Vec3((double)Mth.cos((float)angle), 0.0, (double)Mth.sin((float)angle));
        }
        Vec3 away = this.nightHeron.position().subtract(threatPosition);
        Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
        if (horizontal.lengthSqr() <= 1.0E-4) {
            float angle = this.nightHeron.getRandom().nextFloat() * ((float)Math.PI * 2);
            return new Vec3((double)Mth.cos((float)angle), 0.0, (double)Mth.sin((float)angle));
        }
        double jitter = (this.nightHeron.getRandom().nextDouble() - 0.5) * 0.55;
        double cos = Math.cos(jitter);
        double sin = Math.sin(jitter);
        Vec3 direction = horizontal.normalize();
        return new Vec3(direction.x * cos - direction.z * sin, 0.0, direction.x * sin + direction.z * cos).normalize();
    }

    private Vec3 getThreatPosition() {
        if (this.threat != null) {
            return this.threat.position();
        }
        return this.externalThreatPosition;
    }

    private void notifyNearbyNightHerons(boolean severe) {
        List<NightHeronEntity> neighbors = this.nightHeron.level().getEntitiesOfClass(NightHeronEntity.class, this.nightHeron.getBoundingBox().inflate(12.0), other -> other != this.nightHeron);
        for (NightHeronEntity neighbor : neighbors) {
            neighbor.receiveFlockFright(this.nightHeron.position(), severe);
        }
    }

    private int randomBetween(int min, int max) {
        return min + this.nightHeron.getRandom().nextInt(max - min + 1);
    }

    private float frightAmount(Response response) {
        return switch (response) {
            case ALERT -> NightHeronDefinition.FRIGHT_AMOUNT_ALERT;
            case WALK -> NightHeronDefinition.FRIGHT_AMOUNT_WALK;
            case RUN -> NightHeronDefinition.FRIGHT_AMOUNT_RUN;
            case LOW_FLIGHT -> NightHeronDefinition.FRIGHT_AMOUNT_LOW_FLIGHT;
            case LONG_FLIGHT -> NightHeronDefinition.FRIGHT_AMOUNT_LONG_FLIGHT;
            default -> 0.0F;
        };
    }

    private static enum Response {
        NONE,
        ALERT,
        WALK,
        RUN,
        LOW_FLIGHT,
        LONG_FLIGHT;


        private boolean isFlight() {
            return this == LOW_FLIGHT || this == LONG_FLIGHT;
        }
    }
}
