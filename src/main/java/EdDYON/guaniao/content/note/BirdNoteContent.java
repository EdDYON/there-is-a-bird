package EdDYON.guaniao.content.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * Generates the lootable "bird note" written books that appear in structure chests.
 *
 * <p>Every note is written by its own author — a farmer, a blacksmith's kid, a
 * shepherd boy, a vindicator, a piglin brute, an astrologer, an anonymous keeper —
 * so the tone, the topics (baths, food, habits, nests, droppings, omens, rumours)
 * and even the spelling vary wildly. A village chest usually holds plain gossip; an
 * ancient-city chest usually holds something closer to a secret. Rarely, a note
 * tells a legend about the special mutations (leucistic/melanistic/golden/
 * pure-gold/rainbow). A few secret notes are easter eggs written by the mod's
 * own developers, drawn from the secret pool at the same odds as any other
 * secret note.
 */
public final class BirdNoteContent {
    /** NBT marker so "read the note" gameplay knows this written book is a mod note. */
    public static final String NOTE_TAG = "GuaniaoNote";

    /** Chest difficulty tiers. */
    private static final int VILLAGE = 0;
    private static final int MEDIUM = 1;
    private static final int NETHER = 2;
    private static final int ANCIENT = 3;

    /** Secrecy level of the rarest pool, where the dev easter-egg notes live. */
    private static final int SECRET = 3;

    /** Chance that an eligible chest contains a note at all, per tier. */
    private static final double[] NOTE_CHANCE = {0.20, 0.25, 0.30, 0.35};

    /**
     * Weighted secrecy levels (ordinary/interesting/rare/secret) per chest tier.
     * A village chest mostly gives ordinary notes but can rarely reach a secret one.
     */
    private static final double[][] TIER_WEIGHTS = {
            {0.795, 0.16, 0.04, 0.005},
            {0.40, 0.42, 0.15, 0.03},
            {0.15, 0.40, 0.35, 0.10},
            {0.03, 0.20, 0.50, 0.27},
    };

    /** Chance a note of a given secrecy level is about a special bird. Overall lands near ~1/10. */
    private static final double[] SPECIAL_BY_SECRET = {0.02, 0.12, 0.35, 0.75};

    /** Which mutation a legend describes, weighted per chest tier (rarer mutations in harder places). */
    private static final double[][] MUTATION_WEIGHTS = {
            {35, 40, 20, 4, 1},
            {25, 30, 30, 10, 5},
            {15, 20, 30, 20, 15},
            {10, 15, 25, 25, 25},
    };

    private static final String[] MUTATION_KEYS = {"leucistic", "melanistic", "golden", "puregold", "rainbow"};
    private static final String[] MUTATION_TITLES = {"关于白鸟的传说", "关于黑鸟的传说", "关于金鸟的传说", "关于纯金之鸟的传说", "关于彩虹之鸟的传说"};
    private static final String MYSTERY_AUTHOR = "佚名";

    /** One note: its literal title/author and the translatable body key. */
    private record NoteTemplate(String title, String author, String bodyKey) {
    }

