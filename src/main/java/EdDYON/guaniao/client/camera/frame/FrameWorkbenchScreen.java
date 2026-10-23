package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.content.camera.PhotographFrameStyle;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.frame.FrameDesign;
import EdDYON.guaniao.content.camera.frame.FrameWorkbenchMenu;
import EdDYON.guaniao.network.CraftFramePacket;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.registry.GuaniaoItems;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Vanilla widgets in a scaled container coordinate space; no web view or extra screen texture. */
public final class FrameWorkbenchScreen extends AbstractContainerScreen<FrameWorkbenchMenu> {
    private static final int PANEL_WIDTH=390,PANEL_HEIGHT=300;
    private static final int CANVAS_X=92,CANVAS_Y=52,CELL=4;
    private static final int PALETTE_COLUMNS=14,PALETTE_ROWS=10,PALETTE_PAGE_SIZE=PALETTE_COLUMNS*PALETTE_ROWS;
    private static final int PALETTE_X=108,PALETTE_Y=58,PALETTE_STEP=18;
    private static final int WOOD_TEXT=0xFFF0D2,WOOD_ERROR=0xFFAAAA;
    private static final String KEY="gui.guaniao.frame_workbench.";
    private FrameDesignLibrary library=new FrameDesignLibrary();
    private final FrameWorkshopClock workshopClock=new FrameWorkshopClock();
    private final FrameObserverClock observerClock=new FrameObserverClock();
    private final FrameObserverClock progressBirdClock=new FrameObserverClock();
    private FrameDesign design=FrameDesign.DEFAULT;
    private ItemStack lastSource=ItemStack.EMPTY,previewStack=ItemStack.EMPTY,craftingPreview=ItemStack.EMPTY,lastResult=ItemStack.EMPTY;
    private int readyFlashTicks;
    private boolean widgetsLocked,awaitingCraftResponse,workSnapshotReady,playWorkStartSound;
    private FrameDesign workDesign=FrameDesign.DEFAULT;
    private FrameDesign observerDesign=FrameDesign.DEFAULT;
    private long activeWorkSeed;
    private int currentWorkTicks,workSoundStage=-1;
    private float frameWorkTicks;
    private int page,libraryIndex,statusTicks,pendingResponse,pendingRevision=-1,selectedDecoration=-1;
    private float viewYaw=-15,viewPitch=7,viewZoom=1,uiScale=1;
    private int originX,originY;
    private boolean editShape,eraser,painting,turning,touched,invalidOutline,draggingDecoration;
    private ResourceLocation decorationItem=new ResourceLocation("minecraft","feather");
    private FrameMaterialPalette.Kind paletteKind;
    private FrameMaterialPalette.Category paletteCategory=FrameMaterialPalette.Category.ALL;
    private List<FrameMaterialPalette.Category> paletteCategories=List.of();
    private List<ItemStack> paletteEntries=List.of();
    private String paletteSearch="";
    private int palettePage,lastMinimum=-1,lastMaximum=-1;
    private EditBox searchBox;
    private Button palettePrevious,paletteNext;
    private FrameOutlineEditor outlineEditor;
    private Button outlineConfirm;
    private double decorationDragOffsetX,decorationDragOffsetY;
    private int[] strokeMask;
    private int[] workingMask;
    private int lastCellX,lastCellY;
    private boolean strokeErase;
    private String name="";
    private Component status=Component.empty();
    private EditBox nameBox;
    private Button craftButton;

    public FrameWorkbenchScreen(FrameWorkbenchMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        this.imageWidth=PANEL_WIDTH;
        this.imageHeight=PANEL_HEIGHT;
    }

    @Override protected void init() {
        super.init();
        this.library=new FrameDesignLibrary();
        this.uiScale=Math.min(1.0F,Math.min((this.width-8.0F)/PANEL_WIDTH,(this.height-8.0F)/PANEL_HEIGHT));
        this.uiScale=Math.max(0.25F,this.uiScale);
        this.originX=Math.round((this.width-PANEL_WIDTH*this.uiScale)/2);
        this.originY=Math.round((this.height-PANEL_HEIGHT*this.uiScale)/2);
        this.leftPos=0;
        this.topPos=0;
        this.buildWidgets();
        this.refreshPreview();
    }

    private void buildWidgets() {
        if(this.nameBox!=null) this.name=this.nameBox.getValue();
        this.clearWidgets();
        this.nameBox=null;
        this.searchBox=null;
        if(this.paletteKind!=null) {this.paletteWidgets();return;}
        if(this.outlineEditor!=null) {this.outlineWidgets();return;}
        String[] tabs={"shape","material","photo","decoration","design"};
        for(int i=0;i<5;i++) {
            final int tab=i;
            Button button=button(78+i*60,18,59,20,text("tab."+tabs[i]),()->{
                this.page=tab;
                if(tab!=0) this.editShape=false;
                this.buildWidgets();
            });
            button.active=this.page!=i;
        }
        if(this.page==0) this.shapeWidgets();
        else if(this.page==1) this.materialWidgets();
        else if(this.page==2) this.photoWidgets();
        else if(this.page==3) this.decorationWidgets();
        else this.designWidgets();
        if(this.editShape) {
            button(9,161,58,18,text("brush"),()->{this.eraser=false;this.buildWidgets();}).active=this.eraser;
            button(9,182,58,18,text("eraser"),()->{this.eraser=true;this.buildWidgets();}).active=!this.eraser;
        }
        this.craftButton=button(244,278,132,18,text("craft"),()->{
            if(this.invalidOutline || !this.menu.canCraft(this.design) || this.isCraftingUi()) return;
            this.refreshPreview();
            this.craftingPreview=this.previewStack.copy();
            this.pendingRevision=this.menu.getCraftRevision();
            this.pendingResponse=this.menu.getCraftResponse();
            this.awaitingCraftResponse=true;
            this.observerClock.clear();
            this.playWorkStartSound=true;
            this.workSnapshotReady=false;
            this.currentWorkTicks=0;this.frameWorkTicks=0;this.workSoundStage=-1;
            this.workshopClock.reset(0,System.nanoTime());
            this.turning=false;this.draggingDecoration=false;this.painting=false;
            this.readyFlashTicks=0;this.statusTicks=0;this.setFocused(null);
            GuaniaoNetwork.sendToServer(new CraftFramePacket(this.menu.containerId,this.pendingRevision,this.design));
            this.updateCraftingWidgets();
        });
        this.craftButton.active=!this.invalidOutline && !this.isCraftingUi() && this.menu.canCraft(this.design);
        this.updateCraftingWidgets();
    }

    private void shapeWidgets() {
        number(44,"width",this.menu.getMinSizePixels(),this.menu.getMaxSizePixels(),16,()->this.design.widthPixels(),()->1,value->this.intValueLive("Width",value));
        number(78,"height",this.menu.getMinSizePixels(),this.menu.getMaxSizePixels(),16,()->this.design.heightPixels(),()->1,value->this.intValueLive("Height",value));
        number(112,"border",FrameDesign.MIN_BORDER,FrameDesign.MAX_BORDER,1,()->this.design.borderCells(),()->1,value->this.intValueLive("Border",value));
        number(146,"depth",FrameDesign.MIN_DEPTH,FrameDesign.MAX_DEPTH,16,()->this.design.depthPixels(),()->1,value->this.intValueLive("Depth",value));
        button(244,180,64,16,text("template."+this.design.template().id()),()->{
            FrameDesign.Template[] values=FrameDesign.Template.values();
            FrameDesign.Template next=values[(this.design.template().ordinal()+1)%(values.length-1)];
            this.change(tag->{tag.putString("Template",next.id());tag.putIntArray("Mask",FrameDesign.templateMask(next));});
        });
        button(312,180,64,16,text("edit_shape"),()->{this.outlineEditor=new FrameOutlineEditor(this.design.mask());this.buildWidgets();});
    }

