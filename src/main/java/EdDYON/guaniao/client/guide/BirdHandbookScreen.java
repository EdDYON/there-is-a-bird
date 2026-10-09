package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.config.BirdConfigClient;
import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import EdDYON.guaniao.event.BurialPlumeAnvilEvents;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

/** Offline field handbook. All widgets, models and recipes use Minecraft's client renderer. */
public final class BirdHandbookScreen extends Screen {
    private static final int PAPER=0xFFEEE9D6, INK=0xFF283A2C, MUTED=0xFF68735D,
            GREEN=0xFF426338, LINE=0xFFC9C9AD, LIGHT=0xFFF7F3E3, WASH=0xFFE0E4CB;
    private static final ResourceLocation FOREST=new ResourceLocation("guaniao","textures/gui/handbook/forest.png");
    private final HandbookPreview preview=new HandbookPreview();
    private final ArrayDeque<Visit> history=new ArrayDeque<>();
    private final List<TextRow> textRows=new ArrayList<>();
    private final List<RecipeRow> recipeRows=new ArrayList<>();
    private final List<GuideButton> flowingButtons=new ArrayList<>();
    private final Map<String,Integer> anchors=new HashMap<>();
    private HandbookData data;
    private HandbookLayout layout;
    private HandbookViewport viewport;
    private HandbookData.Page page;
    private EditBox search;
    private String route="birds", query="", category="";
    private final int[] scroll={0,0,0}, extent={0,0,0};
    private int draggingBar=-1, modelY, modelW, modelH;
    private boolean draggingModel;
    private float readingScale=1.15f;
    private SimpleSoundInstance playingSound;
    private record Visit(String route,String query,String category,int scroll) {}
    private record TextRow(FormattedCharSequence text,int x,int y,int color,float scale) {}
    private record RecipeRow(int y,Recipe<?> recipe,HandbookAnvilRecipe anvil) {
        int height() { return anvil!=null?92:recipe instanceof AbstractCookingRecipe?112:100; }
    }

    public BirdHandbookScreen() { super(tr("title")); }
    private static Component tr(String key,Object... args) { return Component.translatable("gui.guaniao.handbook."+key,args); }
    @Override public boolean isPauseScreen() { return false; }

    private HandbookData loadData() {
        String language=minecraft.getLanguageManager().getSelected().startsWith("zh") ? "zh_cn" : "en_us";
        for(String code:List.of(language,"en_us")) {
            try(var input=minecraft.getResourceManager().open(new ResourceLocation("guaniao","guide/"+code+".json"));
                var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
                return HandbookData.read(reader);
            } catch(Exception e) { LogUtils.getLogger().error("Cannot load offline handbook {}",code,e); }
        }
        return new HandbookData(1,List.of(),List.of());
    }

    @Override protected void init() {
        if(data==null) data=loadData();
        var window=minecraft.getWindow();
        viewport=HandbookViewport.of(window.getWidth(),window.getHeight(),window.getGuiScale());
        width=viewport.width();height=viewport.height();
        rebuild();
    }

    private void rebuild() {
        clearWidgets(); flowingButtons.clear(); textRows.clear(); recipeRows.clear(); anchors.clear();
        extent[0]=extent[1]=extent[2]=0; draggingModel=false; draggingBar=-1;
        page=data.page(route);
        layout=HandbookLayout.of(width,height,page!=null);
        int tabX=width>=460?134:8;
        int tabWidth=Math.min(76,(width-tabX-12)/3);
        for(int i=0;i<3;i++) {
            String id=List.of("birds","items","docs").get(i);
            GuideButton b=button(tabX+i*(tabWidth+4),8,tabWidth,22,tr(id),()->navigate(id));
            b.selected=section().equals(id); b.dark=true;
        }
        GuiLayoutRect paper=layout.paper();
        search=new EditBox(font,paper.x()+13,paper.y()+15,paper.w()-26,14,tr("search"));
        search.setBordered(false); search.setTextColor(INK); search.setTextColorUneditable(MUTED);
        search.setMaxLength(100); search.setHint(tr("search")); search.setValue(query);
        search.setResponder(value->{query=value;scroll[0]=0;rebuildDocument();});
        addRenderableWidget(search);
        int footer=layout.footerY();
        button(paper.x(),footer,42,19,tr("back"),this::back).active=!history.isEmpty();
        button(paper.x()+46,footer,23,19,Component.literal("A−"),()->resizeText(-.15f));
        button(paper.x()+72,footer,23,19,Component.literal("A+"),()->resizeText(.15f));
        button(paper.right()-113,footer,69,19,tr("settings"),BirdConfigClient::requestOpen);
        button(paper.right()-40,footer,40,19,tr("close"),this::onClose);
        rebuildDocument();
    }

