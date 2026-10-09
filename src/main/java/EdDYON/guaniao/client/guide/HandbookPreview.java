package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.content.bird.budgerigar.BudgerigarEntity;
import EdDYON.guaniao.content.bird.hummingbird.HummingbirdEntity;
import EdDYON.guaniao.content.bird.columbid.AbstractColumbidEntity;
import EdDYON.guaniao.content.bird.crow.CrowEntity;
import EdDYON.guaniao.content.bird.kiwi.KiwiEntity;
import EdDYON.guaniao.content.bird.myna.MynaEntity;
import EdDYON.guaniao.content.bird.nightheron.NightHeronEntity;
import EdDYON.guaniao.content.bird.seagull.SeagullEntity;
import EdDYON.guaniao.content.bird.kestrel.KestrelEntity;
import EdDYON.guaniao.content.bird.cassowary.CassowaryEntity;
import EdDYON.guaniao.content.bird.scale.BirdModelScale;
import EdDYON.guaniao.content.bird.sparrow.SparrowEntity;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.joml.Quaternionf;

/** One unspawned client model; no world AI, sound, or gameplay state is advanced. */
final class HandbookPreview {
    private LivingEntity entity;
    private String species = "";
    private int pose;
    private float yaw = 30, pitch = -12, zoom = 1;
    void select(String id) {
        if (species.equals(id)) return;
        species=id; entity=null; pose=0; yaw=30; pitch=-12; zoom=1;
        var level=Minecraft.getInstance().level;
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(new ResourceLocation("guaniao",id));
        if (level!=null && type.isPresent() && type.get().create(level) instanceof LivingEntity bird) {
            entity=bird;
            if (bird instanceof Mob mob) mob.setNoAi(true);
            if (bird instanceof ScalableBirdModel scalable) scalable.setIndividualModelScale(1);
            bird.setSilent(true); bird.setNoGravity(true); bird.setOnGround(true);
            tick();
        }
    }
    void clear() { entity=null; species=""; }
    boolean flightless() { return species.equals("kiwi") || species.equals("cassowary"); }
    void pose(int value) { if (value==2 && flightless()) return; pose=value; }
    void tick() {
        if (entity==null) return;
        ++entity.tickCount;
        applyPreviewAnimation(entity,switch(pose) {
            case 1 -> species.equals("night_heron") ? GuidePreviewAnimation.SCRATCH : GuidePreviewAnimation.LOOK_2;
            case 2 -> GuidePreviewAnimation.FLY_FLAP;
            case 3 -> GuidePreviewAnimation.LOOK_3;
            default -> GuidePreviewAnimation.IDLE;
        });
    }
    void drag(double x,double y) { yaw=Mth.wrapDegrees(yaw+(float)x*1.7f); pitch=Mth.clamp(pitch-(float)y*.7f,-35,35); }
    void zoom(double delta) { zoom=Mth.clamp(zoom+(float)Math.signum(delta)*.1f,.4f,1.4f); }
    void render(GuiGraphics g,GuiLayoutRect box) {
        if(entity==null) return;
        float h=entity instanceof ScalableBirdModel b ? b.modelScaleProfile().targetHeightBlocks() : Math.max(.2f,entity.getBbHeight());
        int scale=BirdModelScale.fitPreviewScale(Math.max(1,Math.round(Math.min(box.h()*.82f,box.w()*.78f)/h*zoom)));
        g.enableScissor(box.x(),box.y(),box.right(),box.bottom());
        // Rotate the whole specimen rather than the inventory helper's limited head-look angles.
        entity.yBodyRot=entity.yBodyRotO=entity.yHeadRot=entity.yHeadRotO=180+yaw;
        entity.setYRot(180+yaw);entity.yRotO=180+yaw;entity.setXRot(0);entity.xRotO=0;
        Quaternionf tilt=new Quaternionf().rotateX(pitch*Mth.DEG_TO_RAD);
        InventoryScreen.renderEntityInInventory(g,box.centerX(),box.bottom()-6,scale,
                new Quaternionf().rotateZ((float)Math.PI).mul(tilt),new Quaternionf(tilt),entity);
        g.disableScissor();
    }
    private void applyPreviewAnimation(LivingEntity entity, GuidePreviewAnimation animation) {
        if (entity instanceof HummingbirdEntity hummingbird) {
            // This unspawned specimen never ticks AI or travels. Supply only the visual
            // speed query, so the flight button uses the forward-flight wing pitch.
            hummingbird.setDeltaMovement(animation == GuidePreviewAnimation.FLY_FLAP
                    || animation == GuidePreviewAnimation.GLIDE
                    ? new net.minecraft.world.phys.Vec3(0, 0, .15)
                    : net.minecraft.world.phys.Vec3.ZERO);
            hummingbird.setGuidePreviewAnimation(switch (animation) {
                case FLY_FLAP, GLIDE -> "fly";
                case LOOK_2, SCRATCH -> "nectar";
                case LOOK_3 -> "sleep";
                default -> "idle";
            });
        } else if (entity instanceof NightHeronEntity nightHeron) {
            nightHeron.setGuidePreviewAnimation(this.toNightHeronPreviewAnimation(animation));
        } else if (entity instanceof MynaEntity myna) {
            myna.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> MynaEntity.GuidePreviewAnimation.WALK;
                case FLY_FLAP, GLIDE -> MynaEntity.GuidePreviewAnimation.FLY;
                case LOOK_2, SCRATCH -> MynaEntity.GuidePreviewAnimation.IDLE_2;
                case LOOK_1, LOOK_3, LOOK_5 -> MynaEntity.GuidePreviewAnimation.IDLE_1;
                default -> MynaEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof KiwiEntity kiwi) {
            kiwi.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> KiwiEntity.GuidePreviewAnimation.WALK;
                case LOOK_2, SCRATCH -> KiwiEntity.GuidePreviewAnimation.FORAGE;
                case LOOK_1, LOOK_3, LOOK_5 -> KiwiEntity.GuidePreviewAnimation.ALERT;
                default -> KiwiEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof SparrowEntity sparrow) {
            sparrow.setGuidePreviewAnimation(this.toSparrowPreviewAnimation(animation));
        } else if (entity instanceof BudgerigarEntity budgerigar) {
            // Subclasses with a richer crest (the umbrella cockatoo) override this method
            // and re-map the budgerigar poses onto their own overlay animations.
            budgerigar.setGuidePreviewAnimation(this.toBudgerigarPreviewAnimation(animation));
        } else if (entity instanceof AbstractColumbidEntity columbid) {
            columbid.setGuidePreviewAnimation(this.toColumbidPreviewAnimation(animation));
        } else if (entity instanceof CrowEntity crow) {
            crow.setGuidePreviewAnimation(this.toCrowPreviewAnimation(animation));
        } else if (entity instanceof SeagullEntity seagull) {
            seagull.setGuidePreviewAnimation(this.toSeagullPreviewAnimation(animation));
        } else if (entity instanceof KestrelEntity kestrel) {
            kestrel.setGuidePreviewAnimation(switch (animation) {
                case WALK, RUN -> KestrelEntity.GuidePreviewAnimation.WALK;
                case FLY_FLAP -> KestrelEntity.GuidePreviewAnimation.HOVER;
                case GLIDE -> KestrelEntity.GuidePreviewAnimation.FLY;
                default -> KestrelEntity.GuidePreviewAnimation.IDLE;
            });
        } else if (entity instanceof CassowaryEntity cassowary) {
            cassowary.setGuidePreviewAnimation(switch (animation) {
                case WALK -> CassowaryEntity.GuidePreviewAnimation.WALK;
                case RUN -> CassowaryEntity.GuidePreviewAnimation.SPRINT;
                case LOOK_1, LOOK_3, LOOK_5 -> CassowaryEntity.GuidePreviewAnimation.LOOK;
                case LOOK_2, SCRATCH -> CassowaryEntity.GuidePreviewAnimation.ALERT;
                case FLY_FLAP -> CassowaryEntity.GuidePreviewAnimation.WARNING;
                case GLIDE -> CassowaryEntity.GuidePreviewAnimation.REST;
                default -> CassowaryEntity.GuidePreviewAnimation.IDLE;
            });
        }
    }