    private void number(int y,String key,int min,int max,int divisor,java.util.function.IntSupplier getter,java.util.function.IntSupplier step,java.util.function.IntConsumer setter) {
        FrameNumberControl control=new FrameNumberControl(this.font,244,y,132,text("number."+key),min,max,divisor,getter,step,setter);
        this.addRenderableWidget(control);
    }

    private void intValueLive(String key,int value) {
        CompoundTag tag=this.design.toTag();tag.putInt(key,value);
        FrameDesign updated=FrameDesign.fromTag(tag);
        if(updated==null || updated.equals(this.design)) return;
        this.design=updated;this.touched=true;this.refreshPreview();
    }

    private void materialWidgets() {
        materialButton(62,this.design.frameMaterial(),FrameMaterialPalette.Kind.FRAME);
        materialButton(110,this.design.trimMaterial(),FrameMaterialPalette.Kind.TRIM);
        materialButton(158,this.design.liningMaterial(),FrameMaterialPalette.Kind.LINING);
    }

    private void materialButton(int y,ResourceLocation id,FrameMaterialPalette.Kind kind) {
        ItemStack stack=FrameMaterialPalette.stack(id);
        button(244,y,132,22,stack.isEmpty() ? text("none") : stack.getHoverName(),()->this.openPalette(kind));
    }

    private void openPalette(FrameMaterialPalette.Kind kind) {
        this.paletteKind=kind;this.paletteSearch="";this.palettePage=0;
        this.paletteCategory=FrameMaterialPalette.Category.ALL;
        this.paletteCategories=FrameMaterialPalette.categories(kind);
        this.paletteEntries=FrameMaterialPalette.entries(kind,this.paletteCategory,"");this.buildWidgets();
    }

    private void paletteWidgets() {
        this.searchBox=new EditBox(this.font,14,28,310,20,text("palette_search"));
        this.searchBox.setMaxLength(64);this.searchBox.setValue(this.paletteSearch);
        this.searchBox.setHint(text("palette_search"));
        this.searchBox.setResponder(value->{this.paletteSearch=value;this.refreshPaletteEntries();});
        this.addRenderableWidget(this.searchBox);
        button(330,28,46,20,text("cancel"),this::closeModal);
        for(int index=0;index<this.paletteCategories.size();index++) {
            FrameMaterialPalette.Category category=this.paletteCategories.get(index);
            button(14,PALETTE_Y+index*18,82,17,text("palette_category."+category.name().toLowerCase(Locale.ROOT)),()->{
                this.paletteCategory=category;this.refreshPaletteEntries();this.setFocused(null);this.buildWidgets();
            }).active=category!=this.paletteCategory;
        }
        this.palettePrevious=button(14,264,64,20,text("previous_page"),()->{
            if(this.palettePage>0)this.palettePage--;this.updatePaletteButtons();
        });
        this.paletteNext=button(312,264,64,20,text("next_page"),()->{
            if((this.palettePage+1)*PALETTE_PAGE_SIZE<this.paletteEntries.size())this.palettePage++;
            this.updatePaletteButtons();
        });
        this.updatePaletteButtons();
        if(this.paletteKind==FrameMaterialPalette.Kind.TRIM) button(144,264,100,20,text("none"),()->this.choosePalette(new ResourceLocation("minecraft","air")));
    }

    private void refreshPaletteEntries() {
        this.palettePage=0;
        this.paletteEntries=FrameMaterialPalette.entries(this.paletteKind,this.paletteCategory,this.paletteSearch);
        this.updatePaletteButtons();
    }

    private void updatePaletteButtons() {
        if(this.palettePrevious!=null)this.palettePrevious.active=this.palettePage>0;
        if(this.paletteNext!=null)this.paletteNext.active=(this.palettePage+1)*PALETTE_PAGE_SIZE<this.paletteEntries.size();
    }

    private void choosePalette(ResourceLocation id) {
        FrameMaterialPalette.Kind kind=this.paletteKind;this.paletteKind=null;
        if(kind==FrameMaterialPalette.Kind.FRAME) this.apply(this.design.withFrameMaterial(id));
        else if(kind==FrameMaterialPalette.Kind.TRIM) this.apply(this.design.withTrimMaterial(id));
        else if(kind==FrameMaterialPalette.Kind.LINING) this.apply(this.design.withLiningMaterial(id));
        else {
            this.decorationItem=id;
            FrameDesign.Decoration selected=this.selectedDecoration();
            if(selected!=null) this.editDecoration(new FrameDesign.Decoration(id,selected.x(),selected.y(),selected.size(),selected.rotation(),selected.locked()));
            else this.buildWidgets();
        }
    }

    private void outlineWidgets() {
        FrameOutlineEditor.Tool[] tools=FrameOutlineEditor.Tool.values();
        for(int index=0;index<tools.length;index++) {
            FrameOutlineEditor.Tool tool=tools[index];
            button(276+(index%2)*51,34+(index/2)*22,49,20,text("tool."+tool.name().toLowerCase(Locale.ROOT)),()->{this.outlineEditor.tool=tool;this.buildWidgets();}).active=this.outlineEditor.tool!=tool;
        }
        this.addRenderableWidget(new FrameNumberControl(this.font,276,104,100,text("brush_size"),1,8,1,()->this.outlineEditor.brushSize,()->1,value->this.outlineEditor.brushSize=value));
        button(276,143,49,20,text("clear"),()->this.outlineEditor.replace(false));
        button(327,143,49,20,text("fill"),()->this.outlineEditor.replace(true));
        button(276,166,100,20,text("grid"),()->this.outlineEditor.grid=!this.outlineEditor.grid);
        this.outlineConfirm=button(276,266,49,20,text("confirm"),()->{
            if(!this.outlineEditor.valid())return;
            this.setFocused(null);
            int[] mask=this.outlineEditor.mask();this.outlineEditor=null;
            this.change(tag->{tag.putString("Template","custom");tag.putIntArray("Mask",mask);});
        });
        button(327,266,49,20,text("cancel"),this::closeModal);
    }

    private void closeModal() {this.setFocused(null);this.paletteKind=null;this.outlineEditor=null;this.searchBox=null;this.outlineConfirm=null;this.buildWidgets();}