    private void resizeText(float amount) {
        readingScale=Mth.clamp(readingScale+amount,1,1.45f); rebuildDocument();
    }

    private String section() {
        return route.startsWith("bird/")?"birds":route.startsWith("item/")?"items":route.startsWith("doc/")?"docs":route;
    }

    private void rebuildDocument() {
        for(GuideButton b:flowingButtons) removeWidget(b);
        flowingButtons.clear();textRows.clear();recipeRows.clear();anchors.clear();
        extent[0]=extent[1]=extent[2]=0;modelH=0;
        buildSidebar();
        if(page==null || !query.isBlank()) buildCatalogue(); else buildArticle();
        for(int i=0;i<3;i++) scroll[i]=HandbookLayout.clampScroll(scroll[i],extent[i],region(i).h());
        placeButtons();
    }

    private void buildSidebar() {
        if(!layout.hasSidebar()) return;
        int y=0;
        for(String id:List.of("birds","items","docs")) {
            GuideButton b=flowButton(1,0,y,layout.sidebar().w()-4,24,tr(id),()->navigate(id));
            b.selected=section().equals(id); b.dark=true; y+=29;
        }
        y+=9;
        if(section().equals("items")) {
            GuideButton all=flowButton(1,0,y,layout.sidebar().w()-4,25,tr("all"),()->filterCategory(""));
            all.dark=true;all.selected=category.isEmpty();y+=29;
            for(var c:data.categories()) {
                int h=Math.max(25,font.split(Component.literal(c.title()),layout.sidebar().w()-16).size()*12+10);
                GuideButton b=flowButton(1,0,y,layout.sidebar().w()-4,h,Component.literal(c.title()),()->filterCategory(c.id()));
                b.dark=true;b.selected=c.id().equals(category);y+=h+4;
            }
        } else {
            for(var p:data.pages()) if(p.kind().equals("doc")) {
                int h=Math.max(25,font.split(Component.literal(p.title()),layout.sidebar().w()-16).size()*12+10);
                GuideButton b=flowButton(1,0,y,layout.sidebar().w()-4,h,Component.literal(p.title()),()->navigate(p.id()));
                b.dark=true;b.selected=route.equals(p.id());y+=h+4;
            }
        }
        extent[1]=y;
    }

    private void filterCategory(String id) { category=id;route="items";page=null;query="";scroll[0]=0;rebuild(); }

