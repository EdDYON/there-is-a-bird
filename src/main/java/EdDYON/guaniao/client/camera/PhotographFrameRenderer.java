package EdDYON.guaniao.client.camera;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.PhotographFrameStyle;
import EdDYON.guaniao.content.camera.PhotographFrameStyle.Material;
import EdDYON.guaniao.content.camera.PhotographFrameStyle.Ornament;
import EdDYON.guaniao.content.camera.PhotographFrameStyle.Shape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared, immutable geometry for wall frames and their item preview. Coordinates are in blocks. */
public final class PhotographFrameRenderer {
    private static final float FRONT = 1.0F / 32.0F;
    private static final float BACK = -FRONT;
    private static final float MAT_FRONT = FRONT - 0.004F;
    private static final ResourceLocation OAK = texture("oak_planks");
    private static final ResourceLocation SPRUCE = texture("spruce_planks");
    private static final ResourceLocation BIRCH = texture("birch_planks");
    private static final ResourceLocation COPPER = texture("copper_block");
    private static final ResourceLocation PAPER = texture("white_wool");
    private static final ResourceLocation FEATHER = ResourceLocation.fromNamespaceAndPath(GuaniaoMod.MOD_ID, "textures/item/feather_white.png");
    private static final Map<Shape, Mesh> MESHES = new EnumMap<>(Shape.class);

    static {
        for (Shape shape : Shape.values()) {
            MESHES.put(shape, build(shape));
        }
    }

    private PhotographFrameRenderer() { }

    public static void render(ItemStack stack, PhotographFrameStyle style, int rotation,
                              PoseStack poses, MultiBufferSource buffers, int light) {
        Mesh mesh = MESHES.get(style.shape());
        ResourceLocation wood = switch (style.material()) {
            case SPRUCE -> SPRUCE;
            case BIRCH -> BIRCH;
            default -> OAK;
        };
        PoseStack.Pose pose = poses.last();
        drawFaces(mesh.wood, wood, pose, buffers, light, 1.0F, 1.0F, 1.0F);
        drawPaper(mesh.mat, pose, buffers, light);
        drawFaces(mesh.rimShadow, wood, pose, buffers, light, 0.60F, 0.57F, 0.52F);
        drawFaces(mesh.rimHighlight, PAPER, pose, buffers, light, 1.0F, 0.95F, 0.81F);
        if (style.material() == Material.COPPER) {
            drawFaces(mesh.copper, COPPER, pose, buffers, light, 1.0F, 1.0F, 1.0F);
        }
        renderPhoto(stack, rotation, mesh.photo, pose, buffers, light);
        if (style.ornament() == Ornament.LEAVES) {
            drawDecorations(mesh.leaves, PAPER, pose, buffers, light);
        } else if (style.ornament() == Ornament.FEATHER) {
            drawDecorations(mesh.feathers, FEATHER, pose, buffers, light);
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/" + name + ".png");
    }

    private static void drawPaper(List<Face> faces, PoseStack.Pose pose, MultiBufferSource buffers, int light) {
        // Keep the world's packed light, as photographs do, without adding the
        // directional entity shade that turned the pale mount grey-green.
        VertexConsumer out = buffers.getBuffer(RenderType.text(PAPER));
        for (Face face : faces) {
            paperVertex(out,pose,face.x0,face.y0,face.z1,light);
            paperVertex(out,pose,face.x1,face.y0,face.z1,light);
            paperVertex(out,pose,face.x1,face.y1,face.z1,light);
            paperVertex(out,pose,face.x0,face.y1,face.z1,light);
        }
    }

    private static void paperVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, int light) {
        out.addVertex(pose.pose(),x,y,z).setColor(255,245,219,255).setUv(x,-y).setLight(light);
    }