    private void decorationWidgets() {
        if(this.invalidOutline) return;
        FrameDesign.Decoration selected=this.selectedDecoration();
        ResourceLocation id=selected==null ? this.decorationItem : selected.itemId();
        button(244,48,132,22,FrameMaterialPalette.stack(id).getHoverName(),()->this.openPalette(FrameMaterialPalette.Kind.STICKER));
        button(244,74,64,20,text("add_decoration"),this::addDecoration).active=this.design.decorations().size()<16;
        button(312,74,64,20,text("delete"),this::deleteDecoration).active=selected!=null;
        FrameNumberControl size=new FrameNumberControl(this.font,244,101,132,text("number.decoration_size"),FrameDesign.MIN_DECORATION_SIZE,FrameDesign.MAX_DECORATION_SIZE,1,
                ()->{FrameDesign.Decoration piece=this.selectedDecoration();return piece==null ? 20 : piece.size();},()->2,value->{
                    FrameDesign.Decoration piece=this.selectedDecoration();if(piece!=null)this.editDecorationLive(new FrameDesign.Decoration(piece.itemId(),piece.x(),piece.y(),value,piece.rotation(),piece.locked()));
                });
        size.active=selected!=null;this.addRenderableWidget(size);
        button(244,137,132,20,text("rotate_decoration").append(selected==null ? "" : " "+selected.rotation()*90+"°"),()->{
            FrameDesign.Decoration current=this.selectedDecoration();
            if(current!=null) this.editDecoration(new FrameDesign.Decoration(current.itemId(),current.x(),current.y(),current.size(),(current.rotation()+1)&3,current.locked()));
        }).active=selected!=null;
        button(244,160,132,18,text(selected!=null && selected.locked() ? "unlock" : "lock"),()->{
            FrameDesign.Decoration current=this.selectedDecoration();
            if(current!=null) this.editDecoration(new FrameDesign.Decoration(current.itemId(),current.x(),current.y(),current.size(),current.rotation(),!current.locked()));
        }).active=selected!=null;
        button(244,181,64,16,text("select_previous"),()->this.selectDecoration(-1)).active=!this.design.decorations().isEmpty();
        button(312,181,64,16,text("select_next"),()->this.selectDecoration(1)).active=!this.design.decorations().isEmpty();
    }

    private void photoWidgets() {
        button(244,46,132,20,label("fit",text("fit."+this.design.fit().id())),()->this.change(tag->tag.putString("Fit",next(FrameDesign.PhotoFit.values(),this.design.fit()).id())));
        number(71,"zoom",FrameDesign.MIN_ZOOM,FrameDesign.MAX_ZOOM,1,()->this.design.zoomPercent(),()->10,value->this.intValueLive("Zoom",value));
        number(105,"offset_x",-100,100,1,()->this.design.offsetX(),()->5,value->this.intValueLive("OffsetX",value));
        number(139,"offset_y",-100,100,1,()->this.design.offsetY(),()->5,value->this.intValueLive("OffsetY",value));
        button(244,178,64,18,text("rotate").append(" "+this.design.rotation()*90+"°"),()->this.intValue("Rotation",(this.design.rotation()+1)&3));
        button(312,178,64,18,text("reset_photo"),()->this.change(tag->{tag.putString("Fit","contain");tag.putInt("Zoom",100);tag.putInt("OffsetX",0);tag.putInt("OffsetY",0);tag.putInt("Rotation",0);}));
    }

