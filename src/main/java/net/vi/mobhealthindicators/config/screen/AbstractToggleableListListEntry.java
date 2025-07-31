package net.vi.mobhealthindicators.config.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collectors;

import me.shedaniel.math.Rectangle;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@SuppressWarnings("unused")
public abstract class AbstractToggleableListListEntry<T, C extends AbstractToggleableListListEntry.AbstractListCell<T, C, SELF>, SELF extends AbstractToggleableListListEntry<T, C, SELF>> extends ToggleableBetterBaseListEntry<T, C, SELF> {
    protected final BiFunction<T, SELF, C> createNewCell;
    protected Function<T, Optional<Text>> cellErrorSupplier;
    protected List<T> original;
    protected boolean originalToggled;

    @Internal
    public AbstractToggleableListListEntry(Text fieldName, List<T> value, boolean toggled, boolean defaultExpanded, Supplier<Optional<Text[]>> tooltipSupplier, BiConsumer<List<T>, Boolean> saveConsumer, Supplier<List<T>> defaultValue, Supplier<Boolean> defaultToggle, Text resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, SELF, C> createNewCell) {
        super(fieldName, tooltipSupplier, toggled, defaultValue, defaultToggle, (abstractListListEntry) -> createNewCell.apply(null, abstractListListEntry), saveConsumer, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront);
        this.createNewCell = createNewCell;
        this.original = new ArrayList<>(value);
        originalToggled = toggled;

        for(T f : value) {
            this.cells.add(createNewCell.apply(f, this.self()));
        }

        this.widgets.addAll(this.cells);
        this.setExpanded(defaultExpanded);
    }

    public Function<T, Optional<Text>> getCellErrorSupplier() {
        return this.cellErrorSupplier;
    }

    public void setCellErrorSupplier(Function<T, Optional<Text>> cellErrorSupplier) {
        this.cellErrorSupplier = cellErrorSupplier;
    }

    public List<T> getValue() {
        return this.cells.stream().map(AbstractToggleableListListEntry.AbstractListCell::getValue).collect(Collectors.toList());
    }

    protected C getFromValue(T value) {
        return this.createNewCell.apply(value, this.self());
    }

    public boolean isEdited() {
        if (super.isEdited()) {
            return true;
        } else {
            List<T> value = this.getValue();
            if (value.size() != this.original.size()) {
                return true;
            } else {
                for(int i = 0; i < value.size(); ++i) {
                    if (!Objects.equals(value.get(i), this.original.get(i))) {
                        return true;
                    }
                }

                return originalToggled != toggled.get();
            }
        }
    }

    public abstract static class AbstractListCell<T, SELF extends AbstractToggleableListListEntry.AbstractListCell<T, SELF, OUTER_SELF>, OUTER_SELF extends AbstractToggleableListListEntry<T, SELF, OUTER_SELF>> extends BetterBaseListCell {
        protected final OUTER_SELF listListEntry;
        protected final Rectangle cellBounds = new Rectangle();

        public AbstractListCell(@Nullable T value, OUTER_SELF listListEntry) {
            this.listListEntry = listListEntry;
            this.setErrorSupplier(() -> Optional.ofNullable(listListEntry.cellErrorSupplier).flatMap((cellErrorFn) -> cellErrorFn.apply(this.getValue())));
        }

        public abstract T getValue();

        public void updateBounds(boolean expanded, int x, int y, int entryWidth, int entryHeight) {
            if (expanded) {
                this.cellBounds.setBounds(x, y, entryWidth, entryHeight);
            } else {
                this.cellBounds.setBounds(0, 0, 0, 0);
            }

        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.cellBounds.contains(mouseX, mouseY);
        }
    }
}
