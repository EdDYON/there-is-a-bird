package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdGlobalConfig;
import EdDYON.guaniao.config.BirdSpeciesConfig;
import EdDYON.guaniao.event.BirdDroppingEvents;
import EdDYON.guaniao.event.BirdPopulationTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public final class SaveBirdConfigPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveBirdConfigPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("guaniao", "save_bird_config"));

    @Override
    public CustomPacketPayload.Type<SaveBirdConfigPacket> type() { return TYPE; }

    private final BirdConfigData data;

    public SaveBirdConfigPacket(BirdConfigData data) {
        this.data = data.copy();
    }

    public static void encode(SaveBirdConfigPacket packet, FriendlyByteBuf buffer) {
        BirdConfigPacketCodec.encode(buffer, packet.data);
    }

    public static SaveBirdConfigPacket decode(FriendlyByteBuf buffer) {
        return new SaveBirdConfigPacket(BirdConfigPacketCodec.decode(buffer));
    }

    public static void handle(SaveBirdConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null);
            if (player == null || (!player.hasPermissions(2)
                    && (player.server == null || !player.server.isSingleplayerOwner(player.getGameProfile())))) {
                return;
            }
            // Full-world scans (population re-tally, per-bird dropping cooldowns) are expensive;
            // only run them when the fields that drive them actually changed.
            BirdConfigData before = BirdConfigManager.snapshot();
            if (BirdConfigManager.replaceAndSave(packet.data, player.server)) {
                BirdConfigData after = BirdConfigManager.snapshot();
                if (before.global.populationRegionChunks != after.global.populationRegionChunks) {
                    BirdPopulationTracker.rebuild(player.server);
                }
                if (droppingsChanged(before, after)) {
                    BirdDroppingEvents.refreshLoadedBirdCooldowns(player.server);
                }
                player.displayClientMessage(Component.translatable("message.guaniao.bird_config.saved"), false);
                GuaniaoNetwork.sendToPlayer(new OpenBirdConfigPacket(BirdConfigManager.snapshot()), player);
                for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
                    GuaniaoNetwork.sendToPlayer(new BirdRuntimeConfigPacket(
                            BirdConfigManager.birdsPassThroughLeaves(),
                            BirdConfigManager.aprilFoolsMode(),
                            BirdConfigManager.skyBirdEcologyEnabled()), online);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.guaniao.bird_config.save_failed"), false);
            }
        });
    }

    private static boolean droppingsChanged(BirdConfigData before, BirdConfigData after) {
        BirdGlobalConfig oldGlobal = before.global;
        BirdGlobalConfig newGlobal = after.global;
        if (Double.compare(oldGlobal.droppingFrequencyMultiplier, newGlobal.droppingFrequencyMultiplier) != 0
                || oldGlobal.naturalDroppingsEnabled != newGlobal.naturalDroppingsEnabled
                || oldGlobal.droppingPressurePlatePulseEnabled != newGlobal.droppingPressurePlatePulseEnabled
                || oldGlobal.maxGroundDroppingsNearby != newGlobal.maxGroundDroppingsNearby
                || oldGlobal.droppingPressurePlatePulseTicks != newGlobal.droppingPressurePlatePulseTicks) {
            return true;
        }
        Set<String> speciesKeys = new HashSet<>(before.birds.keySet());
        speciesKeys.addAll(after.birds.keySet());
        for (String key : speciesKeys) {
            BirdSpeciesConfig oldSpecies = before.birds.get(key);
            BirdSpeciesConfig newSpecies = after.birds.get(key);
            double oldMultiplier = oldSpecies == null ? 1.0D : oldSpecies.droppingFrequencyMultiplier;
            double newMultiplier = newSpecies == null ? 1.0D : newSpecies.droppingFrequencyMultiplier;
            if (Double.compare(oldMultiplier, newMultiplier) != 0) {
                return true;
            }
        }
        return false;
    }
}
