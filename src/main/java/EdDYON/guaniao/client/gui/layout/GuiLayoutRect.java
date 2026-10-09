package EdDYON.guaniao.client.gui.layout;

public record GuiLayoutRect(int x, int y, int w, int h) {
    public int right() {
        return this.x + this.w;
    }

    public int bottom() {
        return this.y + this.h;
    }

    public int centerX() {
        return this.x + this.w / 2;
    }

    public int centerY() {
        return this.y + this.h / 2;
    }

    public GuiLayoutRect scale(float scaleX, float scaleY) {
        return new GuiLayoutRect(
                Math.round(this.x * scaleX),
                Math.round(this.y * scaleY),
                Math.round(this.w * scaleX),
                Math.round(this.h * scaleY));
    }

    public GuiLayoutRect inset(int amount) {
        return new GuiLayoutRect(
                this.x + amount,
                this.y + amount,
                Math.max(0, this.w - amount * 2),
                Math.max(0, this.h - amount * 2));
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.right()
                && mouseY >= this.y && mouseY <= this.bottom();
    }

    public boolean isValid() {
        return this.w > 0 && this.h > 0;
    }
}
