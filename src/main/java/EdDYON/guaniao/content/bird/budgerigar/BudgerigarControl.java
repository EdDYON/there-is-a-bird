package EdDYON.guaniao.content.bird.budgerigar;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Public bridge onto the package-private parrot control surface.
 *
 * <p>{@link BudgerigarEntity} keeps its behaviour-state setters, trust counters and
 * the eat/forage gates package-private so that only the shared parrot goals can
 * drive them. Species that live in their own package (macaw, cockatiel and now
 * umbrella cockatoo) still need that same control surface instead of forking a
 * second locomotion, taming or eating system. This class exposes exactly the
 * operations those species need and nothing else.</p>
 */
public final class BudgerigarControl {
    private BudgerigarControl() {
    }

    public static void setBehaviorState(BudgerigarEntity bird, BudgerigarBehaviorState state) {
        bird.setBehaviorState(state);
    }

    public static void setBehaviorStateFor(BudgerigarEntity bird, BudgerigarBehaviorState state, int ticks) {
        bird.setBehaviorStateFor(state, ticks);
    }

    public static boolean isEating(BudgerigarEntity bird) {
        return bird.isEating();
    }

    public static boolean isSleepingOrRoosting(BudgerigarEntity bird) {
        return bird.isSleepingOrRoosting();
    }

    public static int trustTicks(BudgerigarEntity bird) {
        return bird.trustTicks();
    }

    public static void addTrust(BudgerigarEntity bird, int amount) {
        bird.addTrust(amount);
    }

    public static void startEatingFood(BudgerigarEntity bird, ItemStack foodStack, boolean trustedFood) {
        bird.startEatingFood(foodStack, trustedFood);
    }

    public static boolean canStartFoodGoal(BudgerigarEntity bird) {
        return bird.canStartFoodGoal();
    }

    public static boolean canStartSocialGoal(BudgerigarEntity bird) {
        return bird.canStartSocialGoal();
    }

    public static boolean isActiveTime(BudgerigarEntity bird) {
        return bird.isActiveTime();
    }

    public static void frightenFrom(BudgerigarEntity bird, Vec3 sourcePos, int ticks) {
        bird.frightenFrom(sourcePos, ticks);
    }

    public static void queueFrightFrom(BudgerigarEntity bird, Vec3 sourcePos, int ticks, int delayTicks) {
        bird.queueFrightFrom(sourcePos, ticks, delayTicks);
    }
}