    /** Pool of notes for each secrecy level (ordinary/interesting/rare/secret). */
    private static final NoteTemplate[][] NOTES = {
            {
                    new NoteTemplate("观鸟入门", "王小麦", "village_bath"),
                    new NoteTemplate("麻雀的口粮", "村口的小花", "village_crumbs"),
                    new NoteTemplate("白点点", "老奶奶", "village_poop"),
                    new NoteTemplate("谷仓里的新房客", "铁匠老张", "village_barn"),
                    new NoteTemplate("小偷与乌鸦", "村长", "village_crow"),
                    new NoteTemplate("雨后的水坑", "阿毛", "village_puddle"),
                    new NoteTemplate("鸡？！", "王小麦", "village_chicken"),
                    new NoteTemplate("清晨的歌", "磨坊主", "village_morning"),
                    new NoteTemplate("别吓到它们", "花匠", "village_dont_startle"),
                    new NoteTemplate("喂鸟要手稳", "农夫阿牛", "village_feeding"),
                    new NoteTemplate("水池边", "村长", "village_fountain"),
                    new NoteTemplate("屋檐下的斑鸠", "木匠阿福", "village_dove"),
                    new NoteTemplate("冬天的麻雀", "老奶奶", "village_winter"),
                    new NoteTemplate("收麦的时候", "王小麦", "village_harvest"),
                    new NoteTemplate("稻草人没用", "王小麦", "village_scarecrow"),
                    new NoteTemplate("燕子回家", "村长", "village_swallow_omen"),
                    new NoteTemplate("公鸡打鸣", "屠夫阿壮", "village_rooster"),
                    new NoteTemplate("小鸡认亲", "屠夫阿壮", "village_chick"),
                    new NoteTemplate("打铁声", "铁匠老张", "village_blacksmith_shiny"),
                    new NoteTemplate("脚手架上的麻雀", "石匠老刘", "village_mason"),
                    new NoteTemplate("花匠的帮手", "花匠", "village_garden"),
                    new NoteTemplate("药草园的客人", "药剂师", "village_apothecary"),
                    new NoteTemplate("书页里的羽毛", "图书管理员", "village_librarian"),
                    new NoteTemplate("教堂的钟声", "牧师", "village_priest"),
                    new NoteTemplate("酒馆的剩饭", "酒馆老板", "village_inn"),
                    new NoteTemplate("送信要快", "信使", "village_messenger"),
                    new NoteTemplate("钟楼的住客", "钟楼守夜人", "village_bell"),
                    new NoteTemplate("菜摊的偷嘴", "菜贩", "village_greengrocer"),
                    new NoteTemplate("羊群里的鸟", "牧童", "village_shepherd"),
                    new NoteTemplate("牛背上的鸟", "放牛娃", "village_cowherd"),
                    new NoteTemplate("打水的乐趣", "打水的孩子", "village_waterfetch"),
                    new NoteTemplate("蘑菇与鸟", "采蘑菇的孩子", "village_mushroom"),
                    new NoteTemplate("追蜻蜓", "追蜻蜓的孩子", "village_dragonfly"),
                    new NoteTemplate("下雨前", "老奶奶", "village_rain"),
                    new NoteTemplate("黄昏归巢", "村长", "village_dusk"),
                    new NoteTemplate("深冬觅食", "王小麦", "village_hungry"),
                    new NoteTemplate("池塘边的鹭", "渔夫老周", "village_fisher_heron"),
                    new NoteTemplate("屋顶的合唱", "钟楼守夜人", "village_roof"),
                    new NoteTemplate("雪地里的爪印", "牧童", "village_snow"),
                    new NoteTemplate("起风的时候", "磨坊主", "village_wind"),
                    new NoteTemplate("鸟窝的方位", "石匠老刘", "village_nest_secret"),
                    new NoteTemplate("向日葵上的雀", "花匠", "village_sunflower"),
                    new NoteTemplate("井沿的早晨", "打水的孩子", "village_well"),
                    new NoteTemplate("灯笼下的蛾与鸟", "菜贩", "village_lantern")
            },
            {
                    new NoteTemplate("海鸟领航", "老水手", "interesting_seagull"),
                    new NoteTemplate("会说人话的鹦鹉", "丛林探险家", "interesting_macaw"),
                    new NoteTemplate("黄昏的猎手", "守夜人", "interesting_nightheron"),
                    new NoteTemplate("矿洞里的旅鸟", "矿工老李", "interesting_mineshaft"),
                    new NoteTemplate("一团毛球", "制图师", "interesting_tit"),
                    new NoteTemplate("信鸽的家", "驿站的邮差", "interesting_pigeon"),
                    new NoteTemplate("鸟都吃什么", "猎户", "interesting_diet"),
                    new NoteTemplate("沙漠绿洲", "商队向导", "interesting_desert"),
                    new NoteTemplate("沉船上的海鸥", "幸存的水手", "interesting_shipwreck"),
                    new NoteTemplate("被抢走的三明治", "遇难的商人", "interesting_steal"),
                    new NoteTemplate("商队的歌", "商队向导", "interesting_trader_route"),
                    new NoteTemplate("星星与鸟", "老水手", "interesting_navigation"),
                    new NoteTemplate("学舌的边界", "丛林探险家", "interesting_parrot_mimic"),
                    new NoteTemplate("雪团坠落", "制图师", "interesting_tit_ball_fall"),
                    new NoteTemplate("认路的花招", "驿站的邮差", "interesting_pigeon_homing_trick"),
                    new NoteTemplate("猎人要学会安静", "猎户", "interesting_hunter_quiet"),
                    new NoteTemplate("黑暗中的鸟", "矿工老李", "interesting_mine_bird_dark"),
                    new NoteTemplate("守夜人的火堆", "守夜人", "interesting_watchfire"),
                    new NoteTemplate("会钓鱼的鹭", "渔夫老周", "interesting_fisher_knowledge"),
                    new NoteTemplate("高度与迁徙", "制图师", "interesting_cartographer_height"),
                    new NoteTemplate("谁更快", "驿站的邮差", "interesting_messenger_speed"),
                    new NoteTemplate("炼金术士的鸟", "药剂师", "interesting_alchemist"),
                    new NoteTemplate("仰望的鸟", "天文学家", "interesting_astronomer"),
                    new NoteTemplate("药草与羽毛", "药草师", "interesting_herbalist"),
                    new NoteTemplate("旅人的歌", "流浪歌手", "interesting_traveller_tales"),
                    new NoteTemplate("沼泽的鹭", "采药人", "interesting_swamp"),
                    new NoteTemplate("崖边的巢", "灯塔看守人", "interesting_coast"),
                    new NoteTemplate("风暴的预报", "老水手", "interesting_storm_warning"),
                    new NoteTemplate("数乌鸦", "守夜人", "interesting_crow_count"),
                    new NoteTemplate("巢的材料", "木匠阿福", "interesting_nest_materials"),
                    new NoteTemplate("镜子里的自己", "老奶奶", "interesting_mirror"),
                    new NoteTemplate("换季的胃口", "猎户", "interesting_diet_change"),
                    new NoteTemplate("人字形", "商队向导", "interesting_migration_line"),
                    new NoteTemplate("森林的歌", "丛林探险家", "interesting_forest_song"),
                    new NoteTemplate("潮汐与海鸥", "灯塔看守人", "interesting_tide"),
                    new NoteTemplate("青蓝的影子", "药草师", "interesting_kingfisher_dream")
            },
            {
                    new NoteTemplate("穿越门户的鸟", "卫道士", "rare_portal"),
                    new NoteTemplate("金色的羽", "资深观鸟者", "rare_gold_feather"),
                    new NoteTemplate("粪便是武器", "卫道士", "rare_poop_weapon"),
                    new NoteTemplate("熔岩边的倒影", "被诅咒的旅人", "rare_lava"),
                    new NoteTemplate("黑鸟的影子", "守夜人", "rare_black_shadow"),
                    new NoteTemplate("会记仇的鸟", "老猎人", "rare_grudge"),
                    new NoteTemplate("迁徙的路线", "资深观鸟者", "rare_migration"),
                    new NoteTemplate("羽毛换黄金", "猪灵", "rare_piglin"),
                    new NoteTemplate("军中的号角", "退伍士兵", "rare_soldier_watch"),
                    new NoteTemplate("佣兵的酬劳", "佣兵", "rare_mercenary"),
                    new NoteTemplate("蛮兵的战利品", "猪灵蛮兵", "rare_brute"),
                    new NoteTemplate("巡逻的鹰眼", "守卫队长", "rare_guard_captain"),
                    new NoteTemplate("边境的渡鸦", "边境巡逻兵", "rare_border"),
                    new NoteTemplate("驯鹰", "老猎人", "rare_veteran_falcon"),
                    new NoteTemplate("羽毛入药", "炼金术士", "rare_herbalist_secret"),
                    new NoteTemplate("彗星与鸟", "天文学家", "rare_astronomer_comet"),
                    new NoteTemplate("博物学家的标本", "博物学家", "rare_naturalist"),
                    new NoteTemplate("地脉", "地理学家", "rare_geomancer"),
                    new NoteTemplate("禁书区", "图书馆长", "rare_librarian_forbidden"),
                    new NoteTemplate("卷轴上的羽毛", "史官", "rare_scroll"),
                    new NoteTemplate("火山口的鸟", "登山者", "rare_volcano"),
                    new NoteTemplate("地牢的伴生", "洞穴探险家", "rare_dungeon"),
                    new NoteTemplate("藏宝图上的爪印", "寻宝者", "rare_treasure"),
                    new NoteTemplate("三只乌鸦", "老预言家", "rare_omen"),
                    new NoteTemplate("劫掠兽与鸟", "卫道士", "rare_ravager"),
                    new NoteTemplate("下界的集市", "猪灵", "rare_piglin_trade"),
                    new NoteTemplate("列队", "退伍士兵", "rare_soldier_drill"),
                    new NoteTemplate("血月", "守卫队长", "rare_blood_moon")
            },
            {
                    new NoteTemplate("远古的低语", "佚名", "secret_whisper"),
                    new NoteTemplate("世界的边缘", "末地学者", "secret_end"),
                    new NoteTemplate("失传的观鸟法", "图书馆长", "secret_forgotten"),
                    new NoteTemplate("第一只鸟", "佚名", "secret_first_bird"),
                    new NoteTemplate("沉没的图书馆", "图书馆长", "secret_ancient_library"),
                    new NoteTemplate("星图上的鸟", "占星师", "secret_astrologer"),
                    new NoteTemplate("观察者", "佚名", "secret_watcher"),
                    new NoteTemplate("月之鸟", "老预言家", "secret_moon_bird"),
                    new NoteTemplate("远行者的信", "末地学者", "secret_farlander"),
                    new NoteTemplate("深处的回声", "佚名", "secret_underworld"),
                    new NoteTemplate("消失的歌", "史官", "secret_song"),
                    new NoteTemplate("禁忌图书馆的看守", "看守者", "secret_keep"),
                    new NoteTemplate("第一百只鸟", "佚名", "secret_census"),
                    new NoteTemplate("蛋的秘密", "图书馆长", "secret_eggs"),
                    new NoteTemplate("梦游者的话", "梦游者", "secret_dreamer"),
                    new NoteTemplate("影子王", "佚名", "secret_shadow_king"),
                    new NoteTemplate("彩虹的秘密", "占星师", "secret_rainbow_secret"),
                    new NoteTemplate("无钟的时辰", "史官", "secret_clock"),
                    new NoteTemplate("寂静之日", "末地学者", "secret_silence"),
                    new NoteTemplate("传承", "佚名", "secret_inherit")
            }
    };

