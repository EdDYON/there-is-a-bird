package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.bird.umbrellacockatoo.UmbrellaCockatooEntity;
import java.util.Collection;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator preview of the umbrella cockatoo's existing rare call, without changing its taming or commands. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class CockatooSpecialCallCommands {
    private CockatooSpecialCallCommands() { }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("cockatooSpecial")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> nearest(context.getSource()))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> trigger(context.getSource(),
                                        EntityArgument.getEntities(context, "targets"))))));
    }

    private static int nearest(CommandSourceStack source) {
        Vec3 position = source.getPosition();
        return trigger(source, source.getLevel().getEntitiesOfClass(UmbrellaCockatooEntity.class,
                        new AABB(position, position).inflate(32),
                        bird -> bird.isAlive() && bird.position().distanceToSqr(position) <= 32 * 32)
                .stream().min(Comparator.comparingDouble(bird -> bird.position().distanceToSqr(position)))
                .stream().toList());
    }

    private static int trigger(CommandSourceStack source, Collection<? extends Entity> targets) {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof UmbrellaCockatooEntity bird && bird.forceSpecialCall()) count++;
        }
        return count;
    }
}
