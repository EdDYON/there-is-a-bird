package EdDYON.guaniao.content.bird.scale;

public interface ScalableBirdModel {
    BirdModelScaleProfile modelScaleProfile();

    float getIndividualModelScale();

    void setIndividualModelScale(float scale);

    default float getModelRenderScale() {
        return BirdModelScale.renderScale(this.modelScaleProfile(), this.getIndividualModelScale());
    }
}