    /**
     * Easter-egg notes written by the mod's own developers. They live in the
     * secret pool and roll at the same odds as any other secret note.
     */
    private static final NoteTemplate[] DEV_NOTES = {
            new NoteTemplate("敲门三下", "蛋炒饭", "secret_dev_programmer"),
            new NoteTemplate("学咳嗽", "伊洛哥斯拉", "secret_dev_keeper"),
            new NoteTemplate("歪头大赛", "多雨", "secret_dev_animator"),
            new NoteTemplate("破音", "老三", "secret_dev_sound"),
            new NoteTemplate("斜着飞", "千年村庄", "secret_dev_modeler")
    };

    private static final Map<String, NoteTemplate> NOTES_BY_KEY = new HashMap<>();

    static {
        for (NoteTemplate[] pool : NOTES) {
            for (NoteTemplate note : pool) {
                NOTES_BY_KEY.put(note.bodyKey, note);
            }
        }
        for (NoteTemplate note : DEV_NOTES) {
            NOTES_BY_KEY.put(note.bodyKey, note);
        }
    }

    private static final Set<String> MEDIUM_TABLES = Set.of(
            "desert_pyramid", "jungle_temple", "shipwreck_treasure", "shipwreck_supply",
            "shipwreck_map", "pillager_outpost", "igloo_chest", "underwater_ruin_big",
            "underwater_ruin_small", "buried_treasure", "abandoned_mineshaft", "simple_dungeon",
            "woodland_mansion", "stronghold_corridor", "stronghold_crossing", "stronghold_library");
    private static final Set<String> NETHER_TABLES = Set.of(
            "nether_bridge", "bastion_treasure", "bastion_other", "bastion_bridge",
            "bastion_hoglin_stable", "ruined_portal", "end_city_treasure");