    private void buildCatalogue() {
        preview.clear();
        int w=layout.textWidth();
        String kind=section().equals("birds")?"bird":section().equals("items")?"item":"doc";
        boolean searching=!query.isBlank();
        var matches=data.find(searching?"":kind,searching?"":kind.equals("item")?category:"",query);
        int y=addText(tr(searching?"results":section()),0,2,w,1.35f,INK)+5;
        y=addText(tr("count",matches.size()),0,y,w,1,MUTED)+8;
        if(kind.equals("item") && !searching && !layout.hasSidebar()) {
            int x=0;
            List<HandbookData.Category> categories=new ArrayList<>();
            categories.add(new HandbookData.Category("",tr("all").getString()));categories.addAll(data.categories());
            for(var c:categories) {
                int bw=Math.min(w,font.width(c.title())+14);
                if(x+bw>w){x=0;y+=24;}
                GuideButton b=flowButton(0,x,y,bw,20,Component.literal(c.title()),()->filterCategory(c.id()));
                b.selected=c.id().equals(category); x+=bw+4;
            }
            y+=34;
        }
        if(data.pages().isEmpty()) y=addText(tr("load_error"),0,y,w,readingScale,INK)+16;
        else if(matches.isEmpty()) y=addText(tr("no_results"),0,y,w,readingScale,MUTED)+16;
        int columns=searching||kind.equals("bird")?layout.birdColumns():kind.equals("doc")?1:layout.columns();
        int cw=(w-(columns-1)*6)/columns;
        int rowH=searching||kind.equals("bird")?70:kind.equals("doc")?43:52;
        String previous="";int col=0;
        for(var p:matches) {
            if(kind.equals("item") && !searching && !p.category().equals(previous)) {
                if(col>0){y+=rowH+6;col=0;}
                String name=data.categories().stream().filter(c->c.id().equals(p.category())).map(HandbookData.Category::title).findFirst().orElse("");
                anchors.put(p.category(),y);
                y=addText(Component.literal(name),0,y+8,w,1.12f,INK)+8;previous=p.category();
            }
            GuideButton b=flowButton(0,col*(cw+6),y,cw,rowH,Component.literal(p.title()),()->navigate(p.id()));
            b.card=p;
            if(++col==columns){col=0;y+=rowH+6;}
        }
        if(col>0)y+=rowH+6;
        extent[0]=y+10;
    }

