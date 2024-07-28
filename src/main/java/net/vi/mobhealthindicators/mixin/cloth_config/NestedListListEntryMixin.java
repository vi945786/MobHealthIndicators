package net.vi.mobhealthindicators.mixin.cloth_config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.vi.mobhealthindicators.addmethods.AddedMethodsInBaseListCell;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NestedListListEntry.NestedListCell.class)
public abstract class NestedListListEntryMixin<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> implements ReferenceProvider<T>, AddedMethodsInBaseListCell {

    @Shadow @Final private INNER nestedEntry;

    public NestedListListEntryMixin(@Nullable T value, NestedListListEntry<T, INNER> listListEntry) {
        super(value, listListEntry);
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
        nestedEntry.setParent((DynamicEntryListWidget) listListEntry.getParent());
        nestedEntry.setScreen(listListEntry.getConfigScreen());
        nestedEntry.lateRender(graphics, mouseX, mouseY, delta);
    }

    @Override
    public int getMorePossibleHeight() {
        return nestedEntry.getMorePossibleHeight();
    }
}
