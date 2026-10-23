package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.content.camera.PhotographEntity;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One unspawned preview, using the same footprint and support check as placement. */
@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID, value = Dist.CLIENT)
public final class FramePlacementPreview {
    private static ClientLevel level;
    private static PhotographEntity preview;
    private static FrameDesign design;
    private static ItemStack snapshot = ItemStack.EMPTY;
    private static BlockPos hitPos;
    private static Direction face;
    private static int ticks;
    private static boolean valid;
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(4096));

    private FramePlacementPreview() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        update(Minecraft.getInstance(),++ticks%5==0);
    }

    private static void update(Minecraft mc,boolean checkSupport) {
        if (mc.level == null || mc.player == null || mc.screen != null
                || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getDirection().getAxis().isHorizontal()) {
            clear();
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!held.is(GuaniaoItems.PHOTOGRAPH.get())) held = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        if (!held.is(GuaniaoItems.PHOTOGRAPH.get())) {
            clear();
            return;
        }
        boolean changed = level != mc.level || !hit.getBlockPos().equals(hitPos)
                || face != hit.getDirection() || !ItemStack.isSameItemSameTags(held, snapshot);
        if (changed) {
            design = FrameDesign.read(held);
            if (design == null || !design.hasArea()) {
                clear();
                return;
            }
            level = mc.level;
            snapshot = held.copy();
            snapshot.setCount(1);
            hitPos = hit.getBlockPos().immutable();
            face = hit.getDirection();
            preview = new PhotographEntity(level, hitPos.relative(face), face, snapshot);
        }
        if (preview != null && (changed || checkSupport)) {
            BlockPos anchor = hitPos.relative(face);
            valid = mc.player.mayBuild() && level.mayInteract(mc.player, anchor)
                    && mc.player.mayUseItemAt(anchor, face, held) && preview.survives();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // AFTER_LEVEL supplies GameRenderer's projection stack in Forge 1.20.1,
        // whereas this stage supplies the same world-view stack used by entities.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        // The ray and held item can change between ticks; never draw a stale ghost.
        update(mc,false);
        if (preview == null || design == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            poses.translate(preview.getX() - camera.x, preview.getY() - camera.y, preview.getZ() - camera.z);
            poses.mulPose(Axis.XP.rotationDegrees(preview.getXRot()));
            poses.mulPose(Axis.YP.rotationDegrees(-preview.getYRot()));
            FrameDesignRenderer.renderGhost(snapshot, design, poses, BUFFERS, 0xF000F0, valid);
            BUFFERS.endBatch();
        } finally {
            poses.popPose();
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {clear();}

    private static void clear() {
        preview = null;
        design = null;
        level = null;
        snapshot = ItemStack.EMPTY;
        hitPos = null;
        face = null;
        ticks = 0;
        valid = false;
    }
}
