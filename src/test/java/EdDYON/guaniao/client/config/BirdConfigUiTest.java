package EdDYON.guaniao.client.config;

import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Standalone layout/coverage regressions; no client window or world required. */
public final class BirdConfigUiTest {
    public static void main(String[] args) throws Exception {
        int layouts = 0;
        for (int width : new int[] {272, 320, 400, 427, 480, 540, 600, 640, 960, 1920}) {
            for (int height : new int[] {180, 200, 240, 270, 320, 360, 540, 1080}) {
                BirdConfigLayout layout = new BirdConfigLayout(width, height);
                GuiLayoutRect screen = new GuiLayoutRect(0, 0, width, height);
                require(contains(screen, layout.left) && contains(screen, layout.right), "Panels fit screen");
                require(!overlaps(layout.left, layout.right), "Panels do not overlap");
                List<GuiLayoutRect> left = new ArrayList<>();
                for (int i = 0; i < 2; i++) left.add(layout.mode(i));
                for (int i = 0; i < layout.listRows(); i++) left.add(layout.subject(i));
                left.add(layout.listArrow(false)); left.add(layout.listArrow(true));
                checkControls(layout.left, left);
                for (boolean species : new boolean[] {false, true}) {
                    List<GuiLayoutRect> right = new ArrayList<>();
                    if (species) for (int i = 0; i < 5; i++) right.add(layout.tab(i));
                    for (int row = 0; row < layout.fieldRows(species); row++) {
                        GuiLayoutRect r = layout.row(row, species);
                        require(contains(layout.fields(species), r), "Every field stays in its viewport");
                        require(contains(r, layout.control(row, species)), "Control fits row");
                        require(layout.control(row, species).x() - r.x() >= 75, "Labels retain usable width");
                        right.add(r);
                    }
                    GuiLayoutRect track = layout.fieldScrollbar(species);
                    right.add(track);
                    for (int count : new int[] {0, 1, 4, 8, 10, 52}) {
                        int max = layout.maxFieldScroll(species, count);
                        GuiLayoutRect first = layout.fieldThumb(species, count, 0);
                        GuiLayoutRect last = layout.fieldThumb(species, count, max);
                        require(contains(track, first) && contains(track, last), "Scrollbar thumb stays within its track");
                        require(first.y() == track.y() && last.bottom() == track.bottom(), "First and last rows reach track endpoints");
                        require(layout.fieldScrollAt(species, count, track.y() - 100) == 0, "Dragging above track clamps to first row");
                        require(layout.fieldScrollAt(species, count, track.bottom() + 100) == max, "Dragging below track reaches last row");
                        int previous = 0;
                        for (int y = track.y(); y <= track.bottom(); y++) {
                            int scroll = layout.fieldScrollAt(species, count, y);
                            require(scroll >= previous && scroll <= max, "Dragging scrolls monotonically within bounds");
                            previous = scroll;
                        }
                        if (max == 0) require(first.equals(track), "Short lists have a full-height disabled thumb");
                    }
                    right.add(layout.status());
                    for (int i = 0; i < 3; i++) right.add(layout.footer(i));
                    if (layout.hasPreview(species)) right.add(layout.preview());
                    checkControls(layout.right, right);
                }
                layouts++;
            }
        }
        String source = Files.readString(Path.of("src/main/java/EdDYON/guaniao/client/config/BirdConfigScreen.java"));
        int start = source.indexOf("private List<SettingSpec> allSettingsForSelection()");
        int split = source.indexOf("BirdSpeciesConfig bird =", start);
        int end = source.indexOf("private LivingEntity previewEntity()", split);
        List<String> global = keys(source.substring(start, split)); global.add("scope");
        List<String> species = keys(source.substring(split, end));
        require(global.size() == 52, "All 52 original global settings remain reachable");
        require(species.size() == 14, "All 14 original per-species settings remain reachable");
        for (String key : global) coverage(key, List.of(BirdConfigCategory.values()));
        for (String key : species) coverage(key, BirdConfigCategory.SPECIES);
        for (String lang : new String[] {"zh_cn", "en_us"}) {
            var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/guaniao/lang/" + lang + ".json"))).getAsJsonObject();
            for (BirdConfigCategory category : BirdConfigCategory.values()) require(json.has(category.translationKey()), "Category translation: " + lang);
            for (BirdConfigCategory category : BirdConfigCategory.SPECIES) require(json.has(category.translationKey() + ".short"), "Species tab translation: " + lang);
            for (String key : global) require(json.has("gui.guaniao.bird_config.setting." + key), "Global label: " + key);
            for (String key : species) require(json.has("gui.guaniao.bird_config.setting." + key), "Species label: " + key);
        }
        System.out.println("PASS: " + layouts + " settings layouts, scrollbar bounds/dragging, category coverage and zh/en labels (52 global + 14 species settings)");
    }

    private static List<String> keys(String source) {
        var matcher = Pattern.compile("SettingSpec\\.(?:toggle|number)\\(\"([^\"]+)\"").matcher(source);
        List<String> keys = new ArrayList<>();
        while (matcher.find()) keys.add(matcher.group(1));
        return keys;
    }
    private static void coverage(String key, List<BirdConfigCategory> categories) {
        require(categories.stream().filter(c -> c.contains(key)).count() == 1, "Exactly one reachable category for " + key);
    }
    private static void checkControls(GuiLayoutRect panel, List<GuiLayoutRect> controls) {
        for (int i = 0; i < controls.size(); i++) {
            require(contains(panel, controls.get(i)), "Control within panel: " + controls.get(i));
            for (int j = 0; j < i; j++) require(!overlaps(controls.get(i), controls.get(j)), "Separate hitboxes: " + controls.get(i) + " / " + controls.get(j));
        }
    }
    private static boolean contains(GuiLayoutRect outer, GuiLayoutRect inner) {
        return inner.isValid() && inner.x() >= outer.x() && inner.y() >= outer.y() && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }
    private static boolean overlaps(GuiLayoutRect a, GuiLayoutRect b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
