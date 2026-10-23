package EdDYON.guaniao.content.camera;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Author display formatting does not modify the saved item data. */
public final class PhotoAuthorText {
    private static final String DEVELOPER = "开发组";

    private PhotoAuthorText() { }

    public static Component component(String author) {
        var text = Component.literal(author);
        return DEVELOPER.equals(author) ? text.withStyle(ChatFormatting.GOLD) : text;
    }

    public static int color(String author, int fallback) {
        return DEVELOPER.equals(author) ? ChatFormatting.GOLD.getColor() : fallback;
    }
}
