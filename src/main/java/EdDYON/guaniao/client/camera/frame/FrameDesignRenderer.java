package EdDYON.guaniao.client.camera.frame;

import EdDYON.guaniao.client.camera.PhotographTextureCache;
import EdDYON.guaniao.content.camera.PhotographData;
import EdDYON.guaniao.content.camera.frame.FrameDesign;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** A bounded mesh cache shared by the workbench, placed frames, item icons and placement ghosts. */
public final class FrameDesignRenderer {
    private static final int MAX_CACHED = 128;
    private static final int MAX_CACHED_QUADS = 65536;
    private static int cachedQuads;
    private static final Map<GeometryKey,Mesh> MESHES = new LinkedHashMap<>(16,0.75F,true);
    private static final Map<CompoundTag,FrameDesign> PARSED = new LinkedHashMap<>(16,0.75F,true);
    private static final Map<GeometryKey,List<WorkshopPiece>> WORKSHOP_PIECES = new LinkedHashMap<>(4,0.75F,true);
    private static int workshopQuads;
    private static FrameDesign activeWorkshopDesign;
    private static Mesh activeWorkshopMesh;
    private static List<WorkshopPiece> activeWorkshopPieces;

    private FrameDesignRenderer() { }

    /** Match GuiGraphics' screen-space Y flip without corrupting the lighting normals. */
    public static void scaleForGui(PoseStack poses,float scale) {
        // PoseStack.scale with one negative axis sends a negative determinant to
        // 1.20.1's fastInvCubeRoot and produces enormous, overlit normals.
        poses.mulPoseMatrix(new Matrix4f().scaling(1,-1,1));
        poses.scale(scale,scale,scale);
    }

    @Nullable
    public static FrameDesign designFor(ItemStack stack) {
        CompoundTag tag=stack.getTagElement(FrameDesign.TAG);
        if (tag==null) return null;
        FrameDesign cached=PARSED.get(tag);
        if (cached!=null) return cached;
        FrameDesign design=FrameDesign.fromTag(tag);
        if (design!=null) {
            CompoundTag canonical=design.toTag();
            // Unknown user NBT must not turn the bounded entry count into an
            // unbounded byte cache. Only retain the small validated schema.
            if(tag.equals(canonical)) {
                PARSED.put(canonical,design);
                trim(PARSED);
            }
        }
        return design;
    }

    public static void render(ItemStack photo,FrameDesign design,int extraRotation,PoseStack poses,
                              MultiBufferSource buffers,int light) {
        if (design.widthPixels() <= 0 || design.heightPixels() <= 0) return;
        Mesh mesh=mesh(design);
        renderFrame(design,mesh,poses.last(),buffers,light,1,1,1,1);
        if (PhotographData.hasImage(photo)) renderPhoto(photo,design,mesh,extraRotation,poses.last(),buffers,light);
        renderDecorations(design,poses.last(),buffers,light,1,1,1,1);
    }

    public static void renderGhost(ItemStack photo,FrameDesign design,PoseStack poses,MultiBufferSource buffers,int light,boolean valid) {
        if (design.widthPixels() <= 0 || design.heightPixels() <= 0) return;
        Mesh mesh=mesh(design);
        PoseStack.Pose pose=poses.last();
        var frame=FrameVisualResources.material(design.frameMaterial());
        // A placement guide has one visible backing sheet, not the front and
        // rear surfaces of a translucent solid frame stacked over each other.
        drawGhostMaterial(mesh.ghostLining,FrameVisualResources.material(design.liningMaterial()),pose,buffers,light,.18F);
        drawGhostMaterial(mesh.wood,frame,pose,buffers,light,.58F);
        drawGhostMaterial(mesh.rim,frame,pose,buffers,light,.30F);
        if(!design.trimMaterial().equals(FrameVisualResources.AIR)) {
            drawGhostMaterial(mesh.trim,FrameVisualResources.material(design.trimMaterial()),pose,buffers,light,.60F);
        }
        if(PhotographData.hasImage(photo)) renderPhoto(photo,design,mesh,0,pose,buffers,light,.60F,true);
        for(int i=0;i<design.decorations().size();i++) {
            renderDecoration(design,design.decorations().get(i),i,pose,buffers,light,1,1,1,.65F,true);
        }
        VertexConsumer outline=buffers.getBuffer(GhostRenderTypes.OUTLINE);
        float r=valid ? .35F : 1, g=valid ? .88F : .27F, b=valid ? .38F : .22F;
        for(OutlineEdge edge:mesh.outline) {
            float dx=edge.x1-edge.x0,dy=edge.y1-edge.y0,length=Mth.sqrt(dx*dx+dy*dy);
            outline.vertex(pose.pose(),edge.x0,edge.y0,mesh.front+.002F).color(r,g,b,.95F)
                    .normal(pose.normal(),dx/length,dy/length,0).endVertex();
            outline.vertex(pose.pose(),edge.x1,edge.y1,mesh.front+.002F).color(r,g,b,.95F)
                    .normal(pose.normal(),dx/length,dy/length,0).endVertex();
        }
    }