    private static void drawFaces(List<Face> faces, ResourceLocation texture, PoseStack.Pose pose,
                                  MultiBufferSource buffers, int light, float r, float g, float b) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (Face face : faces) {
            float x0 = face.x0, y0 = face.y0, z0 = face.z0;
            float x1 = face.x1, y1 = face.y1, z1 = face.z1;
            switch (face.side) {
                case 0 -> {
                    vertex(out, pose, x0,y0,z1, x0,-y0, 0,0,1, r,g,b,light);
                    vertex(out, pose, x1,y0,z1, x1,-y0, 0,0,1, r,g,b,light);
                    vertex(out, pose, x1,y1,z1, x1,-y1, 0,0,1, r,g,b,light);
                    vertex(out, pose, x0,y1,z1, x0,-y1, 0,0,1, r,g,b,light);
                }
                case 1 -> {
                    vertex(out, pose, x1,y0,z0, x1,-y0, 0,0,-1, r,g,b,light);
                    vertex(out, pose, x0,y0,z0, x0,-y0, 0,0,-1, r,g,b,light);
                    vertex(out, pose, x0,y1,z0, x0,-y1, 0,0,-1, r,g,b,light);
                    vertex(out, pose, x1,y1,z0, x1,-y1, 0,0,-1, r,g,b,light);
                }
                case 2, 3 -> {
                    float x = face.side == 2 ? x0 : x1;
                    float nx = face.side == 2 ? -1 : 1;
                    vertex(out, pose, x,y0,z0, z0,-y0, nx,0,0, r,g,b,light);
                    vertex(out, pose, x,y0,z1, z1,-y0, nx,0,0, r,g,b,light);
                    vertex(out, pose, x,y1,z1, z1,-y1, nx,0,0, r,g,b,light);
                    vertex(out, pose, x,y1,z0, z0,-y1, nx,0,0, r,g,b,light);
                }
                default -> {
                    float y = face.side == 4 ? y0 : y1;
                    float ny = face.side == 4 ? -1 : 1;
                    vertex(out, pose, x0,y,z0, x0,z0, 0,ny,0, r,g,b,light);
                    vertex(out, pose, x1,y,z0, x1,z0, 0,ny,0, r,g,b,light);
                    vertex(out, pose, x1,y,z1, x1,z1, 0,ny,0, r,g,b,light);
                    vertex(out, pose, x0,y,z1, x0,z1, 0,ny,0, r,g,b,light);
                }
            }
        }
    }

    private static void renderPhoto(ItemStack stack, int rotation, PhotoWindow window,
                                    PoseStack.Pose pose, MultiBufferSource buffers, int light) {
        float ratio = PhotographData.width(stack) / (float)Math.max(1, PhotographData.height(stack));
        if (!Float.isFinite(ratio) || ratio <= 0.0F) {
            ratio = 1.0F;
        }
        if ((rotation & 1) != 0) {
            ratio = 1.0F / ratio;
        }
        float width = window.size * Math.min(1.0F, ratio);
        float height = window.size / Math.max(1.0F, ratio);
        float x0 = -width * 0.5F, x1 = width * 0.5F;
        float y0 = window.y - height * 0.5F, y1 = window.y + height * 0.5F;
        VertexConsumer out = buffers.getBuffer(RenderType.text(PhotographTextureCache.textureFor(stack)));
        photoVertex(out, pose, x0,y0, 0,1,rotation,light);
        photoVertex(out, pose, x1,y0, 1,1,rotation,light);
        photoVertex(out, pose, x1,y1, 1,0,rotation,light);
        photoVertex(out, pose, x0,y1, 0,0,rotation,light);
    }

    private static void photoVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y,
                                     float u, float v, int rotation, int light) {
        float rotatedU = u;
        float rotatedV = v;
        switch (rotation & 3) {
            case 1 -> { rotatedU = v; rotatedV = 1.0F - u; }
            case 2 -> { rotatedU = 1.0F - u; rotatedV = 1.0F - v; }
            case 3 -> { rotatedU = 1.0F - v; rotatedV = u; }
            default -> { }
        }
        out.addVertex(pose.pose(), x, y, MAT_FRONT + 0.001F).setColor(255,255,255,255)
                .setUv(rotatedU,rotatedV).setLight(light);
    }

    private static void drawDecorations(List<Decoration> decorations, ResourceLocation texture,
                                        PoseStack.Pose pose, MultiBufferSource buffers, int light) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (Decoration d : decorations) {
            vertex(out,pose,d.x0,d.y0,d.z,d.u0,d.v1,0,0,1,d.r,d.g,d.b,light);
            vertex(out,pose,d.x1,d.y0,d.z,d.u1,d.v1,0,0,1,d.r,d.g,d.b,light);
            vertex(out,pose,d.x1,d.y1,d.z,d.u1,d.v0,0,0,1,d.r,d.g,d.b,light);
            vertex(out,pose,d.x0,d.y1,d.z,d.u0,d.v0,0,0,1,d.r,d.g,d.b,light);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz,
                               float r, float g, float b, int light) {
        out.addVertex(pose.pose(),x,y,z).setColor(r,g,b,1.0F).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose,nx,ny,nz);
    }

    private static Mesh build(Shape shape) {
        int width = shape.widthPixels(), height = shape.heightPixels();
        int border = width >= 32 && height >= 32 ? 2 : 1;
        int[][] cells = new int[height][width];
        boolean[][] copper = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!inside(shape,x,y)) continue;
                boolean edge = false;
                for (int dy = -border; dy <= border && !edge; dy++) {
                    for (int dx = -border; dx <= border; dx++) {
                        if (!inside(shape,x+dx,y+dy)) { edge = true; break; }
                    }
                }
                cells[y][x] = edge ? 1 : 2;
                copper[y][x] = edge && copperCorner(shape,x,y);
            }
        }
        List<Face> wood = new ArrayList<>();
        List<Face> mat = new ArrayList<>();
        List<Face> metal = new ArrayList<>();
        List<Face> rimShadow = new ArrayList<>();
        List<Face> rimHighlight = new ArrayList<>();
        addMergedFront(cells,1,width,height,FRONT,wood);
        addMergedFront(cells,2,width,height,MAT_FRONT,mat);
        addRims(cells,width,height,rimShadow,rimHighlight);
        int[][] back = new int[height][width];
        int[][] caps = new int[height][width];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                back[y][x] = cells[y][x] == 0 ? 0 : 1;
                caps[y][x] = copper[y][x] ? 1 : 0;
            }
        }
        List<Face> rear = new ArrayList<>();
        addMergedFront(back,1,width,height,BACK,rear);
        for (Face f : rear) wood.add(new Face(f.x0,f.y0,BACK,f.x1,f.y1,BACK,1));
        addMergedFront(caps,1,width,height,FRONT+0.0015F,metal);
        addSides(shape,wood);
        PhotoWindow photo = photoWindow(shape);
        List<Decoration> leaves = new ArrayList<>();
        List<Decoration> feathers = new ArrayList<>();
        float ax = shape == Shape.ROUND ? -11.0F : -width/2.0F + 2.0F;
        float ay = shape == Shape.ROUND ? 11.0F : height/2.0F - 2.0F;
        if (shape == Shape.ARCH) { ax = -5.0F; ay = 12.0F; }
        float bx = -ax, by = shape == Shape.ROUND ? -11.0F : -height/2.0F+2.0F;
        addLeafSprig(leaves,shape,photo,ax,ay,false);
        addLeafSprig(leaves,shape,photo,bx,by,true);
        addDecoration(feathers,shape,photo,ax-2.0F,ay-3.0F,ax+2.0F,ay+2.0F,
                0,0,1,1,FRONT+0.003F,1,1,1);
        addDecoration(feathers,shape,photo,bx-2.0F,by-2.0F,bx+2.0F,by+3.0F,
                1,1,0,0,FRONT+0.003F,0.92F,0.89F,0.83F);
        return new Mesh(List.copyOf(wood),List.copyOf(mat),List.copyOf(metal),
                List.copyOf(rimShadow),List.copyOf(rimHighlight),
                List.copyOf(leaves),List.copyOf(feathers),photo);
    }

    private static boolean inside(Shape shape, int x, int y) {
        int w = shape.widthPixels(), h = shape.heightPixels();
        if (x<0 || y<0 || x>=w || y>=h) return false;
        double dx = x + 0.5D - w/2.0D;
        if (shape == Shape.ROUND) {
            double dy = y + 0.5D - h/2.0D;
            return dx*dx + dy*dy <= w*w/4.0D;
        }
        if (shape == Shape.ARCH && y >= h-w/2) {
            double dy = y + 0.5D - (h-w/2.0D);
            return dx*dx + dy*dy <= w*w/4.0D;
        }
        return true;
    }

    private static boolean copperCorner(Shape shape, int x, int y) {
        int w = shape.widthPixels(), h = shape.heightPixels();
        if (shape == Shape.ROUND) {
            return Math.abs(x+0.5F-w/2.0F)>=8 && Math.abs(y+0.5F-h/2.0F)>=8;
        }
        if (shape == Shape.ARCH && y >= h-10) return Math.abs(x+0.5F-w/2.0F)>=3;
        return (x<3 || x>=w-3) && (y<3 || y>=h-3);
    }

    private static void addMergedFront(int[][] cells, int kind, int width, int height, float z, List<Face> out) {
        boolean[][] used = new boolean[height][width];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (used[y][x] || cells[y][x]!=kind) continue;
                int endX=x+1, endY=y+1;
                while (endX<width && !used[y][endX] && cells[y][endX]==kind) endX++;
                boolean extend=true;
                while (endY<height && extend) {
                    for (int dx=x; dx<endX; dx++) {
                        if (used[endY][dx] || cells[endY][dx]!=kind) { extend=false; break; }
                    }
                    if (extend) endY++;
                }
                for (int dy=y; dy<endY; dy++) for (int dx=x; dx<endX; dx++) used[dy][dx]=true;
                out.add(new Face((x-width/2.0F)/16,(y-height/2.0F)/16,z,
                        (endX-width/2.0F)/16,(endY-height/2.0F)/16,z,0));
            }
        }
    }

    private static void addSides(Shape shape, List<Face> out) {
        int width=shape.widthPixels(), height=shape.heightPixels();
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (!inside(shape,x,y)) continue;
                float x0=(x-width/2.0F)/16, y0=(y-height/2.0F)/16;
                float x1=x0+1.0F/16, y1=y0+1.0F/16;
                if (!inside(shape,x-1,y)) out.add(new Face(x0,y0,BACK,x0,y1,FRONT,2));
                if (!inside(shape,x+1,y)) out.add(new Face(x1,y0,BACK,x1,y1,FRONT,3));
                if (!inside(shape,x,y-1)) out.add(new Face(x0,y0,BACK,x1,y0,FRONT,4));
                if (!inside(shape,x,y+1)) out.add(new Face(x0,y1,BACK,x1,y1,FRONT,5));
            }
        }
    }

    private static void addRims(int[][] cells, int width, int height,
                                List<Face> shadow, List<Face> highlight) {
        final float inset = 0.25F;
        for (int side=2; side<=5; side++) {
            boolean vertical = side<=3;
            int lines = vertical ? width : height;
            int length = vertical ? height : width;
            for (int line=0; line<lines; line++) {
                int start=0;
                while (start<length) {
                    int x=vertical ? line : start, y=vertical ? start : line;
                    int kind=rimKind(cells,width,height,x,y,side);
                    if (kind==0) { start++; continue; }
                    int end=start+1;
                    while (end<length && rimKind(cells,width,height,
                            vertical ? line : end,vertical ? end : line,side)==kind) end++;
                    float x0=x, y0=y, x1=vertical ? x+1 : end, y1=vertical ? end : y+1;
                    if (side==2) x1=x0+inset;
                    else if (side==3) x0=x1-inset;
                    else if (side==4) y1=y0+inset;
                    else y0=y1-inset;
                    float z=FRONT+(kind==2 ? 0.0008F : 0.0006F);
                    (kind==2 ? highlight : shadow).add(new Face(
                            (x0-width/2.0F)/16,(y0-height/2.0F)/16,z,
                            (x1-width/2.0F)/16,(y1-height/2.0F)/16,z,0));
                    start=end;
                }
            }
        }
    }

    private static int rimKind(int[][] cells,int width,int height,int x,int y,int side) {
        if (cells[y][x]!=1) return 0;
        int nx=x+(side==2 ? -1 : side==3 ? 1 : 0);
        int ny=y+(side==4 ? -1 : side==5 ? 1 : 0);
        int neighbour=nx<0 || ny<0 || nx>=width || ny>=height ? 0 : cells[ny][nx];
        if (neighbour==1) return 0;
        return neighbour==0 && (side==2 || side==5) ? 2 : 1;
    }

    private static PhotoWindow photoWindow(Shape shape) {
        int size = switch (shape) {
            case LEGACY -> 10;
            case LARGE -> 26;
            case ROUND -> 18;
            default -> 12;
        };
        return new PhotoWindow(size/16.0F, shape==Shape.ARCH ? -4.0F/16 : 0.0F);
    }

    private static void addLeafSprig(List<Decoration> out, Shape shape, PhotoWindow photo,
                                     float x, float y, boolean reverse) {
        float direction=reverse ? -1 : 1;
        addDecoration(out,shape,photo,x-0.3F,y-2.7F,x+0.3F,y+2.5F,
                0,0,1,1,FRONT+0.002F,0.36F,0.25F,0.12F);
        addLeaf(out,shape,photo,x-1.6F*direction,y+0.5F,0.32F,0.52F,0.15F);
        addLeaf(out,shape,photo,x+0.5F*direction,y-2.2F,0.43F,0.63F,0.20F);
        addLeaf(out,shape,photo,x+0.2F*direction,y+1.3F,0.52F,0.68F,0.25F);
    }

    private static void addLeaf(List<Decoration> out, Shape shape, PhotoWindow photo,
                                float x, float y, float r, float g, float b) {
        addDecoration(out,shape,photo,x,y,x+1.8F,y+1.8F,0,0,1,1,FRONT+0.004F,r,g,b);
        addDecoration(out,shape,photo,x+0.6F,y-0.6F,x+1.2F,y+2.4F,0,0,1,1,FRONT+0.004F,r,g,b);
        addDecoration(out,shape,photo,x+0.6F,y,x+1.2F,y+1.8F,0,0,1,1,FRONT+0.005F,r*0.72F,g*0.78F,b*0.8F);
    }

    /** Clip ornaments to the outer bounds and outside the complete photograph window. */
    private static void addDecoration(List<Decoration> out, Shape shape, PhotoWindow photo,
                                      float px0,float py0,float px1,float py1,
                                      float u0,float v0,float u1,float v1,float z,float r,float g,float b) {
        float x0=Math.max(px0,-shape.widthPixels()/2.0F), x1=Math.min(px1,shape.widthPixels()/2.0F);
        float y0=Math.max(py0,-shape.heightPixels()/2.0F), y1=Math.min(py1,shape.heightPixels()/2.0F);
        float left=-photo.size*8-0.2F, right=photo.size*8+0.2F;
        float bottom=photo.y*16-photo.size*8-0.2F, top=photo.y*16+photo.size*8+0.2F;
        addDecorationPiece(out,x0,y0,Math.min(x1,left),y1,px0,py0,px1,py1,u0,v0,u1,v1,z,r,g,b);
        addDecorationPiece(out,Math.max(x0,right),y0,x1,y1,px0,py0,px1,py1,u0,v0,u1,v1,z,r,g,b);
        float mid0=Math.max(x0,left), mid1=Math.min(x1,right);
        addDecorationPiece(out,mid0,y0,mid1,Math.min(y1,bottom),px0,py0,px1,py1,u0,v0,u1,v1,z,r,g,b);
        addDecorationPiece(out,mid0,Math.max(y0,top),mid1,y1,px0,py0,px1,py1,u0,v0,u1,v1,z,r,g,b);
    }

    private static void addDecorationPiece(List<Decoration> out,float x0,float y0,float x1,float y1,
                                           float px0,float py0,float px1,float py1,
                                           float u0,float v0,float u1,float v1,float z,float r,float g,float b) {
        if (x1<=x0 || y1<=y0) return;
        out.add(new Decoration(x0/16,y0/16,x1/16,y1/16,z,
                u0+(u1-u0)*(x0-px0)/(px1-px0),v0+(v1-v0)*(py1-y1)/(py1-py0),
                u0+(u1-u0)*(x1-px0)/(px1-px0),v0+(v1-v0)*(py1-y0)/(py1-py0),r,g,b));
    }

    private record Face(float x0,float y0,float z0,float x1,float y1,float z1,int side) { }
    private record Decoration(float x0,float y0,float x1,float y1,float z,
                              float u0,float v0,float u1,float v1,float r,float g,float b) { }
    private record PhotoWindow(float size,float y) { }
    private record Mesh(List<Face> wood,List<Face> mat,List<Face> copper,
                        List<Face> rimShadow,List<Face> rimHighlight,
                        List<Decoration> leaves,List<Decoration> feathers,PhotoWindow photo) { }
}
