package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.client.entity.budgerigar.BudgerigarModel;
import EdDYON.guaniao.client.entity.cockatiel.CockatielModel;
import EdDYON.guaniao.client.entity.longtailedtit.LongTailedTitModel;
import EdDYON.guaniao.client.entity.myna.MynaModel;
import EdDYON.guaniao.client.entity.sparrow.SparrowModel;
import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.cockatiel.CockatielEntity;
import EdDYON.guaniao.content.bird.longtailedtit.LongTailedTitEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;

/** Two silent, unspawned GUI actors. No AI, world particles or entity ticks. */
@Mod.EventBusSubscriber(modid="guaniao",value=Dist.CLIENT)
public final class FrameWorkshopBird {
    private static final Species[] SPECIES={
            new Species("sparrow",1.30F,1.65F,.65F),
            new Species("long_tailed_tit",2.30F,2.60F,.85F),
            new Species("cockatiel",.95F,1.10F,.65F),
            new Species("budgerigar",.90F,1.15F,.60F),
            new Species("myna",1.20F,1.55F,.70F)
    };
    private static Actor<?> actor;
    private static long actorSeed;
    private static Actor<?> progressActor;
    private static long progressActorSeed;
    private static final long PROGRESS_SEED_SALT=0x6a09e667f3bcc909L;
    private static double workTicks;
    private static float headYaw,headPitch,headRoll;

    public enum Action { IDLE, LOOK_AROUND, FLY, WALK, EAT }

    private FrameWorkshopBird() { }

    static void clear() {actor=null;progressActor=null;workTicks=0;headYaw=0;headPitch=0;headRoll=0;}

    @SubscribeEvent public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {clear();}

    /** Stable across reopening the same job, independently of render count and the world RNG. */
    static String speciesForSeed(long seed) {return species(seed).id;}

    static String progressSpeciesForSeed(long seed) {return species(seed^PROGRESS_SEED_SALT).id;}

    /** A fresh default stack; each choice belongs to the corresponding bird's existing food tag. */
    static ItemStack progressFood(long seed) {
        return new ItemStack(switch(progressSpeciesForSeed(seed)) {
            case "long_tailed_tit" -> Items.SWEET_BERRIES;
            case "budgerigar" -> Items.MELON_SEEDS;
            case "myna" -> Items.APPLE;
            default -> Items.WHEAT_SEEDS;
        });
    }

    private static Species species(long seed) {
        long mixed=(seed^(seed>>>33))*0xff51afd7ed558ccdL;
        mixed=(mixed^(mixed>>>33))*0xc4ceb9fe1a85ec53L;
        return SPECIES[Math.floorMod(mixed^(mixed>>>33),SPECIES.length)];
    }

    /** Standing height which fits the complete wing cycle and tail in both dimensions. */
    static float fittedStandingHeight(long seed,float size) {
        Species species=species(seed);
        float box=Float.isFinite(size) ? Mth.clamp(size,0,56) : 0;
        return box*.92F/Math.max(species.halfWidth*2,species.aboveOrigin+species.belowOrigin);
    }

    static void render(GuiGraphics graphics,float x,float feetY,float height,float yaw,float lean,
                       float ticks,long seed,Action action) {
        render(graphics,x,feetY,height,yaw,lean,ticks,seed,action,0,0,0);
    }

    static void render(GuiGraphics graphics,float x,float feetY,float height,float yaw,float lean,
                       float ticks,long seed,Action action,float lookYaw,float lookPitch) {
        render(graphics,x,feetY,height,yaw,lean,ticks,seed,action,lookYaw,lookPitch,0);
    }

    static void render(GuiGraphics graphics,float x,float feetY,float height,float yaw,float lean,
                       float ticks,long seed,Action action,float lookYaw,float lookPitch,float lookRoll) {
        Minecraft minecraft=Minecraft.getInstance();
        float standingHeight=fittedStandingHeight(seed,height);
        if(minecraft.level==null || standingHeight<=0)return;
        if(actor==null || actor.bird.level()!=minecraft.level || actorSeed!=seed) {
            actor=createActor(minecraft,species(seed));actorSeed=seed;
            if(actor==null)return;
        }
        renderActor(graphics,actor,x,feetY,standingHeight,yaw,lean,ticks,action,lookYaw,lookPitch,lookRoll);
    }