    private static void drawGhostMaterial(List<Quad> faces,FrameVisualResources.Material material,PoseStack.Pose pose,
                                          MultiBufferSource buffers,int light,float alpha) {
        VertexConsumer out=buffers.getBuffer(GhostRenderTypes.texture(FrameVisualResources.ATLAS));
        for(Quad quad:faces) {
            if(quad.nz<0)continue;
            for(int i=0;i<4;i++) {
                int p=i*3,uv=i*2;
                vertex(out,pose,quad.points[p],quad.points[p+1],quad.points[p+2],
                        material.sprite().getU(quad.uv[uv]*16),material.sprite().getV(quad.uv[uv+1]*16),
                        quad.nx,quad.ny,quad.nz,material.red(),material.green(),material.blue(),alpha,light);
            }
        }
    }

    /** Workbench-only layers. The placed-frame and item rendering paths stay unchanged. */
    public static void renderWorkshop(ItemStack photo,FrameDesign design,PoseStack poses,
                                      MultiBufferSource buffers,int light,WorkshopState state) {
        if(design.widthPixels()<=0 || design.heightPixels()<=0)return;
        Mesh mesh=workshopMesh(design);
        float lining=unit(state.lining),frame=unit(state.frame),picture=unit(state.photo);
        float trim=unit(state.trim),points=unit(state.decorations);
        float slip=Float.isFinite(state.slip) ? Mth.clamp(state.slip,-1,1) : 0;
        if(lining>0) {
            poses.pushPose();
            float amount=ease(lining),height=design.heightPixels()/16F;
            // A sheet unfurls from the upper edge, rather than fading a complete frame.
            poses.translate(0,height*(1-amount)*0.5F,0);
            poses.scale(1,Math.max(0.005F,amount),1);
            drawMaterial(mesh.backing,FrameVisualResources.material(design.liningMaterial()),poses.last(),buffers,light,1,1,1,1);
            poses.popPose();
        }
        var material=FrameVisualResources.material(design.frameMaterial());
        if(frame>=1) {
            drawMaterial(mesh.wood,material,poses.last(),buffers,light,1,1,1,1);
            drawMaterial(mesh.rim,material,poses.last(),buffers,light,.62F,.60F,.55F,1);
        } else if(frame>0) {
            List<WorkshopPiece> pieces=workshopPieces(design);
            float count=frame*pieces.size();
            for(int i=0;i<pieces.size() && i<count;i++) {
                WorkshopPiece piece=pieces.get(i);
                float landing=ease(unit(count-i));
                poses.pushPose();
                float lift=(1-landing),span=Math.max(design.widthPixels(),design.heightPixels())/16F;
                poses.translate((piece.x>=0 ? 1 : -1)*lift*span*.22F+slip*span*.045F*4*landing*(1-landing),
                        lift*span*.26F,lift*span*.15F);
                poses.mulPose(Axis.ZP.rotationDegrees(lift*(piece.x>=0 ? -12 : 12)));
                float alpha=ease(unit((count-i)/.2F));
                drawMaterial(piece.wood,material,poses.last(),buffers,light,1,1,1,alpha);
                drawMaterial(piece.rim,material,poses.last(),buffers,light,.62F,.60F,.55F,alpha);
                poses.popPose();
            }
        }
        if(picture>0 && PhotographData.hasImage(photo)) {
            poses.pushPose();
            WorkshopPhotoPose motion=workshopPhotoPose(design,picture);
            poses.translate(0,motion.offsetY,motion.lift);
            poses.mulPose(Axis.ZP.rotationDegrees(motion.roll));
            renderPhoto(photo,design,mesh,0,poses.last(),buffers,light,motion.alpha);
            poses.popPose();
        }
        if(!design.trimMaterial().equals(FrameVisualResources.AIR) && trim>0) {
            var edge=FrameVisualResources.material(design.trimMaterial());
            if(trim>=1)drawMaterial(mesh.trim,edge,poses.last(),buffers,light,1,1,1,1);
            else {
                List<WorkshopPiece> pieces=workshopPieces(design);
                float count=trim*pieces.size();
                for(int i=0;i<pieces.size() && i<count;i++) {
                    poses.pushPose();
                    poses.translate(0,0,(1-ease(unit(count-i)))*.2F);
                    drawMaterial(pieces.get(i).trim,edge,poses.last(),buffers,light,1,1,1,1);
                    poses.popPose();
                }
            }
        }
        for(int i=0;i<design.decorations().size();i++) {
            float arrival=unit(points*design.decorations().size()-i);
            if(arrival<=0)continue;
            FrameDesign.Decoration decoration=design.decorations().get(i);
            DecorationBounds bounds=decorationBounds(design,decoration);
            float cx=(bounds.x0+bounds.x1)*.5F,cy=(bounds.y0+bounds.y1)*.5F;
            float flight=1-ease(arrival),w=design.widthPixels()/16F,h=design.heightPixels()/16F;
            poses.pushPose();
            poses.translate(flight*(-w*.55F-cx),flight*(-h*.55F-cy),flight*.35F);
            renderDecoration(design,decoration,i,poses.last(),buffers,light,1,1,1,ease(unit(arrival/.2F)));
            poses.popPose();
        }
    }

    private static float unit(float value) {return Float.isFinite(value) ? Mth.clamp(value,0,1) : 0;}
    private static float ease(float value) {return value*value*(3-2*value);}

