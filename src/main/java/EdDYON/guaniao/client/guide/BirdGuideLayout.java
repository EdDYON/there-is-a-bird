package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.gui.layout.GuiLayoutConfig;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import java.util.Map;
import java.util.Set;

/** Fixed GUI pixels, like vanilla containers; compact windows show one panel at a time. */
final class BirdGuideLayout {
    private static final Map<String, GuiLayoutRect> DEFAULTS = Map.ofEntries(
            Map.entry("catalogue_panel", new GuiLayoutRect(0, 0, 144, 230)),
            Map.entry("main_panel", new GuiLayoutRect(150, 0, 252, 230)),
            Map.entry("header", new GuiLayoutRect(158, 8, 207, 10)),
            Map.entry("detail_header", new GuiLayoutRect(158, 26, 236, 10)),
            Map.entry("preview_box", new GuiLayoutRect(158, 42, 86, 101)),
            Map.entry("info_card", new GuiLayoutRect(254, 42, 138, 158)),
            Map.entry("pose_buttons", new GuiLayoutRect(158, 158, 86, 44)),
            Map.entry("config_button", new GuiLayoutRect(158, 206, 114, 20)),
            Map.entry("close_button", new GuiLayoutRect(278, 206, 116, 20)),
            Map.entry("catalogue_toggle", new GuiLayoutRect(374, 5, 20, 20)),
            Map.entry("species_header", new GuiLayoutRect(8, 10, 104, 10)),
            Map.entry("search", new GuiLayoutRect(8, 28, 128, 18)),
            Map.entry("species_list", new GuiLayoutRect(8, 54, 128, 142)),
            Map.entry("previous_page", new GuiLayoutRect(8, 206, 20, 20)),
            Map.entry("next_page", new GuiLayoutRect(116, 206, 20, 20)),
            Map.entry("catalogue_back", new GuiLayoutRect(116, 5, 20, 20)));
    private static final Set<String> CATALOGUE_RECTS = Set.of(
            "catalogue_panel", "species_header", "search", "species_list",
            "previous_page", "next_page", "catalogue_back");
    private static final Set<String> BOTTOM_RECTS = Set.of(
            "pose_buttons", "config_button", "close_button", "previous_page", "next_page");
    private static final Set<String> FLEX_RECTS = Set.of(
            "catalogue_panel", "main_panel", "preview_box", "info_card", "species_list");
    private final GuiLayoutConfig config;
    private final int mainOffset;
    private final int catalogueOffset;
    private final int top;
    private final int heightDelta;
    final boolean wide;

    BirdGuideLayout(int width, int height, GuiLayoutConfig config) {
        this.config = config;
        GuiLayoutRect main = raw("main_panel");
        GuiLayoutRect catalogue = raw("catalogue_panel");
        this.wide = width >= main.right() + 8;
        int panelHeight = Math.min(main.h(), Math.max(164, height - 8));
        this.top = Math.max(0, (height - panelHeight) / 2);
        this.heightDelta = panelHeight - main.h();
        int origin = (width - main.right()) / 2;
        this.mainOffset = this.wide ? origin : (width - main.w()) / 2 - main.x();
        this.catalogueOffset = this.wide ? origin : (width - catalogue.w()) / 2 - catalogue.x();
    }

    GuiLayoutRect rect(String id) {
        GuiLayoutRect r = raw(id);
        return new GuiLayoutRect(r.x() + (CATALOGUE_RECTS.contains(id) ? this.catalogueOffset : this.mainOffset),
                r.y() + this.top + (BOTTOM_RECTS.contains(id) ? this.heightDelta : 0),
                r.w(), r.h() + (FLEX_RECTS.contains(id) ? this.heightDelta : 0));
    }

    GuiLayoutRect pose(int index) {
        GuiLayoutRect area = rect("pose_buttons");
        int width = (area.w() - 4) / 2;
        return new GuiLayoutRect(area.x() + index % 2 * (width + 4), area.y() + index / 2 * 24, width, 20);
    }

    int pageSize() {
        return 4 * Math.min(4, Math.max(1, (rect("species_list").h() + 4) / 31));
    }

    GuiLayoutRect slot(int index) {
        GuiLayoutRect area = rect("species_list");
        return new GuiLayoutRect(area.x() + 3 + index % 4 * 31, area.y() + index / 4 * 31, 27, 27);
    }

    private GuiLayoutRect raw(String id) {
        GuiLayoutRect fallback = DEFAULTS.get(id);
        return this.config == null ? fallback : this.config.rects().getOrDefault(id, fallback);
    }
}
