package EdDYON.guaniao.client.config;

import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;

/** Uses GUI pixels rather than scaling fonts and controls with window size. */
final class BirdConfigLayout {
    static final int ROW_HEIGHT = 30;
    final GuiLayoutRect left;
    final GuiLayoutRect right;

    BirdConfigLayout(int width, int height) {
        int w = Math.min(600, width - 8);
        int h = Math.min(320, height - 8);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int side = width < 320 ? 82 : 110;
        this.left = new GuiLayoutRect(x, y, side, h);
        this.right = new GuiLayoutRect(x + side + 6, y, w - side - 6, h);
    }

    GuiLayoutRect mode(int index) {
        int w = (left.w() - 18) / 2;
        return new GuiLayoutRect(left.x() + 7 + index * (w + 4), left.y() + 24, w, 20);
    }
    int listRows() { return Math.max(1, (left.h() - 78) / 22); }
    GuiLayoutRect subject(int row) { return new GuiLayoutRect(left.x() + 7, left.y() + 50 + row * 22, left.w() - 14, 20); }
    GuiLayoutRect listArrow(boolean next) {
        return new GuiLayoutRect(next ? left.right() - 27 : left.x() + 7, left.bottom() - 27, 20, 20);
    }
    GuiLayoutRect tab(int index) {
        int w = (right.w() - 24) / 5;
        return new GuiLayoutRect(right.x() + 8 + index * (w + 2), right.y() + 25, w, 20);
    }
    boolean hasPreview(boolean species) { return species && right.w() >= 410 && right.h() >= 240; }
    GuiLayoutRect preview() { return new GuiLayoutRect(right.right() - 109, right.y() + 55, 96, 104); }
    GuiLayoutRect fields(boolean species) {
        int top = right.y() + (species ? 51 : 39);
        return new GuiLayoutRect(right.x() + 8, top, right.w() - 26 - (hasPreview(species) ? 108 : 0),
                right.bottom() - 48 - top);
    }
    int fieldRows(boolean species) { return Math.max(1, fields(species).h() / ROW_HEIGHT); }
    GuiLayoutRect row(int row, boolean species) {
        GuiLayoutRect fields = fields(species);
        return new GuiLayoutRect(fields.x(), fields.y() + row * ROW_HEIGHT, fields.w(), ROW_HEIGHT - 2);
    }
    GuiLayoutRect control(int row, boolean species) {
        GuiLayoutRect r = row(row, species);
        int w = Math.min(80, Math.max(52, r.w() / 3));
        return new GuiLayoutRect(r.right() - w - 4, r.y() + 4, w, 20);
    }
    GuiLayoutRect fieldScrollbar(boolean species) {
        GuiLayoutRect fields = fields(species);
        return new GuiLayoutRect(fields.right() + 4, fields.y(), 6, fields.h());
    }
    int maxFieldScroll(boolean species, int count) { return Math.max(0, count - fieldRows(species)); }
    GuiLayoutRect fieldThumb(boolean species, int count, int scroll) {
        GuiLayoutRect track = fieldScrollbar(species);
        int max = maxFieldScroll(species, count);
        int h = max == 0 ? track.h() : Math.max(12, track.h() * fieldRows(species) / count);
        int y = track.y() + (max == 0 ? 0 : (track.h() - h) * Math.max(0, Math.min(max, scroll)) / max);
        return new GuiLayoutRect(track.x(), y, track.w(), h);
    }
    int fieldScrollAt(boolean species, int count, double thumbTop) {
        GuiLayoutRect track = fieldScrollbar(species);
        int travel = track.h() - fieldThumb(species, count, 0).h();
        if (travel <= 0) return 0;
        double progress = Math.max(0, Math.min(1, (thumbTop - track.y()) / travel));
        return (int)Math.round(progress * maxFieldScroll(species, count));
    }
    GuiLayoutRect footer(int index) {
        int w = (right.w() - 24) / 3;
        return new GuiLayoutRect(right.x() + 8 + index * (w + 4), right.bottom() - 28, w, 20);
    }
    GuiLayoutRect status() { return new GuiLayoutRect(right.x() + 8, right.bottom() - 41, right.w() - 16, 10); }
}