    /** Every layer is normalized; slip is only the current fragment's brief, reversible mishap. */
    public record WorkshopState(float lining,float frame,float photo,float trim,float decorations,float slip) { }

    /** The paper stays wholly in front of the frame until its XY outline is exactly aligned. */
    public static WorkshopPhotoPose workshopPhotoPose(FrameDesign design,float progress) {
        float p=unit(progress),approach=ease(unit(p/.65F)),seat=ease(unit((p-.65F)/.35F));
        float height=design.heightPixels()/16F,span=Math.max(design.widthPixels(),design.heightPixels())/16F;
        return new WorkshopPhotoPose((height+span*.9F)*(1-approach),
                (span*.10F+.004F)*(1-seat),4*(1-approach),ease(unit(p/.12F)));
    }

    public record WorkshopPhotoPose(float offsetY,float lift,float roll,float alpha) { }

    /** A continuous seam-following path, including when the next angular fragment starts. */
    public static WorkshopPoint workshopFrameContact(FrameDesign design,float progress,float slip) {
        List<WorkshopPiece> pieces=workshopPieces(design);
        if(pieces.isEmpty())return new WorkshopPoint(0,0,0);
        float count=unit(progress)*pieces.size();
        int index=Math.min(pieces.size()-1,(int)count);
        WorkshopPiece piece=pieces.get(index),previous=pieces.get(Math.max(0,index-1));
        float blend=ease(unit(count-index));
        float span=Math.max(design.widthPixels(),design.heightPixels())/16F;
        float shift=Float.isFinite(slip) ? Mth.clamp(slip,-1,1) : 0;
        return new WorkshopPoint(Mth.lerp(blend,previous.x,piece.x)+shift*span*.045F,
                Mth.lerp(blend,previous.y,piece.y),design.depthPixels()/32F);
    }

    public record WorkshopPoint(float x,float y,float z) { }

    public static WorkshopPoint workshopTrimContact(FrameDesign design,float progress) {
        List<WorkshopPiece> pieces=workshopPieces(design);
        if(pieces.isEmpty())return new WorkshopPoint(0,0,0);
        float count=unit(progress)*pieces.size();
        int index=Math.min(pieces.size()-1,(int)count);
        WorkshopPiece piece=pieces.get(index),previous=pieces.get(Math.max(0,index-1));
        float blend=ease(unit(count-index));
        return new WorkshopPoint(Mth.lerp(blend,previous.x,piece.x),Mth.lerp(blend,previous.y,piece.y),design.depthPixels()/32F);
    }

