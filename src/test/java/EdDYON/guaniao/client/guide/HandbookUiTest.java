package EdDYON.guaniao.client.guide;

import EdDYON.guaniao.client.gui.layout.GuiLayoutRect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tests the packaged offline book and the layout extremes which truncate reading areas. */
public final class HandbookUiTest {
    public static void main(String[] args) throws Exception {
        Path assets=Path.of("src/main/resources/assets/guaniao");
        Set<String> baseline=null;
        for(String language:List.of("zh_cn","en_us")) {
            HandbookData book;
            try(var reader=Files.newBufferedReader(assets.resolve("guide/"+language+".json"))) {book=HandbookData.read(reader);}
            Set<String> ids=new HashSet<>();
            for(var page:book.pages()) {
                require(ids.add(page.id()),"Duplicate page: "+page.id());
                require(!page.title().isBlank() && !page.sections().isEmpty(),"Readable page: "+page.id());
                Set<String> anchors=anchors(page);
                require(anchors.size()>=page.sections().size(),"Section anchors available");
                if(page.kind().equals("bird")) require(Files.exists(assets.resolve("textures/gui/handbook/"+page.model()+".png")),"Standing thumbnail exists");
                for(var section:page.sections()) for(var block:section.blocks()) {
                    if(block.type().equals("recipe"))require(Files.exists(Path.of("src/main/resources/data/guaniao/recipes/"+block.recipe().split(":")[1]+".json")),"Referenced recipe exists: "+block.recipe());
                    if(block.type().equals("sound"))require(block.sound().startsWith("guaniao:guide.cockatiel."),"Local preview sound");
                    for(var span:block.spans()) {
                        String target=span.link();
                        if(target.isEmpty() || target.startsWith("https://") || target.startsWith("http://"))continue;
                        String[] parts=target.split("#",2);
                        if(List.of("birds","items","docs").contains(parts[0]))continue;
                        var linked=parts[0].isEmpty()?page:book.page(parts[0]);
                        require(linked!=null,"Broken offline link: "+target);
                        if(parts.length>1)require(anchors(linked).contains(parts[1]),"Broken section link: "+target);
                    }
                }
            }
            require(book.find("bird","","").size()==17,"All 17 birds imported");
            require(book.page("doc/getting-started")==null,"Installation page removed from offline book");
            require(book.find("doc","","").size()==6,"Only the six in-game manuals remain");
            var anvilBlocks=book.page("doc/recipes").sections().stream().flatMap(s->s.blocks().stream())
                    .filter(b->b.type().equals("anvil")).toList();
            require(anvilBlocks.size()==3,"All three anvil diagrams retained as structured blocks");
            require(anvilBlocks.stream().map(HandbookData.Block::book).collect(java.util.stream.Collectors.toSet())
                    .equals(Set.of("guaniao:burial_plume_book","guaniao:riven_plume_book","guaniao:hunting_return_book")),"All three distinct anvil books present");
            require(book.page("doc/recipes").sections().stream().flatMap(s->s.blocks().stream())
                    .noneMatch(b->b.spans().size()==1 && Set.of("＋","→").contains(b.spans().get(0).text())),"No orphaned diagram operators");
            require(book.find("item","","").size()==41,"All 41 remaining item entries present");
            require(book.find("bird",""," NIGHT_HERON ").size()==1,"ID search and casing");
            require(book.find("bird","","__no_such_bird__").isEmpty(),"No-result state");
            require(book.find("","","nikon_d750").stream().anyMatch(p->p.id().equals("item/nikon_d750")),"Global search reaches other sections");
            require(book.find("item","food","").stream().allMatch(p->p.category().equals("food")),"Category filtering");
            require(book.find("bird","",language.equals("zh_cn")?"钓鱼竿":"fishing rod").stream().anyMatch(p->p.id().equals("bird/night_heron")),"Search includes practical instructions");
            if(baseline==null)baseline=ids;else require(baseline.equals(ids),"Language switch preserves routes");
        }
        int cases=0;
        for(int w:new int[]{272,320,427,599,600,619,640,819,820,960,1280}) for(int h:new int[]{180,200,240,270,360,540,720}) for(boolean article:new boolean[]{false,true}) {
            HandbookLayout l=HandbookLayout.of(w,h,article);
            GuiLayoutRect screen=new GuiLayoutRect(0,0,w,h);
            require(contains(screen,l.paper()) && contains(l.paper(),l.view()),"Reading region remains on screen");
            require(l.textWidth()>=225,"Keep a readable width at large GUI scales");
            require(l.paper().bottom()<l.footerY(),"Footer never covers text");
            if(l.hasSidebar())require(l.sidebar().right()<l.paper().x(),"Left rail separate");
            if(l.hasToc())require(l.toc().x()>l.paper().right() && l.toc().right()<=w,"Right rail separate");
            require(HandbookLayout.clampScroll(100000,1000,l.view().h())==1000-l.view().h(),"Last lines reachable");
            require(HandbookLayout.clampScroll(-1,1000,l.view().h())==0,"No scroll above page");
            require(HandbookLayout.clampScroll(50,20,l.view().h())==0,"Short page resets scroll");
            int thumb=HandbookLayout.thumbHeight(l.view().h(),1000);
            require(thumb>=16 && thumb<=l.view().h(),"Usable scrollbar");
            cases++;
        }
        int viewportCases=0;
        for(int[] resolution:new int[][]{{854,480},{1280,720},{1920,1080},{2560,1509},{3440,1440},{3840,2160}}) {
            HandbookViewport baselineViewport=HandbookViewport.of(resolution[0],resolution[1],1);
            for(int guiScale:new int[]{1,2,3,4,5,6,8}) {
                HandbookViewport v=HandbookViewport.of(resolution[0],resolution[1],guiScale);
                require(v.width()==baselineViewport.width() && v.height()==baselineViewport.height(),"HUD scale must not magnify the handbook");
                require(v.pixelScale()==baselineViewport.pixelScale(),"Pixel scale depends on the actual window");
                require(v.width()>=640 && v.height()>=360,"Readable viewport at ordinary game resolutions");
                for(double coordinate:new double[]{0,28.5,160,300,517.25})
                    require(Math.abs(v.fromGui(coordinate*v.pixelScale()/guiScale)-coordinate)<.00001,"Click and drag coordinate round trip");
                HandbookLayout catalogue=HandbookLayout.of(v.width(),v.height(),false);
                require(!catalogue.hasToc(),"No empty chapter rail in the catalogue");
                require((catalogue.view().h()-50)/76*catalogue.birdColumns()>=4,"At least four complete bird cards visible");
                viewportCases++;
            }
        }
        require(HandbookViewport.of(2560,1509,6).pixelScale()==3,"Reported 6x GUI case uses a readable 3x book");
        System.out.println("PASS: both offline languages, 70 pages, installation page removed, three structured anvil diagrams, internal links, recipe/thumbnail references, full-text search; "+cases+" responsive layouts; "+viewportCases+" resolution/GUI-scale combinations and coordinate round trips");
    }
    private static Set<String> anchors(HandbookData.Page p) {
        Set<String> ids=new HashSet<>();
        for(var s:p.sections()){ids.add(s.id());for(var b:s.blocks())if(b.id()!=null&&!b.id().isEmpty())ids.add(b.id());}
        return ids;
    }
    private static boolean contains(GuiLayoutRect a,GuiLayoutRect b) {return b.x()>=a.x()&&b.y()>=a.y()&&b.right()<=a.right()&&b.bottom()<=a.bottom();}
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
