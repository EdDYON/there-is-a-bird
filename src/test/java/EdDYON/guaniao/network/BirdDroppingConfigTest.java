package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigScope;
import EdDYON.guaniao.config.BirdGlobalConfig;
import EdDYON.guaniao.config.BirdSpeciesConfig;
import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/** Standalone compatibility checks for persisted settings and the configuration packet. */
public final class BirdDroppingConfigTest {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        BirdConfigData legacy = GSON.fromJson(
                "{\"global\":{\"maxGroundDroppingsNearby\":8,\"droppingFrequencyMultiplier\":0.5}}",
                BirdConfigData.class);
        check(legacy.global.maxGroundDroppingsNearby == 8, "preserve an explicitly saved old cap");
        check(legacy.global.droppingFrequencyMultiplier == 0.5D, "preserve custom rate");
        check(legacy.global.naturalDroppingsEnabled && legacy.global.droppingNearbyRadius == 16,
                "missing legacy settings receive defaults");
        check(legacy.global.droppingAreaCooldownMinSeconds == 20 && legacy.global.droppingAreaCooldownMaxSeconds == 40,
                "legacy area defaults");
        check(legacy.global.droppingLifetimeMinMinutes == 5 && legacy.global.droppingLifetimeMaxMinutes == 8,
                "legacy lifetime defaults");
        check(new BirdGlobalConfig().maxGroundDroppingsNearby == 4, "fresh configuration cap");
        check(!legacy.global.sparrowTideMode, "legacy configs leave Sparrow Tide disabled");

        BirdConfigData data = new BirdConfigData();
        data.global.sparrowTideMode = true;
        data.global.naturalDroppingsEnabled = false;
        data.global.droppingNearbyRadius = 24;
        data.global.droppingAreaCooldownMinSeconds = 35;
        data.global.droppingAreaCooldownMaxSeconds = 70;
        data.global.droppingLifetimeMinMinutes = 2;
        data.global.droppingLifetimeMaxMinutes = 9;
        data.global.maxGroundDroppingsNearby = 3;
        data.global.droppingFrequencyMultiplier = 0.5D;
        data.global.crowNestSearchDistance = 80;
        data.global.photoDownloadKiBPerTick = 128;
        BirdSpeciesConfig bird = new BirdSpeciesConfig();
        bird.droppingFrequencyMultiplier = 2.0D;
        data.birds.put("sparrow", bird);
        data.storageScope = BirdConfigScope.WORLD;
        data.worldScopeAllowed = true;
        check(GSON.toJson(data).equals(GSON.toJson(data.copy())), "copy retains all new settings");
        check(GSON.toJson(data).equals(GSON.toJson(GSON.fromJson(GSON.toJson(data), BirdConfigData.class))),
                "JSON round trip retains all settings");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BirdConfigPacketCodec.encode(buffer, data);
            BirdConfigData decoded = BirdConfigPacketCodec.decode(buffer);
            check(GSON.toJson(data).equals(GSON.toJson(decoded)), "packet round trip preserves settings and species");
            check(decoded.storageScope == BirdConfigScope.WORLD && decoded.worldScopeAllowed,
                    "trailing packet fields remain aligned");
            check(buffer.readableBytes() == 0, "packet fully consumed");
        } finally {
            buffer.release();
        }
        System.out.println("PASS: legacy defaults, custom settings, copy, JSON and packet round trips");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
