package EdDYON.guaniao.content.dropping;

import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PrankFoodUtil {
    public static final String TAG_PRANK_FOOD = "GuaniaoPrankFood";
    public static final String TAG_ORIGINAL_ITEM = "GuaniaoOriginalItem";
    public static final String TAG_DROPPING_VARIANT = "GuaniaoDroppingVariant";
    public static final String TAG_PRANK_NAME = "GuaniaoPrankName";

    private static final Map<String, String> ZH_PHRASE_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<Character, Character> ZH_CORE_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<Character, Character> ZH_MODIFIER_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<String, String> EN_REPLACEMENTS = new LinkedHashMap<>();

    static {
        ZH_PHRASE_REPLACEMENTS.put("\u9644\u9B54\u91D1\u82F9\u679C", "\u9644\u9B54\u5168\u82F9\u679C");
        ZH_PHRASE_REPLACEMENTS.put("\u91D1\u82F9\u679C", "\u5168\u82F9\u679C");
        ZH_PHRASE_REPLACEMENTS.put("\u5357\u74DC\u6D3E", "\u5357\u722A\u6D3E");
        ZH_PHRASE_REPLACEMENTS.put("\u836F\u6C34", "\u7EA6\u6C34");
        ZH_PHRASE_REPLACEMENTS.put("胡萝卜", "胡萝扑");
        ZH_PHRASE_REPLACEMENTS.put("河豚", "河钝");
        ZH_PHRASE_REPLACEMENTS.put("蜘蛛眼", "蜘蛛眠");
        ZH_PHRASE_REPLACEMENTS.put("曲奇", "由奇");
        ZH_PHRASE_REPLACEMENTS.put("干海带", "千海带");
        ZH_PHRASE_REPLACEMENTS.put("牛排", "牛牌");

        ZH_CORE_REPLACEMENTS.put('\u8089', '\u5185');
        ZH_CORE_REPLACEMENTS.put('猪', '诸');
        ZH_CORE_REPLACEMENTS.put('牛', '午');
        ZH_CORE_REPLACEMENTS.put('羊', '半');
        ZH_CORE_REPLACEMENTS.put('\u9E21', '\u53FD');
        ZH_CORE_REPLACEMENTS.put('\u9E2D', '\u5440');
        ZH_CORE_REPLACEMENTS.put('\u9E45', '\u997F');
        ZH_CORE_REPLACEMENTS.put('\u5154', '\u514D');
        ZH_CORE_REPLACEMENTS.put('排', '牌');
        ZH_CORE_REPLACEMENTS.put('\u9C7C', '\u6E14');
        ZH_CORE_REPLACEMENTS.put('鳕', '雪');
        ZH_CORE_REPLACEMENTS.put('鲑', '桂');
        ZH_CORE_REPLACEMENTS.put('豚', '钝');
        ZH_CORE_REPLACEMENTS.put('\u867E', '\u5413');
        ZH_CORE_REPLACEMENTS.put('\u86CB', '\u866B');
        ZH_CORE_REPLACEMENTS.put('\u5305', '\u53E5');
        ZH_CORE_REPLACEMENTS.put('\u996D', '\u53CD');
        ZH_CORE_REPLACEMENTS.put('\u997C', '\u5E76');
        ZH_CORE_REPLACEMENTS.put('\u7CD5', '\u7F94');
        ZH_CORE_REPLACEMENTS.put('\u83DC', '\u8388');
        ZH_CORE_REPLACEMENTS.put('炖', '顿');
        ZH_CORE_REPLACEMENTS.put('\u679C', '\u545C');
        ZH_CORE_REPLACEMENTS.put('苹', '平');
        ZH_CORE_REPLACEMENTS.put('\u74DC', '\u722A');
        ZH_CORE_REPLACEMENTS.put('胡', '湖');
        ZH_CORE_REPLACEMENTS.put('萝', '罗');
        ZH_CORE_REPLACEMENTS.put('卜', '扑');
        ZH_CORE_REPLACEMENTS.put('\u85AF', '\u6691');
        ZH_CORE_REPLACEMENTS.put('\u836F', '\u7EA6');
        ZH_CORE_REPLACEMENTS.put('\u6C34', '\u51B0');
        ZH_CORE_REPLACEMENTS.put('瓶', '屏');
        ZH_CORE_REPLACEMENTS.put('桶', '捅');
        ZH_CORE_REPLACEMENTS.put('蘑', '魔');
        ZH_CORE_REPLACEMENTS.put('\u83C7', '\u59D1');
        ZH_CORE_REPLACEMENTS.put('\u7172', '\u5305');
        ZH_CORE_REPLACEMENTS.put('\u76D0', '\u76EF');
        ZH_CORE_REPLACEMENTS.put('蜘', '知');
        ZH_CORE_REPLACEMENTS.put('蛛', '株');
        ZH_CORE_REPLACEMENTS.put('眼', '眠');
        ZH_CORE_REPLACEMENTS.put('曲', '由');
        ZH_CORE_REPLACEMENTS.put('奇', '寄');
        ZH_CORE_REPLACEMENTS.put('海', '每');
        ZH_CORE_REPLACEMENTS.put('带', '袋');
        ZH_CORE_REPLACEMENTS.put('浆', '酱');
        ZH_CORE_REPLACEMENTS.put('蜂', '峰');

        ZH_MODIFIER_REPLACEMENTS.put('生', '笙');
        ZH_MODIFIER_REPLACEMENTS.put('\u719F', '\u5B70');
        ZH_MODIFIER_REPLACEMENTS.put('\u70E4', '\u62F7');
        ZH_MODIFIER_REPLACEMENTS.put('\u751C', '\u7518');
        ZH_MODIFIER_REPLACEMENTS.put('毒', '独');
        ZH_MODIFIER_REPLACEMENTS.put('金', '全');
        ZH_MODIFIER_REPLACEMENTS.put('附', '咐');
        ZH_MODIFIER_REPLACEMENTS.put('魔', '磨');
        ZH_MODIFIER_REPLACEMENTS.put('干', '千');
        ZH_MODIFIER_REPLACEMENTS.put('\u8150', '\u5E9C');
        ZH_MODIFIER_REPLACEMENTS.put('\u5976', '\u4E43');
        ZH_MODIFIER_REPLACEMENTS.put('\u871C', '\u5BC6');
        ZH_MODIFIER_REPLACEMENTS.put('\u6C64', '\u70EB');
        ZH_MODIFIER_REPLACEMENTS.put('\u6839', '\u8DCB');
        ZH_MODIFIER_REPLACEMENTS.put('喷', '盆');
        ZH_MODIFIER_REPLACEMENTS.put('\u6E85', '\u6D45');
        ZH_MODIFIER_REPLACEMENTS.put('型', '刑');
        ZH_MODIFIER_REPLACEMENTS.put('\u6EDE', '\u6C41');
        ZH_MODIFIER_REPLACEMENTS.put('留', '溜');
        ZH_MODIFIER_REPLACEMENTS.put('粗', '组');
        ZH_MODIFIER_REPLACEMENTS.put('制', '治');
        ZH_MODIFIER_REPLACEMENTS.put('平', '苹');
        ZH_MODIFIER_REPLACEMENTS.put('凡', '帆');
        ZH_MODIFIER_REPLACEMENTS.put('浓', '农');
        ZH_MODIFIER_REPLACEMENTS.put('稠', '绸');
        ZH_MODIFIER_REPLACEMENTS.put('长', '常');
        ZH_MODIFIER_REPLACEMENTS.put('效', '笑');
        ZH_MODIFIER_REPLACEMENTS.put('可', '珂');
        ZH_MODIFIER_REPLACEMENTS.put('疑', '凝');
        ZH_MODIFIER_REPLACEMENTS.put('迷', '咪');
        ZH_MODIFIER_REPLACEMENTS.put('谜', '咪');
        ZH_MODIFIER_REPLACEMENTS.put('的', '地');

        EN_REPLACEMENTS.put("Enchanted Golden Apple", "Enchanted Golden Appie");
        EN_REPLACEMENTS.put("Cooked Porkchop", "Cooked Porkchob");
        EN_REPLACEMENTS.put("Poisonous Potato", "Poisonous Pototo");
        EN_REPLACEMENTS.put("Suspicious Stew", "Suspicious Staw");
        EN_REPLACEMENTS.put("Lingering Potion", "Lingering Lotion");
        EN_REPLACEMENTS.put("Cooked Chicken", "Cooked Chacken");
        EN_REPLACEMENTS.put("Cooked Rabbit", "Cooked Rabbot");
        EN_REPLACEMENTS.put("Cooked Salmon", "Cooked Salmom");
        EN_REPLACEMENTS.put("Beetroot Soup", "Beetroot Soop");
        EN_REPLACEMENTS.put("Mushroom Stew", "Mushroom Staw");
        EN_REPLACEMENTS.put("Rabbit Stew", "Rabbit Staw");
        EN_REPLACEMENTS.put("Golden Carrot", "Golden Parrot");
        EN_REPLACEMENTS.put("Golden Apple", "Golden Appie");
        EN_REPLACEMENTS.put("Baked Potato", "Baked Pototo");
        EN_REPLACEMENTS.put("Splash Potion", "Splash Lotion");
        EN_REPLACEMENTS.put("Cooked Mutton", "Cooked Mutten");
        EN_REPLACEMENTS.put("Cooked Beef", "Cooked Beeg");
        EN_REPLACEMENTS.put("Cooked Cod", "Cooked Cot");
        EN_REPLACEMENTS.put("Chorus Fruit", "Chorus Fruut");
        EN_REPLACEMENTS.put("Tropical Fish", "Tropical Fosh");
        EN_REPLACEMENTS.put("Sweet Berries", "Sweet Berriez");
        EN_REPLACEMENTS.put("Glow Berries", "Glow Berriez");
        EN_REPLACEMENTS.put("Melon Slice", "Melon Slicf");
        EN_REPLACEMENTS.put("Honey Bottle", "Honey Bottie");
        EN_REPLACEMENTS.put("Milk Bucket", "Milk Backet");
        EN_REPLACEMENTS.put("Rotten Flesh", "Rotten Flesk");
        EN_REPLACEMENTS.put("Spider Eye", "Spider Bye");
        EN_REPLACEMENTS.put("Pumpkin Pie", "Pumpkin Pue");
        EN_REPLACEMENTS.put("Dried Kelp", "Dried Help");
        EN_REPLACEMENTS.put("Pufferfish", "Pufferfosh");
        EN_REPLACEMENTS.put("Beetroot", "Beetroof");
        EN_REPLACEMENTS.put("Porkchop", "Porkchob");
        EN_REPLACEMENTS.put("Chicken", "Chacken");
        EN_REPLACEMENTS.put("Mutton", "Mutten");
        EN_REPLACEMENTS.put("Rabbit", "Rabbot");
        EN_REPLACEMENTS.put("Salmon", "Salmom");
        EN_REPLACEMENTS.put("Carrot", "Parrot");
        EN_REPLACEMENTS.put("Potato", "Pototo");
        EN_REPLACEMENTS.put("Cookie", "Cookle");
        EN_REPLACEMENTS.put("Potion", "Lotion");
        EN_REPLACEMENTS.put("Apple", "Appie");
        EN_REPLACEMENTS.put("Bread", "Breod");
        EN_REPLACEMENTS.put("Cake", "Coke");
        EN_REPLACEMENTS.put("Beef", "Beeg");
        EN_REPLACEMENTS.put("Cod", "Cot");
    }

    private PrankFoodUtil() {
    }

    public static boolean isPrankFood(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return !stack.isEmpty() && tag != null && tag.getBoolean(TAG_PRANK_FOOD);
    }

    public static boolean isDropping(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(GuaniaoItems.BIRD_DROPPING_1.get())
                || stack.is(GuaniaoItems.BIRD_DROPPING_2.get())
                || stack.is(GuaniaoItems.BIRD_DROPPING_3.get())
                || stack.is(GuaniaoItems.BIRD_DROPPING_4.get()));
    }

    public static int droppingVariant(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BirdDroppingItem droppingItem) {
            return droppingItem.variant().id();
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_2.get())) {
            return BirdDroppingVariant.TWO.id();
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_3.get())) {
            return BirdDroppingVariant.THREE.id();
        }
        if (stack.is(GuaniaoItems.BIRD_DROPPING_4.get())) {
            return BirdDroppingVariant.FOUR.id();
        }
        return BirdDroppingVariant.ONE.id();
    }

    public static boolean isEligibleFood(ItemStack stack) {
        return !stack.isEmpty() && (stack.isEdible() || stack.getItem() instanceof PotionItem) && !isPrankFood(stack) && !isDropping(stack);
    }

    public static ItemStack makePrankFood(ItemStack food, ItemStack dropping) {
        if (!isEligibleFood(food) || !isDropping(dropping)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = food.copy();
        result.setCount(1);
        ResourceLocation originalItem = ForgeRegistries.ITEMS.getKey(food.getItem());
        Component prankName = makePrankDisplayName(food.getHoverName());

        CompoundTag tag = result.getOrCreateTag();
        tag.putBoolean(TAG_PRANK_FOOD, true);
        tag.putString(TAG_ORIGINAL_ITEM, originalItem == null ? "unknown" : originalItem.toString());
        tag.putInt(TAG_DROPPING_VARIANT, droppingVariant(dropping));
        tag.putString(TAG_PRANK_NAME, prankName.getString());

        result.setHoverName(prankName);
        return result;
    }

    public static Component makePrankDisplayName(Component originalName) {
        String plainName = originalName.getString();
        String prankName = containsChinese(plainName) ? corruptChineseName(plainName) : corruptEnglishName(plainName);
        return Component.literal(sanitizeQuestionMarks(prankName)).withStyle(originalName.getStyle());
    }

    public static Component storedPrankDisplayName(ItemStack stack, Component fallbackOriginalName) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_PRANK_NAME)) {
            return Component.literal(sanitizeQuestionMarks(tag.getString(TAG_PRANK_NAME))).withStyle(fallbackOriginalName.getStyle());
        }
        return makePrankDisplayName(fallbackOriginalName);
    }

    private static String corruptChineseName(String name) {
        for (Map.Entry<String, String> entry : ZH_PHRASE_REPLACEMENTS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return replaceFirstLiteral(name, entry.getKey(), entry.getValue());
            }
        }

        StringBuilder builder = new StringBuilder(name);
        if (replaceChineseChar(builder, ZH_CORE_REPLACEMENTS) || replaceChineseChar(builder, ZH_MODIFIER_REPLACEMENTS)) {
            return builder.toString();
        }

        for (int i = 0; i < builder.length(); i++) {
            if (!Character.isWhitespace(builder.charAt(i))) {
                builder.setCharAt(i, chineseFallback(builder.charAt(i)));
                return builder.toString();
            }
        }
        return name;
    }

    private static boolean replaceChineseChar(StringBuilder builder, Map<Character, Character> replacements) {
        for (int i = 0; i < builder.length(); i++) {
            Character replacement = replacements.get(builder.charAt(i));
            if (replacement != null && replacement != builder.charAt(i)) {
                builder.setCharAt(i, replacement);
                return true;
            }
        }
        return false;
    }

    private static String corruptEnglishName(String name) {
        String exactReplacement = EN_REPLACEMENTS.get(name);
        if (isSingleCharacterReplacement(name, exactReplacement)) {
            return exactReplacement;
        }

        for (Map.Entry<String, String> entry : EN_REPLACEMENTS.entrySet()) {
            String source = entry.getKey();
            String replacement = entry.getValue();
            int index = name.indexOf(source);
            if (index >= 0 && isSingleCharacterReplacement(source, replacement)) {
                return name.substring(0, index) + replacement + name.substring(index + source.length());
            }
        }

        StringBuilder builder = new StringBuilder(name);
        for (int i = 0; i < builder.length(); i++) {
            char replacement = latinFallback(builder.charAt(i));
            if (replacement != builder.charAt(i)) {
                builder.setCharAt(i, replacement);
                return builder.toString();
            }
        }
        return name;
    }

    private static boolean isSingleCharacterReplacement(String source, String replacement) {
        if (source == null || replacement == null || source.length() != replacement.length()) {
            return false;
        }

        int differences = 0;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) != replacement.charAt(i)) {
                differences++;
                if (differences > 1) {
                    return false;
                }
            }
        }
        return differences == 1;
    }

    private static char latinFallback(char character) {
        return switch (character) {
            case 'a' -> 'o';
            case 'e' -> 'a';
            case 'i' -> 'e';
            case 'o' -> 'u';
            case 'u' -> 'o';
            case 'A' -> 'O';
            case 'E' -> 'A';
            case 'I' -> 'E';
            case 'O' -> 'U';
            case 'U' -> 'O';
            default -> {
                if (character >= 'b' && character <= 'z') {
                    yield (char)(character - 1);
                }
                if (character >= 'B' && character <= 'Z') {
                    yield (char)(character - 1);
                }
                yield character;
            }
        };
    }

    private static char chineseFallback(char character) {
        if (character == '?' || character == '\uFF1F') {
            return '\uFF01';
        }
        if (Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN) {
            return '臭';
        }
        return Character.isWhitespace(character) ? character : '\uFF01';
    }

    private static String sanitizeQuestionMarks(String name) {
        return name.replace('?', '\uFF01').replace('\uFF1F', '\uFF01');
    }

    private static String replaceFirstLiteral(String name, String source, String replacement) {
        if (!isSingleCharacterReplacement(source, replacement)) {
            return name;
        }
        int index = name.indexOf(source);
        if (index < 0) {
            return name;
        }
        return name.substring(0, index) + replacement + name.substring(index + source.length());
    }

    private static boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
