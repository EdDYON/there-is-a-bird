package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.event.BurialPlumeAnvilEvents;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local QA source set only. Compares display examples with the real anvil handler. */
@Mod.EventBusSubscriber(modid="guaniao", value=Dist.CLIENT)
public final class HandbookRecipeProbe {
    private static boolean checked;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft client=Minecraft.getInstance();
        if(checked || event.phase!=TickEvent.Phase.END || client.level==null || client.player==null)return;
        var recipe=client.level.getRecipeManager().byKey(new ResourceLocation("guaniao","cooked_fries")).orElse(null);
        if(recipe==null)return; // Wait until the world recipe synchronization has completed.
        checked=true;
        for(String book:List.of("burial_plume_book","riven_plume_book","hunting_return_book")) {
            var display=HandbookAnvilRecipe.fromBook("guaniao:"+book);
            var operation=new AnvilUpdateEvent(display.input().copy(),display.book().copy(),null,0,client.player);
            BurialPlumeAnvilEvents.onAnvilUpdate(operation);
            require(!display.input().isEnchanted(),"Preview must not mutate its input");
            require(!operation.getOutput().isEmpty() && ItemStack.matches(display.output(),operation.getOutput()),"Displayed anvil output must match actual NBT: "+book);
            require(operation.getCost()==BurialPlumeAnvilEvents.ANVIL_LEVEL_COST && operation.getMaterialCost()==1,"Displayed experience and material costs");
        }
        require(recipe instanceof AbstractCookingRecipe,"Furnace recipe type");
        var cooking=(AbstractCookingRecipe)recipe;
        require(cooking.getCookingTime()==200 && Math.abs(cooking.getExperience()-.35f)<.00001f,"Live world cooking metadata");
        require(!cooking.getIngredients().get(0).isEmpty() && !cooking.getResultItem(client.level.registryAccess()).isEmpty(),"Cooking input and output exist");
        System.out.println("HANDBOOK_RECIPE_QA PASS: all 3 anvil display stacks match real handler output/NBT/cost; live cooking input/output, 200 ticks, 0.35 XP");
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
