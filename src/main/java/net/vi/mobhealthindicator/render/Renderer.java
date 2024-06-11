package net.vi.mobhealthindicator.render;

public interface Renderer {

    float minU = 0F;
    float maxU = 1F;
    float minV = 0F;
    float maxV = 1F;
    float heartSize = 9F;

    void init();
    void startRendering(HeartType type);
    void render(float x);
    void endRendering();
}
