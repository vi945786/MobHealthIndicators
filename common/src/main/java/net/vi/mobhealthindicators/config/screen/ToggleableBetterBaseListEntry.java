package net.vi.mobhealthindicators.config.screen;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.Expandable;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.vi.mobhealthindicators.ModInit.client;

public abstract class ToggleableBetterBaseListEntry<T, C extends BetterBaseListCell, SELF extends ToggleableBetterBaseListEntry<T, C, SELF>> extends TooltipListEntry<List<T>> implements Expandable {
    protected static final ResourceLocation CONFIG_TEX = ResourceLocation.fromNamespaceAndPath("cloth-config2", "textures/gui/cloth_config.png");
    protected final @NotNull List<C> cells;
    protected final @NotNull List<GuiEventListener> widgets;
    protected final @NotNull List<NarratableEntry> narratables;
    protected boolean expanded;
    protected boolean insertButtonEnabled;
    protected boolean deleteButtonEnabled;
    protected boolean insertInFront;
    protected ListLabelWidget labelWidget;
    protected AbstractWidget resetWidget;
    protected AbstractWidget toggleWidget;
    protected @NotNull Function<SELF, C> createNewInstance;
    protected @NotNull Supplier<List<T>> defaultValue;
    protected @NotNull Supplier<Boolean> defaultToggle;
    protected @Nullable Component addTooltip;
    protected @Nullable Component removeTooltip;
    protected AtomicBoolean toggled = new AtomicBoolean();
    protected BiConsumer<List<T>, Boolean> saveConsumer;

