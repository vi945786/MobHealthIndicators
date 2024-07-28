package net.vi.mobhealthindicators.mixin.cloth_config;

import me.shedaniel.clothconfig2.ClothConfigInitializer;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Selectable;
import net.vi.mobhealthindicators.addmethods.AddedMethodsInBaseListCell;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseListCell.class)
public abstract class BaseListCellMixin extends AbstractParentElement implements Selectable, AddedMethodsInBaseListCell {

    @Override
    public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public int getMorePossibleHeight() {
        return 0;
    }
}