    private void designWidgets() {
        this.nameBox=new EditBox(this.font,244,60,132,20,text("name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(this.name);
        this.addRenderableWidget(this.nameBox);
        List<FrameDesignLibrary.Entry> entries=this.library.entries();
        this.libraryIndex=Mth.clamp(this.libraryIndex,0,Math.max(0,entries.size()-1));
        Component selected=entries.isEmpty() ? text("empty_library") : Component.literal(entries.get(this.libraryIndex).name());
        button(244,98,132,20,selected,()->{
            if(!this.library.entries().isEmpty()) this.libraryIndex=(this.libraryIndex+1)%this.library.entries().size();
            this.buildWidgets();
        }).active=!entries.isEmpty();
        button(244,125,64,20,text("save"),this::saveDesign).active=!this.invalidOutline;
        button(312,125,64,20,text("load"),this::loadDesign).active=!entries.isEmpty();
        button(244,151,132,20,text("delete"),this::deleteDesign).active=!entries.isEmpty();
    }

    private static <E extends Enum<E>> E next(E[] values,E current) {return values[(current.ordinal()+1)%values.length];}
    private static <E extends Enum<E>> E cycle(E[] values,E current,int direction) {
        int index=java.util.Arrays.asList(values).indexOf(current);
        return values[Math.floorMod(index+direction,values.length)];
    }
    private static String blocks(int pixels) {return String.format(Locale.ROOT,"%.2f",pixels/16.0F);}
    private static MutableComponent text(String key,Object... args) {return Component.translatable(KEY+key,args);}
    private static Component label(String key,Object value) {return text(key,value);}

    private Button button(int x,int y,int width,int height,Component message,Runnable action) {
        Button button=new Button(x,y,width,height,message,ignored->action.run(),narration->narration.get()) {
            @Override public void renderString(GuiGraphics graphics,Font font,int color) {
                // Vanilla's marquee scissors use screen coordinates. This screen
                // scales a virtual canvas, so use a bounded label without scissoring.
                String label=this.getMessage().getString();
                int available=Math.max(0,this.getWidth()-4);
                if(font.width(label)>available) {
                    String suffix="…";
                    label=font.plainSubstrByWidth(label,Math.max(0,available-font.width(suffix)))+suffix;
                }
                graphics.drawCenteredString(font,label,this.getX()+this.getWidth()/2,
                        this.getY()+(this.getHeight()-8)/2,color);
            }
        };
        return this.addRenderableWidget(button);
    }

    private void stepper(int y,String key,String value,Runnable minus,Runnable plus,boolean canMinus,boolean canPlus) {
        button(336,y,18,20,Component.literal("−"),minus).active=canMinus;
        button(358,y,18,20,Component.literal("+"),plus).active=canPlus;
    }

    private void intValue(String key,int value) {this.change(tag->tag.putInt(key,value));}
    private void change(Consumer<CompoundTag> edit) {
        CompoundTag tag=this.design.toTag();
        if(this.workingMask!=null) {tag.putIntArray("Mask",this.workingMask);tag.putString("Template","custom");}
        edit.accept(tag);
        FrameDesign updated=FrameDesign.fromTag(tag);
        if(updated==null) {
            this.workingMask=tag.getIntArray("Mask");this.invalidOutline=true;this.editShape=true;this.page=0;
            this.showStatus("invalid_shape");this.buildWidgets();return;
        }
        this.workingMask=null;this.invalidOutline=false;
        if(updated.equals(this.design)) {this.buildWidgets();return;}
        this.apply(updated);
    }

    private void apply(FrameDesign updated) {
        this.workingMask=null;this.invalidOutline=false;
        if(updated.equals(this.design)) {this.buildWidgets();return;}
        this.design=updated;
        this.selectedDecoration=Mth.clamp(this.selectedDecoration,-1,this.design.decorations().size()-1);
        this.touched=true;
        this.refreshPreview();
        this.buildWidgets();
    }

    private FrameDesign.Decoration selectedDecoration() {
        return this.selectedDecoration>=0 && this.selectedDecoration<this.design.decorations().size()
                ? this.design.decorations().get(this.selectedDecoration) : null;
    }

    private void addDecoration() {
        if(this.invalidOutline || this.design.decorations().size()>=16) return;
        List<FrameDesign.Decoration> pieces=new ArrayList<>(this.design.decorations());
        int index=pieces.size();
        pieces.add(new FrameDesign.Decoration(this.decorationItem,150+(index%5)*150,150+(index/5)*180,20,0,false));
        this.apply(this.design.withDecorations(pieces));
        this.selectedDecoration=index;
        this.buildWidgets();
    }

    private void editDecoration(FrameDesign.Decoration piece) {
        if(this.invalidOutline || this.selectedDecoration()==null) return;
        List<FrameDesign.Decoration> pieces=new ArrayList<>(this.design.decorations());
        pieces.set(this.selectedDecoration,piece);
        this.apply(this.design.withDecorations(pieces));
    }

    private void editDecorationLive(FrameDesign.Decoration piece) {
        if(this.selectedDecoration()==null)return;
        List<FrameDesign.Decoration> pieces=new ArrayList<>(this.design.decorations());pieces.set(this.selectedDecoration,piece);
        FrameDesign updated=this.design.withDecorations(pieces);
        if(!updated.equals(this.design)) {
            this.design=updated;this.touched=true;this.refreshPreview();
        }
    }

    private void resizeDecoration(int amount) {
        FrameDesign.Decoration piece=this.selectedDecoration();
        if(piece!=null) this.editDecoration(new FrameDesign.Decoration(piece.itemId(),piece.x(),piece.y(),
                Mth.clamp(piece.size()+amount,8,40),piece.rotation(),piece.locked()));
    }

    private void deleteDecoration() {
        if(this.invalidOutline || this.selectedDecoration()==null) return;
        List<FrameDesign.Decoration> pieces=new ArrayList<>(this.design.decorations());
        pieces.remove(this.selectedDecoration);
        this.apply(this.design.withDecorations(pieces));
    }

    private void selectDecoration(int direction) {
        if(this.design.decorations().isEmpty()) return;
        this.selectedDecoration=this.selectedDecoration<0 ? (direction<0 ? this.design.decorations().size()-1 : 0)
                : Math.floorMod(this.selectedDecoration+direction,this.design.decorations().size());
        this.decorationItem=this.selectedDecoration().itemId();
        this.buildWidgets();
    }

    private void loadSourceStyle() {
        ItemStack source=this.menu.getPhotograph();
        if(source.isEmpty()) return;
        FrameDesign saved=FrameDesignRenderer.designFor(source);
        FrameDesign loaded=saved!=null ? saved : source.is(GuaniaoItems.FILM.get()) ? FrameDesign.DEFAULT
                : FrameDesign.fromStyle(PhotographFrameStyle.read(source));
        this.apply(loaded);
    }

    private void refreshPreview() {
        if(this.isCraftingUi() && !this.craftingPreview.isEmpty()) {this.previewStack=this.craftingPreview.copy();return;}
        ItemStack source=this.menu.getPhotograph();
        if(source.isEmpty())source=this.menu.getSlot(FrameWorkbenchMenu.OUTPUT_SLOT).getItem();
        if(source.isEmpty() && this.workSnapshotReady)source=this.craftingPreview;
        this.previewStack=new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
        if(source.hasTag()) this.previewStack.setTag(source.getTag().copy());
        FrameDesign.write(this.previewStack,this.design);
    }

    private void saveDesign() {
        try {
            this.name=this.nameBox==null ? this.name : this.nameBox.getValue();
            this.library.save(this.name,this.design);
            for(int i=0;i<this.library.entries().size();i++) if(this.library.entries().get(i).name().equals(this.name.strip())) this.libraryIndex=i;
            this.showStatus("saved");this.buildWidgets();
        } catch(IOException exception) {this.showStatus("save_failed");}
    }

    private void loadDesign() {
        List<FrameDesignLibrary.Entry> entries=this.library.entries();
        if(entries.isEmpty()) return;
        FrameDesignLibrary.Entry entry=entries.get(Mth.clamp(this.libraryIndex,0,entries.size()-1));
        this.name=entry.name();
        if(this.nameBox!=null) this.nameBox.setValue(this.name);
        FrameDesign loaded=entry.design();
        this.apply(loaded);
        this.showStatus("loaded");
    }

    private void deleteDesign() {
        try {this.library.delete(this.libraryIndex);this.showStatus("deleted");this.buildWidgets();}
        catch(IOException exception) {this.showStatus("save_failed");}
    }

    private void showStatus(String key) {this.status=text(key);this.statusTicks=100;}

    private boolean isCraftingUi() {return this.awaitingCraftResponse || this.menu.isCrafting();}

    private boolean isWorkshopVisible() {return !this.awaitingCraftResponse && this.menu.isCrafting() && this.workSnapshotReady;}

    private void updateCraftingWidgets() {
        boolean busy=this.isCraftingUi();
        if(busy) {
            this.widgetsLocked=true;
            for(var child:this.children())if(child instanceof AbstractWidget widget)widget.active=false;
        }
        if(this.craftButton!=null) {
            this.craftButton.setMessage(text(this.isWorkshopVisible() ? "crafting" : busy ? "waiting" : "craft"));
            this.craftButton.active=!busy && !this.invalidOutline && this.menu.canCraft(this.design);
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        if(this.statusTicks>0)this.statusTicks--;
        if(this.readyFlashTicks>0)this.readyFlashTicks--;
        if(this.awaitingCraftResponse && this.pendingResponse!=this.menu.getCraftResponse())this.awaitingCraftResponse=false;
        this.updateWorkshop();
        boolean busy=this.isCraftingUi();
        if(this.lastMinimum!=this.menu.getMinSizePixels() || this.lastMaximum!=this.menu.getMaxSizePixels()) {
            this.lastMinimum=this.menu.getMinSizePixels();this.lastMaximum=this.menu.getMaxSizePixels();
            if(this.paletteKind==null && this.outlineEditor==null)this.buildWidgets();
        }
        ItemStack source=this.menu.getPhotograph();
        if(!ItemStack.matches(source,this.lastSource)) {
            this.lastSource=source.copy();
            if(!busy && !this.touched && !source.isEmpty())this.loadSourceStyle();
            this.refreshPreview();
        }
        ItemStack result=this.menu.getSlot(FrameWorkbenchMenu.OUTPUT_SLOT).getItem();
        if(!ItemStack.matches(result,this.lastResult)) {
            if(!result.isEmpty())this.readyFlashTicks=20;
            this.lastResult=result.copy();
            this.refreshPreview();
        }
        if(!busy && this.widgetsLocked) {
            this.widgetsLocked=false;
            if(!this.workSnapshotReady)this.craftingPreview=ItemStack.EMPTY;
            this.playWorkStartSound=false;
            this.refreshPreview();
            this.buildWidgets();
        }
        this.updateCraftingWidgets();
    }

    private void updateWorkshop() {
        if(this.awaitingCraftResponse)return;
        ItemStack snapshot=this.menu.getWorkPreview();
        if(snapshot.isEmpty()) {
            if(!this.menu.isCrafting() && !this.awaitingCraftResponse) {
                this.workSnapshotReady=false;
                this.workSoundStage=-1;
            }
            return;
        }
        FrameDesign actual=this.menu.getWorkDesign();
        if(actual==null)return;
        long seed=this.menu.getWorkSeed();
        boolean newWork=!this.workSnapshotReady || this.activeWorkSeed!=seed;
        if(newWork || !ItemStack.matches(snapshot,this.craftingPreview)) {
            this.craftingPreview=snapshot.copy();
            this.workDesign=actual;
        }
        if(newWork) {
            this.observerClock.clear();
            this.progressBirdClock.clear();
            this.activeWorkSeed=seed;
            this.workSnapshotReady=true;
            this.design=actual;
            this.touched=true;
            this.workingMask=null;this.invalidOutline=false;this.editShape=false;
            this.painting=false;this.turning=false;this.draggingDecoration=false;
            this.paletteKind=null;this.outlineEditor=null;this.setFocused(null);
            this.selectedDecoration=-1;
            this.refreshPreview();
            this.buildWidgets();
        }
        int elapsed=Mth.clamp(FrameWorkbenchMenu.CRAFT_DURATION_TICKS-this.menu.getCraftTicksRemaining(),0,FrameWorkbenchMenu.CRAFT_DURATION_TICKS);
        if(!this.menu.isCrafting()) {
            this.observerClock.complete(seed,System.nanoTime(),this.minecraft.isPaused());
            this.currentWorkTicks=this.menu.getSlot(FrameWorkbenchMenu.OUTPUT_SLOT).hasItem() ? FrameWorkbenchMenu.CRAFT_DURATION_TICKS : 0;
            this.workSoundStage=-1;
            return;
        }
        if(newWork || this.workSoundStage<0) {
            this.currentWorkTicks=elapsed;
            this.workshopClock.reset(elapsed,System.nanoTime());
            this.workSoundStage=workshopStage(elapsed);
            if(this.playWorkStartSound && elapsed<40)this.playWorkshopSound(0);
            this.playWorkStartSound=false;
        } else {
            this.currentWorkTicks=Math.max(this.currentWorkTicks,elapsed);
            this.workshopClock.observe(this.currentWorkTicks,System.nanoTime(),this.minecraft.isPaused());
            int stage=workshopStage((int)this.workshopClock.position());
            if(stage>this.workSoundStage) {
                if(this.workSoundStage>=0)this.playWorkshopSound(stage);
                this.workSoundStage=stage;
            }
        }
    }

    private static int workshopStage(int ticks) {
        return ticks<40 ? 0 : ticks<120 ? 1 : ticks<180 ? 2 : ticks<240 ? 3 : ticks<280 ? 4 : 5;
    }

    private void playWorkshopSound(int stage) {
        SoundEvent sound=switch(stage) {
            case 0,2 -> SoundEvents.BOOK_PAGE_TURN;
            case 1,3 -> SoundEvents.WOOD_PLACE;
            case 4 -> SoundEvents.UI_BUTTON_CLICK.value();
            default -> SoundEvents.ITEM_FRAME_ADD_ITEM;
        };
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound,stage==4 ? 1.25F : 1.05F,0.22F));
    }

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(this.isWorkshopVisible())this.frameWorkTicks=this.workshopClock.render(System.nanoTime(),this.minecraft.isPaused());
        this.renderBackground(graphics);
        int x=(int)this.localX(mouseX),y=(int)this.localY(mouseY);
        graphics.pose().pushPose();
        graphics.pose().translate(this.originX,this.originY,0);
        graphics.pose().scale(this.uiScale,this.uiScale,1);
        if(this.paletteKind!=null || this.outlineEditor!=null) {
            this.renderModal(graphics,x,y,partialTick);
            graphics.pose().popPose();
            this.renderModalTooltip(graphics,mouseX,mouseY,x,y);
            this.renderObserver(graphics);
            return;
        }
        super.render(graphics,x,y,partialTick);
        if(this.isWorkshopVisible()) {
            FrameCraftingAnimation.renderDelivery(graphics,this.craftingPreview,this.workDesign,this.workshopProgressTicks(partialTick),this.activeWorkSeed);
        }
        graphics.pose().popPose();
        // Tooltips use actual GUI coordinates and normal font size. Passing the
        // virtual 390px canvas to vanilla's screen-edge clamp broke small screens.
        this.renderTooltip(graphics,mouseX,mouseY);
        if(this.hoveredSlot==null && this.menu.getCarried().isEmpty()) {
            for(var child:this.children()) {
                if(child instanceof AbstractWidget widget && widget.isMouseOver(x,y)
                        && this.font.width(widget.getMessage())>widget.getWidth()-8) {
                    graphics.renderTooltip(this.font,widget.getMessage(),mouseX,mouseY);
                    break;
                }
            }
        }
        this.renderObserver(graphics);
    }

