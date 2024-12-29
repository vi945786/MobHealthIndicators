package net.vi.mobhealthindicators.config;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class ToggleableNestedListListEntry<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry<T, ToggleableNestedListListEntry.ToggleableNestedListCell<T, INNER>, ToggleableNestedListListEntry<T, INNER>> {
    private final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
    private final AtomicBoolean toggled = new AtomicBoolean();
    private final boolean originalToggled;
    private final BiConsumer<List<T>, Boolean> saveConsumer;

    public ToggleableNestedListListEntry(Text fieldName, List<T> value, boolean toggled, boolean defaultExpanded, Supplier<Optional<Text[]>> tooltipSupplier, BiConsumer<List<T>, Boolean> saveConsumer, Supplier<List<T>> defaultValue, Text resetButtonKey, boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, ToggleableNestedListListEntry<T, INNER>, INNER> createNewCell) {
        super(fieldName, value, defaultExpanded, tooltipSupplier, null, defaultValue, resetButtonKey, false, deleteButtonEnabled, insertInFront, (t, toggleableNestedListListEntry) -> new ToggleableNestedListListEntry.ToggleableNestedListCell(t, toggleableNestedListListEntry, (AbstractConfigListEntry) createNewCell.apply(t, toggleableNestedListListEntry)));
        originalToggled = toggled;
        this.toggled.set(toggled);
        this.saveConsumer = saveConsumer;

        resetWidget = ButtonWidget.builder(getYesNoText(toggled), (widget) -> {
            this.toggled.set(!this.toggled.get());
            resetWidget.setMessage(getYesNoText(this.toggled.get()));
        }).dimensions(0, 0, 115, 20).build();

        for(ToggleableNestedListListEntry.ToggleableNestedListCell<T, INNER> cell : this.cells) {
            this.referencableEntries.add(cell.nestedEntry);
        }

        this.setReferenceProviderEntries(this.referencableEntries);
    }

    public boolean isEdited() {
        return super.isEdited() || toggled.get() != originalToggled;
    }

    public Text getYesNoText(boolean bool) {
        return Text.translatable("text.cloth-config.boolean.value." + bool);
    }

    public boolean getToggled() {
        return toggled.get();
    }

    public void save() {
        if (this.saveConsumer != null) {
            this.saveConsumer.accept(this.getValue(), getToggled());
        }
    }

    public Iterator<String> getSearchTags() {
        return Iterators.concat(super.getSearchTags(), Iterators.concat(this.cells.stream().map((cell) -> cell.nestedEntry.getSearchTags()).iterator()));
    }

    public ToggleableNestedListListEntry<T, INNER> self() {
        return this;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return resetWidget.isMouseOver(mouseX, mouseY) || super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return resetWidget.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }

    public static class ToggleableNestedListCell<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry.AbstractListCell<T, ToggleableNestedListListEntry.ToggleableNestedListCell<T, INNER>, ToggleableNestedListListEntry<T, INNER>> implements ReferenceProvider<T> {
        private final INNER nestedEntry;

        public ToggleableNestedListCell(@Nullable T value, ToggleableNestedListListEntry<T, INNER> listListEntry, INNER nestedEntry) {
            super(value, listListEntry);
            this.nestedEntry = nestedEntry;
        }

        public @NotNull AbstractConfigEntry<T> provideReferenceEntry() {
            return this.nestedEntry;
        }

        public T getValue() {
            return (T)this.nestedEntry.getValue();
        }

        public Optional<net.minecraft.text.Text> getError() {
            return this.nestedEntry.getError();
        }

        public int getCellHeight() {
            return this.nestedEntry.getItemHeight();
        }

        public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            this.nestedEntry.setParent(((ToggleableNestedListListEntry) this.listListEntry).getParent());
            this.nestedEntry.setScreen(((ToggleableNestedListListEntry) this.listListEntry).getConfigScreen());
            this.nestedEntry.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
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
            ((ToggleableNestedListListEntry) this.listListEntry).referencableEntries.add(this.nestedEntry);
            ((ToggleableNestedListListEntry) this.listListEntry).requestReferenceRebuilding();
        }

        public void onDelete() {
            super.onDelete();
            ((ToggleableNestedListListEntry) this.listListEntry).referencableEntries.remove(this.nestedEntry);
            ((ToggleableNestedListListEntry) this.listListEntry).requestReferenceRebuilding();
        }

        public Selectable.SelectionType getType() {
            return SelectionType.NONE;
        }

        public void appendNarrations(NarrationMessageBuilder narrationElementOutput) {
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) || this.nestedEntry.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
            Element focused = getFocused();
            if (focused != null) {
                return focused.mouseScrolled(mouseX, mouseY, amountX, amountY);
            }
            return false;
        }

        public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {
            nestedEntry.setParent((DynamicEntryListWidget) listListEntry.getParent());
            nestedEntry.setScreen(listListEntry.getConfigScreen());
            nestedEntry.lateRender(graphics, mouseX, mouseY, delta);
        }

        public int getMorePossibleHeight() {
            return nestedEntry.getMorePossibleHeight();
        }
    }
}
