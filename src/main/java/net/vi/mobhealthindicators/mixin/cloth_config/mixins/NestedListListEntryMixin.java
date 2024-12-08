package net.vi.mobhealthindicators.mixin.cloth_config.mixins;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInBaseListCell;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = NestedListListEntry.class, remap = false)
public abstract class NestedListListEntryMixin<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> {

    public NestedListListEntryMixin(Text fieldName, List<T> value, boolean defaultExpanded, Supplier<Optional<Text[]>> tooltipSupplier, Consumer<List<T>> saveConsumer, Supplier<List<T>> defaultValue, Text resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, NestedListListEntry<T, INNER>, NestedListListEntry.NestedListCell<T, INNER>> createNewCell) {
        super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, createNewCell);
    }

    @Mixin(value = NestedListListEntry.NestedListCell.class, remap = false)
    public abstract static class NestedListCellMixin<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> implements ReferenceProvider<T>, AddedMethodsInBaseListCell {

        @Shadow(remap = false)
        @Final
        private INNER nestedEntry;

        public NestedListCellMixin(@Nullable T value, NestedListListEntry<T, INNER> listListEntry) {
            super(value, listListEntry);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
            Element focused = getFocused();
            if (focused != null) {
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
}