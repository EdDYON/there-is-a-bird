package EdDYON.guaniao.mixin;

import EdDYON.guaniao.event.BirdServerTickManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public class ServerEntityTrackingMixin {
    // Join events may still be canceled, and tracking can resume without another join event.
    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"), require = 1)
    private void guaniao$trackEntity(Entity entity, CallbackInfo ci) {
        BirdServerTickManager.trackEntity(entity);
    }
}
