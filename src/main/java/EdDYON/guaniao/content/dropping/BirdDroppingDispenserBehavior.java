package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public final class BirdDroppingDispenserBehavior extends DefaultDispenseItemBehavior {
    private final BirdDroppingVariant variant;

    private BirdDroppingDispenserBehavior(BirdDroppingVariant variant) {
        this.variant = variant;
    }

    public static void registerAll() {
        DispenserBlock.registerBehavior(GuaniaoItems.BIRD_DROPPING_1.get(), new BirdDroppingDispenserBehavior(BirdDroppingVariant.ONE));
        DispenserBlock.registerBehavior(GuaniaoItems.BIRD_DROPPING_2.get(), new BirdDroppingDispenserBehavior(BirdDroppingVariant.TWO));
        DispenserBlock.registerBehavior(GuaniaoItems.BIRD_DROPPING_3.get(), new BirdDroppingDispenserBehavior(BirdDroppingVariant.THREE));
        DispenserBlock.registerBehavior(GuaniaoItems.BIRD_DROPPING_4.get(), new BirdDroppingDispenserBehavior(BirdDroppingVariant.FOUR));
    }

    @Override
    public ItemStack execute(net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
        var direction = source.state().getValue(DispenserBlock.FACING);
        var position = DispenserBlock.getDispensePosition(source);
        Projectile projectile = getProjectile(source.level(), position, stack);
        projectile.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), getPower(), 6.0F);
        source.level().addFreshEntity(projectile);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(net.minecraft.core.dispenser.BlockSource source) {
        source.level().levelEvent(1002, source.pos(), 0);
    }

    protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
        BirdDroppingProjectileEntity projectile = new BirdDroppingProjectileEntity(GuaniaoEntityTypes.BIRD_DROPPING_PROJECTILE.get(), level);
        projectile.setPos(position.x(), position.y(), position.z());
        projectile.setVariant(this.variant);
        ItemStack renderedStack = stack.copy();
        renderedStack.setCount(1);
        projectile.setItem(renderedStack);
        return projectile;
    }

    protected float getPower() {
        return 1.25F;
    }
}