    private static List<WorkshopPiece> workshopPieces(FrameDesign design) {
        if(activeWorkshopDesign==design && activeWorkshopPieces!=null)return activeWorkshopPieces;
        workshopMesh(design);
        GeometryKey key=geometryKey(design);
        List<WorkshopPiece> cached=WORKSHOP_PIECES.get(key);
        if(cached!=null){activeWorkshopPieces=cached;return cached;}
        // Twenty-four bounded angular groups follow the real silhouette, including custom outlines.
        List<List<Quad>> wood=new ArrayList<>(),rim=new ArrayList<>(),trim=new ArrayList<>();
        float[] centres=new float[24],centreY=new float[24];int[] counts=new int[24];
        for(int i=0;i<24;i++){wood.add(new ArrayList<>());rim.add(new ArrayList<>());trim.add(new ArrayList<>());}
        int[] outer=design.mask(),window=design.windowMask(),frame=new int[32];
        for(int y=0;y<32;y++)frame[y]=outer[y]&~window[y];
        float w=design.widthPixels()/16F,h=design.heightPixels()/16F,front=design.depthPixels()/32F;
        for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            if(!occupied(frame,x,y))continue;
            double angle=Math.atan2(x-15.5,15.5-y);
            if(angle<0)angle+=Math.PI*2;
            int group=Math.min(23,(int)(angle/(Math.PI*2)*24));
            Bounds b=bounds(new FrameDesign.CellRect(x,y,x+1,y+1),w,h);
            List<Quad> faces=wood.get(group),lines=rim.get(group),edges=trim.get(group);
            faces.add(face(b.x0,b.y0,front,b.x1,b.y1,front,0));
            faces.add(face(b.x0,b.y0,-front,b.x1,b.y1,-front,1));
            centres[group]+=(b.x0+b.x1)*.5F;centreY[group]+=(b.y0+b.y1)*.5F;counts[group]++;
            float thickness=Math.min(w,h)/32*.20F;
            for(int side=2;side<=5;side++) {
                int nx=x+(side==2 ? -1 : side==3 ? 1 : 0),ny=y+(side==4 ? 1 : side==5 ? -1 : 0);
                if(occupied(frame,nx,ny))continue;
                boolean outside=!occupied(outer,nx,ny);
                if(!outside || design.trimMaterial().equals(FrameVisualResources.AIR))
                    faces.add(face(b.x0,b.y0,-front,b.x1,b.y1,front,side));
                Bounds line=switch(side) {
                    case 2 -> new Bounds(b.x0,b.y0,b.x0+thickness,b.y1);
                    case 3 -> new Bounds(b.x1-thickness,b.y0,b.x1,b.y1);
                    case 4 -> new Bounds(b.x0,b.y0,b.x1,b.y0+thickness);
                    default -> new Bounds(b.x0,b.y1-thickness,b.x1,b.y1);
                };
                lines.add(face(line.x0,line.y0,front+.0005F,line.x1,line.y1,front+.0005F,0));
                if(outside) {
                    edges.add(face(line.x0,line.y0,front+.001F,line.x1,line.y1,front+.001F,0));
                    edges.add(face(b.x0,b.y0,-front,b.x1,b.y1,front,side));
                }
            }
        }
        List<WorkshopPiece> result=new ArrayList<>();
        for(int i=0;i<24;i++)if(counts[i]>0)result.add(new WorkshopPiece(tile(wood.get(i)),tile(rim.get(i)),tile(trim.get(i)),centres[i]/counts[i],centreY[i]/counts[i]));
        cached=List.copyOf(result);WORKSHOP_PIECES.put(key,cached);
        workshopQuads+=cached.stream().mapToInt(WorkshopPiece::quadCount).sum();
        while(WORKSHOP_PIECES.size()>4 || workshopQuads>49152) {
            List<WorkshopPiece> removed=WORKSHOP_PIECES.remove(WORKSHOP_PIECES.keySet().iterator().next());
            workshopQuads-=removed.stream().mapToInt(WorkshopPiece::quadCount).sum();
        }
        activeWorkshopPieces=cached;
        return cached;
    }

    private static Mesh workshopMesh(FrameDesign design) {
        if(activeWorkshopDesign!=design || activeWorkshopMesh==null) {
            activeWorkshopDesign=design;activeWorkshopMesh=mesh(design);activeWorkshopPieces=null;
        }
        return activeWorkshopMesh;
    }

    private static Mesh mesh(FrameDesign design) {
        GeometryKey key=geometryKey(design);
        Mesh mesh=MESHES.get(key);
        if (mesh==null) {
            mesh=build(design);
            MESHES.put(key,mesh);
            cachedQuads+=mesh.quadCount();
            while(MESHES.size()>MAX_CACHED || cachedQuads>MAX_CACHED_QUADS) {
                Mesh oldest=MESHES.remove(MESHES.keySet().iterator().next());
                cachedQuads-=oldest.quadCount();
            }
        }
        return mesh;
    }

    private static GeometryKey geometryKey(FrameDesign design) {
        return new GeometryKey(design.widthPixels(),design.heightPixels(),design.mask(),design.borderCells(),
                design.depthPixels(),!design.trimMaterial().equals(FrameVisualResources.AIR),
                FrameVisualResources.isGlass(design.liningMaterial()));
    }

    private static <K,V> void trim(Map<K,V> cache) {
        while(cache.size()>MAX_CACHED) cache.remove(cache.keySet().iterator().next());
    }

    public static void onResourceReload() {
        MESHES.clear();
        cachedQuads=0;
        PARSED.clear();
        WORKSHOP_PIECES.clear();
        workshopQuads=0;
        activeWorkshopDesign=null;activeWorkshopMesh=null;activeWorkshopPieces=null;
        GhostRenderTypes.clear();
        FrameWorkshopBird.clear();
    }

    private static void renderFrame(FrameDesign design,Mesh mesh,PoseStack.Pose pose,MultiBufferSource buffers,
                                     int light,float r,float g,float b,float alpha) {
        var frame=FrameVisualResources.material(design.frameMaterial());
        drawMaterial(mesh.wood,frame,pose,buffers,light,r,g,b,alpha);
        drawMaterial(mesh.rim,frame,pose,buffers,light,r*0.62F,g*0.60F,b*0.55F,alpha);
        if (!design.trimMaterial().equals(FrameVisualResources.AIR)) {
            drawMaterial(mesh.trim,FrameVisualResources.material(design.trimMaterial()),pose,buffers,light,r,g,b,alpha);
        }
        // This includes both faces and every occupied silhouette cell. Material
        // changes never turn the rear of an old frame into an empty hole.
        drawMaterial(mesh.backing,FrameVisualResources.material(design.liningMaterial()),pose,buffers,light,r,g,b,alpha);
    }

    /** Bounds in frame-local blocks, with the origin at its centre and positive Y upwards. */
    public static DecorationBounds decorationBounds(FrameDesign design,FrameDesign.Decoration decoration) {
        float w=design.widthPixels()/16.0F,h=design.heightPixels()/16.0F;
        float size=Math.min(w,h)*decoration.size()/100.0F;
        float x=(decoration.x()/1000.0F-0.5F)*w,y=(0.5F-decoration.y()/1000.0F)*h;
        return new DecorationBounds(x-size/2,y-size/2,x+size/2,y+size/2);
    }

    /** The screen uses this same plane and bounds for point selection and dragging. */
    public static void renderDecoration(FrameDesign design,FrameDesign.Decoration decoration,PoseStack poses,
                                        MultiBufferSource buffers,int light) {
        if (design.widthPixels() <= 0 || design.heightPixels() <= 0) return;
        renderDecoration(design,decoration,0,poses.last(),buffers,light,1,1,1,1);
    }

    private static void renderDecorations(FrameDesign design,PoseStack.Pose pose,MultiBufferSource buffers,
                                           int light,float r,float g,float b,float alpha) {
        for (int i=0;i<design.decorations().size();i++) {
            renderDecoration(design,design.decorations().get(i),i,pose,buffers,light,r,g,b,alpha);
        }
    }

    private static void renderDecoration(FrameDesign design,FrameDesign.Decoration decoration,int order,
                                          PoseStack.Pose pose,MultiBufferSource buffers,int light,
                                          float r,float g,float b,float alpha) {
        renderDecoration(design,decoration,order,pose,buffers,light,r,g,b,alpha,false);
    }

    private static void renderDecoration(FrameDesign design,FrameDesign.Decoration decoration,int order,
                                          PoseStack.Pose pose,MultiBufferSource buffers,int light,
                                          float r,float g,float b,float alpha,boolean ghost) {
        ResourceLocation texture=FrameVisualResources.sticker(decoration.itemId());
        if (texture == null) return;
        // The captured inventory icon already contains vanilla shading. Like
        // the photograph, a printed sticker only needs the ambient lightmap;
        // entity lighting would shade it a second time when hung on a wall.
        VertexConsumer out=buffers.getBuffer(ghost ? GhostRenderTypes.photo(texture) : RenderType.text(texture));
        DecorationBounds bounds=decorationBounds(design,decoration);
        float z=design.depthPixels()/32.0F+0.0035F+order*0.0002F;
        // Later points are in front of earlier ones, even when texture batches flush in a different order.
        decorationVertex(out,pose,bounds.x0,bounds.y0,z,0,1,decoration.rotation(),r,g,b,alpha,light);
        decorationVertex(out,pose,bounds.x1,bounds.y0,z,1,1,decoration.rotation(),r,g,b,alpha,light);
        decorationVertex(out,pose,bounds.x1,bounds.y1,z,1,0,decoration.rotation(),r,g,b,alpha,light);
        decorationVertex(out,pose,bounds.x0,bounds.y1,z,0,0,decoration.rotation(),r,g,b,alpha,light);
    }

    private static void decorationVertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,
                                          float u,float v,int rotation,float r,float g,float b,float alpha,int light) {
        float ru=u,rv=v;
        switch(rotation) {
            case 1 -> {ru=v;rv=1-u;}
            case 2 -> {ru=1-u;rv=1-v;}
            case 3 -> {ru=1-v;rv=u;}
            default -> { }
        }
        out.vertex(pose.pose(),x,y,z).color(r,g,b,alpha).uv(ru,rv).uv2(light).endVertex();
    }

    private static void drawMaterial(List<Quad> faces,FrameVisualResources.Material material,PoseStack.Pose pose,
                                     MultiBufferSource buffers,int light,float r,float g,float b,float alpha) {
        VertexConsumer out=buffers.getBuffer(alpha<1 || material.translucent() ? RenderType.entityTranslucent(FrameVisualResources.ATLAS)
                : RenderType.entityCutoutNoCull(FrameVisualResources.ATLAS));
        for (Quad quad : faces) {
            for(int i=0;i<4;i++) {
                int p=i*3,uv=i*2;
                vertex(out,pose,quad.points[p],quad.points[p+1],quad.points[p+2],
                        material.sprite().getU(quad.uv[uv]*16),material.sprite().getV(quad.uv[uv+1]*16),
                        quad.nx,quad.ny,quad.nz,r*material.red(),g*material.green(),b*material.blue(),alpha,light);
            }
        }
    }

    private static void renderPhoto(ItemStack stack,FrameDesign design,Mesh mesh,int extraRotation,
                                     PoseStack.Pose pose,MultiBufferSource buffers,int light) {
        renderPhoto(stack,design,mesh,extraRotation,pose,buffers,light,1);
    }

    private static void renderPhoto(ItemStack stack,FrameDesign design,Mesh mesh,int extraRotation,
                                     PoseStack.Pose pose,MultiBufferSource buffers,int light,float alpha) {
        renderPhoto(stack,design,mesh,extraRotation,pose,buffers,light,alpha,false);
    }

    private static void renderPhoto(ItemStack stack,FrameDesign design,Mesh mesh,int extraRotation,
                                     PoseStack.Pose pose,MultiBufferSource buffers,int light,float alpha,boolean ghost) {
        if (mesh.window.isEmpty()) return;
        int opacity=Math.round(unit(alpha)*255);
        if(opacity==0)return;
        int rotation=(design.rotation()+extraRotation)&3;
        float ratio=PhotographData.width(stack)/(float)Math.max(1,PhotographData.height(stack));
        if (!Float.isFinite(ratio) || ratio<=0) ratio=1;
        if ((rotation&1)!=0) ratio=1/ratio;
        Bounds window=mesh.windowBounds;
        float ww=window.x1-window.x0,wh=window.y1-window.y0;
        if (!Float.isFinite(ww) || !Float.isFinite(wh) || ww<=0 || wh<=0) return;
        float scale=design.fit()==FrameDesign.PhotoFit.COVER ? Math.max(ww/ratio,wh) : Math.min(ww/ratio,wh);
        scale*=design.zoomPercent()/100.0F;
        float imageWidth=ratio*scale,imageHeight=scale;
        if (!Float.isFinite(imageWidth) || !Float.isFinite(imageHeight) || imageWidth<=0 || imageHeight<=0) return;
        float cx=(window.x0+window.x1)/2+design.offsetX()/100.0F*ww;
        float cy=(window.y0+window.y1)/2+design.offsetY()/100.0F*wh;
        float ix0=cx-imageWidth/2,iy0=cy-imageHeight/2;
        float ix1=cx+imageWidth/2,iy1=cy+imageHeight/2;
        ResourceLocation texture=PhotographTextureCache.textureFor(stack);
        VertexConsumer out=buffers.getBuffer(ghost ? GhostRenderTypes.photo(texture) : RenderType.text(texture));
        for(Bounds rect:mesh.window) {
            float x0=Math.max(ix0,rect.x0),x1=Math.min(ix1,rect.x1);
            float y0=Math.max(iy0,rect.y0),y1=Math.min(iy1,rect.y1);
            if(x0>=x1 || y0>=y1) continue;
            photoVertex(out,pose,x0,y0,mesh.front-0.001F,(x0-ix0)/imageWidth,(iy1-y0)/imageHeight,rotation,light,opacity);
            photoVertex(out,pose,x1,y0,mesh.front-0.001F,(x1-ix0)/imageWidth,(iy1-y0)/imageHeight,rotation,light,opacity);
            photoVertex(out,pose,x1,y1,mesh.front-0.001F,(x1-ix0)/imageWidth,(iy1-y1)/imageHeight,rotation,light,opacity);
            photoVertex(out,pose,x0,y1,mesh.front-0.001F,(x0-ix0)/imageWidth,(iy1-y1)/imageHeight,rotation,light,opacity);
        }
    }

    private static void photoVertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,int rotation,int light,int opacity) {
        float ru=u,rv=v;
        switch(rotation) {
            case 1 -> {ru=v;rv=1-u;}
            case 2 -> {ru=1-u;rv=1-v;}
            case 3 -> {ru=1-v;rv=u;}
            default -> { }
        }
        out.vertex(pose.pose(),x,y,z).color(255,255,255,opacity).uv(ru,rv).uv2(light).endVertex();
    }

    private static Mesh build(FrameDesign design) {
        float w=design.widthPixels()/16.0F,h=design.heightPixels()/16.0F,front=design.depthPixels()/32.0F;
        int[] outer=design.mask(),window=design.windowMask(),frame=new int[32];
        for(int y=0;y<32;y++) frame[y]=outer[y]&~window[y];
        List<Quad> wood=new ArrayList<>(),rims=new ArrayList<>(),trim=new ArrayList<>();
        List<Bounds> windows=new ArrayList<>();
        List<Quad> backing=new ArrayList<>(),ghostLining=new ArrayList<>();
        boolean glassLining=FrameVisualResources.isGlass(design.liningMaterial());
        for(FrameDesign.CellRect rect:FrameDesign.rectangles(outer)) {
            Bounds b=bounds(rect,w,h);
            backing.add(liningFace(b,front-0.0025F,0,w,h,glassLining));
            backing.add(liningFace(b,-front+0.0005F,1,w,h,glassLining));
        }
        for(FrameDesign.CellRect rect:FrameDesign.rectangles(frame)) {
            Bounds b=bounds(rect,w,h);
            wood.add(face(b.x0,b.y0,front,b.x1,b.y1,front,0));
            wood.add(face(b.x0,b.y0,-front,b.x1,b.y1,-front,1));
        }
        float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,maxX=-Float.POSITIVE_INFINITY,maxY=-Float.POSITIVE_INFINITY;
        for(FrameDesign.CellRect rect:FrameDesign.rectangles(window)) {
            Bounds b=bounds(rect,w,h);
            windows.add(b);
            ghostLining.add(liningFace(b,front-.0025F,0,w,h,glassLining));
            minX=Math.min(minX,b.x0);minY=Math.min(minY,b.y0);maxX=Math.max(maxX,b.x1);maxY=Math.max(maxY,b.y1);
        }
        addBoundaryRuns(frame,outer,w,h,front,wood,rims,trim,!design.trimMaterial().equals(FrameVisualResources.AIR));
        Bounds windowBounds=windows.isEmpty() ? new Bounds(0,0,0,0) : new Bounds(minX,minY,maxX,maxY);
        return new Mesh(tile(wood),tile(rims),tile(trim),List.copyOf(windows),
                tile(backing),tile(ghostLining),outline(outer,w,h),windowBounds,front);
    }

    /** Adjacent cell edges share a plane, normal and linear UVs; only their internal seams disappear. */
    private static void addBoundaryRuns(int[] frame,int[] outer,float w,float h,float front,
                                        List<Quad> wood,List<Quad> rims,List<Quad> trim,boolean hasTrim) {
        float thickness=Math.min(w,h)/32*0.20F;
        for(int side=2;side<=5;side++) for(int line=0;line<32;line++) {
            int start=-1,rimStart=-1,trimStart=-1;
            for(int cell=0;cell<=32;cell++) {
                int x=side<4 ? line : cell,y=side<4 ? cell : line;
                int nx=x+(side==2 ? -1 : side==3 ? 1 : 0),ny=y+(side==4 ? 1 : side==5 ? -1 : 0);
                boolean edge=cell<32 && occupied(frame,x,y) && !occupied(frame,nx,ny);
                boolean outside=edge && !occupied(outer,nx,ny);
                // The trim owns an exterior side. Two coplanar materials flicker and blend twice.
                boolean boundary=edge && (!hasTrim || !outside);
                if(boundary && start<0)start=cell;
                if(!boundary && start>=0) {
                    Bounds b=boundaryBounds(side,line,start,cell,w,h);
                    wood.add(face(b.x0,b.y0,-front,b.x1,b.y1,front,side));
                    start=-1;
                }
                if(edge && rimStart<0)rimStart=cell;
                if(!edge && rimStart>=0) {
                    Bounds b=boundaryBounds(side,line,rimStart,cell,w,h);
                    Bounds rim=boundaryRim(b,side,thickness);
                    rims.add(face(rim.x0,rim.y0,front+.0005F,rim.x1,rim.y1,front+.0005F,0));
                    rimStart=-1;
                }
                // Trim stops where an exterior edge becomes an inner window edge.
                if(outside && trimStart<0)trimStart=cell;
                if(!outside && trimStart>=0) {
                    Bounds b=boundaryBounds(side,line,trimStart,cell,w,h);
                    Bounds rim=boundaryRim(b,side,thickness);
                    trim.add(face(rim.x0,rim.y0,front+.001F,rim.x1,rim.y1,front+.001F,0));
                    trim.add(face(b.x0,b.y0,-front,b.x1,b.y1,front,side));
                    trimStart=-1;
                }
            }
        }
    }

    private static Bounds boundaryBounds(int side,int line,int start,int end,float w,float h) {
        return bounds(side<4 ? new FrameDesign.CellRect(line,start,line+1,end)
                : new FrameDesign.CellRect(start,line,end,line+1),w,h);
    }

    private static Bounds boundaryRim(Bounds b,int side,float thickness) {
        return switch(side) {
            case 2 -> new Bounds(b.x0,b.y0,b.x0+thickness,b.y1);
            case 3 -> new Bounds(b.x1-thickness,b.y0,b.x1,b.y1);
            case 4 -> new Bounds(b.x0,b.y0,b.x1,b.y0+thickness);
            default -> new Bounds(b.x0,b.y1-thickness,b.x1,b.y1);
        };
    }

    /** Merge straight runs on the actual mask, including holes, rather than outlining its bounding box. */
    private static List<OutlineEdge> outline(int[] mask,float width,float height) {
        List<OutlineEdge> edges=new ArrayList<>();
        for(int y=0;y<=32;y++) {
            int start=-1;
            for(int x=0;x<=32;x++) {
                boolean boundary=x<32 && occupied(mask,x,y-1)!=occupied(mask,x,y);
                if(boundary && start<0)start=x;
                if(!boundary && start>=0) {
                    float py=(.5F-y/32F)*height;
                    edges.add(new OutlineEdge((start/32F-.5F)*width,py,(x/32F-.5F)*width,py));
                    start=-1;
                }
            }
        }
        for(int x=0;x<=32;x++) {
            int start=-1;
            for(int y=0;y<=32;y++) {
                boolean boundary=y<32 && occupied(mask,x-1,y)!=occupied(mask,x,y);
                if(boundary && start<0)start=y;
                if(!boundary && start>=0) {
                    float px=(x/32F-.5F)*width;
                    edges.add(new OutlineEdge(px,(.5F-start/32F)*height,px,(.5F-y/32F)*height));
                    start=-1;
                }
            }
        }
        return List.copyOf(edges);
    }

    private static boolean occupied(int[] mask,int x,int y) {return x>=0 && y>=0 && x<32 && y<32 && (mask[y]&(1<<x))!=0;}
    private static Bounds bounds(FrameDesign.CellRect r,float w,float h) {
        return new Bounds((r.left()/32.0F-0.5F)*w,(0.5F-r.bottom()/32.0F)*h,
                (r.right()/32.0F-0.5F)*w,(0.5F-r.top()/32.0F)*h);
    }

    private static Quad face(float x0,float y0,float z0,float x1,float y1,float z1,int side) {
        float[] p;float nx=0,ny=0,nz=0;
        switch(side) {
            case 0 -> {p=new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1};nz=1;}
            case 1 -> {p=new float[]{x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0};nz=-1;}
            case 2 -> {p=new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0};nx=-1;}
            case 3 -> {p=new float[]{x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1};nx=1;}
            case 4 -> {p=new float[]{x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1};ny=-1;}
            default -> {p=new float[]{x0,y1,z1,x1,y1,z1,x1,y1,z0,x0,y1,z0};ny=1;}
        }
        float[] uv=new float[8];
        for(int i=0;i<4;i++) {uv[i*2]=side==2 || side==3 ? p[i*3+2] : p[i*3];uv[i*2+1]=side>=4 ? p[i*3+2] : -p[i*3+1];}
        return new Quad(p,uv,nx,ny,nz);
    }

    private static Quad liningFace(Bounds bounds,float z,int side,float width,float height,boolean glass) {
        Quad face=face(bounds.x0,bounds.y0,z,bounds.x1,bounds.y1,z,side);
        if(!glass)return face;
        float[] uv=new float[8];
        for(int i=0;i<4;i++) {
            uv[i*2]=face.points[i*3]/width+.5F;
            uv[i*2+1]=.5F-face.points[i*3+1]/height;
        }
        return new Quad(face.points,uv,face.nx,face.ny,face.nz);
    }

    /** Split only when building geometry: atlas sprites cannot use GL_REPEAT. */
    private static List<Quad> tile(List<Quad> faces) {
        List<Quad> result=new ArrayList<>();
        for (Quad face:faces) {
            float u0=face.uv[0],u1=face.uv[2],v0=face.uv[1],v1=face.uv[7];
            if (Math.abs(u1-u0)<0.000001F || Math.abs(v1-v0)<0.000001F) continue;
            float minU=Math.min(u0,u1),maxU=Math.max(u0,u1),minV=Math.min(v0,v1),maxV=Math.max(v0,v1);
            for(int tu=(int)Math.floor(minU);tu<Math.ceil(maxU);tu++) {
                for(int tv=(int)Math.floor(minV);tv<Math.ceil(maxV);tv++) {
                    float left=Math.max(minU,tu),right=Math.min(maxU,tu+1);
                    float top=Math.max(minV,tv),bottom=Math.min(maxV,tv+1);
                    float ua=u0<u1 ? left : right,ub=u0<u1 ? right : left;
                    float va=v0<v1 ? top : bottom,vb=v0<v1 ? bottom : top;
                    float[] uv={ua,va,ub,va,ub,vb,ua,vb},points=new float[12];
                    for(int i=0;i<4;i++) {
                        float du=(uv[i*2]-u0)/(u1-u0),dv=(uv[i*2+1]-v0)/(v1-v0);
                        for(int axis=0;axis<3;axis++) points[i*3+axis]=face.points[axis]
                                +(face.points[3+axis]-face.points[axis])*du+(face.points[9+axis]-face.points[axis])*dv;
                        uv[i*2]-=tu;uv[i*2+1]-=tv;
                    }
                    result.add(new Quad(points,uv,face.nx,face.ny,face.nz));
                }
            }
        }
        return List.copyOf(result);
    }

    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,
                                 float nx,float ny,float nz,float r,float g,float b,float alpha,int light) {
        out.vertex(pose.pose(),x,y,z).color(r,g,b,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(),nx,ny,nz).endVertex();
    }

    private record Quad(float[] points,float[] uv,float nx,float ny,float nz) { }
    private record OutlineEdge(float x0,float y0,float x1,float y1) { }
    private record WorkshopPiece(List<Quad> wood,List<Quad> rim,List<Quad> trim,float x,float y) {
        int quadCount(){return wood.size()+rim.size()+trim.size();}
    }
    private record Bounds(float x0,float y0,float x1,float y1) { }
    public record DecorationBounds(float x0,float y0,float x1,float y1) { }
    private record Mesh(List<Quad> wood,List<Quad> rim,List<Quad> trim,List<Bounds> window,
                        List<Quad> backing,List<Quad> ghostLining,List<OutlineEdge> outline,Bounds windowBounds,float front) {
        int quadCount() {return wood.size()+rim.size()+trim.size()+backing.size()+ghostLining.size()+outline.size();}
    }

    /** Depth-tested overlays leave scene depth and the current graphics target untouched. */
    private static final class GhostRenderTypes extends RenderType {
        private static final Map<ResourceLocation,RenderType> TEXTURES=new LinkedHashMap<>(16,.75F,true);
        private static final Map<ResourceLocation,RenderType> PHOTOS=new LinkedHashMap<>(16,.75F,true);
        private static final RenderType OUTLINE=create("guaniao_frame_ghost_outline",DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,256,false,false,CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2)))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private GhostRenderTypes() {
            super("guaniao_frame_ghost_internal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,
                    false,false,()->{},()->{});
        }

        private static RenderType texture(ResourceLocation texture) {
            RenderType type=TEXTURES.get(texture);
            if(type==null) {
                type=create("guaniao_frame_ghost",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,
                        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                                .setTextureState(new TextureStateShard(texture,false,false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
                TEXTURES.put(texture,type);trim(TEXTURES);
            }
            return type;
        }

        private static RenderType photo(ResourceLocation texture) {
            RenderType type=PHOTOS.get(texture);
            if(type==null) {
                type=create("guaniao_frame_ghost_photo",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,
                        CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SHADER)
                                .setTextureState(new TextureStateShard(texture,false,false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP)
                                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
                PHOTOS.put(texture,type);trim(PHOTOS);
            }
            return type;
        }

        private static void clear() {TEXTURES.clear();PHOTOS.clear();}
    }
    private record GeometryKey(int width,int height,int[] mask,int border,int depth,boolean hasTrim,boolean glassLining) {
        // Every construction site already supplies FrameDesign.mask()'s defensive copy.
        @Override public boolean equals(Object value) {
            return value instanceof GeometryKey key && width==key.width && height==key.height
                    && border==key.border && depth==key.depth && hasTrim==key.hasTrim
                    && glassLining==key.glassLining && Arrays.equals(mask,key.mask);
        }
        @Override public int hashCode() {
            int hash=31*(31*(31*(31*(31*width+height)+border)+depth)+(hasTrim?1:0))+(glassLining?1:0);
            return 31*hash+Arrays.hashCode(mask);
        }
    }
}