    private BirdNoteContent() {
    }

    /** Returns a note book for this loot context, or {@link ItemStack#EMPTY} when no note spawns. */
    public static ItemStack roll(LootContext context) {
        int tier = tierIndex(context.getQueriedLootTableId());
        if (tier < 0) {
            return ItemStack.EMPTY;
        }
        RandomSource random = context.getRandom();
        if (random.nextDouble() >= NOTE_CHANCE[tier]) {
            return ItemStack.EMPTY;
        }
        int secret = rollIndex(random, TIER_WEIGHTS[tier]);
        if (random.nextDouble() < SPECIAL_BY_SECRET[secret]) {
            int mutation = rollIndex(random, MUTATION_WEIGHTS[tier]);
            return makeBook(MUTATION_TITLES[mutation], MYSTERY_AUTHOR, "note.guaniao.body." + MUTATION_KEYS[mutation]);
        }
        NoteTemplate note = pickNote(random, secret);
        return makeBook(note.title, note.author, "note.guaniao.body." + note.bodyKey);
    }

    private static NoteTemplate pickNote(RandomSource random, int secret) {
        if (secret != SECRET) {
            return NOTES[secret][random.nextInt(NOTES[secret].length)];
        }
        // Dev easter eggs roll at the same odds as ordinary secret notes.
        int normalCount = NOTES[SECRET].length;
        int roll = random.nextInt(normalCount + DEV_NOTES.length);
        if (roll < normalCount) {
            return NOTES[SECRET][roll];
        }
        return DEV_NOTES[roll - normalCount];
    }