    /** A second one-entry cache keeps the progress bird independent of the curious visitor. */
    static void renderProgress(GuiGraphics graphics,float x,float feetY,float size,float yaw,float lean,
                               float ticks,long seed,Action action) {
        Minecraft minecraft=Minecraft.getInstance();
        long selectionSeed=seed^PROGRESS_SEED_SALT;
        float standingHeight=fittedStandingHeight(selectionSeed,size);
        if(minecraft.level==null || standingHeight<=0)return;
        if(progressActor==null || progressActor.bird.level()!=minecraft.level || progressActorSeed!=seed) {
            progressActor=createActor(minecraft,species(selectionSeed));progressActorSeed=seed;
            if(progressActor==null)return;
        }
        renderActor(graphics,progressActor,x,feetY,standingHeight,yaw,lean,ticks,action,0,0,0);
    }

    private static void renderActor(GuiGraphics graphics,Actor<?> current,float x,float feetY,float standingHeight,
                                    float yaw,float lean,float ticks,Action action,float lookYaw,float lookPitch,float lookRoll) {
        Minecraft minecraft=Minecraft.getInstance();
        Mob bird=current.bird;
        float time=Float.isFinite(ticks) ? Math.max(0,ticks) : 0;
        bird.tickCount=Math.max(1,(int)time+1);
        workTicks=time+1D;
        headYaw=headAngle(lookYaw,25);headPitch=headAngle(lookPitch,18);headRoll=headAngle(lookRoll,12);
        applyAction(bird,action);
        bird.setOnGround(action!=Action.FLY);bird.setDeltaMovement(Vec3.ZERO);
        bird.yBodyRot=bird.yBodyRotO=bird.yHeadRot=bird.yHeadRotO=180+yaw;
        bird.setYRot(180+yaw);bird.yRotO=180+yaw;bird.setXRot(0);bird.xRotO=0;
        float modelHeight=Math.max(.05F,current.bird.modelScaleProfile().targetHeightBlocks());
        float scale=standingHeight/modelHeight;
        var dispatcher=minecraft.getEntityRenderDispatcher();
        Quaternionf oldCamera=new Quaternionf(dispatcher.cameraOrientation());
        graphics.flush();graphics.pose().pushPose();
        try {
            graphics.pose().translate(x,feetY,260);
            graphics.pose().scale(scale,scale,-scale);
            Quaternionf tilt=new Quaternionf().rotateX(-8*Mth.DEG_TO_RAD).rotateZ(lean*Mth.DEG_TO_RAD);
            graphics.pose().mulPose(new Quaternionf().rotateZ((float)Math.PI).mul(tilt));
            dispatcher.overrideCameraOrientation(new Quaternionf(tilt).conjugate());
            Lighting.setupForEntityInInventory();RenderSystem.enableDepthTest();
            current.render(graphics,time);
            graphics.flush();
        } finally {
            graphics.pose().popPose();dispatcher.overrideCameraOrientation(oldCamera);
            Lighting.setupFor3DItems();RenderSystem.setShaderColor(1,1,1,1);
        }
    }

    private static Actor<?> createActor(Minecraft minecraft,Species species) {
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(new ResourceLocation("guaniao",species.id));
        var created=type.isPresent() ? type.get().create(minecraft.level) : null;
        // Subclass checks precede their sparrow/budgerigar bases to retain each real model and texture.
        if(created instanceof LongTailedTitEntity bird)return new Actor<>(minecraft,bird,new ObserverTitModel());
        if(created instanceof MynaEntity bird)return new Actor<>(minecraft,bird,new ObserverMynaModel());
        if(created instanceof CockatielEntity bird)return new Actor<>(minecraft,bird,new ObserverCockatielModel());
        if(created instanceof BudgerigarEntity bird)return new Actor<>(minecraft,bird,new ObserverBudgerigarModel());
        if(created instanceof SparrowEntity bird)return new Actor<>(minecraft,bird,new ObserverSparrowModel());
        return null;
    }

