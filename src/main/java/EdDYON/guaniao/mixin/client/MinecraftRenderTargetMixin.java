package EdDYON.guaniao.mixin.client;

import EdDYON.guaniao.client.camera.CameraClientCapture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderTargetMixin {
    @Inject(method = "getMainRenderTarget", at = @At("RETURN"), cancellable = true, require = 1)
    private void guaniao$redirectCameraCaptureTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget target = CameraClientCapture.redirectedRenderTarget();
        if (target != null) {
            cir.setReturnValue(target);
        }
    }
}
