package EdDYON.guaniao.content.bird.nightheron;

import EdDYON.guaniao.content.bird.command.BirdCommandMode;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Follow is a persistent command, including after gifts and outside gift range. */
@GameTestHolder("guaniao_heron_qa")
@PrefixGameTestTemplate(false)
public final class NightHeronFollowGameTests {
    private static final class Scenario implements AutoCloseable {
        final GameTestHelper helper;
        final NightHeronGameTests.Fixture fixture;
        final NightHeronEntity bird;
        boolean closed;
        Vec3 lastPosition;
        long lastMotionTick=-1;
        final Set<ChunkPos> addedForcedChunks=new HashSet<>();

        Scenario(GameTestHelper helper, int endX, boolean tame) {
            this.helper=helper;
            // The persistent QA world may contain old flowing water above the original
            // templates. Run these long observations on a new elevated dry platform.
            this.fixture=new NightHeronGameTests.Fixture(helper,false,128);
            // Build actual walkable ground, not an artificial fence that contains bad AI.
            // A wider dry fixture also keeps surrounding test-world water out of the route.
            BlockPos origin=this.fixture.origin;
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-8,0,-8),origin.offset(endX,13,19)))
                this.fixture.level.setBlockAndUpdate(pos,pos.getY()<=origin.getY()+1
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            this.fixture.stranger.moveTo(origin.getX()-100,origin.getY()+2,origin.getZ()-100);
            this.bird=this.fixture.bird(tame);
            if(tame)NightHeronGameTests.timer(this.bird,72000);
        }

        void at(int tick,Runnable action) {
            this.helper.runAtTickTime(tick,()->{
                if(this.closed)return;
                try { action.run(); }
                catch(RuntimeException failure) {this.close();throw failure;}
            });
        }

        void require(boolean condition,String message) {
            if(!condition)throw new GameTestAssertException(message+" [tick="+this.helper.getTick()
                    +", bird="+this.bird.position().subtract(Vec3.atLowerCornerOf(this.fixture.origin))
                    +", owner="+this.fixture.owner.position().subtract(Vec3.atLowerCornerOf(this.fixture.origin))
                    +", state="+this.bird.getBehaviorState()+", command="+this.bird.getBirdCommandMode()
                    +", entityTicks="+this.bird.tickCount+", velocity="+this.bird.getDeltaMovement()
                    +", ground="+this.bird.onGround()+", mouth="+this.bird.getHeldFishForRendering()
                    +", feetFluid="+this.fixture.level.getFluidState(this.bird.blockPosition())
                    +", belowFluid="+this.fixture.level.getFluidState(this.bird.blockPosition().below())
                    +", aboveFluid="+this.fixture.level.getFluidState(this.bird.blockPosition().above())+"]");
        }

        void requireFollowing() {
            this.require(this.bird.isAlive() && this.bird.isOwnedBy(this.fixture.owner),"following pet remains alive and owned");
            this.require(this.bird.getBirdCommandMode()==BirdCommandMode.FOLLOW,"FOLLOW remains selected");
        }

        void observe(int first,int last,Runnable action) {
            for(int tick=first;tick<=last;tick++)this.at(tick,()->{
                long now=this.helper.getTick();
                if(this.lastPosition!=null && now==this.lastMotionTick+1)
                    this.require(this.bird.position().distanceToSqr(this.lastPosition)<=4.0,
                            "FOLLOW movement must remain continuous, never teleport");
                this.lastPosition=this.bird.position();this.lastMotionTick=now;
                action.run();
            });
        }

        void forceArea(BlockPos first,BlockPos last) {
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++) {
                    ChunkPos chunk=new ChunkPos(x,z);
                    if(!this.fixture.level.getForcedChunks().contains(chunk.toLong())
                            && this.fixture.level.setChunkForced(x,z,true))this.addedForcedChunks.add(chunk);
                }
            System.out.println("HERON_QA forced entity-ticking chunks added="+this.addedForcedChunks.size());
        }

        void succeed(String label) {
            NightHeronGameTests.check(true,label);
            this.close();this.helper.succeed();
        }

        @Override public void close() {
            if(this.closed)return;
            this.closed=true;
            try {this.fixture.close();}
            finally {
                // Preserve tickets installed by GameTest or any other fixture.
                for(ChunkPos chunk:this.addedForcedChunks)this.fixture.level.setChunkForced(chunk.x,chunk.z,false);
                this.addedForcedChunks.clear();
            }
        }
    }

    private static final class InteractionProbe {
        int nearbyGroundTicks;
        int consecutiveTicks;
        int longestDwell;
        void sample(Scenario s) {
            if(!s.bird.onGround() || s.bird.distanceToSqr(s.fixture.owner)>9
                    || !s.fixture.owner.hasLineOfSight(s.bird)
                    || !NightHeronFishing.safeStand(s.bird,s.bird.blockPosition())) {
                this.consecutiveTicks=0;return;
            }
            ++this.nearbyGroundTicks;
            this.longestDwell=Math.max(this.longestDwell,++this.consecutiveTicks);
        }
        void requireInteractable(Scenario s,String label) {
            s.require(this.longestDwell>=80 && this.consecutiveTicks>=40,
                    label+" [nearbyGroundTicks="+this.nearbyGroundTicks+", longestDwell="+this.longestDwell+", currentDwell="+this.consecutiveTicks+"]");
            s.require(s.bird.onGround() && s.bird.distanceToSqr(s.fixture.owner)<=9
                    && s.fixture.owner.hasLineOfSight(s.bird),"owner can reach and see the resting follower");
        }
    }

    @GameTest(template="shore",timeoutTicks=1100,batch="heron_follow_after_gift")
    public static void followAfterGiftRemainsWithOwner(GameTestHelper helper) {
        Scenario s=new Scenario(helper,27,true);
        NightHeronGameTests.gift(s.bird);
        int[] deliveredAt={-1};
        InteractionProbe resting=new InteractionProbe();
        s.observe(1,1000,()->{
            s.requireFollowing();
            if(!s.bird.hasDeliveryFish() && deliveredAt[0]<0) {
                s.require(s.bird.onGround(),"gift transfer still happens after landing");
                deliveredAt[0]=(int)helper.getTick();
            }
            if(deliveredAt[0]>=0 && helper.getTick()>deliveredAt[0]+40)resting.sample(s);
            if(helper.getTick()>25) {
                s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"pet must remain in the owner's nearby flight area after gifting");
                s.require(!s.bird.isInWaterOrBubble(),"FOLLOW remains on the dry owner platform throughout the test");
            }
        });
        s.at(1001,()->{
            s.require(deliveredAt[0]>0 && deliveredAt[0]<=200,"gift is delivered without ending the FOLLOW test");
            s.require(helper.getTick()-deliveredAt[0]>700,"normal FOLLOW is observed for over 700 ticks after delivery");
            var drops=s.fixture.level.getEntitiesOfClass(ItemEntity.class,s.fixture.owner.getBoundingBox().inflate(12),s.fixture::ownsDrop);
            int count=NightHeronGameTests.fishCount(s.fixture.owner);
            for(ItemEntity drop:drops)count+=drop.getItem().getCount();
            s.require(count==1,"extended FOLLOW creates no duplicate gift");
            resting.requireInteractable(s,"after delivering the gift, FOLLOW stays on nearby visible ground for interaction");
            s.succeed("FOLLOW remains accessible for over 700 ticks after gift delivery");
        });
    }

    @GameTest(template="shore",timeoutTicks=950,batch="heron_follow_far")
    public static void followBeyondGiftRange(GameTestHelper helper) {
        Scenario s=new Scenario(helper,99,true);
        // FakePlayer.moveTo does not move a real client's chunk-tracking center.
        // Keep the whole bounded flight corridor entity-ticking for this test only.
        s.forceArea(s.fixture.origin.offset(-16,0,-24),s.fixture.origin.offset(112,0,40));
        boolean[] crossedOutward={false},reachedFirst={false},crossedReturn={false};
        int[] lastLoggedEntityTick={-1};
        InteractionProbe resting=new InteractionProbe();
        s.at(40,()->{
            s.fixture.owner.moveTo(s.fixture.origin.getX()+85.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
            s.require(s.bird.distanceToSqr(s.fixture.owner)>64*64,"long-distance scenario exceeds both the old 32-block gift limit and 64-block flight limit");
        });
        // Retarget while the bird is still on its outward flight, then reverse course.
        s.at(100,()->s.fixture.owner.moveTo(s.fixture.origin.getX()+85.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+15.5));
        s.at(420,()->s.fixture.owner.moveTo(s.fixture.origin.getX()+35.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5));
        s.observe(1,899,()->{
            s.requireFollowing();
            if(helper.getTick()%100==0) {
                System.out.println("HERON_FOLLOW_DISTANCE tick="+helper.getTick()+" entityTick="+s.bird.tickCount
                        +" ticking="+s.fixture.level.isPositionEntityTicking(s.bird.blockPosition())
                        +" position="+s.bird.position().subtract(Vec3.atLowerCornerOf(s.fixture.origin))
                        +" velocity="+s.bird.getDeltaMovement()+" owner="+s.fixture.owner.position().subtract(Vec3.atLowerCornerOf(s.fixture.origin)));
                s.require(s.bird.tickCount>lastLoggedEntityTick[0],"far-flight fixture must continue ticking the bird, not freeze at a chunk boundary");
                lastLoggedEntityTick[0]=s.bird.tickCount;
            }
            double x=s.bird.getX()-s.fixture.origin.getX();
            if(helper.getTick()>40 && helper.getTick()<420) {
                if(!s.bird.onGround() && x>35 && x<55)crossedOutward[0]=true;
                if(s.bird.distanceToSqr(s.fixture.owner)<=100)reachedFirst[0]=true;
            }
            if(helper.getTick()>420 && !s.bird.onGround() && x>50 && x<70)crossedReturn[0]=true;
            if(helper.getTick()>650)resting.sample(s);
        });
        s.at(900,()->{
            s.require(crossedOutward[0] && reachedFirst[0] && crossedReturn[0],"long-distance FOLLOW actually flies through intermediate positions and retargets to the moving owner");
            s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"bird flies back into the moved owner's nearby area");
            resting.requireInteractable(s,"long-distance catch-up ends on nearby visible ground after the owner stops");
            s.succeed("FOLLOW crosses over 64 blocks and retargets continuously without any teleportation");
        });
    }

    @GameTest(template="shore",timeoutTicks=500,batch="heron_follow_day")
    public static void daytimeFollowRespondsToOwnerMovement(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,true);
        s.fixture.level.setDayTime(6000);
        InteractionProbe resting=new InteractionProbe();
        s.at(40,()->s.fixture.owner.moveTo(s.fixture.origin.getX()+22.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5));
        s.observe(1,399,()->{s.requireFollowing();if(helper.getTick()>160)resting.sample(s);});
        s.at(400,()->{
            s.require(!s.bird.isActiveTime(),"daytime scenario remains outside normal active hours");
            s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"an explicit FOLLOW command still follows during daytime");
            resting.requireInteractable(s,"daytime FOLLOW walks or flies to the owner and settles within reach");
            s.succeed("daytime rest does not override the owner's FOLLOW command");
        });
    }

    @GameTest(template="shore",timeoutTicks=750,batch="heron_follow_visitor")
    public static void ordinaryVisitorDoesNotScatterTamedFollower(GameTestHelper helper) {
        Scenario s=new Scenario(helper,27,true);
        s.fixture.stranger.setGameMode(GameType.SURVIVAL);
        InteractionProbe resting=new InteractionProbe();
        s.at(60,()->s.fixture.stranger.moveTo(s.fixture.origin.getX()+7.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5));
        s.observe(61,650,()->{
            s.requireFollowing();
            s.require(!s.bird.getBehaviorState().isEscape(),"an ordinary non-attacking visitor must not put a tamed follower into escape");
            s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"visitor must not drive the pet away from its owner");
            if(helper.getTick()>100)resting.sample(s);
        });
        s.at(651,()->{
            resting.requireInteractable(s,"pet stays accessible beside its owner and a peaceful visitor");
            s.succeed("ordinary nearby survival players do not scatter a tamed follower");
        });
    }

    @GameTest(template="shore",timeoutTicks=750,batch="heron_follow_flyby")
    public static void naturalFlybyCannotOverrideFollow(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,false);
        BlockPos away=s.fixture.origin.offset(27,2,5);
        s.bird.startFlybyFlight(new Vec3(1,0,0),away,600);
        s.bird.tame(s.fixture.owner);
        s.bird.setBirdCommandMode(BirdCommandMode.FOLLOW);
        InteractionProbe resting=new InteractionProbe();
        // An already queued natural flyby trigger is also possible after taming.
        s.at(60,()->s.bird.startFlybyFlight(new Vec3(1,0,0),away,600));
        s.observe(20,650,()->{
            s.requireFollowing();
            s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"old or queued natural flyby cannot carry a follower away");
            if(helper.getTick()>100)resting.sample(s);
            if(helper.getTick()>80)s.require(s.bird.getBehaviorState()!=NightHeronBehaviorState.HIGH_TRANSIT,
                    "FOLLOW must release the natural long-transit state");
        });
        s.at(651,()->{
            resting.requireInteractable(s,"follower settles beside its owner after interrupted natural flyby");
            s.succeed("taming and FOLLOW cancel old and queued natural flyby movement");
        });
    }

    @GameTest(template="shore",timeoutTicks=850,batch="heron_follow_elevated")
    public static void followFliesToElevatedOwnerAndStays(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,true);
        for(BlockPos pos:BlockPos.betweenClosed(s.fixture.origin.offset(17,2,4),s.fixture.origin.offset(20,4,7)))
            s.fixture.level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        s.at(40,()->s.fixture.owner.moveTo(s.fixture.origin.getX()+18.5,s.fixture.origin.getY()+5,s.fixture.origin.getZ()+5.5));
        boolean[] flew={false};
        int[] arrived={-1};
        InteractionProbe resting=new InteractionProbe();
        s.observe(41,750,()->{
            s.requireFollowing();
            if(s.bird.getY()>s.fixture.origin.getY()+2.8)flew[0]=true;
            if(arrived[0]<0 && s.bird.onGround() && Math.abs(s.bird.getY()-s.fixture.owner.getY())<0.5
                    && s.bird.distanceToSqr(s.fixture.owner)<=9)arrived[0]=(int)helper.getTick();
            if(arrived[0]>=0) {
                s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"pet must stay around the elevated owner after arrival");
                resting.sample(s);
            }
        });
        s.at(751,()->{
            s.require(flew[0],"FOLLOW uses flight when the elevated owner has no ground path");
            s.require(arrived[0]>0 && arrived[0]<=550,"FOLLOW lands at the elevated owner's accessible height");
            resting.requireInteractable(s,"pet lands within reach of its elevated owner");
            s.succeed("FOLLOW flies to a raised owner and lands at an accessible height");
        });
    }

    @GameTest(template="shore",timeoutTicks=1100,batch="heron_follow_shore_landing")
    public static void shoreDeliveryLandsBeforeWaitingAndKeepsFollowing(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,true);
        buildPool(s);
        s.fixture.owner.moveTo(s.fixture.origin.getX()+13.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
        NightHeronGameTests.gift(s.bird);
        boolean[] flew={false};
        int[] deliveredAt={-1};
        InteractionProbe resting=new InteractionProbe();
        s.observe(1,1000,()->{
            s.requireFollowing();
            if(s.bird.getY()>s.fixture.origin.getY()+2.8)flew[0]=true;
            s.require(!s.bird.isInWaterOrBubble(),"approaching the owner must finish landing on the bank, never drop into the pool");
            if(!s.bird.hasDeliveryFish() && deliveredAt[0]<0) {
                s.require(s.bird.onGround() && NightHeronFishing.safeStand(s.bird,s.bird.blockPosition()),"shore gift transfer requires landing on dry ground");
                deliveredAt[0]=(int)helper.getTick();
            }
            if(deliveredAt[0]>=0) {
                s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,"after shore delivery FOLLOW must remain near the owner");
                if(helper.getTick()>deliveredAt[0]+40)resting.sample(s);
            }
        });
        s.at(1001,()->{
            s.require(flew[0],"shore fixture actually exercises flying navigation across water");
            s.require(deliveredAt[0]>0 && deliveredAt[0]<=550,"shore delivery completes with over 400 ticks left to observe FOLLOW");
            var drops=s.fixture.level.getEntitiesOfClass(ItemEntity.class,s.fixture.owner.getBoundingBox().inflate(12),s.fixture::ownsDrop);
            int count=NightHeronGameTests.fishCount(s.fixture.owner);
            for(ItemEntity drop:drops)count+=drop.getItem().getCount();
            s.require(count==1 && s.bird.distanceToSqr(s.fixture.owner)<=144,
                    "one gift is delivered and the bird remains near the owner");
            resting.requireInteractable(s,"shore delivery is followed by accessible ground companionship without entering the water");
            s.succeed("shore approach finishes its landing and remains stable for over 400 ticks after delivery");
        });
    }

    private static void buildPool(Scenario s) {
        // Two banks separated by a short pool. The owner is about two blocks from
        // the water, so merely being inside the follow radius is not a safe landing.
        for(BlockPos pos:BlockPos.betweenClosed(s.fixture.origin.offset(5,-1,-25),s.fixture.origin.offset(12,1,35))) {
            boolean wall=pos.getX()==s.fixture.origin.getX()+5 || pos.getX()==s.fixture.origin.getX()+12
                    || pos.getZ()==s.fixture.origin.getZ()-25 || pos.getZ()==s.fixture.origin.getZ()+35
                    || pos.getY()==s.fixture.origin.getY()-1;
            s.fixture.level.setBlockAndUpdate(pos,wall?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState());
        }
    }

    @GameTest(template="shore",timeoutTicks=1100,batch="heron_follow_owner_unavailable")
    public static void ownerUnavailableDuringFlightStillLandsAndResumesFollow(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,true);
        buildPool(s);
        s.fixture.owner.moveTo(s.fixture.origin.getX()+13.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
        int[] unavailableAt={-1},landedAt={-1};
        InteractionProbe resting=new InteractionProbe();
        s.observe(1,1000,()->{
            s.requireFollowing();
            s.require(!s.bird.isInWaterOrBubble(),"temporarily unavailable owner must not make flying pet fall into the pool");
            double x=s.bird.getX()-s.fixture.origin.getX();
            if(unavailableAt[0]<0 && !s.bird.onGround() && s.bird.getY()>s.fixture.origin.getY()+2.8 && x>6 && x<12) {
                unavailableAt[0]=(int)helper.getTick();
                s.fixture.owner.setGameMode(GameType.SPECTATOR);
                s.require(s.bird.fishTask().owner()==null,"spectator owner exercises the unavailable-owner branch during actual flight");
                System.out.println("HERON_FOLLOW_OWNER_UNAVAILABLE tick="+helper.getTick()+" bird="+s.bird.position());
            }
            if(unavailableAt[0]>0 && landedAt[0]<0 && helper.getTick()>unavailableAt[0]+2 && s.bird.onGround()) {
                s.require(NightHeronFishing.safeStand(s.bird,s.bird.blockPosition()),"pet finishes on safe dry ground while its owner is unavailable");
                s.require(s.fixture.owner.isSpectator(),"owner remains unavailable until safe landing completes");
                landedAt[0]=(int)helper.getTick();
                s.fixture.owner.setGameMode(GameType.SURVIVAL);
                s.fixture.owner.moveTo(s.fixture.origin.getX()+21.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+9.5);
                System.out.println("HERON_FOLLOW_OWNER_RETURN tick="+helper.getTick()+" bird="+s.bird.position()+" owner="+s.fixture.owner.position());
            }
            if(landedAt[0]>0 && helper.getTick()>landedAt[0]+100)resting.sample(s);
        });
        s.at(1001,()->{
            s.require(unavailableAt[0]>0 && landedAt[0]>unavailableAt[0] && landedAt[0]<=650,
                    "owner loss happens in flight and the pet completes its original safe landing");
            s.require(!s.fixture.owner.isSpectator() && s.bird.distanceToSqr(s.fixture.owner)<=144,
                    "pet resumes FOLLOW after the owner becomes available at a new position");
            resting.requireInteractable(s,"returning owner receives an accessible companion after the safe landing");
            s.succeed("owner loss during water crossing preserves safe landing and FOLLOW resumes when the owner returns");
        });
    }

    @GameTest(template="shore",timeoutTicks=750,batch="heron_follow_flock_warning")
    public static void passiveFlockWarningDiffersFromRealDamage(GameTestHelper helper) {
        Scenario s=new Scenario(helper,27,true);
        InteractionProbe resting=new InteractionProbe();
        s.at(20,()->{
            s.bird.startEatingFish(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD),65);
            Vec3 flockSource=s.bird.position().add(3,0,0);
            s.bird.receiveFlockFright(flockSource,false);
            s.require(!s.bird.hasExternalFright() && s.bird.isEatingFish(),"passive flock warning must not frighten a pet or interrupt its meal");
            s.bird.receiveFlockFright(flockSource,true);
            s.require(!s.bird.hasExternalFright() && s.bird.isEatingFish(),"a wild bird's stronger warning also cannot replace the pet's calm owner interaction");
            float health=s.bird.getHealth();
            s.require(s.bird.hurt(s.bird.damageSources().playerAttack(s.fixture.stranger),1),"real attack still damages the pet");
            s.require(s.bird.getHealth()<health && s.bird.hasExternalFright() && s.bird.isBirdEmergencyOverrideActive()
                    && !s.bird.isEatingFish(),"real damage retains emergency reaction and interrupts eating");
            s.requireFollowing();
        });
        s.observe(21,650,()->{s.requireFollowing();if(helper.getTick()>400)resting.sample(s);});
        s.at(250,()->{
            s.require(!s.bird.hasExternalFright(),"a single hit does not maintain the emergency forever");
            s.fixture.owner.moveTo(s.fixture.origin.getX()+19.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+8.5);
        });
        s.at(651,()->{
            s.require(!s.bird.hasExternalFright() && !s.bird.isBirdEmergencyOverrideActive(),"temporary damage response ends without clearing FOLLOW");
            s.require(s.bird.distanceToSqr(s.fixture.owner)<=144,
                    "after avoiding real danger the pet automatically follows the owner's subsequent movement");
            resting.requireInteractable(s,"recovered pet settles beside its moved owner after the temporary danger");
            s.succeed("passive warnings are ignored, real damage triggers avoidance, and FOLLOW resumes through 630 more ticks");
        });
    }

    @GameTest(template="shore",timeoutTicks=1050,batch="heron_follow_stay")
    public static void stayStopsCirclingUntilFollowIsSelectedAgain(GameTestHelper helper) {
        Scenario s=new Scenario(helper,35,true);
        Vec3[] stayPosition={null};
        int[] stayAt={-1};
        InteractionProbe resumed=new InteractionProbe();
        s.observe(1,950,()->{
            long tick=helper.getTick();
            // GameTest does not guarantee ordering between separate tasks at the
            // same tick. Perform each transition before its assertions here.
            if(tick==150)s.fixture.owner.moveTo(s.fixture.origin.getX()+28.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
            if(tick>150 && tick<220 && stayAt[0]<0 && !s.bird.onGround()
                    && s.bird.getY()>s.fixture.origin.getY()+2.8) {
                stayAt[0]=(int)tick;
                stayPosition[0]=s.bird.position();
                s.bird.setBirdCommandMode(BirdCommandMode.STAY);
            }
            if(tick==230)s.fixture.owner.moveTo(s.fixture.origin.getX()+23.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
            if(tick==500) {
                s.require(stayAt[0]>150,"STAY scenario actually interrupts a FOLLOW flight");
                s.require(s.bird.getBirdCommandMode()==BirdCommandMode.STAY && s.bird.onGround()
                        && NightHeronFishing.safeStand(s.bird,s.bird.blockPosition()),"STAY ends the orbit and settles on dry ground");
                s.require(s.bird.distanceToSqr(s.fixture.owner)>64,"STAY does not follow the owner who moved away");
                s.bird.setBirdCommandMode(BirdCommandMode.FOLLOW);
            }
            if(stayAt[0]<0 || tick<stayAt[0] || tick>=500)s.requireFollowing();
            else {
                s.require(s.bird.getBirdCommandMode()==BirdCommandMode.STAY,"STAY remains selected despite owner movement");
                s.require(s.bird.position().subtract(stayPosition[0]).horizontalDistanceSqr()<=4,
                        "STAY stops horizontal circling instead of continuing to chase the owner");
            }
            if(tick>650)resumed.sample(s);
        });
        s.at(951,()->{
            resumed.requireInteractable(s,"selecting FOLLOW again returns the pet to reachable ground beside the moved owner");
            s.succeed("STAY stops the flight, holds position, and FOLLOW returns to the owner when selected again");
        });
    }

    @GameTest(template="shore",timeoutTicks=1150,batch="heron_follow_mixed")
    public static void movingOwnerGetsGroundAndAirCompanionshipThenCanInteract(GameTestHelper helper) {
        Scenario s=new Scenario(helper,55,true);
        s.forceArea(s.fixture.origin.offset(-16,0,-24),s.fixture.origin.offset(64,0,40));
        int[] walkingTicks={0},airTicks={0},groundAfterFlight={0},firstAir={-1};
        int[] sectors={0};
        InteractionProbe resting=new InteractionProbe();
        Vec3[] previous={s.bird.position()};
        s.observe(1,1050,()->{
            long tick=helper.getTick();
            // A genuinely moving owner, rather than occasional large relocations that
            // correctly select urgent flight, exercises the ordinary walk/orbit cycle.
            if(tick<=650)s.fixture.owner.moveTo(s.fixture.origin.getX()+4.5+tick*0.045,
                    s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5);
            s.requireFollowing();
            s.require(!s.bird.isInWaterOrBubble(),"mixed FOLLOW remains on the dry test route");
            if(tick<=650) {
                if(s.bird.onGround()) {
                    if(s.bird.position().subtract(previous[0]).horizontalDistanceSqr()>0.000225)++walkingTicks[0];
                    if(firstAir[0]>0)++groundAfterFlight[0];
                } else if(s.bird.getY()>s.fixture.owner.getY()+0.5) {
                    if(firstAir[0]<0)firstAir[0]=(int)tick;
                    ++airTicks[0];
                    Vec3 relative=s.bird.position().subtract(s.fixture.owner.position());
                    if(relative.horizontalDistance()<9) {
                        double angle=Math.atan2(relative.z,relative.x);
                        sectors[0]|=1<<Math.min(7,(int)((angle+Math.PI)*8/(2*Math.PI)));
                    }
                }
                s.require(s.bird.distanceToSqr(s.fixture.owner)<=196,"ordinary alternating FOLLOW stays near its moving owner");
            } else resting.sample(s);
            previous[0]=s.bird.position();
        });
        s.at(1051,()->{
            s.require(walkingTicks[0]>=60 && firstAir[0]>=150,
                    "moving owner first gets sustained real ground walking [walkTicks="+walkingTicks[0]+", firstAir="+firstAir[0]+"]");
            s.require(airTicks[0]>=60 && Integer.bitCount(sectors[0])>=4,
                    "ordinary FOLLOW also spends a flight phase circling the moving owner [airTicks="+airTicks[0]+", sectors="+Integer.bitCount(sectors[0])+"]");
            s.require(groundAfterFlight[0]>=60,"the flight phase returns to ground while the owner is still moving");
            resting.requireInteractable(s,"stopping ends flight and leaves a continuously reachable companion");
            s.fixture.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            s.fixture.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            s.fixture.owner.setShiftKeyDown(true);
            var result=s.bird.mobInteract(s.fixture.owner,InteractionHand.MAIN_HAND);
            s.require(result.consumesAction() && s.bird.getBirdCommandMode()!=BirdCommandMode.FOLLOW,
                    "owner can use the existing empty-hand command interaction after the real landing");
            s.fixture.owner.setShiftKeyDown(false);
            s.succeed("FOLLOW alternates ground walking and flight, then lands within visible right-click reach when its owner stops");
        });
    }

    @GameTest(template="shore",timeoutTicks=700,batch="heron_follow_low_ceiling")
    public static void lowCeilingUsesGroundFollowInsteadOfForcedTakeoff(GameTestHelper helper) {
        Scenario s=new Scenario(helper,31,true);
        // Two blocks of clear height fit the standing bird and player, but do not
        // provide the vertical clearance required by the real takeoff controller.
        for(BlockPos pos:BlockPos.betweenClosed(s.fixture.origin.offset(-8,4,-8),s.fixture.origin.offset(31,4,19)))
            s.fixture.level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        s.at(40,()->s.fixture.owner.moveTo(s.fixture.origin.getX()+22.5,s.fixture.origin.getY()+2,s.fixture.origin.getZ()+5.5));
        s.observe(1,600,()->{
            s.requireFollowing();
            s.require(!s.bird.getBehaviorState().isAirborne() && s.bird.getY()<=s.fixture.origin.getY()+2.25,
                    "low ceiling must use ground movement instead of repeated takeoff into the roof");
        });
        s.at(601,()->{
            s.require(s.bird.onGround() && s.bird.distanceToSqr(s.fixture.owner)<=25,
                    "ground FOLLOW reaches the moved owner under the low ceiling");
            s.succeed("low ceiling prevents takeoff while available ground navigation still follows the owner");
        });
    }
}
