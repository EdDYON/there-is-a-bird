package EdDYON.guaniao.client.cage;

import EdDYON.guaniao.content.cage.BirdCageBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class BirdCageRenderer extends GeoBlockRenderer<BirdCageBlockEntity> {
    public BirdCageRenderer(BlockEntityRendererProvider.Context context) {
        super(new BirdCageModel());
    }

    @Override
    public boolean shouldRenderOffScreen(BirdCageBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
