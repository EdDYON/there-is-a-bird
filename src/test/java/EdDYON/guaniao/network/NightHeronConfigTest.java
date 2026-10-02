package EdDYON.guaniao.network;

import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public final class NightHeronConfigTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Gson gson=new Gson();
        BirdConfigData legacy=gson.fromJson("{\"global\":{\"maxPhotosPerPlayer\":42}}",BirdConfigData.class);
        check(legacy.global.nightHeronTamingEnabled && legacy.global.nightHeronFishingEnabled && legacy.global.nightHeronGiftsEnabled,"legacy opt-in defaults");
        check(legacy.global.nightHeronTamingChance==1.0/3 && legacy.global.nightHeronFishingRadius==16 && legacy.global.nightHeronOwnerRange==32,"default chance/ranges");
        check(legacy.global.nightHeronWorkMinSeconds==30 && legacy.global.nightHeronWorkMaxSeconds==60 && legacy.global.nightHeronGiftMinSeconds==300 && legacy.global.nightHeronGiftMaxSeconds==600,"default effective time windows");
        BirdConfigData data=new BirdConfigData();
        data.global.nightHeronTamingEnabled=false; data.global.nightHeronFishingEnabled=false;data.global.nightHeronGiftsEnabled=false;
        data.global.nightHeronTamingChance=.7;data.global.nightHeronFishingRadius=22;data.global.nightHeronOwnerRange=45;
        data.global.nightHeronWorkMinSeconds=71;data.global.nightHeronWorkMaxSeconds=92;data.global.nightHeronGiftMinSeconds=321;data.global.nightHeronGiftMaxSeconds=654;
        check(gson.toJson(data).equals(gson.toJson(data.copy())),"copy preserves all ten settings");
        FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
        try {
            BirdConfigPacketCodec.encode(buf,data);
            check(gson.toJson(data).equals(gson.toJson(BirdConfigPacketCodec.decode(buf))),"packet preserves every setting and following fields");
            check(buf.readableBytes()==0,"packet fully consumed");
        } finally {buf.release();}
        System.out.println("PASS: night heron legacy defaults, custom values, copy and network round trip");
    }
}
