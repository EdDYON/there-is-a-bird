package EdDYON.guaniao.mixin;

import EdDYON.guaniao.content.dropping.BirdDroppingPressurePlatePulse;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeightedPressurePlateBlock.class)
public class WeightedPressurePlateBlockMixin {
    @Inject(method = "getSignalStrength", at = @At("RETURN"), cancellable = true)
    private void guaniao$birdDroppingSignal(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0 && level instanceof ServerLevel serverLevel
                && BirdDroppingPressurePlatePulse.isActive(serverLevel, pos)) {
            cir.setReturnValue(15);
        }
    }
}
