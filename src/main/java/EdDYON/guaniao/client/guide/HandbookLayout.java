package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;

/** Preserve reading width at high GUI scales by collapsing the two navigation rails. */
record HandbookLayout(GuiLayoutRect paper, GuiLayoutRect view, GuiLayoutRect sidebar, GuiLayoutRect toc,
                      int footerY, int width, int height) {
    static HandbookLayout of(int width, int height) {
        return of(width, height, true);
    }

    static HandbookLayout of(int width, int height, boolean article) {
        int margin = width < 360 ? 5 : 10;
        int left = width >= 600 ? 110 : 0;
        int right = article && width >= 820 ? 110 : 0;
        int content = Math.min(article ? 620 : 760, width - margin * 2 - left - right);
        int x = (width - content - left - right) / 2;
        int bottom = Math.max(100, height - 29);
        GuiLayoutRect paper = new GuiLayoutRect(x + left, 38, content, bottom - 38);
        return new HandbookLayout(paper, new GuiLayoutRect(paper.x()+12, 76, paper.w()-24, bottom-83),
                new GuiLayoutRect(x, 46, Math.max(0,left-8), bottom-46),
                new GuiLayoutRect(paper.right()+8,46,Math.max(0,right-8),bottom-46),
                height-24,width,height);
    }
    boolean hasSidebar() { return sidebar.w() > 0; }
    boolean hasToc() { return toc.w() > 0; }
    int textWidth() { return view.w()-9; }
    int columns() { return Math.max(1, Math.min(4, (textWidth()+6)/156)); }
    int birdColumns() { return Math.max(1, Math.min(3, (textWidth()+6)/205)); }

    static int clampScroll(int value, int contentHeight, int viewHeight) {
        return Math.max(0, Math.min(value, Math.max(0,contentHeight-viewHeight)));
    }
    static int thumbHeight(int viewHeight, int contentHeight) {
        return Math.min(viewHeight, Math.max(16,viewHeight*viewHeight/Math.max(1,contentHeight)));
    }
}