    private static void applyAction(Mob bird,Action action) {
        if(bird instanceof MynaEntity myna) {
            myna.setGuidePreviewAnimation(switch(action) {
                case IDLE -> MynaEntity.GuidePreviewAnimation.IDLE;
                case LOOK_AROUND -> MynaEntity.GuidePreviewAnimation.IDLE_2;
                case FLY -> MynaEntity.GuidePreviewAnimation.FLY;
                case WALK -> MynaEntity.GuidePreviewAnimation.WALK;
                // This species has no separate eat clip; its second idle is the existing low peck.
                case EAT -> MynaEntity.GuidePreviewAnimation.IDLE_2;
            });
        }else if(bird instanceof SparrowEntity sparrow) {
            sparrow.setGuidePreviewAnimation(switch(action) {
                case IDLE -> SparrowEntity.GuidePreviewAnimation.IDLE;
                case LOOK_AROUND -> SparrowEntity.GuidePreviewAnimation.LOOK_AROUND;
                case FLY -> SparrowEntity.GuidePreviewAnimation.FLY;
                case WALK -> SparrowEntity.GuidePreviewAnimation.WALK;
                case EAT -> SparrowEntity.GuidePreviewAnimation.PECK;
            });
        }else if(bird instanceof BudgerigarEntity budgerigar) {
            budgerigar.setGuidePreviewAnimation(switch(action) {
                case IDLE -> BudgerigarEntity.GuidePreviewAnimation.IDLE;
                case LOOK_AROUND -> BudgerigarEntity.GuidePreviewAnimation.CURIOUS;
                case FLY -> BudgerigarEntity.GuidePreviewAnimation.FLY;
                case WALK -> BudgerigarEntity.GuidePreviewAnimation.WALK;
                case EAT -> BudgerigarEntity.GuidePreviewAnimation.EAT;
            });
        }
    }

    /** Handbook preview envelopes, in standing-height units, including full wings and long tails. */
    private record Species(String id,float halfWidth,float aboveOrigin,float belowOrigin) { }

    private static final class Actor<T extends Mob & GeoAnimatable & ScalableBirdModel> {
        private final T bird;
        private final GeoEntityRenderer<T> renderer;
        private Actor(Minecraft minecraft,T bird,GeoModel<T> model) {
            this.bird=bird;
            bird.setNoAi(true);bird.setSilent(true);bird.setNoGravity(true);bird.setOnGround(true);
            bird.setIndividualModelScale(1);bird.setDeltaMovement(Vec3.ZERO);
            var dispatcher=minecraft.getEntityRenderDispatcher();
            renderer=new GeoEntityRenderer<>(new EntityRendererProvider.Context(dispatcher,minecraft.getItemRenderer(),
                    minecraft.getBlockRenderer(),dispatcher.getItemInHandRenderer(),minecraft.getResourceManager(),
                    minecraft.getEntityModels(),minecraft.font),model);
        }
        private void render(GuiGraphics graphics,float ticks) {
            // GUI framing is independent of the world's individual-size and April Fools multipliers.
            renderer.withScale(bird.modelScaleProfile().baseRenderScale());
            renderer.render(bird,0,ticks-(int)ticks,graphics.pose(),graphics.bufferSource(),LightTexture.FULL_BRIGHT);
        }
    }

    /** GeckoLib adds Minecraft's partial tick internally; cancel it only for the isolated actor. */
    private static <T extends GeoAnimatable> void clock(AnimationState<T> state) {
        state.setData(DataTickets.TICK,workTicks-Minecraft.getInstance().getFrameTime());
    }

    private static float headAngle(float degrees,float limit) {
        return Float.isFinite(degrees) ? Mth.clamp(degrees,-limit,limit) : 0;
    }

    private static void look(GeoModel<?> model) {
        model.getBone("head").ifPresent(head -> {
            head.setRotX(head.getRotX()+headPitch*Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY()+headYaw*Mth.DEG_TO_RAD);
            head.setRotZ(head.getRotZ()+headRoll*Mth.DEG_TO_RAD);
            // This visual offset must not masquerade as a keyframed bone update next frame.
            // Gecko then restores its saved animation pose even in clips with no head track.
            head.resetStateChanges();
        });
    }

    /** Gecko's baked bones are mutable and shared globally; each local actor needs its own pose tree. */
    private static final class LocalBones {
        private BakedGeoModel source,local;
        private BakedGeoModel get(GeoModel<?> owner,BakedGeoModel baked) {
            if(source!=baked) {
                source=baked;
                var roots=new ArrayList<GeoBone>(baked.topLevelBones().size());
                for(GeoBone bone:baked.topLevelBones())roots.add(copyBone(bone,null));
                local=new BakedGeoModel(roots,baked.properties());
                owner.getAnimationProcessor().setActiveModel(local);
            }
            return local;
        }
    }

