package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.gui.layout.GuiLayoutConfig;
import EdDYON.guaniao.client.gui.layout.GuiLayoutLoader;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BirdGuideUiTest {
    public static void main(String[] args) {
        GuiLayoutConfig config = GuiLayoutLoader.loadBirdGuideLayout();
        require(config != null, "Load the actual packaged layout JSON");
        int layouts = 0;
        for (int width : new int[] {272, 320, 400, 409, 410, 427, 480, 640, 960, 1920}) {
            for (int height : new int[] {180, 200, 240, 270, 360, 540, 1080}) {
                BirdGuideLayout layout = new BirdGuideLayout(width, height, config);
                BirdGuideLayout fallback = new BirdGuideLayout(width, height, null);
                GuiLayoutRect screen = new GuiLayoutRect(0, 0, width, height);
                GuiLayoutRect main = layout.rect("main_panel");
                GuiLayoutRect catalogue = layout.rect("catalogue_panel");
                require(contains(screen, main) && contains(screen, catalogue), "Panels stay on screen");
                require(main.w() == 252 && catalogue.w() == 144, "Large windows must not stretch the pixel UI");
                require(layout.wide == (width >= 410), "Use compact navigation when both panels cannot fit");
                if (layout.wide) require(!overlaps(main, catalogue), "Side-by-side panels must not overlap");
                for (String id : List.of("header", "detail_header", "preview_box", "info_card", "pose_buttons",
                        "config_button", "close_button", "catalogue_toggle")) {
                    require(contains(main, layout.rect(id)), id + " stays inside the main panel");
                    require(layout.rect(id).equals(fallback.rect(id)), "Packaged and fallback layout agree: " + id);
                }
                require(!overlaps(layout.rect("preview_box"), layout.rect("info_card")), "Model cannot cover notes");
                require(!overlaps(layout.rect("pose_buttons"), layout.rect("config_button")), "Pose and footer separate");
                require(!overlaps(layout.rect("info_card"), layout.rect("close_button")), "Notes cannot cover close");
                for (int i = 0; i < 4; i++) {
                    require(contains(layout.rect("pose_buttons"), layout.pose(i)), "All four poses stay clickable");
                    for (int j = 0; j < i; j++) require(!overlaps(layout.pose(i), layout.pose(j)), "Pose hitboxes separate");
                }
                Set<GuiLayoutRect> slots = new HashSet<>();
                for (int i = 0; i < layout.pageSize(); i++) {
                    GuiLayoutRect slot = layout.slot(i);
                    require(contains(layout.rect("species_list"), slot), "Every visible bird fits the grid");
                    for (GuiLayoutRect other : slots) require(!overlaps(slot, other), "Bird hitboxes separate");
                    slots.add(slot);
                }
                layouts++;
            }
        }

        BirdGuideIndex index = new BirdGuideIndex();
        List<String> birds = List.of("夜鹭 night heron 夜行", "麻雀 sparrow 昼行", "雨伞巴丹 umbrella cockatoo 学舌", "几维鸟 kiwi 夜行");
        index.search(birds.size(), "  COCKATOO  umbrella ", birds::get);
        require(index.visible().equals(List.of(2)), "Multiword English search is case-insensitive and keeps original index");
        index.search(birds.size(), "夜行", birds::get);
        require(index.visible().equals(List.of(0, 3)), "Search localized tags as well as names");
        index.search(birds.size(), "雨伞", birds::get);
        require(index.visible().equals(List.of(2)), "Chinese partial names work");
        index.search(birds.size(), "does not exist", birds::get);
        index.movePage(10);
        require(index.count() == 0 && index.visible().isEmpty() && index.page() == 0, "Empty search stays safe");
        index.search(37, " ", i -> "bird " + i);
        index.setPageSize(16);
        require(index.count() == 37 && index.pageCount() == 3, "Blank query restores all entries");
        index.movePage(2);
        require(index.visible().equals(List.of(32, 33, 34, 35, 36)), "Last page preserves each original species index");
        index.setPageSize(8);
        require(index.page() == 4 && index.visible().get(0) == 32, "Resizing retains the first visible result");
        index.movePage(99);
        require(index.page() == 4, "Clamp beyond last page");
        index.movePage(-99);
        require(index.page() == 0, "Clamp before first page");
        index.search(37, "bird 36", i -> "bird " + i);
        require(index.visible().equals(List.of(36)) && index.page() == 0, "New search resets stale page offsets");
        System.out.println("BirdGuideUiTest passed: " + layouts + " window layouts; containment, hitboxes, fallback, Chinese/English search, paging and resize checked");
    }

    private static boolean contains(GuiLayoutRect outer, GuiLayoutRect inner) {
        return inner.isValid() && inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static boolean overlaps(GuiLayoutRect a, GuiLayoutRect b) {
        return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
