package net.vi.mobhealthindicators.mixin.cloth_config.mixins;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NestedListListEntry.NestedListCell.class)
public abstract class NestedListListEntryMixin$NestedListCellMixin<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> implements ReferenceProvider<T> {

    @Shadow @Final private INNER nestedEntry;

    public NestedListListEntryMixin$NestedListCellMixin(@Nullable T value, NestedListListEntry<T, INNER> listListEntry) {
        super(value, listListEntry);
    }

    public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {
        this.nestedEntry.setParent((DynamicEntryListWidget) listListEntry.getParent());
        nestedEntry.setScreen(listListEntry.getConfigScreen());
        nestedEntry.lateRender(graphics, mouseX, mouseY, delta);
    }

    public int getMorePossibleHeight() {
        return nestedEntry.getMorePossibleHeight();
    }
}

