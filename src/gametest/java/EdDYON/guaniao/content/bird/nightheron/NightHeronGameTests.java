package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;


@GameTestHolder("guaniao_heron_qa")
@PrefixGameTestTemplate(false)
public final class NightHeronGameTests {
    static void roll(NightHeronEntity bird, boolean success) {
        for (long seed=0;seed<100000;seed++) {
            RandomSource random=RandomSource.create(seed);
            random.nextInt(BirdConfigManager.nightHeronGiftMaxSeconds()-BirdConfigManager.nightHeronGiftMinSeconds()+1);
            if ((random.nextFloat()<NightHeronFishTask.CATCH_CHANCE)==success) { bird.getRandom().setSeed(seed);return; }
        }
        throw new AssertionError("No deterministic seed");
    }
    static void timer(NightHeronEntity bird, int ticks) {
        CompoundTag saved=new CompoundTag();bird.addAdditionalSaveData(saved);
        saved.getCompound("NightHeronPet").putInt("GiftRemaining",ticks);
        bird.readAdditionalSaveData(saved);
    }
    static void gift(NightHeronEntity bird) {
        timer(bird,1);roll(bird,true);bird.fishTask().tick();
        check(bird.hasDeliveryFish(),"successful companionship roll puts one gift in mouth");
    }
    static int fishCount(FakePlayer player) {
        return player.getInventory().countItem(Items.COD)+player.getInventory().countItem(Items.SALMON);
    }
    static void check(boolean result, String label) {
        if (!result) throw new net.minecraft.gametest.framework.GameTestAssertException(label);
        System.out.println("HERON_QA PASS: " + label);
    }

    static final class CancelHooks {
        boolean tame, capture, join, death, drops;
        @SubscribeEvent public void tame(AnimalTameEvent e) { if (tame && e.getAnimal() instanceof NightHeronEntity) e.setCanceled(true); }
        @SubscribeEvent public void capture(NightHeronFishCaptureEvent e) { if (capture) e.setCanceled(true); }
        @SubscribeEvent public void join(EntityJoinLevelEvent e) { if (join && e.getEntity() instanceof ItemEntity i && i.getPersistentData().getBoolean(NightHeronFishTask.DELIVERED_MARKER)) e.setCanceled(true); }
        @SubscribeEvent public void death(LivingDeathEvent e) { if (death && e.getEntity() instanceof NightHeronEntity) e.setCanceled(true); }
        @SubscribeEvent public void drops(LivingDropsEvent e) { if (drops && e.getEntity() instanceof NightHeronEntity) e.setCanceled(true); }
    }