    /** Builds the note book for a body key ("leucistic", "village_poop", ...), matching its title and author. */
    public static ItemStack noteFor(String bodyKey) {
        for (int i = 0; i < MUTATION_KEYS.length; i++) {
            if (bodyKey.equals(MUTATION_KEYS[i])) {
                return makeBook(MUTATION_TITLES[i], MYSTERY_AUTHOR, "note.guaniao.body." + bodyKey);
            }
        }
        NoteTemplate note = NOTES_BY_KEY.get(bodyKey);
        if (note == null) {
            return makeBook("佚名的笔记", MYSTERY_AUTHOR, "note.guaniao.body." + bodyKey);
        }
        return makeBook(note.title, note.author, "note.guaniao.body." + bodyKey);
    }

    /** Returns every possible note book: the 128 villagers' notes, the 5 dev easter eggs and the 5 mutation legends. */
    public static List<ItemStack> allNotes() {
        List<ItemStack> notes = new ArrayList<>();
        for (NoteTemplate[] pool : NOTES) {
            for (NoteTemplate note : pool) {
                notes.add(noteFor(note.bodyKey));
            }
        }
        for (NoteTemplate note : DEV_NOTES) {
            notes.add(noteFor(note.bodyKey));
        }
        for (String mutationKey : MUTATION_KEYS) {
            notes.add(noteFor(mutationKey));
        }
        return notes;
    }

    /** Returns one representative book from each secrecy level for the creative tab. */
    public static List<ItemStack> creativeTabNotes() {
        List<ItemStack> notes = new ArrayList<>(NOTES.length);
        for (NoteTemplate[] pool : NOTES) {
            if (pool.length > 0) {
                notes.add(noteFor(pool[0].bodyKey));
            }
        }
        return notes;
    }

    private static ItemStack makeBook(String title, String author, String bodyKey) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putBoolean("resolved", true);
        tag.putByte(NOTE_TAG, (byte) 1);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("{\"translate\":\"" + bodyKey + "\"}"));
        tag.put("pages", pages);
        book.setTag(tag);
        return book;
    }

    private static int rollIndex(RandomSource random, double[] weights) {
        double total = 0.0;
        for (double weight : weights) {
            total += weight;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0.0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    private static int tierIndex(ResourceLocation table) {
        if (!"minecraft".equals(table.getNamespace())) {
            return -1;
        }
        String path = table.getPath();
        if (!path.startsWith("chests/")) {
            return -1;
        }
        String key = path.substring("chests/".length());
        if (key.startsWith("village/") || key.equals("spawn_bonus_chest") || key.equals("desert_well")) {
            return VILLAGE;
        }
        if (key.equals("ancient_city")) {
            return ANCIENT;
        }
        if (MEDIUM_TABLES.contains(key)) {
            return MEDIUM;
        }
        if (NETHER_TABLES.contains(key)) {
            return NETHER;
        }
        return -1;
    }
}
