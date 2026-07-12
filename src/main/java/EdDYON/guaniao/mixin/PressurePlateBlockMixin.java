package EdDYON.guaniao.mixin;

import EdDYON.guaniao.content.dropping.BirdDroppingProjectileEntity;
import EdDYON.guaniao.content.dropping.BirdDroppingSplatEntity;
import EdDYON.guaniao.content.dropping.PrankFoodUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PressurePlateBlock.class)
public class PressurePlateBlockMixin {
    @Unique
    private static final AABB guaniao$TOUCH_AABB = new AABB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.25D, 0.9375D);

    @Inject(method = "getSignalStrength", at = @At("RETURN"), cancellable = true)
    private void guaniao$birdDroppingSignal(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) {
            return;
        }

        AABB area = guaniao$TOUCH_AABB.move(pos);
        boolean hasDropping = !level.getEntitiesOfClass(BirdDroppingSplatEntity.class, area, PressurePlateBlockMixin::canTrigger).isEmpty()
                || !level.getEntitiesOfClass(BirdDroppingProjectileEntity.class, area, PressurePlateBlockMixin::canTrigger).isEmpty()
                || !level.getEntitiesOfClass(ItemEntity.class, area, item -> canTrigger(item) && PrankFoodUtil.isDropping(item.getItem())).isEmpty();
        if (hasDropping) {
            cir.setReturnValue(15);
        }
    }

    @Unique
    private static boolean canTrigger(Entity entity) {
        return entity.isAlive() && !entity.isIgnoringBlockTriggers();
    }
}
