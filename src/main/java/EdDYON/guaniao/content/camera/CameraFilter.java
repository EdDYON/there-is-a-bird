package EdDYON.guaniao.content.camera;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum CameraFilter {
    NONE(0, "gui.guaniao.camera_filter.none", CameraFilterCategory.NATURAL),
    BLACK_AND_WHITE(1, "gui.guaniao.camera_filter.black_and_white", CameraFilterCategory.MONO),
    FILM_GRAIN(2, "gui.guaniao.camera_filter.film_grain", CameraFilterCategory.FILM),
    EXPOSURE(3, "gui.guaniao.camera_filter.exposure", CameraFilterCategory.NATURAL),
    COLOR_BALANCE(4, "gui.guaniao.camera_filter.color_balance", CameraFilterCategory.NATURAL),
    BW_HARD(5, "gui.guaniao.camera_filter.bw_hard", CameraFilterCategory.MONO),
    VIVID(6, "gui.guaniao.camera_filter.vivid", CameraFilterCategory.NATURAL),
    VINTAGE_WARM(7, "gui.guaniao.camera_filter.vintage_warm", CameraFilterCategory.FILM),
    TEAL_ORANGE(8, "gui.guaniao.camera_filter.teal_orange", CameraFilterCategory.FILM),
    DREAMY(9, "gui.guaniao.camera_filter.dreamy", CameraFilterCategory.MOOD),
    LOMO(10, "gui.guaniao.camera_filter.lomo", CameraFilterCategory.FILM),
    SOFT_LIGHT(11, "gui.guaniao.camera_filter.soft_light", CameraFilterCategory.NATURAL),
    DEEP_CONTRAST(12, "gui.guaniao.camera_filter.deep_contrast", CameraFilterCategory.NATURAL),
    WARM_GLOW(13, "gui.guaniao.camera_filter.warm_glow", CameraFilterCategory.NATURAL),
    COOL_CLEAR(14, "gui.guaniao.camera_filter.cool_clear", CameraFilterCategory.NATURAL),
    SKY_BOOST(15, "gui.guaniao.camera_filter.sky_boost", CameraFilterCategory.NATURAL),
    FOREST_BOOST(16, "gui.guaniao.camera_filter.forest_boost", CameraFilterCategory.NATURAL),
    SUNSET_BOOST(17, "gui.guaniao.camera_filter.sunset_boost", CameraFilterCategory.NATURAL),
    VINTAGE_COOL(18, "gui.guaniao.camera_filter.vintage_cool", CameraFilterCategory.FILM),
    FADED_FILM(19, "gui.guaniao.camera_filter.faded_film", CameraFilterCategory.FILM),
    BLEACH_BYPASS(20, "gui.guaniao.camera_filter.bleach_bypass", CameraFilterCategory.FILM),
    SEPIA(21, "gui.guaniao.camera_filter.sepia", CameraFilterCategory.FILM),
    CINE_BLUE_GOLD(22, "gui.guaniao.camera_filter.cine_blue_gold", CameraFilterCategory.FILM),
    CROSS_PROCESS(23, "gui.guaniao.camera_filter.cross_process", CameraFilterCategory.FILM),
    TOY_CAMERA(24, "gui.guaniao.camera_filter.toy_camera", CameraFilterCategory.MOOD),
    BW_SOFT(25, "gui.guaniao.camera_filter.bw_soft", CameraFilterCategory.MONO),
    BW_HIGH_KEY(26, "gui.guaniao.camera_filter.bw_high_key", CameraFilterCategory.MONO),
    BW_LOW_KEY(27, "gui.guaniao.camera_filter.bw_low_key", CameraFilterCategory.MONO),
    BW_WARM(28, "gui.guaniao.camera_filter.bw_warm", CameraFilterCategory.MONO),
    BW_COOL(29, "gui.guaniao.camera_filter.bw_cool", CameraFilterCategory.MONO),
    SILVER(30, "gui.guaniao.camera_filter.silver", CameraFilterCategory.MONO),
    CHARCOAL(31, "gui.guaniao.camera_filter.charcoal", CameraFilterCategory.MONO),
    NOIR_GRAIN(32, "gui.guaniao.camera_filter.noir_grain", CameraFilterCategory.MONO),
    MISTY(33, "gui.guaniao.camera_filter.misty", CameraFilterCategory.MOOD),
    DAWN(34, "gui.guaniao.camera_filter.dawn", CameraFilterCategory.MOOD),
    MOONLIGHT(35, "gui.guaniao.camera_filter.moonlight", CameraFilterCategory.MOOD),
    ROMANTIC_PINK(36, "gui.guaniao.camera_filter.romantic_pink", CameraFilterCategory.MOOD),
    AUTUMN(37, "gui.guaniao.camera_filter.autumn", CameraFilterCategory.MOOD),
    SPRING(38, "gui.guaniao.camera_filter.spring", CameraFilterCategory.MOOD),
    WINTER(39, "gui.guaniao.camera_filter.winter", CameraFilterCategory.MOOD),
    SUMMER(40, "gui.guaniao.camera_filter.summer", CameraFilterCategory.MOOD),
    CYBERPUNK(41, "gui.guaniao.camera_filter.cyberpunk", CameraFilterCategory.CREATIVE),
    HORROR_GREEN(42, "gui.guaniao.camera_filter.horror_green", CameraFilterCategory.CREATIVE),
    APOCALYPSE(43, "gui.guaniao.camera_filter.apocalypse", CameraFilterCategory.CREATIVE),
    GLITCH_RGB(44, "gui.guaniao.camera_filter.glitch_rgb", CameraFilterCategory.CREATIVE),
    CHROMATIC_ABERRATION(45, "gui.guaniao.camera_filter.chromatic_aberration", CameraFilterCategory.CREATIVE),
    NEON(46, "gui.guaniao.camera_filter.neon", CameraFilterCategory.CREATIVE),
    POSTERIZE(47, "gui.guaniao.camera_filter.posterize", CameraFilterCategory.CREATIVE),
    DUOTONE_BLUE(48, "gui.guaniao.camera_filter.duotone_blue", CameraFilterCategory.CREATIVE),
    NIGHT_VISION(49, "gui.guaniao.camera_filter.night_vision", CameraFilterCategory.CREATIVE),
    THERMAL(50, "gui.guaniao.camera_filter.thermal", CameraFilterCategory.CREATIVE);

    public static final int MAX_ID = 50;
    private static final CameraFilter[] BY_ID = Arrays.stream(values())
            .sorted(Comparator.comparingInt(CameraFilter::id))
            .toArray(CameraFilter[]::new);

    private final int id;
    private final String translationKey;
    private final CameraFilterCategory category;

    CameraFilter(int id, String translationKey, CameraFilterCategory category) {
        this.id = id;
        this.translationKey = translationKey;
        this.category = category;
    }

    public int id() {
        return this.id;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String descriptionKey() {
        return this.translationKey + ".description";
    }

    public CameraFilterCategory category() {
        return this.category;
    }

    public CameraFilter next() {
        return byId(this.id >= MAX_ID ? 0 : this.id + 1);
    }

    public CameraFilter previous() {
        return byId(this.id <= 0 ? MAX_ID : this.id - 1);
    }

    public static CameraFilter byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return NONE;
        }
        CameraFilter result = BY_ID[id];
        return result.id == id ? result : NONE;
    }

    public static List<CameraFilter> inCategory(CameraFilterCategory category) {
        return Arrays.stream(values())
                .filter(filter -> filter != NONE && filter.category == category)
                .toList();
    }
}
