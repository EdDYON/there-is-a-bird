package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated development scene. Not included by normal builds. */
@Mod.EventBusSubscriber(modid="guaniao")
public final class NightHeronVisualProbe {
    private static NightHeronEntity bird;
    private static boolean pauseOnCatch;
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("heronqa").requires(s->s.hasPermission(2))
            .executes(c->{setup(c.getSource().getPlayerOrException(),false);return 1;})
            .then(Commands.literal("preview").executes(c->{setup(c.getSource().getPlayerOrException(),true);return 1;}))
            .then(Commands.literal("resume").executes(c->{
                var player=c.getSource().getPlayerOrException();
                pauseOnCatch=false;
                if(bird==null || bird.isRemoved()) bird=player.serverLevel().getEntitiesOfClass(NightHeronEntity.class,
                    player.getBoundingBox().inflate(12),b->b.isOwnedBy(player) && b.hasDeliveryFish()).stream().findFirst().orElse(null);
                if(bird!=null)bird.setBirdCommandMode(BirdCommandMode.FOLLOW);return 1;
            })));
    }
    private static void setup(ServerPlayer p, boolean preview) {
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var level=p.serverLevel();BlockPos o=new BlockPos(96,80,96);
        for(var old:level.getEntitiesOfClass(net.minecraft.world.entity.animal.AbstractFish.class,
                new net.minecraft.world.phys.AABB(o,o.offset(14,7,14))))old.discard();
        for(var old:level.getEntitiesOfClass(NightHeronEntity.class,
                new net.minecraft.world.phys.AABB(o,o.offset(14,7,14))))old.discard();
        for(BlockPos b:BlockPos.betweenClosed(o,o.offset(13,6,13)))
            level.setBlockAndUpdate(b,b.getY()<=81?Blocks.SEA_LANTERN.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setDayTime(13000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        p.teleportTo(level,99.5,82,104.5,-124,19);
        bird=GuaniaoEntityTypes.NIGHT_HERON.get().create(level);bird.moveTo(102.5,82,102.5,90,0);bird.setOnGround(true);
        bird.tame(p);bird.setBirdCommandMode(BirdCommandMode.FOLLOW);level.addFreshEntity(bird);
        pauseOnCatch=true;
        if (preview) {
            CompoundTag saved=new CompoundTag();bird.addAdditionalSaveData(saved);
            saved.getCompound("NightHeronPet").putInt("GiftRemaining",1);bird.readAdditionalSaveData(saved);
            for(long seed=0;seed<100000;seed++) {
                RandomSource random=RandomSource.create(seed);
                random.nextInt(BirdConfigManager.nightHeronGiftMaxSeconds()-BirdConfigManager.nightHeronGiftMinSeconds()+1);
                if(random.nextFloat()<NightHeronFishTask.CATCH_CHANCE) {bird.getRandom().setSeed(seed);break;}
            }
            // Call exactly the production timer once before unrelated AI randomness advances.
            bird.fishTask().tick();
            p.sendSystemMessage(Component.literal("QA PREVIEW: dry land; gift timer accelerated to one tick for mouth/delivery inspection only. This does not validate the normal waiting interval."));
        } else {
            p.sendSystemMessage(Component.literal("QA: dry land, normal configured gift interval (default 300-600 seconds), 50% chance. Gift pauses in STAY; /heronqa resume delivers."));
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase==TickEvent.Phase.END && pauseOnCatch && bird!=null && bird.hasDeliveryFish()) {
            pauseOnCatch=false;bird.setBirdCommandMode(BirdCommandMode.STAY);
            System.out.println("HERON_VISUAL generated gift held for inspection: "+bird.getHeldFishForRendering()+" dry="+!bird.isInWaterOrBubble()+" position="+bird.position());
        }
    }
}
