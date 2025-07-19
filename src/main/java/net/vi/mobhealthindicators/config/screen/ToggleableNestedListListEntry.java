package net.vi.mobhealthindicators.config.screen;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ToggleableNestedListListEntry<T, INNER extends AbstractConfigListEntry<T>> extends AbstractToggleableListListEntry<T, ToggleableNestedListListEntry.NestedListCell<T, INNER>, ToggleableNestedListListEntry<T, INNER>> {
    private final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();

    public ToggleableNestedListListEntry(Text fieldName, List<T> value, boolean toggled, boolean defaultExpanded, Supplier<Optional<Text[]>> tooltipSupplier, BiConsumer<List<T>, Boolean> saveConsumer, Supplier<List<T>> defaultValue, Supplier<Boolean> defaultToggle, Text resetButtonKey, boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, ToggleableNestedListListEntry<T, INNER>, INNER> createNewCell) {
        super(fieldName, value, toggled, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, defaultToggle, resetButtonKey, false, deleteButtonEnabled, insertInFront, (t, nestedListListEntry) -> new NestedListCell(t, nestedListListEntry, createNewCell.apply(t, nestedListListEntry)));

        for(NestedListCell<T, INNER> cell : this.cells) {
            this.referencableEntries.add(cell.nestedEntry);
        }

        this.setReferenceProviderEntries(this.referencableEntries);
    }

    public Iterator<String> getSearchTags() {
        return Iterators.concat(super.getSearchTags(), Iterators.concat(this.cells.stream().map((cell) -> cell.nestedEntry.getSearchTags()).iterator()));
    }

    public ToggleableNestedListListEntry<T, INNER> self() {
        return this;
    }

    public static class NestedListCell<T, INNER extends AbstractConfigListEntry<T>> extends AbstractToggleableListListEntry.AbstractListCell<T, NestedListCell<T, INNER>, ToggleableNestedListListEntry<T, INNER>> implements ReferenceProvider<T> {
        private final INNER nestedEntry;

        @Internal
        public NestedListCell(@Nullable T value, ToggleableNestedListListEntry<T, INNER> listListEntry, INNER nestedEntry) {
            super(value, listListEntry);
            this.nestedEntry = nestedEntry;
        }

        public @NotNull AbstractConfigEntry<T> provideReferenceEntry() {
            return this.nestedEntry;
        }

        public T getValue() {
            return (T)this.nestedEntry.getValue();
        }

        public Optional<Text> getError() {
            return this.nestedEntry.getError();
        }

        public int getCellHeight() {
            return this.nestedEntry.getItemHeight();
        }

        public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            this.nestedEntry.setParent(((ToggleableNestedListListEntry)this.listListEntry).getParent());
            this.nestedEntry.setScreen(((ToggleableNestedListListEntry)this.listListEntry).getConfigScreen());
            this.nestedEntry.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
        }

        public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {
        	nestedEntry.setParent((DynamicEntryListWidget) listListEntry.getParent());
            nestedEntry.setScreen(listListEntry.getConfigScreen());
            nestedEntry.lateRender(graphics, mouseX, mouseY, delta);
        }

        public int getMorePossibleHeight() {
        	return nestedEntry.getMorePossibleHeight();
        }

        public void updateBounds(boolean expanded, int x, int y, int entryWidth, int entryHeight) {
            super.updateBounds(expanded, x, y, entryWidth, entryHeight);
            if (expanded) {
                this.nestedEntry.setBounds(new Rectangle(x, y, entryWidth, this.nestedEntry.getItemHeight()));
            } else {
                this.nestedEntry.setBounds(new Rectangle());
            }

        }

        public List<? extends Element> children() {
            return Collections.singletonList(this.nestedEntry);
        }

        public boolean isRequiresRestart() {
            return this.nestedEntry.isRequiresRestart();
        }

        public void updateSelected(boolean isSelected) {
            this.nestedEntry.updateSelected(isSelected);
        }

        public boolean isEdited() {
            return super.isEdited() || this.nestedEntry.isEdited();
        }

        public void onAdd() {
            super.onAdd();
            ((ToggleableNestedListListEntry)this.listListEntry).referencableEntries.add(this.nestedEntry);
            ((ToggleableNestedListListEntry)this.listListEntry).requestReferenceRebuilding();
        }

        public void onDelete() {
            super.onDelete();
            ((ToggleableNestedListListEntry)this.listListEntry).referencableEntries.remove(this.nestedEntry);
            ((ToggleableNestedListListEntry)this.listListEntry).requestReferenceRebuilding();
        }

        public Selectable.SelectionType getType() {
            return SelectionType.NONE;
        }

        public void appendNarrations(NarrationMessageBuilder narrationElementOutput) {
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) || this.nestedEntry.isMouseOver(mouseX, mouseY);
        }
    }
}