    private NightHeronEntity.GuidePreviewAnimation toNightHeronPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> NightHeronEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1 -> NightHeronEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2 -> NightHeronEntity.GuidePreviewAnimation.LOOK_2;
            case LOOK_3 -> NightHeronEntity.GuidePreviewAnimation.LOOK_3;
            case SCRATCH -> NightHeronEntity.GuidePreviewAnimation.SCRATCH;
            case LOOK_5 -> NightHeronEntity.GuidePreviewAnimation.LOOK_5;
            case WALK -> NightHeronEntity.GuidePreviewAnimation.WALK;
            case RUN -> NightHeronEntity.GuidePreviewAnimation.RUN;
            case FLY_FLAP -> NightHeronEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> NightHeronEntity.GuidePreviewAnimation.GLIDE;
        };
    }

    private SparrowEntity.GuidePreviewAnimation toSparrowPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> SparrowEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, LOOK_5 -> SparrowEntity.GuidePreviewAnimation.TAIL;
            case LOOK_2, SCRATCH -> SparrowEntity.GuidePreviewAnimation.PECK;
            case LOOK_3 -> SparrowEntity.GuidePreviewAnimation.LOOK_AROUND;
            case WALK, RUN -> SparrowEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> SparrowEntity.GuidePreviewAnimation.FLY;
        };
    }

    private BudgerigarEntity.GuidePreviewAnimation toBudgerigarPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> BudgerigarEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> BudgerigarEntity.GuidePreviewAnimation.PREEN;
            case LOOK_2, LOOK_5 -> BudgerigarEntity.GuidePreviewAnimation.CURIOUS;
            case LOOK_3 -> BudgerigarEntity.GuidePreviewAnimation.DANCE;
            case WALK, RUN -> BudgerigarEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> BudgerigarEntity.GuidePreviewAnimation.FLY;
        };
    }

    private AbstractColumbidEntity.GuidePreviewAnimation toColumbidPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> AbstractColumbidEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2 -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_2;
            case LOOK_3, LOOK_5 -> AbstractColumbidEntity.GuidePreviewAnimation.LOOK_3;
            case WALK, RUN -> AbstractColumbidEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP -> AbstractColumbidEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> AbstractColumbidEntity.GuidePreviewAnimation.GLIDE;
        };
    }

    private CrowEntity.GuidePreviewAnimation toCrowPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> CrowEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> CrowEntity.GuidePreviewAnimation.LOOK_1;
            case LOOK_2, LOOK_3, LOOK_5 -> CrowEntity.GuidePreviewAnimation.LOOK_2;
            case WALK, RUN -> CrowEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP, GLIDE -> CrowEntity.GuidePreviewAnimation.FLY;
        };
    }

    private SeagullEntity.GuidePreviewAnimation toSeagullPreviewAnimation(GuidePreviewAnimation animation) {
        return switch (animation) {
            case IDLE -> SeagullEntity.GuidePreviewAnimation.IDLE;
            case LOOK_1, SCRATCH -> SeagullEntity.GuidePreviewAnimation.MOUTH_SCRATCH;
            case LOOK_2 -> SeagullEntity.GuidePreviewAnimation.LAUGH_1;
            case LOOK_3 -> SeagullEntity.GuidePreviewAnimation.IDLE_VARIATION;
            case LOOK_5 -> SeagullEntity.GuidePreviewAnimation.BIG_LAUGH;
            case WALK, RUN -> SeagullEntity.GuidePreviewAnimation.WALK;
            case FLY_FLAP -> SeagullEntity.GuidePreviewAnimation.FLY_FLAP;
            case GLIDE -> SeagullEntity.GuidePreviewAnimation.GLIDE_BOOST;
        };
    }

    private enum GuidePreviewAnimation { IDLE, LOOK_1, LOOK_2, LOOK_3, SCRATCH, LOOK_5, WALK, RUN, FLY_FLAP, GLIDE }
}