    /** Only entrance and departure cross the preview edge; their paths use real screen bounds. */
    private void renderObserver(GuiGraphics graphics) {
        float progress,exitTicks;
        if(this.isWorkshopVisible()) {
            this.observerClock.observe(this.activeWorkSeed);
            this.observerDesign=this.workDesign;
            progress=this.frameWorkTicks;
            exitTicks=0;
        } else {
            // Completion can arrive between client ticks. Keep the observer visible
            // on that very frame, and do not depend on when the player takes the result.
            if(!this.awaitingCraftResponse && !this.menu.isCrafting()) {
                this.observerClock.complete(this.menu.getWorkSeed(),System.nanoTime(),this.minecraft.isPaused());
            }
            if(!this.observerClock.isDeparting())return;
            exitTicks=this.observerClock.renderExit(System.nanoTime(),this.minecraft.isPaused());
            if(exitTicks>=FrameObserverClock.EXIT_TICKS) {this.observerClock.clear();return;}
            progress=FrameWorkbenchMenu.CRAFT_DURATION_TICKS;
        }
        graphics.flush();
        graphics.pose().pushPose();
        boolean insidePreview=progress>=28 && progress<FrameWorkbenchMenu.CRAFT_DURATION_TICKS;
        try {
            graphics.pose().translate(this.originX,this.originY,0);
            graphics.pose().scale(this.uiScale,this.uiScale,1);
            if(insidePreview)graphics.enableScissor(Math.round(this.originX+79*this.uiScale),Math.round(this.originY+41*this.uiScale),
                    Math.round(this.originX+231*this.uiScale),Math.round(this.originY+191*this.uiScale));
            FrameCraftingAnimation.renderObserver(graphics,this.observerDesign,progress,this.observerClock.seed(),exitTicks,
                    -this.originX/this.uiScale,-this.originY/this.uiScale,
                    (this.width-this.originX)/this.uiScale,(this.height-this.originY)/this.uiScale);
            graphics.flush();
        } finally {
            if(insidePreview)graphics.disableScissor();
            graphics.pose().popPose();
        }
    }

