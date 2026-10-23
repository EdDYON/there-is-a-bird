package EdDYON.guaniao.client.camera.frame;

import java.math.BigDecimal;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** One keyboard-accessible value with an exact input, slider, and small step buttons. */
final class FrameNumberControl extends AbstractWidget {
    private final Font font;
    private final int minimum, maximum, divisor;
    private final IntSupplier getter, step;
    private final IntConsumer setter;
    private final EditBox field;
    private final ValueSlider slider;
    private final Button minus, plus;
    private boolean synchronizing,inputFocusRequested,sliderDragging;

    FrameNumberControl(Font font,int x,int y,int width,Component label,int minimum,int maximum,int divisor,
                       IntSupplier getter,IntSupplier step,IntConsumer setter) {
        super(x,y,width,32,label);
        this.font=font;this.minimum=minimum;this.maximum=Math.max(minimum,maximum);this.divisor=divisor;
        this.getter=getter;this.step=step;this.setter=setter;
        this.field=new EditBox(font,x+width-48,y,48,14,label);
        this.field.setMaxLength(10);
        this.field.setFilter(value->value.matches("-?[0-9]*(\\.[0-9]*)?"));
        this.field.setValue(format(getter.getAsInt(),divisor));
        this.field.setResponder(value->{
            if(this.synchronizing || value.isEmpty() || value.equals("-") || value.equals(".") || value.equals("-.")) return;
            try {
                double number=Double.parseDouble(value)*divisor;
                if(Double.isFinite(number) && number>=minimum && number<=maximum) this.setter.accept((int)Math.round(number));
            } catch(NumberFormatException ignored) { /* An incomplete edit stays in the box until focus leaves it. */ }
        });
        this.minus=Button.builder(Component.literal("−"),button->this.adjust(-1)).bounds(x,y+16,18,16).build();
        this.plus=Button.builder(Component.literal("+"),button->this.adjust(1)).bounds(x+width-18,y+16,18,16).build();
        this.slider=new ValueSlider(x+20,y+16,width-40);
    }

    private void adjust(int direction) {
        this.field.setFocused(false);
        this.setter.accept(Mth.clamp(this.getter.getAsInt()+direction*this.step.getAsInt(),this.minimum,this.maximum));
        this.syncField();
    }

    private void syncField() {
        this.synchronizing=true;
        this.field.setValue(format(this.getter.getAsInt(),this.divisor));
        this.synchronizing=false;
    }

    static String format(int value,int divisor) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(divisor)).stripTrailingZeros().toPlainString();
    }

    boolean isEditing() { return this.field.isFocused(); }
    boolean isSliding() { return this.sliderDragging; }

    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(!this.field.isFocused()) this.syncField();
        this.field.setEditable(this.active);this.field.active=this.active;
        this.minus.active=this.active && this.getter.getAsInt()>this.minimum;
        this.plus.active=this.active && this.getter.getAsInt()<this.maximum;
        this.slider.active=this.active && this.maximum>this.minimum;
        this.slider.sync();
        graphics.drawString(this.font,this.font.plainSubstrByWidth(this.getMessage().getString(),this.width-52),this.getX(),this.getY()+3,0x404040,false);
        this.field.render(graphics,mouseX,mouseY,partialTick);
        this.minus.render(graphics,mouseX,mouseY,partialTick);
        this.slider.render(graphics,mouseX,mouseY,partialTick);
        this.plus.render(graphics,mouseX,mouseY,partialTick);
    }

    @Override public boolean mouseClicked(double x,double y,int button) {
        if(!this.active || !this.visible || !this.isMouseOver(x,y)) return false;
        boolean input=this.field.isMouseOver(x,y);
        this.inputFocusRequested=input;
        if(input) {this.field.setFocused(true);this.field.mouseClicked(x,y,button);return true;}
        this.field.setFocused(false);
        this.syncField();
        if(this.minus.mouseClicked(x,y,button) || this.plus.mouseClicked(x,y,button))return true;
        this.sliderDragging=this.slider.mouseClicked(x,y,button);
        return this.sliderDragging;
    }

    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        return this.sliderDragging && this.slider.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        this.sliderDragging=false;
        return this.slider.mouseReleased(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==257 || key==335) {this.inputFocusRequested=false;this.field.setFocused(false);this.syncField();return true;}
        if(this.field.isFocused()) return this.field.keyPressed(key,scan,modifiers) || this.field.canConsumeInput();
        if(key==263 || key==262) {this.adjust(key==263 ? -1 : 1);return true;}
        return false;
    }
    @Override public boolean charTyped(char character,int modifiers) {return this.field.isFocused() && this.field.charTyped(character,modifiers);}
    @Override public void setFocused(boolean focused) {
        super.setFocused(focused);
        if(!focused) {this.field.setFocused(false);this.syncField();}
        else this.field.setFocused(this.inputFocusRequested);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput narration) {this.defaultButtonNarrationText(narration);}

    private final class ValueSlider extends AbstractSliderButton {
        ValueSlider(int x,int y,int width) {super(x,y,width,16,Component.empty(),0);this.sync();}
        void sync() {this.value=maximum==minimum ? 0 : Mth.clamp((getter.getAsInt()-minimum)/(double)(maximum-minimum),0,1);}
        @Override public void renderWidget(GuiGraphics graphics,int x,int y,float partialTick) {
            graphics.blitNineSliced(SLIDER_LOCATION,this.getX(),this.getY(),this.getWidth(),this.getHeight(),20,4,200,20,0,this.getTextureY());
            graphics.blitNineSliced(SLIDER_LOCATION,this.getX()+(int)(this.value*(this.width-8)),this.getY(),8,this.getHeight(),20,4,200,20,0,this.getHandleTextureY());
        }
        @Override protected void updateMessage() { }
        @Override protected void applyValue() {
            setter.accept(Mth.clamp(minimum+(int)Math.round(this.value*(maximum-minimum)),minimum,maximum));
            syncField();
        }
    }
}
