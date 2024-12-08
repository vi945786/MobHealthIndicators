package net.vi.mobhealthindicators.mixin.cloth_config.mixins;

import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Selectable;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInBaseListCell;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BaseListCell.class, remap = false)
public abstract class BaseListCellMixin extends AbstractParentElement implements Selectable, AddedMethodsInBaseListCell {

    @Override
    public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public int getMorePossibleHeight() {
        return 0;
    }
}
