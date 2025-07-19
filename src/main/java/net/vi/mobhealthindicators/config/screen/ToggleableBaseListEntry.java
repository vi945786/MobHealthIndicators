package net.vi.mobhealthindicators.config.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.api.Expandable;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

public abstract class ToggleableBaseListEntry<T, C extends BaseListCell, SELF extends ToggleableBaseListEntry<T, C, SELF>> extends TooltipListEntry<List<T>> implements Expandable {
    protected static final Identifier CONFIG_TEX = Identifier.of("cloth-config2", "textures/gui/cloth_config.png");
    protected final @NotNull List<C> cells;
    protected final @NotNull List<Element> widgets;
    protected final @NotNull List<Selectable> narratables;
    protected boolean expanded;
    protected boolean insertButtonEnabled;
    protected boolean deleteButtonEnabled;
    protected boolean insertInFront;
    protected ListLabelWidget labelWidget;
    protected ClickableWidget resetWidget;
    protected ClickableWidget toggleWidget;
    protected @NotNull Function<SELF, C> createNewInstance;
    protected @NotNull Supplier<List<T>> defaultValue;
    protected @NotNull Supplier<Boolean> defaultToggle;
    protected @Nullable Text addTooltip;
    protected @Nullable Text removeTooltip;
    protected AtomicBoolean toggled = new AtomicBoolean();
    protected BiConsumer<List<T>, Boolean> saveConsumer;

    public ToggleableBaseListEntry(@NotNull Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, boolean toggled, @Nullable Supplier<List<T>> defaultValue, Supplier<Boolean> defaultToggle, @NotNull Function<SELF, C> createNewInstance, @Nullable BiConsumer<List<T>, Boolean> saveConsumer, Text resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.insertButtonEnabled = true;
        this.addTooltip = Text.translatable("text.cloth-config.list.add");
        this.removeTooltip = Text.translatable("text.cloth-config.list.remove");
        this.deleteButtonEnabled = deleteButtonEnabled;
        this.insertInFront = insertInFront;
        this.cells = Lists.newArrayList();
        this.labelWidget = new ListLabelWidget();
        this.widgets = Lists.newArrayList(new Element[]{this.labelWidget});
        this.narratables = Lists.newArrayList();
        this.resetWidget = ButtonWidget.builder(resetButtonKey, (widget) -> {
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
        }).dimensions(0, 0, MinecraftClient.getInstance().textRenderer.getWidth(resetButtonKey) + 6, 20).build();
        this.widgets.add(this.resetWidget);
        this.narratables.add(this.resetWidget);

        toggleWidget = ButtonWidget.builder(Text.empty(), (widget) -> {
            this.toggled.set(!this.toggled.get());
        }).dimensions(0, 0, 150, 20).build();
        this.widgets.add(this.toggleWidget);
        this.narratables.add(this.toggleWidget);

        this.saveCallback = null;
        this.saveConsumer = saveConsumer;
        this.createNewInstance = createNewInstance;
        this.defaultValue = defaultValue;
        this.defaultToggle = defaultToggle;
        this.toggled.set(toggled);
    }

