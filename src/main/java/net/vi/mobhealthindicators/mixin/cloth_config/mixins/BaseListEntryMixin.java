package net.vi.mobhealthindicators.mixin.cloth_config.mixins;

import me.shedaniel.clothconfig2.api.Expandable;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInBaseListCell;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Mixin(value = BaseListEntry.class, remap = false)
public abstract class BaseListEntryMixin<T, C extends BaseListCell, SELF extends BaseListEntry<T, C, SELF>> extends TooltipListEntry<List<T>> implements Expandable {

    public BaseListEntryMixin(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        Element focused = getFocused();
        if(focused != null) {
            return focused.mouseScrolled(mouseX, mouseY, amountX, amountY);
        }
        return false;
    }

    @Override
    public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {
        super.lateRender(graphics, mouseX, mouseY, delta);
        BaseListCell focused = !isExpanded() || getFocused() == null || !(getFocused() instanceof BaseListCell) ? null : (BaseListCell) getFocused();
        if(focused != null) {
            ((AddedMethodsInBaseListCell) focused).lateRender(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public int getMorePossibleHeight() {
        BaseListCell focused = !isExpanded() || getFocused() == null || !(getFocused() instanceof BaseListCell) ? null : (BaseListCell) getFocused();
        return focused != null ? ((AddedMethodsInBaseListCell) focused).getMorePossibleHeight() : 0;
    }
}
