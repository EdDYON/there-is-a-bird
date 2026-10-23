package EdDYON.guaniao.mixin;

import EdDYON.guaniao.content.dropping.BirdDroppingPrankHandler;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerTradePriceMixin {
    @Inject(method = "resetSpecialPrices", at = @At("TAIL"))
    private void guaniao$pricesReset(CallbackInfo ci) {
        BirdDroppingPrankHandler.onTradePricesReset((Villager) (Object) this);
    }

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void guaniao$pricesUpdated(Player player, CallbackInfo ci) {
        BirdDroppingPrankHandler.onTradePricesUpdated((Villager) (Object) this);
    }
}