    @Override public void removed() {
        this.observerClock.clear();
        this.progressBirdClock.clear();
        super.removed();
    }

    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        panel(g,0,0,PANEL_WIDTH,PANEL_HEIGHT);
        recess(g,7,36,65,163,0xFFB8B8B8);
        FrameWorkbenchBackdrop.render(g,8,37,63,161);
        recess(g,78,40,154,152,0xFF565656);
        FrameWorkbenchBackdrop.render(g);
        recess(g,240,40,140,155,0xFFB8B8B8);
        recess(g,240,202,140,73,0xFFB8B8B8);
        FrameWorkbenchBackdrop.render(g,241,203,138,71);
        slot(g,18,54);
        this.renderCraftingArea(g,partialTick);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) slot(g,18+col*18,212+row*18);
        for(int col=0;col<9;col++) slot(g,18+col*18,270);
        if(this.isWorkshopVisible())this.renderWorkshop(g,partialTick);
        else if(this.editShape || this.invalidOutline) this.renderCanvas(g,mouseX,mouseY);
        else this.renderPreview(g,partialTick);
    }

    private float workshopProgressTicks(float partialTick) {
        return this.frameWorkTicks;
    }

    private void renderWorkshop(GuiGraphics graphics,float partialTick) {
        graphics.flush();
        graphics.enableScissor(Math.round(this.originX+79*this.uiScale),Math.round(this.originY+41*this.uiScale),
                Math.round(this.originX+231*this.uiScale),Math.round(this.originY+191*this.uiScale));
        FrameCraftingAnimation.renderPreview(graphics,this.craftingPreview,this.workDesign,this.workshopProgressTicks(partialTick),this.activeWorkSeed);
        graphics.flush();
        graphics.disableScissor();
        Lighting.setupFor3DItems();
    }

    private void renderCraftingArea(GuiGraphics graphics,float partialTick) {
        panel(graphics,331,230,30,30);
        slot(graphics,FrameWorkbenchMenu.OUTPUT_X,FrameWorkbenchMenu.OUTPUT_Y);
        if(this.isWorkshopVisible()) {
            this.progressBirdClock.observe(this.activeWorkSeed);
            int stage=workshopStage((int)this.frameWorkTicks);
            String label=stage==2 && !PhotographData.hasImage(this.craftingPreview) ? "work_stage.lining_only" : "work_stage."+stage;
            graphics.drawString(this.font,text(label),247,211,WOOD_TEXT,false);
            Component countdown=text("seconds_remaining",(Math.max(0,this.menu.getCraftTicksRemaining())+19)/20);
            graphics.drawString(this.font,countdown,285-this.font.width(countdown)/2,224,WOOD_TEXT,false);
            FrameCraftingProgress.render(graphics,this.workshopProgressTicks(partialTick),-1,this.activeWorkSeed);
        } else if(this.isCraftingUi()) {
            graphics.drawString(this.font,text("waiting"),247,237,WOOD_TEXT,false);
        } else {
            // This is only a visual finish: taking the result does not cut off the meal.
            this.progressBirdClock.complete(this.menu.getWorkSeed(),System.nanoTime(),this.minecraft.isPaused());
            if(this.progressBirdClock.isDeparting()) {
                float eating=this.progressBirdClock.renderExit(System.nanoTime(),this.minecraft.isPaused());
                if(eating<FrameObserverClock.EXIT_TICKS) {
                    FrameCraftingProgress.render(graphics,FrameWorkbenchMenu.CRAFT_DURATION_TICKS,eating,this.progressBirdClock.seed());
                } else this.progressBirdClock.clear();
            }
        }
        if(this.readyFlashTicks>0) {
            int alpha=Math.round(255*Math.min(1,this.readyFlashTicks/10F));
            graphics.renderOutline(331,230,30,30,(alpha<<24)|0xFFF0B8);
            graphics.renderOutline(332,231,28,28,(alpha<<24)|0xFFFFFF);
        }
    }

    private void renderModal(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        panel(graphics,0,0,PANEL_WIDTH,PANEL_HEIGHT);
        if(this.paletteKind!=null) {
            graphics.drawString(this.font,text("palette."+this.paletteKind.name().toLowerCase(Locale.ROOT)),14,10,0x404040,false);
            recess(graphics,PALETTE_X-4,PALETTE_Y-4,PALETTE_COLUMNS*PALETTE_STEP+8,PALETTE_ROWS*PALETTE_STEP+8,0xFF8B8B8B);
            int start=this.palettePage*PALETTE_PAGE_SIZE;
            for(int cell=0;cell<PALETTE_PAGE_SIZE;cell++) {
                int x=PALETTE_X+(cell%PALETTE_COLUMNS)*PALETTE_STEP,y=PALETTE_Y+(cell/PALETTE_COLUMNS)*PALETTE_STEP;
                slot(graphics,x,y);
                if(start+cell>=this.paletteEntries.size())continue;
                graphics.renderItem(this.paletteEntries.get(start+cell),x,y);
                if(mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                    graphics.pose().pushPose();graphics.pose().translate(0,0,200);
                    graphics.fill(x,y,x+16,y+16,0x80FFFFFF);graphics.pose().popPose();
                }
            }
            int pages=Math.max(1,(this.paletteEntries.size()+PALETTE_PAGE_SIZE-1)/PALETTE_PAGE_SIZE);
            Component pageLabel=text("palette_page",this.palettePage+1,pages,this.paletteEntries.size());
            graphics.drawString(this.font,pageLabel,195-this.font.width(pageLabel)/2,248,0x404040,false);
        } else if(this.outlineEditor!=null) {
            graphics.drawString(this.font,text("edit_shape"),14,10,0x404040,false);
            graphics.drawString(this.font,Component.literal(this.outlineEditor.zoomPercent()+"%"),228,10,0x404040,false);
            this.outlineEditor.render(graphics,mouseX,mouseY);
            graphics.drawWordWrap(this.font,text("outline_help"),276,216,100,0x404040);
            if(!this.outlineEditor.valid())graphics.drawString(this.font,text("invalid_shape"),276,252,0x9B3030,false);
            if(this.outlineConfirm!=null)this.outlineConfirm.active=this.outlineEditor.valid();
        }
        for(var renderable:this.renderables)renderable.render(graphics,mouseX,mouseY,partialTick);
    }

    private void renderModalTooltip(GuiGraphics graphics,int mouseX,int mouseY,int localX,int localY) {
        if(this.paletteKind==null)return;
        int cell=this.paletteCell(localX,localY),index=this.palettePage*PALETTE_PAGE_SIZE+cell;
        if(cell>=0 && index<this.paletteEntries.size())graphics.renderTooltip(this.font,this.paletteEntries.get(index),mouseX,mouseY);
    }

    private int paletteCell(double x,double y) {
        if(x<PALETTE_X || y<PALETTE_Y || x>=PALETTE_X+PALETTE_COLUMNS*PALETTE_STEP
                || y>=PALETTE_Y+PALETTE_ROWS*PALETTE_STEP)return -1;
        return (int)((y-PALETTE_Y)/PALETTE_STEP)*PALETTE_COLUMNS+(int)((x-PALETTE_X)/PALETTE_STEP);
    }

    private boolean modalClick(double x,double y,int button) {
        if(this.paletteKind!=null && button==0) {
            int cell=this.paletteCell(x,y),index=this.palettePage*PALETTE_PAGE_SIZE+cell;
            if(cell>=0) {
                if(index<this.paletteEntries.size())this.choosePalette(BuiltInRegistries.ITEM.getKey(this.paletteEntries.get(index).getItem()));
                return true;
            }
        }
        if(this.outlineEditor!=null && this.outlineEditor.click(x,y,button))return true;
        // Do not call AbstractContainerScreen's slot handler while a modal is open.
        // Its slots remain backed by the same server menu, but cannot be clicked through.
        for(GuiEventListener child:this.children()) {
            if(child.mouseClicked(x,y,button)) {
                this.setFocused(child);if(button==0)this.setDragging(true);return true;
            }
        }
        this.setFocused(null);return true;
    }

    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(this.font,this.title,12,7,0x404040,false);
        g.drawString(this.font,this.playerInventoryTitle,18,202,0x404040,false);
        g.drawString(this.font,text("photo"),12,42,WOOD_TEXT,false);

        if(this.page==1) {
            g.drawString(this.font,label("wood",""),244,48,0x404040,false);
            g.drawString(this.font,label("trim",""),244,96,0x404040,false);
            g.drawString(this.font,label("mat",""),244,144,0x404040,false);
        } else if(this.page==3 && !this.isCraftingUi()) {
            g.drawString(this.font,text("decoration_count",this.design.decorations().size()),245,207,WOOD_TEXT,false);
        } else if(this.page==4) {
            g.drawString(this.font,text("name"),245,46,0x404040,false);
            g.drawString(this.font,text("library_count",this.library.entries().size()),245,181,0x404040,false);
        }
        if(this.invalidOutline || this.statusTicks>0) {
            Component message=this.invalidOutline ? text("invalid_shape") : this.status;
            g.drawString(this.font,this.font.plainSubstrByWidth(message.getString(),132),244,264,this.invalidOutline ? WOOD_ERROR : WOOD_TEXT,false);
        } else if(!this.isCraftingUi() && !this.menu.canCraft(this.design)) {
            String reason=this.design.widthPixels()==0 || this.design.heightPixels()==0 ? "zero_size" : this.design.widthPixels()<this.menu.getMinSizePixels() || this.design.heightPixels()<this.menu.getMinSizePixels() || this.design.widthPixels()>this.menu.getMaxSizePixels() || this.design.heightPixels()>this.menu.getMaxSizePixels() ? "size_limit" : this.menu.getSlot(FrameWorkbenchMenu.OUTPUT_SLOT).hasItem() ? null : "migrating";
            if(reason!=null)g.drawString(this.font,this.font.plainSubstrByWidth(text(reason).getString(),132),244,264,WOOD_ERROR,false);
        }
    }

    private void rowLabel(GuiGraphics g,int y,String key,String value) {
        g.drawString(this.font,this.font.plainSubstrByWidth(label(key,value).getString(),88),244,y+6,0x404040,false);
    }

    private void renderPreview(GuiGraphics g,float partialTick) {
        g.flush();
        // 1.20.1 scissor coordinates are screen-space, independent of the pose matrix.
        g.enableScissor(Math.round(this.originX+79*this.uiScale),Math.round(this.originY+41*this.uiScale),
                Math.round(this.originX+231*this.uiScale),Math.round(this.originY+191*this.uiScale));
        g.pose().pushPose();
        g.pose().translate(155,116,120);
        float scale=this.previewScale();
        FrameDesignRenderer.scaleForGui(g.pose(),scale);
        if(this.page!=3) {
            g.pose().mulPose(Axis.XP.rotationDegrees(this.viewPitch));
            g.pose().mulPose(Axis.YP.rotationDegrees(this.viewYaw));
        }
        Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        FrameDesignRenderer.render(this.previewStack,this.design,0,g.pose(),this.minecraft.renderBuffers().bufferSource(),LightTexture.FULL_BRIGHT);
        g.flush();
        g.pose().popPose();
        if(this.page==3 && !this.isCraftingUi())this.renderDecorationSelection(g);
        g.disableScissor();
        Lighting.setupFor3DItems();
    }

    private float previewScale() {
        float span=this.page==3 ? 100.0F : 120.0F*this.viewZoom;
        return span/Math.max(0.0625F,Math.max(this.design.widthPixels()/16.0F,this.design.heightPixels()/16.0F));
    }

    private void renderDecorationSelection(GuiGraphics g) {
        if(this.design.widthPixels()==0 || this.design.heightPixels()==0)return;
        FrameDesign.Decoration piece=this.selectedDecoration();
        if(piece==null) return;
        float scale=this.previewScale();
        var bounds=FrameDesignRenderer.decorationBounds(this.design,piece);
        int x=Math.round(155+bounds.x0()*scale)-1,y=Math.round(116-bounds.y1()*scale)-1;
        int width=Math.round((bounds.x1()-bounds.x0())*scale)+2,height=Math.round((bounds.y1()-bounds.y0())*scale)+2;
        g.pose().pushPose();
        g.pose().translate(0,0,180);
        g.renderOutline(x,y,width,height,piece.locked() ? 0xFFFFCE61 : 0xFFFFFFFF);
        g.pose().popPose();
    }

    private void renderCanvas(GuiGraphics g,int mouseX,int mouseY) {
        int[] mask=this.painting ? this.strokeMask : this.workingMask==null ? this.design.mask() : this.workingMask;
        int[] inner=FrameDesign.erode(mask,this.design.borderCells());
        for(int y=0;y<32;y++) for(int x=0;x<32;x++) {
            boolean filled=(mask[y]&(1<<x))!=0,window=(inner[y]&(1<<x))!=0;
            int color=filled ? (window ? 0xFFC9C0A6 : 0xFF86613D) : ((x+y)%2==0 ? 0xFF686868 : 0xFF777777);
            g.fill(CANVAS_X+x*CELL,CANVAS_Y+y*CELL,CANVAS_X+(x+1)*CELL,CANVAS_Y+(y+1)*CELL,color);
        }
        for(int i=0;i<=32;i+=4) {
            g.fill(CANVAS_X+i*CELL,CANVAS_Y,CANVAS_X+i*CELL+1,CANVAS_Y+128,0x44666666);
            g.fill(CANVAS_X,CANVAS_Y+i*CELL,CANVAS_X+128,CANVAS_Y+i*CELL+1,0x44666666);
        }
        if(inCanvas(mouseX,mouseY)) {
            int x=(mouseX-CANVAS_X)/CELL,y=(mouseY-CANVAS_Y)/CELL;
            g.renderOutline(CANVAS_X+x*CELL,CANVAS_Y+y*CELL,CELL,CELL,this.eraser ? 0xFFFF7777 : 0xFFFFFFFF);
        }
    }

    private static void panel(GuiGraphics g,int x,int y,int width,int height) {
        g.fill(x,y,x+width,y+height,0xFF161616);
        g.fill(x+1,y+1,x+width-1,y+height-1,0xFFC6C6C6);
        g.fill(x+2,y+2,x+width-2,y+4,0xFFFFFFFF);
        g.fill(x+2,y+2,x+4,y+height-2,0xFFFFFFFF);
        g.fill(x+4,y+height-4,x+width-2,y+height-2,0xFF555555);
        g.fill(x+width-4,y+4,x+width-2,y+height-2,0xFF555555);
    }

    private static void recess(GuiGraphics g,int x,int y,int width,int height,int fill) {
        g.fill(x,y,x+width,y+height,0xFF373737);
        g.fill(x+1,y+1,x+width,y+height,0xFFFFFFFF);
        g.fill(x+1,y+1,x+width-1,y+height-1,fill);
    }
    private static void slot(GuiGraphics g,int x,int y) {recess(g,x-1,y-1,18,18,0xFF8B8B8B);}
    private double localX(double x) {return (x-this.originX)/this.uiScale;}
    private double localY(double y) {return (y-this.originY)/this.uiScale;}
    private static boolean inCanvas(double x,double y) {return x>=CANVAS_X && x<CANVAS_X+128 && y>=CANVAS_Y && y<CANVAS_Y+128;}
    private static boolean inPreview(double x,double y) {return x>=79 && x<231 && y>=41 && y<191;}

    private int decorationAt(double x,double y) {
        if(this.design.widthPixels()==0 || this.design.heightPixels()==0)return -1;
        float scale=this.previewScale();
        float localX=(float)(x-155)/scale,localY=(float)(116-y)/scale;
        for(int index=this.design.decorations().size()-1;index>=0;index--) {
            var bounds=FrameDesignRenderer.decorationBounds(this.design,this.design.decorations().get(index));
            if(localX>=bounds.x0() && localX<=bounds.x1() && localY>=bounds.y0() && localY<=bounds.y1()) return index;
        }
        return -1;
    }

    private double decorationX(double x) {return 500+(x-155)/(this.previewScale()*Math.max(1,this.design.widthPixels())/16.0)*1000;}
    private double decorationY(double y) {return 500+(y-116)/(this.previewScale()*Math.max(1,this.design.heightPixels())/16.0)*1000;}

    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        double x=this.localX(mouseX),y=this.localY(mouseY);
        if(this.isCraftingUi() && y<212)return true;
        // Commit a numeric edit before another control's click changes the design.
        if(this.getFocused() instanceof FrameNumberControl control && !control.isMouseOver(x,y))this.setFocused(null);
        if(this.paletteKind!=null || this.outlineEditor!=null)return this.modalClick(x,y,button);
        if(this.editShape && inCanvas(x,y) && (button==0 || button==1)) {
            this.painting=true;this.strokeErase=this.eraser || button==1;
            this.strokeMask=this.workingMask==null ? this.design.mask() : this.workingMask.clone();
            this.lastCellX=(int)(x-CANVAS_X)/CELL;this.lastCellY=(int)(y-CANVAS_Y)/CELL;
            this.paintCell(this.lastCellX,this.lastCellY);
            return true;
        }
        if(this.page==3 && inPreview(x,y) && button==0) {
            if(this.invalidOutline) return true;
            this.selectedDecoration=this.decorationAt(x,y);
            FrameDesign.Decoration piece=this.selectedDecoration();
            if(piece!=null) {
                this.decorationItem=piece.itemId();
                if(!piece.locked()) {
                    this.draggingDecoration=true;
                    this.decorationDragOffsetX=piece.x()-this.decorationX(x);
                    this.decorationDragOffsetY=piece.y()-this.decorationY(y);
                }
            }
            this.buildWidgets();
            return true;
        }
        if(!this.editShape && inPreview(x,y) && button==0) {this.turning=true;return true;}
        return super.mouseClicked(x,y,button);
    }

    @Override public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY) {
        if(this.isCraftingUi())return super.mouseDragged(this.localX(mouseX),this.localY(mouseY),button,dragX/this.uiScale,dragY/this.uiScale);
        if(this.paletteKind!=null || this.outlineEditor!=null) {
            if(this.outlineEditor!=null && this.outlineEditor.drag(this.localX(mouseX),this.localY(mouseY),dragX/this.uiScale,dragY/this.uiScale))return true;
            return this.getFocused()!=null && this.getFocused().mouseDragged(this.localX(mouseX),this.localY(mouseY),button,dragX/this.uiScale,dragY/this.uiScale);
        }
        // Container screens consume drags for inventory slots instead of routing
        // them to ordinary child widgets. Forward only an active value slider.
        if(this.getFocused() instanceof FrameNumberControl control && control.isSliding()) {
            return control.mouseDragged(this.localX(mouseX),this.localY(mouseY),button,dragX/this.uiScale,dragY/this.uiScale);
        }
        if(this.draggingDecoration) {
            FrameDesign.Decoration piece=this.selectedDecoration();
            if(this.invalidOutline || piece==null || piece.locked()) return true;
            int x=Mth.clamp((int)Math.round(this.decorationX(this.localX(mouseX))+this.decorationDragOffsetX),0,1000);
            int y=Mth.clamp((int)Math.round(this.decorationY(this.localY(mouseY))+this.decorationDragOffsetY),0,1000);
            if(x!=piece.x() || y!=piece.y()) {
                List<FrameDesign.Decoration> pieces=new ArrayList<>(this.design.decorations());
                pieces.set(this.selectedDecoration,new FrameDesign.Decoration(piece.itemId(),x,y,piece.size(),piece.rotation(),false));
                this.design=this.design.withDecorations(pieces);
                this.touched=true;
                this.refreshPreview();
            }
            return true;
        }
        if(this.painting) {
            int x=Mth.clamp((int)(this.localX(mouseX)-CANVAS_X)/CELL,0,31),y=Mth.clamp((int)(this.localY(mouseY)-CANVAS_Y)/CELL,0,31);
            int dx=Math.abs(x-this.lastCellX),dy=Math.abs(y-this.lastCellY),steps=Math.max(dx,dy);
            for(int i=0;i<=steps;i++) {
                float t=steps==0 ? 0 : i/(float)steps;
                this.paintCell(Math.round(Mth.lerp(t,this.lastCellX,x)),Math.round(Mth.lerp(t,this.lastCellY,y)));
            }
            this.lastCellX=x;this.lastCellY=y;
            return true;
        }
        if(this.turning) {
            this.viewYaw=(this.viewYaw+(float)(dragX/this.uiScale))%360;
            this.viewPitch=Mth.clamp(this.viewPitch+(float)(dragY/this.uiScale),-75,75);
            return true;
        }
        return super.mouseDragged(this.localX(mouseX),this.localY(mouseY),button,dragX/this.uiScale,dragY/this.uiScale);
    }

    private void paintCell(int x,int y) {if(this.strokeErase) this.strokeMask[y]&=~(1<<x);else this.strokeMask[y]|=1<<x;}

    @Override public boolean mouseReleased(double mouseX,double mouseY,int button) {
        if(this.paletteKind!=null || this.outlineEditor!=null) {
            if(this.outlineEditor!=null)this.outlineEditor.release();
            if(this.getFocused()!=null)this.getFocused().mouseReleased(this.localX(mouseX),this.localY(mouseY),button);
            this.setDragging(false);return true;
        }
        if(this.getFocused() instanceof FrameNumberControl control && control.isSliding()) {
            control.mouseReleased(this.localX(mouseX),this.localY(mouseY),button);this.setDragging(false);return true;
        }
        if(this.draggingDecoration) {
            this.draggingDecoration=false;
            this.buildWidgets();
            return true;
        }
        if(this.painting) {
            this.painting=false;
            this.change(tag->{tag.putString("Template","custom");tag.putIntArray("Mask",this.strokeMask);});
            this.strokeMask=null;
            return true;
        }
        if(this.turning) {this.turning=false;return true;}
        return super.mouseReleased(this.localX(mouseX),this.localY(mouseY),button);
    }

    @Override public void mouseMoved(double mouseX,double mouseY) {super.mouseMoved(this.localX(mouseX),this.localY(mouseY));}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount) {
        if(this.isCraftingUi())return true;
        double x=this.localX(mouseX),y=this.localY(mouseY);
        if(this.paletteKind!=null) {
            this.palettePage=Mth.clamp(this.palettePage-(int)Math.signum(amount),0,Math.max(0,(this.paletteEntries.size()-1)/PALETTE_PAGE_SIZE));
            this.updatePaletteButtons();return true;
        }
        if(this.outlineEditor!=null)return this.outlineEditor.scroll(x,y,amount);
        if(!this.editShape && inPreview(x,y)) {
            if(this.page==3) return true;
            this.viewZoom=Mth.clamp(this.viewZoom+(float)amount*0.1F,0.6F,1.3F);
            return true;
        }
        return super.mouseScrolled(x,y,amount);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(this.isCraftingUi())return key==256 || this.minecraft.options.keyInventory.matches(key,scan) ? super.keyPressed(key,scan,modifiers) : true;
        if(this.paletteKind!=null || this.outlineEditor!=null) {
            if(key==256) {this.closeModal();return true;}
            if(this.getFocused()!=null && this.getFocused().keyPressed(key,scan,modifiers))return true;
            return true;
        }
        if(this.getFocused() instanceof FrameNumberControl control && control.isEditing() && key!=256)return control.keyPressed(key,scan,modifiers);
        if(this.nameBox!=null && this.nameBox.isFocused() && key!=256) return this.nameBox.keyPressed(key,scan,modifiers) || this.nameBox.canConsumeInput();
        if(this.page==3 && key==261 && this.selectedDecoration()!=null) {this.deleteDecoration();return true;}
        return super.keyPressed(key,scan,modifiers);
    }

    @Override public boolean charTyped(char character,int modifiers) {
        return this.isCraftingUi() || super.charTyped(character,modifiers);
    }

}