    public boolean isExpanded() {
        return this.expanded && this.isEnabled();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isEdited() {
        return super.isEdited() ? true : this.cells.stream().anyMatch(BaseListCell::isEdited);
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
        return this.cells.stream().anyMatch(BaseListCell::isRequiresRestart);
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

    public @Nullable Text getAddTooltip() {
        return this.addTooltip;
    }

    public void setAddTooltip(@Nullable Text addTooltip) {
        this.addTooltip = addTooltip;
    }

    public @Nullable Text getRemoveTooltip() {
        return this.removeTooltip;
    }

    public void setRemoveTooltip(@Nullable Text removeTooltip) {
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

            for(BaseListCell entry : this.cells) {
                i += entry.getCellHeight();
            }

            return i;
        }
    }

    public List<? extends Element> children() {
        if (!this.isExpanded()) {
            List<Element> elements = new ArrayList(this.widgets);
            elements.removeAll(this.cells);
            return elements;
        } else {
            return this.widgets;
        }
    }

    public List<? extends Selectable> narratables() {
        return this.narratables;
    }

    public Optional<Text> getError() {
        List<Text> errors = (List)this.cells.stream().map(BaseListCell::getConfigError).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        return errors.size() > 1 ? Optional.of(Text.translatable("text.cloth-config.multi_error")) : errors.stream().findFirst();
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

    public Optional<Text[]> getTooltip(int mouseX, int mouseY) {
        if (this.addTooltip != null && this.isInsideCreateNew((double)mouseX, (double)mouseY)) {
            return Optional.of(new Text[]{this.addTooltip});
        } else {
            return this.removeTooltip != null && this.isInsideDelete((double)mouseX, (double)mouseY) ? Optional.of(new Text[]{this.removeTooltip}) : super.getTooltip(mouseX, mouseY);
        }
    }

    public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BaseListCell focused = this.isExpanded() && this.getFocused() != null && this.getFocused() instanceof BaseListCell ? (BaseListCell)this.getFocused() : null;
        boolean insideLabel = this.labelWidget.rectangle.contains(mouseX, mouseY);
        boolean insideCreateNew = this.isInsideCreateNew((double)mouseX, (double)mouseY);
        boolean insideDelete = this.isInsideDelete((double)mouseX, (double)mouseY);
        graphics.drawTexture(RenderLayer::getGuiTextured, CONFIG_TEX, x - 15, y + 5, 33.0F, (float)((this.isEnabled() ? (insideLabel && !insideCreateNew && !insideDelete ? 18 : 0) : 36) + (this.isExpanded() ? 9 : 0)), 9, 9, 256, 256);
        if (this.isInsertButtonEnabled()) {
            graphics.drawTexture(RenderLayer::getGuiTextured, CONFIG_TEX, x - 15 + 13, y + 5, 42.0F, insideCreateNew ? 9.0F : 0.0F, 9, 9, 256, 256);
        }

        if (this.isDeleteButtonEnabled()) {
            graphics.drawTexture(RenderLayer::getGuiTextured, CONFIG_TEX, x - 15 + (this.isInsertButtonEnabled() ? 26 : 13), y + 5, 51.0F, focused == null ? 0.0F : (insideDelete ? 18.0F : 9.0F), 9, 9, 256, 256);
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
        graphics.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getDisplayedFieldName().asOrderedText(), x + offset, y + 6, this.getPreferredTextColor());
        if (this.isExpanded()) {
            int yy = y + 24;

            for(BaseListCell cell : this.cells) {
                cell.render(graphics, -1, yy, x + 14, entryWidth - 14, cell.getCellHeight(), mouseX, mouseY, this.getParent().getFocused() != null && this.getParent().getFocused().equals(this) && this.getFocused() != null && this.getFocused().equals(cell), delta);
                cell.updateBounds(true, x + 14, yy, entryWidth - 14, cell.getCellHeight());
                yy += cell.getCellHeight();
            }
        } else {
            int yy = y + 24;

            for(BaseListCell cell : this.cells) {
                cell.updateBounds(false, x + 14, yy, entryWidth - 14, cell.getCellHeight());
                yy += cell.getCellHeight();
            }
        }

    }

    public Text getYesNoText(boolean bool) {
        return Text.translatable("text.cloth-config.boolean.value." + bool);
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
                for(BaseListCell cell : this.cells) {
                    if (cell.isMouseOver(mouseX, mouseY)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public class ListLabelWidget implements Element {
        protected Rectangle rectangle = new Rectangle();

        public ListLabelWidget() {
        }

        public boolean mouseClicked(double mouseX, double mouseY, int int_1) {
            if (!ToggleableBaseListEntry.this.isEnabled()) {
                return false;
            } else if (ToggleableBaseListEntry.this.resetWidget.isMouseOver(mouseX, mouseY)) {
                return false;
            } else if (ToggleableBaseListEntry.this.toggleWidget.isMouseOver(mouseX, mouseY)) {
                return false;
            } else if (ToggleableBaseListEntry.this.isInsideCreateNew(mouseX, mouseY)) {
                ToggleableBaseListEntry.this.setExpanded(true);
                C cell;
                if (ToggleableBaseListEntry.this.insertInFront()) {
                    ToggleableBaseListEntry.this.cells.add(0, cell = (ToggleableBaseListEntry.this.createNewInstance.apply(ToggleableBaseListEntry.this.self())));
                    ToggleableBaseListEntry.this.widgets.add(0, cell);
                } else {
                    ToggleableBaseListEntry.this.cells.add(cell = (ToggleableBaseListEntry.this.createNewInstance.apply(ToggleableBaseListEntry.this.self())));
                    ToggleableBaseListEntry.this.widgets.add(cell);
                }

                cell.onAdd();
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            } else if (ToggleableBaseListEntry.this.isDeleteButtonEnabled() && ToggleableBaseListEntry.this.isInsideDelete(mouseX, mouseY)) {
                Element focused = ToggleableBaseListEntry.this.getFocused();
                if (ToggleableBaseListEntry.this.isExpanded() && focused instanceof BaseListCell) {
                    ((BaseListCell)focused).onDelete();
                    ToggleableBaseListEntry.this.cells.remove(focused);
                    ToggleableBaseListEntry.this.widgets.remove(focused);
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }

                return true;
            } else if (this.rectangle.contains(mouseX, mouseY)) {
                ToggleableBaseListEntry.this.setExpanded(!ToggleableBaseListEntry.this.expanded);
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
            return this.rectangle.contains(mouseX, mouseY) && !ToggleableBaseListEntry.this.resetWidget.isMouseOver(mouseX, mouseY) && !ToggleableBaseListEntry.this.toggleWidget.isMouseOver(mouseX, mouseY);
        }
    }
}