    public ToggleableBetterBaseListEntry(@NotNull Component fieldName, @Nullable Supplier<Optional<Component[]>> tooltipSupplier, boolean toggled, @Nullable Supplier<List<T>> defaultValue, Supplier<Boolean> defaultToggle, @NotNull Function<SELF, C> createNewInstance, @Nullable BiConsumer<List<T>, Boolean> saveConsumer, Component resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.insertButtonEnabled = true;
        this.addTooltip = Component.translatable("text.cloth-config.list.add");
        this.removeTooltip = Component.translatable("text.cloth-config.list.remove");
        this.deleteButtonEnabled = deleteButtonEnabled;
        this.insertInFront = insertInFront;
        this.cells = Lists.newArrayList();
        this.labelWidget = new ListLabelWidget();
        this.widgets = Lists.newArrayList(this.labelWidget);
        this.narratables = Lists.newArrayList();
        this.resetWidget = Button.builder(resetButtonKey, (widget) -> {
            this.toggled.set(defaultToggle.get());

            this.widgets.removeAll(this.cells);
            this.narratables.removeAll(this.cells);

            for(C cell : this.cells) {
                cell.onDelete();
            }

            this.cells.clear();
            Stream var10000 = defaultValue.get().stream().map(this::getFromValue);
            List var10001 = this.cells;
            Objects.requireNonNull(var10001);
            var10000.forEach(var10001::add);

            for(C cell : this.cells) {
                cell.onAdd();
            }

            this.widgets.addAll(this.cells);
            this.narratables.addAll(this.cells);
        }).bounds(0, 0, client.font.width(resetButtonKey) + 6, 20).build();
        this.widgets.add(this.resetWidget);
        this.narratables.add(this.resetWidget);

        toggleWidget = Button.builder(Component.empty(), (widget) -> {
            this.toggled.set(!this.toggled.get());
        }).bounds(0, 0, 150, 20).build();
        this.widgets.add(this.toggleWidget);
        this.narratables.add(this.toggleWidget);

        this.saveCallback = null;
        this.saveConsumer = saveConsumer;
        this.createNewInstance = createNewInstance;
        this.defaultValue = defaultValue;
        this.defaultToggle = defaultToggle;
        this.toggled.set(toggled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        GuiEventListener focused = getFocused();
        if(focused != null) {
            return focused.mouseScrolled(mouseX, mouseY, amountX, amountY);
        }
        return false;
    }

    public boolean isExpanded() {
        return this.expanded && this.isEnabled();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isEdited() {
        return super.isEdited() ? true : this.cells.stream().anyMatch(BetterBaseListCell::isEdited);
    }

    public boolean isMatchDefault() {
        Optional<List<T>> defaultValueOptional = this.getDefaultValue();
        if (defaultValueOptional.isPresent()) {
            List<T> value = this.getValue();
            List<T> defaultValue = defaultValueOptional.get();
            if (value.size() != defaultValue.size()) {
                return false;
            } else {
                for(int i = 0; i < value.size(); ++i) {
                    if (!Objects.equals(value.get(i), defaultValue.get(i))) {
                        return false;
                    }
                }

                return toggled.get() == defaultToggle.get();
            }
        } else {
            return false;
        }
    }

    public boolean isRequiresRestart() {
        return this.cells.stream().anyMatch(BetterBaseListCell::isRequiresRestart);
    }

    public void setRequiresRestart(boolean requiresRestart) {
    }

    public abstract SELF self();

    public boolean isDeleteButtonEnabled() {
        return this.deleteButtonEnabled && this.isEnabled();
    }

    public boolean isInsertButtonEnabled() {
        return this.insertButtonEnabled && this.isEnabled();
    }

    public void setDeleteButtonEnabled(boolean deleteButtonEnabled) {
        this.deleteButtonEnabled = deleteButtonEnabled;
    }

    public void setInsertButtonEnabled(boolean insertButtonEnabled) {
        this.insertButtonEnabled = insertButtonEnabled;
    }

    protected abstract C getFromValue(T var1);

    public @NotNull Function<SELF, C> getCreateNewInstance() {
        return this.createNewInstance;
    }

    public void setCreateNewInstance(@NotNull Function<SELF, C> createNewInstance) {
        this.createNewInstance = createNewInstance;
    }

    public @Nullable Component getAddTooltip() {
        return this.addTooltip;
    }

    public void setAddTooltip(@Nullable Component addTooltip) {
        this.addTooltip = addTooltip;
    }

    public @Nullable Component getRemoveTooltip() {
        return this.removeTooltip;
    }

    public void setRemoveTooltip(@Nullable Component removeTooltip) {
        this.removeTooltip = removeTooltip;
    }

    public Optional<List<T>> getDefaultValue() {
        return this.defaultValue == null ? Optional.empty() : Optional.ofNullable((List)this.defaultValue.get());
    }

    public int getItemHeight() {
        if (!this.isExpanded()) {
            return 24;
        } else {
            int i = 24;

            for(BetterBaseListCell entry : this.cells) {
                i += entry.getCellHeight();
            }

            return i;
        }
    }

    public List<? extends GuiEventListener> children() {
        if (!this.isExpanded()) {
            List<GuiEventListener> elements = new ArrayList(this.widgets);
            elements.removeAll(this.cells);
            return elements;
        } else {
            return this.widgets;
        }
    }

    public List<? extends NarratableEntry> narratables() {
        return this.narratables;
    }

    public Optional<Component> getError() {
        List<Component> errors = this.cells.stream().map(BetterBaseListCell::getConfigError).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        return errors.size() > 1 ? Optional.of(Component.translatable("text.cloth-config.multi_error")) : errors.stream().findFirst();
    }

    @Override
    public void save() {
        if (this.saveConsumer != null) {
            this.saveConsumer.accept(this.getValue(), toggled.get());
        }
    }

    public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
        this.labelWidget.rectangle.x = x - 15;
        this.labelWidget.rectangle.y = y;
        this.labelWidget.rectangle.width = entryWidth + 15;
        this.labelWidget.rectangle.height = 24;
        return new Rectangle(this.getParent().left, y, this.getParent().right - this.getParent().left, 20);
    }

    protected boolean isInsideCreateNew(double mouseX, double mouseY) {
        return this.isInsertButtonEnabled() && mouseX >= (double)(this.labelWidget.rectangle.x + 12) && mouseY >= (double)(this.labelWidget.rectangle.y + 3) && mouseX <= (double)(this.labelWidget.rectangle.x + 12 + 11) && mouseY <= (double)(this.labelWidget.rectangle.y + 3 + 11);
    }

    protected boolean isInsideDelete(double mouseX, double mouseY) {
        return this.isDeleteButtonEnabled() && mouseX >= (double)(this.labelWidget.rectangle.x + (this.isInsertButtonEnabled() ? 25 : 12)) && mouseY >= (double)(this.labelWidget.rectangle.y + 3) && mouseX <= (double)(this.labelWidget.rectangle.x + (this.isInsertButtonEnabled() ? 25 : 12) + 11) && mouseY <= (double)(this.labelWidget.rectangle.y + 3 + 11);
    }

    public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
        if (this.addTooltip != null && this.isInsideCreateNew((double)mouseX, (double)mouseY)) {
            return Optional.of(new Component[]{this.addTooltip});
        } else {
            return this.removeTooltip != null && this.isInsideDelete((double)mouseX, (double)mouseY) ? Optional.of(new Component[]{this.removeTooltip}) : super.getTooltip(mouseX, mouseY);
        }
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        BetterBaseListCell focused = this.isExpanded() && this.getFocused() != null && this.getFocused() instanceof BetterBaseListCell ? (BetterBaseListCell)this.getFocused() : null;
        boolean insideLabel = this.labelWidget.rectangle.contains(mouseX, mouseY);
        boolean insideCreateNew = this.isInsideCreateNew(mouseX, mouseY);
        boolean insideDelete = this.isInsideDelete(mouseX, mouseY);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, CONFIG_TEX, x - 15, y + 5, 33.0F, (float)((this.isEnabled() ? (insideLabel && !insideCreateNew && !insideDelete ? 18 : 0) : 36) + (this.isExpanded() ? 9 : 0)), 9, 9, 256, 256);
        if (this.isInsertButtonEnabled()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONFIG_TEX, x - 15 + 13, y + 5, 42.0F, insideCreateNew ? 9.0F : 0.0F, 9, 9, 256, 256);
        }

        if (this.isDeleteButtonEnabled()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONFIG_TEX, x - 15 + (this.isInsertButtonEnabled() ? 26 : 13), y + 5, 51.0F, focused == null ? 0.0F : (insideDelete ? 18.0F : 9.0F), 9, 9, 256, 256);
        }

        this.resetWidget.setX(x + entryWidth - this.resetWidget.getWidth());
        this.resetWidget.setY(y);
        this.resetWidget.active = this.isEditable() && this.getDefaultValue().isPresent() && !this.isMatchDefault();
        this.resetWidget.render(graphics, mouseX, mouseY, delta);

        this.toggleWidget.setX(x + entryWidth - 150);
        this.toggleWidget.setY(y);
        this.toggleWidget.active = this.isEditable();
        this.toggleWidget.render(graphics, mouseX, mouseY, delta);
        this.toggleWidget.setMessage(this.getYesNoText(this.toggled.get()));
        this.toggleWidget.setWidth(150 - this.resetWidget.getWidth() - 2);

        int offset = (!this.isInsertButtonEnabled() && !this.isDeleteButtonEnabled() ? 0 : 6) + (this.isInsertButtonEnabled() ? 9 : 0) + (this.isDeleteButtonEnabled() ? 9 : 0);
        graphics.drawString(client.font, this.getDisplayedFieldName().getVisualOrderText(), x + offset, y + 6, this.getPreferredTextColor());
        if (this.isExpanded()) {
            int yy = y + 24;

            for(BetterBaseListCell cell : this.cells) {
                cell.render(graphics, -1, yy, x + 14, entryWidth - 14, cell.getCellHeight(), mouseX, mouseY, this.getParent().getFocused() != null && this.getParent().getFocused().equals(this) && this.getFocused() != null && this.getFocused().equals(cell), delta);
                cell.updateBounds(true, x + 14, yy, entryWidth - 14, cell.getCellHeight());
                yy += cell.getCellHeight();
            }
        } else {
            int yy = y + 24;

            for(BetterBaseListCell cell : this.cells) {
                cell.updateBounds(false, x + 14, yy, entryWidth - 14, cell.getCellHeight());
                yy += cell.getCellHeight();
            }
        }

    }

    public Component getYesNoText(boolean bool) {
        return Component.translatable("text.cloth-config.boolean.value." + bool);
    }

    @Override
    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.lateRender(graphics, mouseX, mouseY, delta);
        BetterBaseListCell focused = !isExpanded() || getFocused() == null || !(getFocused() instanceof BetterBaseListCell) ? null : (BetterBaseListCell) getFocused();
        if(focused != null) {
        	focused.lateRender(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public int getMorePossibleHeight() {
    	BetterBaseListCell focused = !isExpanded() || getFocused() == null || !(getFocused() instanceof BetterBaseListCell) ? null : (BetterBaseListCell) getFocused();
    	return focused != null ? focused.getMorePossibleHeight() : 0;
    }

    public void updateSelected(boolean isSelected) {
        for(C cell : this.cells) {
            cell.updateSelected(isSelected && this.getFocused() == cell && this.isExpanded());
        }

    }

    public int getInitialReferenceOffset() {
        return 24;
    }

    public boolean insertInFront() {
        return this.insertInFront;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        } else {
            if (this.isExpanded()) {
                for(BetterBaseListCell cell : this.cells) {
                    if (cell.isMouseOver(mouseX, mouseY)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public class ListLabelWidget implements GuiEventListener {
        protected Rectangle rectangle = new Rectangle();

        public ListLabelWidget() {
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!ToggleableBetterBaseListEntry.this.isEnabled()) {
                return false;
            } else if (ToggleableBetterBaseListEntry.this.resetWidget.isMouseOver(event.x(), event.y())) {
                return false;
            } else if (ToggleableBetterBaseListEntry.this.toggleWidget.isMouseOver(event.x(), event.y())) {
                return false;
            } else if (ToggleableBetterBaseListEntry.this.isInsideCreateNew(event.x(), event.y())) {
                ToggleableBetterBaseListEntry.this.setExpanded(true);
                C cell;
                if (ToggleableBetterBaseListEntry.this.insertInFront()) {
                    ToggleableBetterBaseListEntry.this.cells.add(0, cell = (ToggleableBetterBaseListEntry.this.createNewInstance.apply(ToggleableBetterBaseListEntry.this.self())));
                    ToggleableBetterBaseListEntry.this.widgets.add(0, cell);
                } else {
                    ToggleableBetterBaseListEntry.this.cells.add(cell = (ToggleableBetterBaseListEntry.this.createNewInstance.apply(ToggleableBetterBaseListEntry.this.self())));
                    ToggleableBetterBaseListEntry.this.widgets.add(cell);
                }

                cell.onAdd();
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            } else if (ToggleableBetterBaseListEntry.this.isDeleteButtonEnabled() && ToggleableBetterBaseListEntry.this.isInsideDelete(event.x(), event.y())) {
                GuiEventListener focused = ToggleableBetterBaseListEntry.this.getFocused();
                if (ToggleableBetterBaseListEntry.this.isExpanded() && focused instanceof BetterBaseListCell) {
                    ((BetterBaseListCell)focused).onDelete();
                    ToggleableBetterBaseListEntry.this.cells.remove(focused);
                    ToggleableBetterBaseListEntry.this.widgets.remove(focused);
                    client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }

                return true;
            } else if (this.rectangle.contains(event.x(), event.y())) {
                ToggleableBetterBaseListEntry.this.setExpanded(!ToggleableBetterBaseListEntry.this.expanded);
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            } else {
                return false;
            }
        }

        public void setFocused(boolean bl) {
        }

        public boolean isFocused() {
            return false;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.rectangle.contains(mouseX, mouseY) && !ToggleableBetterBaseListEntry.this.resetWidget.isMouseOver(mouseX, mouseY) && !ToggleableBetterBaseListEntry.this.toggleWidget.isMouseOver(mouseX, mouseY);
        }
    }
}