    private void buildArticle() {
        int w=layout.textWidth();
        int y=addText(tr(section()).copy().withStyle(s->s.withColor(GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,section()))),0,0,w,1,GREEN)+12;
        y=addText(Component.literal(page.title()),0,y,w,1.65f,INK)+12;
        if(page.kind().equals("bird")) {
            preview.select(page.model()); modelW=Math.min(132,w/3);modelH=118;modelY=y;
            int leftW=w-modelW-10;
            int summaryEnd=addText(Component.literal(page.summary()),0,y,leftW,readingScale,INK)+12;
            for(var fact:page.facts()) summaryEnd=addText(Component.literal(fact.label()+" · "+fact.text()),0,summaryEnd,leftW,1,MUTED)+7;
            y=Math.max(y+modelH,summaryEnd)+7;
            int pw=(w-12)/4;
            for(int i=0;i<4;i++) {
                final int index=i;
                String pose=(page.model().equals("hummingbird")
                        ? List.of("idle","nectar","fly","sleep")
                        : List.of("idle","forage","fly","alert")).get(i);
                GuideButton b=flowButton(0,i*(pw+4),y,pw,22,Component.translatable("gui.guaniao.bird_guide.pose."+pose),()->preview.pose(index));
                b.active=!(i==2 && preview.flightless());
            }
            y=addText(tr("model_hint"),0,y+28,w,1,MUTED)+16;
        } else {
            preview.clear();
            y=addText(Component.literal(page.summary()),0,y,w,readingScale,INK)+14;
            if(page.kind().equals("item")) {
                GuideButton icon=flowButton(0,0,y,w,30,Component.literal(page.icon()),()->{});
                icon.item=item(page.icon());icon.active=false;y+=41;
            }
        }
        if(layout.hasToc()) {
            int ty=0;
            for(var section:page.sections()) {
                int h=Math.max(24,font.split(Component.literal(section.title()),layout.toc().w()-17).size()*12+10);
                GuideButton b=flowButton(2,0,ty,layout.toc().w()-5,h,Component.literal(section.title()),()->jump(section.id()));
                b.dark=true;ty+=h+3;
            }
            extent[2]=ty;
        } else {
            y=addText(tr("contents"),0,y,w,1,MUTED)+6;
            int x=0;
            for(var section:page.sections()) {
                int bw=Math.min(w,font.width(section.title())+14);
                if(x+bw>w){x=0;y+=24;}
                flowButton(0,x,y,bw,20,Component.literal(section.title()),()->jump(section.id()));x+=bw+4;
            }
            y+=34;
        }
        for(var section:page.sections()) {
            anchors.put(section.id(),y);
            y=addText(Component.literal(section.title()),0,y+6,w,1.25f,INK)+10;
            for(var block:section.blocks()) {
                if(block.id()!=null && !block.id().isEmpty()) anchors.put(block.id(),y);
                if(block.type().equals("anchor")) continue;
                if(block.type().equals("recipe")) {y=addRecipe(block.recipe(),y);continue;}
                if(block.type().equals("anvil")) {
                    RecipeRow row=new RecipeRow(y,null,HandbookAnvilRecipe.fromBook(block.book()));
                    recipeRows.add(row);y+=row.height();continue;
                }
                if(block.type().equals("sound")) {
                    flowButton(0,0,y,110,22,tr("listen"),()->playSound(block.sound()));y+=31;continue;
                }
                MutableComponent text=Component.empty();
                for(var span:block.spans()) {
                    MutableComponent run=Component.literal(span.text());
                    if(!span.link().isEmpty()) run.withStyle(s->s.withColor(GREEN).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,span.link())));
                    text.append(run);
                }
                float scale=block.type().equals("h3")?1.1f:readingScale;
                boolean listItem=block.type().equals("li");
                // Keep the marker outside the wrapped text so long Chinese steps never leave it on an empty line.
                if(listItem) addText(Component.literal("•"),0,y,10,scale,INK);
                y=addText(text,listItem?11:0,y,w-(listItem?11:0),scale,INK)
                        +(block.type().equals("h3")?6:10);
            }
            y+=12;
        }
        extent[0]=y+8;
    }

    private int addRecipe(String id,int y) {
        Recipe<?> recipe=minecraft.level==null?null:minecraft.level.getRecipeManager().byKey(new ResourceLocation(id)).orElse(null);
        if(recipe==null) return addText(tr("recipe_missing"),0,y,layout.textWidth(),readingScale,MUTED)+12;
        if(recipe.getIngredients().isEmpty()) return addText(tr("recipe_special"),0,y,layout.textWidth(),readingScale,MUTED)+12;
        RecipeRow row=new RecipeRow(y,recipe,null);
        recipeRows.add(row);return y+row.height();
    }

    private void playSound(String id) {
        if(playingSound!=null)minecraft.getSoundManager().stop(playingSound);
        playingSound=SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(new ResourceLocation(id)),1,.65f);
        minecraft.getSoundManager().play(playingSound);
    }
    @Override public void removed() {
        if(playingSound!=null)minecraft.getSoundManager().stop(playingSound);
    }

    private int addText(Component text,int x,int y,int width,float scale,int color) {
        int lineHeight=Math.round(12*scale)+2;
        for(FormattedCharSequence line:font.split(text,Math.max(20,(int)(width/scale)))) {
            textRows.add(new TextRow(line,x,y,color,scale));y+=lineHeight;
        }
        return y;
    }

    private void navigate(String target) {
        if(target.startsWith("https://") || target.startsWith("http://")) {
            handleComponentClicked(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,target)));return;
        }
        String[] parts=target.split("#",2);
        if(parts[0].isEmpty() || (parts[0].equals(route) && query.isBlank())) {
            if(parts.length>1)jump(parts[1]);
            return;
        }
        if(!List.of("birds","items","docs").contains(parts[0]) && data.page(parts[0])==null) return;
        history.push(new Visit(route,query,category,scroll[0]));
        route=parts[0];query="";category="";scroll[0]=scroll[1]=scroll[2]=0;
        rebuild();
        if(parts.length>1)jump(parts[1]);
    }
    private void back() {
        if(history.isEmpty()) return;
        Visit v=history.pop();route=v.route;query=v.query;category=v.category;scroll[0]=v.scroll;scroll[1]=scroll[2]=0;rebuild();
    }
    private void jump(String id) {
        Integer y=anchors.get(id);if(y!=null){scroll[0]=HandbookLayout.clampScroll(y,extent[0],layout.view().h());placeButtons();}
    }

    private GuideButton button(int x,int y,int w,int h,Component label,Runnable action) {
        return addRenderableWidget(new GuideButton(x,y,w,h,label,action));
    }
    private GuideButton flowButton(int region,int x,int y,int w,int h,Component label,Runnable action) {
        GuideButton b=button(0,0,w,h,label,action);b.region=region;b.localX=x;b.localY=y;flowingButtons.add(b);return b;
    }
    private GuiLayoutRect region(int region) { return region==1?layout.sidebar():region==2?layout.toc():layout.view(); }
    private void placeButtons() {
        for(GuideButton b:flowingButtons) {
            GuiLayoutRect r=region(b.region);b.setX(r.x()+b.localX);b.setY(r.y()+b.localY-scroll[b.region]);
            b.visible=b.getY()+b.getHeight()>r.y() && b.getY()<r.bottom();
        }
    }

    @Override public void tick() { if(search!=null)search.tick();preview.tick(); }

    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.flush();
        HandbookGraphics book=new HandbookGraphics(minecraft,g,viewport);
        renderBook(book,(int)viewport.fromGui(mx),(int)viewport.fromGui(my),partial);
        book.flush();
    }

    private void renderBook(GuiGraphics g,int mx,int my,float partial) {
        g.setColor(1,1,1,1);
        g.blit(FOREST,0,0,0,0,width,height,width,height);
        g.fill(0,0,width,height,0xDE10271F);
        g.fill(0,0,width,35,0xFF142B22);g.fill(0,34,width,35,0xFF50603E);
        if(width>=460) {
            g.renderItem(item("guaniao:bird_guide"),12,10);
            g.drawString(font,tr("brand"),34,14,0xFFF2EDCF,false);
        }
        GuiLayoutRect paper=layout.paper();
        g.fill(paper.x()+2,paper.y()+2,paper.right()+2,paper.bottom()+2,0x60000000);
        g.fill(paper.x(),paper.y(),paper.right(),paper.bottom(),PAPER);
        g.renderOutline(paper.x(),paper.y(),paper.w(),paper.h(),0xFF9FA486);
        g.fill(paper.x()+10,paper.y()+8,paper.right()-10,paper.y()+32,LIGHT);
        g.renderOutline(paper.x()+10,paper.y()+8,paper.w()-20,24,LINE);
        GuiLayoutRect view=layout.view();
        g.enableScissor(view.x(),view.y(),view.right(),view.bottom());
        for(TextRow row:textRows) {
            int y=view.y()+row.y-scroll[0];
            if(y+16*row.scale<view.y() || y>view.bottom())continue;
            g.pose().pushPose();g.pose().translate(view.x()+row.x,y,0);g.pose().scale(row.scale,row.scale,1);
            g.drawString(font,row.text,0,0,row.color,false);g.pose().popPose();
        }
        for(RecipeRow row:recipeRows) renderRecipe(g,row,mx,my);
        if(modelH>0) preview.render(g,modelRect());
        g.disableScissor();
        super.render(g,mx,my,partial);
        for(int i=0;i<3;i++) drawScrollbar(g,i);
        if(view.contains(mx,my)) {
            for(RecipeRow row:recipeRows) recipeTooltip(g,row,mx,my);
        }
    }

    private GuiLayoutRect modelRect() {
        return new GuiLayoutRect(layout.view().x()+layout.textWidth()-modelW,layout.view().y()+modelY-scroll[0],modelW,modelH);
    }

    private void drawScrollbar(GuiGraphics g,int index) {
        GuiLayoutRect r=region(index);
        if(r.w()==0 || extent[index]<=r.h())return;
        int h=HandbookLayout.thumbHeight(r.h(),extent[index]);
        int y=r.y()+(r.h()-h)*scroll[index]/(extent[index]-r.h());
        g.fill(r.right()-4,r.y(),r.right(),r.bottom(),index==0?0xFFDADBC6:0xFF233F31);
        g.fill(r.right()-4,y,r.right(),y+h,index==0?0xFF73845B:0xFF9AAC7B);
    }

    private ItemStack ingredient(Ingredient ingredient) {
        ItemStack[] options=ingredient.getItems();
        return options.length==0?ItemStack.EMPTY:options[(int)(net.minecraft.Util.getMillis()/1200%options.length)];
    }

    private void recipeSlots(RecipeRow row,java.util.function.BiConsumer<GuiLayoutRect,ItemStack> consumer) {
        int x=layout.view().x()+14,y=layout.view().y()+row.y-scroll[0]+26;
        if(row.anvil!=null) {
            consumer.accept(new GuiLayoutRect(x,y,18,18),row.anvil.input());
            consumer.accept(new GuiLayoutRect(x+44,y,18,18),row.anvil.book());
            consumer.accept(new GuiLayoutRect(x+103,y,18,18),row.anvil.output());
            return;
        }
        var ingredients=row.recipe.getIngredients();
        int cols=row.recipe instanceof ShapedRecipe shaped?shaped.getWidth():row.recipe instanceof AbstractCookingRecipe?1:3;
        for(int i=0;i<ingredients.size();i++)consumer.accept(new GuiLayoutRect(x+i%cols*20,y+i/cols*20,18,18),ingredient(ingredients.get(i)));
        if(row.recipe instanceof AbstractCookingRecipe)consumer.accept(new GuiLayoutRect(x,y+38,18,18),new ItemStack(Items.COAL));
        if(minecraft.level!=null)consumer.accept(new GuiLayoutRect(x+103,y+20,18,18),row.recipe.getResultItem(minecraft.level.registryAccess()));
    }
    private void renderRecipe(GuiGraphics g,RecipeRow row,int mx,int my) {
        int y=layout.view().y()+row.y-scroll[0];
        if(y+row.height()<layout.view().y() || y>layout.view().bottom())return;
        int x=layout.view().x()+6,w=Math.min(layout.textWidth()-12,330);
        g.fill(x,y,x+w,y+row.height()-8,0x55DADBC6);
        boolean cooking=row.recipe instanceof AbstractCookingRecipe;
        g.renderItem(new ItemStack(row.anvil!=null?Items.ANVIL:cooking?Items.FURNACE:Items.CRAFTING_TABLE),x+3,y+2);
        Component label=row.anvil!=null?tr("anvil",BurialPlumeAnvilEvents.ANVIL_LEVEL_COST):tr(cooking?"smelting":"crafting");
        g.drawString(font,label,x+24,y+6,MUTED,false);
        int arrowY=y+(row.anvil!=null?34:54);
        // Draw a real arrow: some game fonts make the Unicode arrow resemble a plus sign.
        drawRecipeArrow(g,x+85,arrowY);
        if(row.anvil!=null) {
            g.drawString(font,"+",x+35,y+31,GREEN,false);
            HandbookData.Page result=data.page(row.anvil.outputPage());
            if(result!=null)g.drawString(font,result.title(),x+8,y+52,INK,false);
            g.drawString(font,tr("anvil_hint"),x+8,y+68,MUTED,false);
        } else if(cooking) {
            AbstractCookingRecipe recipe=(AbstractCookingRecipe)row.recipe;
            g.renderItem(new ItemStack(Items.FURNACE),x+51,y+44);
            g.drawString(font,tr("fuel_example"),x+32,y+69,MUTED,false);
            String seconds=java.math.BigDecimal.valueOf(recipe.getCookingTime()).divide(java.math.BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
            g.drawString(font,tr("cooking_details",seconds,Float.toString(recipe.getExperience())),x+8,y+89,MUTED,false);
        }
        recipeSlots(row,(r,stack)->{
            g.fill(r.x(),r.y(),r.right(),r.bottom(),WASH);g.renderOutline(r.x(),r.y(),r.w(),r.h(),LINE);
            g.renderItem(stack,r.x()+1,r.y()+1);g.renderItemDecorations(font,stack,r.x()+1,r.y()+1);
        });
    }
    private static void drawRecipeArrow(GuiGraphics g,int x,int y) {
        g.fill(x,y,x+17,y+2,GREEN);
        for(int i=0;i<5;i++)g.fill(x+12+i,y-4+i,x+13+i,y+6-i,GREEN);
    }
    private void recipeTooltip(GuiGraphics g,RecipeRow row,int mx,int my) {
        recipeSlots(row,(r,stack)->{if(r.contains(mx,my) && !stack.isEmpty())g.renderTooltip(font,stack,mx,my);});
    }
    private static ItemStack item(String id) {
        // These website identifiers describe enchanted stacks, not separately registered items.
        String book=switch(id) {
            case "guaniao:wind_feather_fan_burial" -> "guaniao:burial_plume_book";
            case "guaniao:wind_feather_fan_riven" -> "guaniao:riven_plume_book";
            case "guaniao:wind_feather_fan_hunting" -> "guaniao:hunting_return_book";
            default -> null;
        };
        if(book!=null)return HandbookAnvilRecipe.fromBook(book).output();
        ResourceLocation key=ResourceLocation.tryParse(id);
        return key==null?ItemStack.EMPTY:BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override public boolean mouseClicked(double mx,double my,int key) {
        return clickBook(viewport.fromGui(mx),viewport.fromGui(my),key);
    }

    private boolean clickBook(double mx,double my,int key) {
        if(key==0) {
            for(int i=0;i<3;i++) {
                GuiLayoutRect r=region(i);
                if(r.w()>0 && r.contains(mx,my) && mx>=r.right()-6 && extent[i]>r.h()) {
                    draggingBar=i;scrollTo(my);return true;
                }
            }
            if(modelH>0 && layout.view().contains(mx,my) && modelRect().contains(mx,my)) {draggingModel=true;return true;}
            if(layout.view().contains(mx,my)) {
                for(TextRow row:textRows) {
                    int y=layout.view().y()+row.y-scroll[0];
                    if(my<y || my>=y+12*row.scale)continue;
                    int dx=(int)((mx-layout.view().x()-row.x)/row.scale);
                    if(dx<0)continue;
                    Style style=font.getSplitter().componentStyleAtWidth(row.text,dx);
                    if(style!=null && style.getClickEvent()!=null) {navigate(style.getClickEvent().getValue());return true;}
                }
                for(RecipeRow row:recipeRows) {
                    final ItemStack[] hit={ItemStack.EMPTY};
                    recipeSlots(row,(r,stack)->{if(r.contains(mx,my))hit[0]=stack;});
                    if(!hit[0].isEmpty()) {
                        String id="item/"+BuiltInRegistries.ITEM.getKey(hit[0].getItem()).getPath();
                        if(row.anvil!=null && hit[0]==row.anvil.output())id=row.anvil.outputPage();
                        if(data.page(id)!=null){navigate(id);return true;}
                    }
                }
            }
        }
        return super.mouseClicked(mx,my,key);
    }
    private void scrollTo(double mouseY) {
        GuiLayoutRect r=region(draggingBar);int thumb=HandbookLayout.thumbHeight(r.h(),extent[draggingBar]);
        double ratio=(mouseY-r.y()-thumb*.5)/Math.max(1,r.h()-thumb);
        scroll[draggingBar]=HandbookLayout.clampScroll((int)Math.round(ratio*(extent[draggingBar]-r.h())),extent[draggingBar],r.h());
        placeButtons();
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        x=viewport.fromGui(x);y=viewport.fromGui(y);dx=viewport.fromGui(dx);dy=viewport.fromGui(dy);
        if(button==0 && draggingBar>=0){scrollTo(y);return true;}
        if(button==0 && draggingModel){preview.drag(dx,dy);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        x=viewport.fromGui(x);y=viewport.fromGui(y);
        draggingBar=-1;draggingModel=false;return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        x=viewport.fromGui(x);y=viewport.fromGui(y);
        if(modelH>0 && layout.view().contains(x,y) && modelRect().contains(x,y) && hasControlDown()) {preview.zoom(delta);return true;}
        for(int i=0;i<3;i++) if(region(i).w()>0 && region(i).contains(x,y)) {
            scroll[i]=HandbookLayout.clampScroll(scroll[i]-(int)(delta*34),extent[i],region(i).h());placeButtons();return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void mouseMoved(double x,double y) {
        super.mouseMoved(viewport.fromGui(x),viewport.fromGui(y));
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(key==GLFW.GLFW_KEY_F && hasControlDown()){setFocused(search);search.setFocused(true);return true;}
        if(search.isFocused() && key==GLFW.GLFW_KEY_ESCAPE){search.setValue("");search.setFocused(false);setFocused(null);return true;}
        if(!search.isFocused()) {
            if(key==GLFW.GLFW_KEY_ESCAPE && !history.isEmpty()){back();return true;}
            if(key==GLFW.GLFW_KEY_PAGE_DOWN || key==GLFW.GLFW_KEY_PAGE_UP || key==GLFW.GLFW_KEY_HOME || key==GLFW.GLFW_KEY_END) {
                int value=key==GLFW.GLFW_KEY_HOME?0:key==GLFW.GLFW_KEY_END?extent[0]:scroll[0]+(key==GLFW.GLFW_KEY_PAGE_DOWN?1:-1)*(layout.view().h()-20);
                scroll[0]=HandbookLayout.clampScroll(value,extent[0],layout.view().h());placeButtons();return true;
            }
        }
        return super.keyPressed(key,scan,mods);
    }

    private final class GuideButton extends Button {
        int region=-1,localX,localY;
        boolean selected,dark;
        HandbookData.Page card;
        ItemStack item=ItemStack.EMPTY;
        GuideButton(int x,int y,int w,int h,Component label,Runnable action) {super(x,y,w,h,label,b->action.run(),Button.DEFAULT_NARRATION);}
        @Override public boolean mouseClicked(double x,double y,int button) {
            return (region<0 || region(region).contains(x,y)) && super.mouseClicked(x,y,button);
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            if(region>=0){GuiLayoutRect r=region(region);g.enableScissor(r.x(),r.y(),r.right()-5,r.bottom());}
            boolean hover=isHoveredOrFocused() && (region<0 || region(region).contains(mx,my) || isFocused());
            int fill=dark?(selected?0xFF4C6038:hover?0xFF334D37:0x552A4231):(selected||hover?0xFFD1DCB4:WASH);
            g.fill(getX(),getY(),getX()+width,getY()+height,fill);
            if(!dark || selected||hover) {
                g.renderOutline(getX(),getY(),width,height,dark?0xFF84946B:LINE);
                if(!dark)g.fill(getX()+1,getY()+1,getX()+width-1,getY()+2,LIGHT);
            }
            int color=!active?MUTED:dark?0xFFEDEAD5:INK;
            if(card!=null) renderCard(g,color);
            else if(!item.isEmpty()) {
                g.renderItem(item,getX()+6,getY()+7);
                g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),width-35),getX()+30,getY()+11,MUTED,false);
            } else {
                var lines=font.split(getMessage(),Math.max(10,width-10));
                int y=getY()+(height-lines.size()*11)/2;
                for(var line:lines){g.drawString(font,line,getX()+(width-font.width(line))/2,y,color,false);y+=11;}
            }
            if(region>=0)g.disableScissor();
        }
        private void renderCard(GuiGraphics g,int color) {
            int size=card.kind().equals("bird")?43:22;
            int x=getX()+7,y=getY()+(height-size)/2;
            if(card.kind().equals("bird")) {
                ResourceLocation texture=new ResourceLocation("guaniao","textures/gui/handbook/"+card.model()+".png");
                g.blit(texture,x,y,0,0,size,size,size,size);
            } else {
                g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/16f,size/16f,1);
                g.renderItem(item(card.icon()),0,0);g.pose().popPose();
            }
            int tx=x+size+7,tw=width-size-24;
            var name=font.split(Component.literal(card.title()),Math.max(18,tw));
            int ty=getY()+10;
            for(var line:name){g.drawString(font,line,tx,ty,color,false);ty+=11;}
            if(card.kind().equals("bird")) for(var fact:card.facts()) {
                String text=font.plainSubstrByWidth(fact.text(),Math.max(18,tw));
                if(ty+11<getY()+height) {g.drawString(font,text,tx,ty+5,MUTED,false);ty+=13;}
            }
        }
    }
}
