package EdDYON.guaniao.client.camera.frame;

import java.util.ArrayDeque;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Modal bitmap draft. Cancel never touches the source design. */
final class FrameOutlineEditor {
    static final int X=12,Y=32,SIZE=256;
    enum Tool { BRUSH, ERASER, LINE, RECTANGLE, ELLIPSE, FILL }
    private int[] mask,strokeBase;
    private int startX,startY,lastX,lastY;
    private boolean drawing,erasing,panning;
    private float zoom=1,panX,panY;
    Tool tool=Tool.BRUSH;
    int brushSize=1;
    boolean grid=true;

    FrameOutlineEditor(int[] mask) {this.mask=mask.clone();}
    int[] mask() {return this.mask.clone();}
    boolean valid() {return Arrays.stream(this.mask).anyMatch(row->row!=0);}
    int zoomPercent() {return Math.round(this.zoom*100);}
    void replace(boolean fill) {Arrays.fill(this.mask,fill ? -1 : 0);}
    private float cell() {return 8*this.zoom;}
    private int cellX(double x) {return Mth.clamp((int)Math.floor((x-X-this.panX)/this.cell()),0,31);}
    private int cellY(double y) {return Mth.clamp((int)Math.floor((y-Y-this.panY)/this.cell()),0,31);}
    boolean inside(double x,double y) {return x>=X && y>=Y && x<X+SIZE && y<Y+SIZE;}

    void render(GuiGraphics graphics,int mouseX,int mouseY) {
        float cell=this.cell();
        for(int y=0;y<32;y++) for(int x=0;x<32;x++) {
            int x0=Math.max(X,Math.round(X+this.panX+x*cell)),y0=Math.max(Y,Math.round(Y+this.panY+y*cell));
            int x1=Math.min(X+SIZE,Math.round(X+this.panX+(x+1)*cell)),y1=Math.min(Y+SIZE,Math.round(Y+this.panY+(y+1)*cell));
            if(x1<=x0 || y1<=y0) continue;
            boolean filled=(this.mask[y]&(1<<x))!=0;
            graphics.fill(x0,y0,x1,y1,filled ? 0xFFC7B895 : ((x+y)%2==0 ? 0xFF777777 : 0xFF999999));
            if(this.grid) {
                graphics.fill(x0,y0,x1,Math.min(y1,y0+1),0x44404040);
                graphics.fill(x0,y0,Math.min(x1,x0+1),y1,0x44404040);
            }
        }
        graphics.renderOutline(X-1,Y-1,SIZE+2,SIZE+2,0xFF373737);
        if(this.inside(mouseX,mouseY)) {
            int x=this.cellX(mouseX),y=this.cellY(mouseY),radius=(this.brushSize-1)/2;
            int x0=Math.max(X,Math.round(X+this.panX+(x-radius)*cell));
            int y0=Math.max(Y,Math.round(Y+this.panY+(y-radius)*cell));
            int x1=Math.min(X+SIZE,Math.round(X+this.panX+(x-radius+this.brushSize)*cell));
            int y1=Math.min(Y+SIZE,Math.round(Y+this.panY+(y-radius+this.brushSize)*cell));
            graphics.renderOutline(x0,y0,x1-x0,y1-y0,this.tool==Tool.ERASER ? 0xFFFF7777 : 0xFFFFFFFF);
        }
    }

    boolean click(double x,double y,int button) {
        if(!this.inside(x,y)) return false;
        if(button==2) {this.panning=true;return true;}
        if(button!=0 && button!=1) return false;
        this.strokeBase=this.mask.clone();this.drawing=true;
        this.erasing=button==1 || this.tool==Tool.ERASER;
        this.startX=this.lastX=this.cellX(x);this.startY=this.lastY=this.cellY(y);
        if(this.tool==Tool.FILL && button==0) {this.flood(this.startX,this.startY);this.drawing=false;}
        else this.paint(this.startX,this.startY);
        return true;
    }
    boolean drag(double x,double y,double dx,double dy) {
        if(this.panning) {this.panX+=(float)dx;this.panY+=(float)dy;this.clampPan();return true;}
        if(!this.drawing) return false;
        int cx=this.cellX(x),cy=this.cellY(y);
        if(this.erasing || this.tool==Tool.BRUSH || this.tool==Tool.ERASER) this.line(this.lastX,this.lastY,cx,cy);
        else {
            this.mask=this.strokeBase.clone();
            if(this.tool==Tool.LINE) this.line(this.startX,this.startY,cx,cy);
            else if(this.tool==Tool.RECTANGLE || this.tool==Tool.ELLIPSE) {
                int left=Math.min(cx,this.startX),right=Math.max(cx,this.startX),top=Math.min(cy,this.startY),bottom=Math.max(cy,this.startY);
                double midX=(left+right)/2.0,midY=(top+bottom)/2.0,rx=(right-left+1)/2.0,ry=(bottom-top+1)/2.0;
                for(int py=top;py<=bottom;py++) for(int px=left;px<=right;px++) {
                    if(this.tool==Tool.RECTANGLE || Math.pow((px-midX)/rx,2)+Math.pow((py-midY)/ry,2)<=1) this.set(px,py,true);
                }
            }
        }
        this.lastX=cx;this.lastY=cy;return true;
    }
    boolean release() {boolean used=this.drawing || this.panning;this.drawing=false;this.panning=false;this.strokeBase=null;return used;}
    boolean scroll(double x,double y,double amount) {
        if(!this.inside(x,y)) return false;
        float oldCell=this.cell();double cx=(x-X-this.panX)/oldCell,cy=(y-Y-this.panY)/oldCell;
        this.zoom=Mth.clamp(this.zoom+(float)amount*0.5F,1,4);
        this.panX=(float)(x-X-cx*this.cell());this.panY=(float)(y-Y-cy*this.cell());this.clampPan();return true;
    }
    private void clampPan() {float minimum=SIZE-32*this.cell();this.panX=Mth.clamp(this.panX,minimum,0);this.panY=Mth.clamp(this.panY,minimum,0);}
    private void paint(int x,int y) {
        int radius=(this.brushSize-1)/2;
        for(int py=y-radius;py<y-radius+this.brushSize;py++) for(int px=x-radius;px<x-radius+this.brushSize;px++) this.set(px,py,!this.erasing);
    }
    private void set(int x,int y,boolean filled) {if(x<0 || y<0 || x>31 || y>31)return;if(filled)this.mask[y]|=1<<x;else this.mask[y]&=~(1<<x);}
    private void line(int x0,int y0,int x1,int y1) {
        int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));
        for(int step=0;step<=steps;step++) {float t=steps==0 ? 0 : step/(float)steps;this.paint(Math.round(Mth.lerp(t,x0,x1)),Math.round(Mth.lerp(t,y0,y1)));}
    }
    private void flood(int x,int y) {
        boolean old=(this.mask[y]&(1<<x))!=0;boolean[] visited=new boolean[1024];ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(y*32+x);
        while(!queue.isEmpty()) {
            int index=queue.removeFirst(),cx=index%32,cy=index/32;
            if(visited[index])continue;visited[index]=true;
            if(((this.mask[cy]&(1<<cx))!=0)!=old)continue;this.set(cx,cy,!old);
            if(cx>0)queue.add(index-1);if(cx<31)queue.add(index+1);if(cy>0)queue.add(index-32);if(cy<31)queue.add(index+32);
        }
    }
}