    static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final BlockPos origin;
        final FakePlayer owner, stranger;
        final List<Entity> spawned = new ArrayList<>();
        Fixture(GameTestHelper h) { this(h,false); }
        Fixture(GameTestHelper h, boolean pond) { this(h,pond,0); }
        Fixture(GameTestHelper h, boolean pond, int yOffset) {
            level = h.getLevel(); origin = h.absolutePos(new BlockPos(0,yOffset,0));
            level.setDayTime(16000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(11,5,11))) {
                level.setBlockAndUpdate(p, p.getY() <= origin.getY()+1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            if (pond) for (BlockPos p : BlockPos.betweenClosed(origin.offset(6,1,3), origin.offset(9,1,8))) level.setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());
            owner = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"HeronOwner"));
            stranger = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"HeronOther"));
            owner.moveTo(origin.getX()+4.5,origin.getY()+2,origin.getZ()+5.5,90,0);
            stranger.moveTo(origin.getX()+4.5,origin.getY()+2,origin.getZ()+6.5,90,0);
            level.addNewPlayer(owner); level.addNewPlayer(stranger);
        }
        NightHeronEntity bird(boolean tame) {
            NightHeronEntity b=GuaniaoEntityTypes.NIGHT_HERON.get().create(level);
            b.moveTo(origin.getX()+5.5,origin.getY()+2,origin.getZ()+5.5,0,0);
            b.setOnGround(true); b.setDeltaMovement(Vec3.ZERO);
            if(tame) { b.tame(owner); b.setBirdCommandMode(BirdCommandMode.FOLLOW); }
            level.addFreshEntity(b); spawned.add(b); return b;
        }
        Cod fish() {
            Cod c=EntityType.COD.create(level);
            c.moveTo(origin.getX()+6.4,origin.getY()+1.65,origin.getZ()+5.5,0,0);
            c.setNoAi(true); c.setNoGravity(true); level.addFreshEntity(c); c.tick(); c.setDeltaMovement(Vec3.ZERO);
            spawned.add(c); return c;
        }
        boolean ownsDrop(ItemEntity item) {
            CompoundTag tag=new CompoundTag();item.addAdditionalSaveData(tag);
            return item.getPersistentData().getBoolean(NightHeronFishTask.DELIVERED_MARKER)
                    && tag.hasUUID("Owner") && tag.getUUID("Owner").equals(owner.getUUID());
        }
        @Override public void close() {
            for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(32),this::ownsDrop))item.discard();
            for(Entity e:spawned) if(!e.isRemoved())e.discard();
            owner.discard(); stranger.discard();
        }
    }

    @GameTest(template="shore", timeoutTicks=100)
    public static void transactions(GameTestHelper h) throws Exception {
        var field=BirdConfigManager.class.getDeclaredField("config"); field.setAccessible(true);
        BirdConfigData original=(BirdConfigData)field.get(null), cfg=original.copy();
        cfg.global.nightHeronTamingChance=1;cfg.global.nightHeronGiftsEnabled=true;field.set(null,cfg);
        CancelHooks hooks=new CancelHooks(); MinecraftForge.EVENT_BUS.register(hooks);
        try(Fixture f=new Fixture(h,true)) {
            NightHeronEntity wild=f.bird(false);
            f.owner.getAbilities().instabuild=false;
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,3));
            wild.mobInteract(f.owner,InteractionHand.MAIN_HAND);
            check(wild.isOwnedBy(f.owner) && !wild.isOwnedBy(f.stranger),"actual feeder owns the tamed bird");
            check(wild.getBirdCommandMode()==BirdCommandMode.FOLLOW && f.owner.getMainHandItem().getCount()==2,"one fish consumed and FOLLOW selected");
            check(wild.getBehaviorState()==NightHeronBehaviorState.EATING && wild.isEatingFish(),"successful taming preserves the feeding animation");
            wild.mobInteract(f.owner,InteractionHand.OFF_HAND);
            check(f.owner.getMainHandItem().getCount()==2,"duplicate interaction cannot consume twice");
            check(wild.getHeldFishPurpose()==HeldFishPurpose.SELF_FOOD && !wild.fishTask().deliver(f.owner,true),"hand-fed meal cannot be returned as a gift");
            check(!wild.isFood(new ItemStack(Items.COD)) && !wild.canMate(wild),"no breeding entry point");
            wild.setAge(-24000); check(!wild.isBaby(),"night heron remains adult");
            NightHeronEntity denied=f.bird(false); hooks.tame=true;
            denied.mobInteract(f.owner,InteractionHand.MAIN_HAND); hooks.tame=false;
            check(!denied.isTame(),"cancelled AnimalTameEvent prevents ownership");
            // Use a distance beyond the 256-block bird visibility/tracking guard.
            check(denied.removeWhenFarAway(300 * 300) && !wild.removeWhenFarAway(300 * 300),"wild despawn and pet persistence remain distinct");
            f.stranger.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.stranger.setShiftKeyDown(true);
            wild.mobInteract(f.stranger,InteractionHand.MAIN_HAND);
            check(wild.getBirdCommandMode()==BirdCommandMode.FOLLOW,"non-owner cannot change command");
            f.stranger.setShiftKeyDown(false);f.stranger.moveTo(f.origin.getX()-20,f.origin.getY()+2,f.origin.getZ()+5);
            check(wild.findNearestThreatPlayer(5)==null,"owner is excluded from fright sensing");
            wild.discard();denied.discard();

            NightHeronEntity offhand=f.bird(true);offhand.setHealth(offhand.getMaxHealth()-2);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COD,2));f.owner.setShiftKeyDown(true);
            check(offhand.mobInteract(f.owner,InteractionHand.MAIN_HAND)==InteractionResult.PASS
                    && offhand.getBirdCommandMode()==BirdCommandMode.FOLLOW,"empty main hand defers to offhand food without cycling command");
            offhand.mobInteract(f.owner,InteractionHand.OFF_HAND);
            check(f.owner.getOffhandItem().getCount()==1 && offhand.isEatingFish() && offhand.getHealth()==offhand.getMaxHealth(),"offhand feeding consumes one fish and heals owner pet");
            offhand.discard();
            NightHeronEntity rod=f.bird(true);f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.FISHING_ROD));
            check(rod.mobInteract(f.owner,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"empty main hand does not cycle command while offhand holds a rod");
            rod.mobInteract(f.owner,InteractionHand.OFF_HAND);
            check(rod.getBirdCommandMode()==BirdCommandMode.FOLLOW && !rod.hasHeldFishForRendering(),"offhand rod neither starts fishing nor changes command");rod.discard();
            f.owner.setShiftKeyDown(false);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            cfg.global.nightHeronTamingChance=0;
            NightHeronEntity failed=f.bird(false);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD));
            failed.mobInteract(f.owner,InteractionHand.MAIN_HAND);
            check(!failed.isTame() && failed.isEatingFish() && f.owner.getMainHandItem().isEmpty()
                    && failed.findNearestThreatPlayer(5)==null,"failed taming with last fish keeps feeder safe while eating");
            failed.hurt(failed.damageSources().generic(),1);
            check(failed.findNearestThreatPlayer(5)==f.owner,"temporary feeding tolerance ends when meal is interrupted");failed.discard();
            cfg.global.nightHeronTamingEnabled=false;
            NightHeronEntity disabled=f.bird(false);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));
            disabled.mobInteract(f.owner,InteractionHand.MAIN_HAND);
            check(!disabled.isTame() && !disabled.isEatingFish() && f.owner.getMainHandItem().getCount()==2,"disabled taming does not consume fish");disabled.discard();
            cfg.global.nightHeronTamingEnabled=true;cfg.global.nightHeronTamingChance=1;
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);

            NightHeronEntity b=f.bird(true);Cod fish=f.fish();
            check(NightHeronFishing.canReachFish(b,fish),"surface geometry fixture is reachable");
            fish.setCustomName(Component.literal("Protected"));
            check(!b.canHuntPrey(fish) && !b.catchFish(fish) && !b.doHurtTarget(fish),"named fish protected from work, self-food and fallback attack");
            fish.setCustomName(null);fish.setFromBucket(true);
            check(!b.canHuntPrey(fish) && !b.catchFish(fish),"bucket fish protected from personal foraging too");fish.setFromBucket(false);
            hooks.capture=true;
            check(!b.catchFish(fish,HeldFishPurpose.SELF_FOOD) && fish.isAlive() && !b.hasHeldFishForRendering(),"capture cancellation leaves fish and mouth unchanged");hooks.capture=false;
            BlockPos glass=f.origin.offset(6,2,5);f.level.setBlockAndUpdate(glass,Blocks.GLASS.defaultBlockState());
            check(!b.catchFish(fish,HeldFishPurpose.SELF_FOOD) && fish.isAlive(),"glass blocks personal foraging capture");f.level.setBlockAndUpdate(glass,Blocks.AIR.defaultBlockState());
            b.setHealth(b.getMaxHealth()-2);float health=b.getHealth();
            check(!b.catchFish(fish,HeldFishPurpose.OWNER_DELIVERY),"pet gift never consumes a live fish entity");
            gift(b);
            check(fish.isAlive() && b.getHeldFishForRendering().getCount()==1 && b.getHealth()==health && !b.isEatingFish(),"generated gift leaves live fish alone and neither heals nor eats");
            ItemStack firstFish=b.getHeldFishForRendering();
            for(int i=0;i<100;i++)b.fishTask().tick();
            check(ItemStack.matches(firstFish,b.getHeldFishForRendering()),"occupied mouth cannot create or replace a second fish");
            ItemStack view=b.getHeldFishForRendering();view.shrink(1);check(b.hasDeliveryFish(),"render accessor cannot mutate authoritative slot");
            b.setBirdCommandMode(BirdCommandMode.STAY);check(b.hasDeliveryFish() && !b.fishTask().permitsTravel(),"STAY preserves gift and prevents autonomous delivery");
            b.hurt(b.damageSources().generic(),1);check(b.hasDeliveryFish(),"damage preserves delivery catch");
            CompoundTag saved=new CompoundTag();b.addAdditionalSaveData(saved);b.discard();
            NightHeronEntity loaded=f.bird(false);loaded.readAdditionalSaveData(saved);
            check(loaded.isOwnedBy(f.owner) && loaded.hasDeliveryFish() && loaded.getBirdCommandMode()==BirdCommandMode.STAY,"NBT round trip restores owner, command and mouth once");
            CompoundTag bad=saved.copy();bad.getCompound("NightHeronPet").putInt("Version",99);
            NightHeronEntity unknown=f.bird(false);unknown.readAdditionalSaveData(bad);check(!unknown.hasHeldFishForRendering(),"unknown pet data version has safe fallback");unknown.discard();
            NightHeronEntity legacy=f.bird(false);legacy.readAdditionalSaveData(new CompoundTag());check(!legacy.isTame() && !legacy.hasDeliveryFish(),"legacy wild bird remains wild without fabricated catch");legacy.discard();
            for(int i=0;i<36;i++)f.owner.getInventory().items.set(i,new ItemStack(Items.STONE,64));
            check(!loaded.fishTask().deliver(f.owner,true) && loaded.hasDeliveryFish(),"full inventory keeps carried catch");
            f.owner.getInventory().items.set(0,ItemStack.EMPTY);
            check(!loaded.fishTask().deliver(f.stranger,true),"non-owner cannot claim catch");
            check(loaded.fishTask().deliver(f.owner,true) && !loaded.fishTask().deliver(f.owner,true),"manual delivery succeeds only once even while STAY");
            check(fishCount(f.owner)==1,"exactly one fish enters owner inventory");
            loaded.discard();f.owner.getInventory().clearContent();

            NightHeronEntity dropBird=f.bird(true);dropBird.setHeldFish(new ItemStack(Items.SALMON),HeldFishPurpose.OWNER_DELIVERY);
            hooks.join=true;check(!dropBird.fishTask().deliver(f.owner,false) && dropBird.hasDeliveryFish(),"cancelled ItemEntity spawn retains mouth item");hooks.join=false;
            check(dropBird.fishTask().deliver(f.owner,false),"automatic drop succeeds at safe owner position");
            var drops=f.level.getEntitiesOfClass(ItemEntity.class,dropBird.getBoundingBox().inflate(4),f::ownsDrop);
            check(drops.size()==1 && drops.get(0).getItem().getCount()==1,"automatic delivery creates only one marked fish");
            CompoundTag itemTag=new CompoundTag();drops.get(0).addAdditionalSaveData(itemTag);
            check(itemTag.hasUUID("Owner") && itemTag.getUUID("Owner").equals(f.owner.getUUID()),"drop uses pickup Owner rather than Thrower");
            dropBird.setBirdCommandMode(BirdCommandMode.FREE);dropBird.eatThrownFish(drops.get(0));
            check(drops.get(0).isAlive() && !dropBird.hasHeldFishForRendering(),"delivered fish cannot be eaten again");
            drops.get(0).discard();
            dropBird.setHeldFish(new ItemStack(Items.COD),HeldFishPurpose.OWNER_DELIVERY); hooks.death=true;
            dropBird.die(dropBird.damageSources().generic());hooks.death=false;
            check(dropBird.hasDeliveryFish(),"cancelled death does not remove mouth item");
            hooks.drops=true;dropBird.die(dropBird.damageSources().generic());hooks.drops=false;
            check(f.level.getEntitiesOfClass(ItemEntity.class,dropBird.getBoundingBox().inflate(4),f::ownsDrop).isEmpty(),"cancelled death drops do not bypass Forge event");
            h.succeed();
        } finally {field.set(null,original);MinecraftForge.EVENT_BUS.unregister(hooks);}
    }

    @GameTest(template="shore", timeoutTicks=600, batch="heron_ai")
    public static void realGoalPipeline(GameTestHelper h) {
        Fixture f=new Fixture(h);f.stranger.moveTo(f.origin.getX()-40,f.origin.getY()+2,f.origin.getZ());
        NightHeronEntity b=f.bird(true);gift(b);
        check(f.level.getEntitiesOfClass(Cod.class,new net.minecraft.world.phys.AABB(f.origin).inflate(12)).isEmpty(),"delivery scene contains no live fish");
        b.setBirdCommandMode(BirdCommandMode.FOLLOW);
        boolean[] completed={false};
        for(int tick=1;tick<=200;tick++) h.runAtTickTime(tick,()->{
            if(completed[0])return;
            try {
                // Observe every tick through the transfer itself. Behaviour after the gift
                // has already been delivered is outside this transaction's test window.
                if(b.isInWaterOrBubble()) {
                    System.out.println("HERON_PIPE_WATER tick="+h.getTick()+" mouth="+b.getHeldFishForRendering()+" position="+b.position());
                    check(false,"pet must stay out of water while carrying and delivering the gift");
                }
                if(h.getTick()%30==10)System.out.println("HERON_PIPE_STEP "+h.getTick()+" mouth="+b.getHeldFishForRendering()+" state="+b.getBehaviorState()+" position="+b.position());
                if(!b.hasHeldFishForRendering()) {
                    var items=f.level.getEntitiesOfClass(ItemEntity.class,f.owner.getBoundingBox().inflate(12),f::ownsDrop);
                    System.out.println("HERON_PIPELINE deliveredAt="+h.getTick()+" state="+b.getBehaviorState()+" drops="+items.size()+" inventory="+fishCount(f.owner)+" position="+b.position()+" ownerDistance="+b.distanceTo(f.owner));
                    check((items.size()==1 && items.get(0).getItem().getCount()==1 && fishCount(f.owner)==0)
                            || (items.isEmpty() && fishCount(f.owner)==1),"live ticking Goal returns, offers and delivers exactly one fish on dry land");
                    completed[0]=true;f.close();h.succeed();
                }
            } catch(RuntimeException failure) {
                completed[0]=true;f.close();throw failure;
            }
        });
        h.runAtTickTime(201,()->{
            if(!completed[0]) {
                completed[0]=true;f.close();
                check(false,"pet did not deliver its generated gift within 200 ticks");
            }
        });
    }
    @GameTest(template="shore", timeoutTicks=100, batch="heron_timers")
    public static void effectiveTimers(GameTestHelper h) throws Exception {
        var field=BirdConfigManager.class.getDeclaredField("config");field.setAccessible(true);
        BirdConfigData original=(BirdConfigData)field.get(null),cfg=original.copy();
        cfg.global.nightHeronGiftsEnabled=true;field.set(null,cfg);
        try(Fixture f=new Fixture(h)) {
            f.stranger.moveTo(f.origin.getX()-40,f.origin.getY()+2,f.origin.getZ());
            NightHeronEntity b=f.bird(true);b.fishTask().tick();
            int initial=b.fishTask().save().getInt("GiftRemaining");
            check(!b.hasHeldFishForRendering() && initial>=20*BirdConfigManager.nightHeronGiftMinSeconds()-1
                    && initial<=20*BirdConfigManager.nightHeronGiftMaxSeconds()-1,"new pet waits the configured interval before its first gift roll");
            timer(b,4);
            b.fishTask().tick();check(b.fishTask().save().getInt("GiftRemaining")==3,"awake loaded companionship advances gift countdown");
            b.setBirdCommandMode(BirdCommandMode.STAY);b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"STAY pauses gift time");
            b.setBirdCommandMode(BirdCommandMode.ROOST);b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"ROOST pauses gift time");
            b.setBirdCommandMode(BirdCommandMode.FOLLOW);f.level.setDayTime(6000);b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"daytime sleep pauses gift time");
            f.level.setDayTime(16000);Vec3 position=f.owner.position();f.owner.moveTo(position.add(100,0,0));b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"distant owner pauses gift time");f.owner.moveTo(position);
            b.setOwnerUUID(UUID.randomUUID());b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"unavailable owner pauses gift time");b.setOwnerUUID(f.owner.getUUID());
            b.setNoAi(true);b.fishTask().tick();check(b.fishTask().save().getInt("GiftRemaining")==3,"NoAI pauses gift time");b.setNoAi(false);
            cfg.global.nightHeronGiftsEnabled=false;b.fishTask().tick();
            check(b.fishTask().save().getInt("GiftRemaining")==3,"disabled gifts pause the countdown");cfg.global.nightHeronGiftsEnabled=true;
            timer(b,1);roll(b,false);b.fishTask().tick();
            int remaining=b.fishTask().save().getInt("GiftRemaining");
            check(!b.hasHeldFishForRendering() && remaining>=20*BirdConfigManager.nightHeronGiftMinSeconds()
                    && remaining<=20*BirdConfigManager.nightHeronGiftMaxSeconds(),"failed roll waits another complete configured gift interval");
            b.fishTask().tick();check(!b.hasHeldFishForRendering() && b.fishTask().save().getInt("GiftRemaining")==remaining-1,"failed roll cannot reroll on the next tick");
            CompoundTag reload=new CompoundTag();b.addAdditionalSaveData(reload);b.readAdditionalSaveData(reload);
            check(b.fishTask().save().getInt("GiftRemaining")==remaining-1,"save/load preserves remaining gift time without offline catchup");
            boolean dry=true;
            for(BlockPos pos:BlockPos.betweenClosed(f.origin,f.origin.offset(11,5,11)))if(!f.level.getFluidState(pos).isEmpty())dry=false;
            check(dry,"gift fixture has no water or pond");
            b.setBirdCommandMode(BirdCommandMode.FREE);gift(b);
            check(b.getHeldFishForRendering().getCount()==1 && !b.isEatingFish(),"FREE mode generates exactly one gift on dry land without a fishing assignment");
            int holdingTimer=b.fishTask().save().getInt("GiftRemaining");
            ItemStack held=b.getHeldFishForRendering();
            for(int i=0;i<200;i++)b.fishTask().tick();
            check(ItemStack.matches(held,b.getHeldFishForRendering()) && b.fishTask().save().getInt("GiftRemaining")==holdingTimer,"occupied mouth freezes timer and prevents duplicate generation");
            b.setBirdCommandMode(BirdCommandMode.ROOST);
            check(b.hasDeliveryFish() && !b.fishTask().permitsTravel(),"ROOST keeps the current gift without autonomous delivery");
            b.setHeldFish(ItemStack.EMPTY,HeldFishPurpose.NONE);b.setBirdCommandMode(BirdCommandMode.FOLLOW);
            timer(b,1);b.setHeldFish(new ItemStack(Items.COD),HeldFishPurpose.SELF_FOOD);b.fishTask().tick();
            check(b.getHeldFishPurpose()==HeldFishPurpose.SELF_FOOD && b.fishTask().save().getInt("GiftRemaining")==1,"personal meal cannot be overwritten by a generated gift");
            b.setHeldFish(ItemStack.EMPTY,HeldFishPurpose.NONE);
            CompoundTag oldTask=new CompoundTag();b.addAdditionalSaveData(oldTask);
            CompoundTag oldPet=oldTask.getCompound("NightHeronPet");
            oldPet.putBoolean("Working",true);oldPet.putInt("Source",2);oldPet.putInt("Cooldown",72000);
            oldPet.putLong("Anchor",f.origin.offset(1000,10,1000).asLong());oldPet.putString("Dimension","minecraft:the_nether");
            b.readAdditionalSaveData(oldTask);
            check(!b.hasHeldFishForRendering() && b.fishTask().save().getInt("GiftRemaining")==1,"old shoreline task loads without fabricating a fish");
            roll(b,true);b.fishTask().tick();
            check(b.hasDeliveryFish(),"old shoreline anchor and work cooldown cannot block the lightweight gift timer");
            CompoundTag carried=new CompoundTag();b.addAdditionalSaveData(carried);ItemStack beforeReload=b.getHeldFishForRendering();
            carried.getCompound("NightHeronPet").putBoolean("Working",true);carried.getCompound("NightHeronPet").putInt("Source",1);
            b.readAdditionalSaveData(carried);b.fishTask().tick();
            check(ItemStack.matches(beforeReload,b.getHeldFishForRendering()),"migration preserves an existing carried gift exactly once");
            NightHeronEntity wild=f.bird(false);timer(wild,1);roll(wild,true);wild.fishTask().tick();
            check(!wild.hasHeldFishForRendering(),"wild night herons never generate player gifts");
            var config=BirdConfigManager.class.getDeclaredMethod("normalize",BirdConfigData.class);config.setAccessible(true);
            BirdConfigData invalid=BirdConfigManager.snapshot();invalid.global.nightHeronTamingChance=Double.NaN;
            invalid.global.nightHeronGiftMinSeconds=-2;invalid.global.nightHeronGiftMaxSeconds=-3;
            BirdConfigData normalized=(BirdConfigData)config.invoke(null,invalid);
            check(normalized.global.nightHeronTamingChance==1.0/3 && normalized.global.nightHeronGiftMinSeconds==60
                    && normalized.global.nightHeronGiftMaxSeconds==60,"invalid taming chance and gift intervals normalize to finite ordered bounds");
            h.succeed();
        } finally {field.set(null,original);}
    }

    @GameTest(template="shore", timeoutTicks=650, batch="heron_flight")
    public static void flightDelivery(GameTestHelper h) {
        Fixture f=new Fixture(h);f.stranger.moveTo(f.origin.getX()-40,f.origin.getY()+2,f.origin.getZ());
        NightHeronEntity b=f.bird(true);gift(b);
        b.setBirdCommandMode(BirdCommandMode.FOLLOW);
        for(BlockPos p:BlockPos.betweenClosed(f.origin.offset(1,2,1),f.origin.offset(3,4,3)))f.level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
        f.owner.moveTo(f.origin.getX()+2.5,f.origin.getY()+5,f.origin.getZ()+2.5,0,0);
        boolean[] flew={false};
        for(int tick=1;tick<500;tick+=5) h.runAtTickTime(tick,()->{
            if(b.getY()>f.origin.getY()+2.8)flew[0]=true;
            if(h.getTick()%50==1)System.out.println("HERON_FLIGHT "+h.getTick()+" pos="+b.position().subtract(Vec3.atLowerCornerOf(f.origin))+" state="+b.getBehaviorState()+" ground="+b.onGround()+" fish="+b.hasDeliveryFish());
        });
        h.runAtTickTime(500,()->{
            try {
                check(flew[0],"pet uses controlled flight when elevated owner has no ground path");
                var drops=f.level.getEntitiesOfClass(ItemEntity.class,f.owner.getBoundingBox().inflate(12),f::ownsDrop);
                check(!b.hasDeliveryFish() && (drops.size()==1 || fishCount(f.owner)==1),"flight return lands and delivers exactly one generated gift");
                h.succeed();
            } finally {f.close();}
        });
    }

}
