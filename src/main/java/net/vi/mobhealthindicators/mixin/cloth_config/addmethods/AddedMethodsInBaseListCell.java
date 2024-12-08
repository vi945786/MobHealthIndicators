package net.vi.mobhealthindicators.mixin.cloth_config.addmethods;

import net.minecraft.client.gui.DrawContext;

public interface AddedMethodsInBaseListCell {

    void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta);
    int getMorePossibleHeight();

}