    private static GeoBone copyBone(GeoBone source,GeoBone parent) {
        var copy=new GeoBone(parent,source.getName(),source.getMirror(),source.getInflate(),
                source.shouldNeverRender(),source.getReset());
        var rest=source.getInitialSnapshot();
        copy.setRotX(rest==null ? source.getRotX() : rest.getRotX());
        copy.setRotY(rest==null ? source.getRotY() : rest.getRotY());
        copy.setRotZ(rest==null ? source.getRotZ() : rest.getRotZ());
        copy.setPosX(rest==null ? source.getPosX() : rest.getOffsetX());
        copy.setPosY(rest==null ? source.getPosY() : rest.getOffsetY());
        copy.setPosZ(rest==null ? source.getPosZ() : rest.getOffsetZ());
        copy.setScaleX(rest==null ? source.getScaleX() : rest.getScaleX());
        copy.setScaleY(rest==null ? source.getScaleY() : rest.getScaleY());
        copy.setScaleZ(rest==null ? source.getScaleZ() : rest.getScaleZ());
        copy.setPivotX(source.getPivotX());copy.setPivotY(source.getPivotY());copy.setPivotZ(source.getPivotZ());
        // Cube geometry is read-only. Only bones, transforms and animation snapshots are isolated.
        copy.getCubes().addAll(source.getCubes());
        for(GeoBone child:source.getChildBones())copy.getChildBones().add(copyBone(child,copy));
        copy.resetStateChanges();
        return copy;
    }

    private static final class ObserverSparrowModel extends SparrowModel {
        private final LocalBones bones=new LocalBones();
        @Override public BakedGeoModel getBakedModel(ResourceLocation location) {
            return bones.get(this,super.getBakedModel(location));
        }
        @Override public void handleAnimations(SparrowEntity bird,long id,AnimationState<SparrowEntity> state) {
            clock(state);super.handleAnimations(bird,id,state);
        }
        @Override public void setCustomAnimations(SparrowEntity bird,long id,AnimationState<SparrowEntity> state) {
            super.setCustomAnimations(bird,id,state);look(this);
        }
    }
    private static final class ObserverTitModel extends LongTailedTitModel {
        private final LocalBones bones=new LocalBones();
        @Override public BakedGeoModel getBakedModel(ResourceLocation location) {
            return bones.get(this,super.getBakedModel(location));
        }
        @Override public void handleAnimations(LongTailedTitEntity bird,long id,AnimationState<LongTailedTitEntity> state) {
            clock(state);super.handleAnimations(bird,id,state);
        }
        @Override public void setCustomAnimations(LongTailedTitEntity bird,long id,AnimationState<LongTailedTitEntity> state) {
            super.setCustomAnimations(bird,id,state);look(this);
        }
    }
    private static final class ObserverMynaModel extends MynaModel {
        private final LocalBones bones=new LocalBones();
        @Override public BakedGeoModel getBakedModel(ResourceLocation location) {
            return bones.get(this,super.getBakedModel(location));
        }
        @Override public void handleAnimations(MynaEntity bird,long id,AnimationState<MynaEntity> state) {
            clock(state);super.handleAnimations(bird,id,state);
        }
        @Override public void setCustomAnimations(MynaEntity bird,long id,AnimationState<MynaEntity> state) {
            super.setCustomAnimations(bird,id,state);look(this);
        }
    }
    private static final class ObserverCockatielModel extends CockatielModel {
        private final LocalBones bones=new LocalBones();
        @Override public BakedGeoModel getBakedModel(ResourceLocation location) {
            return bones.get(this,super.getBakedModel(location));
        }
        @Override public void handleAnimations(CockatielEntity bird,long id,AnimationState<CockatielEntity> state) {
            clock(state);super.handleAnimations(bird,id,state);
        }
        @Override public void setCustomAnimations(CockatielEntity bird,long id,AnimationState<CockatielEntity> state) {
            super.setCustomAnimations(bird,id,state);look(this);
        }
    }
    private static final class ObserverBudgerigarModel extends BudgerigarModel {
        private final LocalBones bones=new LocalBones();
        @Override public BakedGeoModel getBakedModel(ResourceLocation location) {
            return bones.get(this,super.getBakedModel(location));
        }
        @Override public void handleAnimations(BudgerigarEntity bird,long id,AnimationState<BudgerigarEntity> state) {
            clock(state);super.handleAnimations(bird,id,state);
        }
        @Override public void setCustomAnimations(BudgerigarEntity bird,long id,AnimationState<BudgerigarEntity> state) {
            super.setCustomAnimations(bird,id,state);look(this);
        }
    }
}
