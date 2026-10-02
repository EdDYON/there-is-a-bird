package EdDYON.guaniao.content.bird.nightheron;

import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.event.GeoRenderEvent;

@Mod.EventBusSubscriber(modid="guaniao", value=Dist.CLIENT)
public final class NightHeronVisualClient {
    private static final Map<Integer,String> LAST=new HashMap<>();
    @SubscribeEvent public static void render(GeoRenderEvent.Entity.Post e) {
        if(!(e.getEntity() instanceof NightHeronEntity bird) || !bird.isTame())return;
        var controller=bird.getAnimatableInstanceCache().getManagerForId(bird.getId()).getAnimationControllers().get("movement");
        if(controller==null || controller.getCurrentAnimation()==null)return;
        String state=controller.getCurrentAnimation().animation().name()+" mouth="+bird.getHeldFishForRendering();
        if(!state.equals(LAST.put(bird.getId(),state)))System.out.println("HERON_RENDER "+bird.getId()+" "+state);
    }
}
