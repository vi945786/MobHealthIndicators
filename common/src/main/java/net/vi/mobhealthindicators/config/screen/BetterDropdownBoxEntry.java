package net.vi.mobhealthindicators.config.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.Window;
import me.shedaniel.clothconfig2.ClothConfigInitializer;
import me.shedaniel.clothconfig2.api.ScrollingContainer;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import static me.shedaniel.clothconfig2.api.scroll.ScrollingContainer.SCROLLER_SPRITE;
import static net.vi.mobhealthindicators.ModInit.client;

public class BetterDropdownBoxEntry<T> extends TooltipListEntry<T> {
    protected AbstractWidget resetButton;
    protected BetterDropdownBoxEntry.SelectionElement<T> selectionElement;
    private final @NotNull Supplier<T> defaultValue;
    private boolean suggestionMode = true;
    protected boolean dontReFocus = false;

    public BetterDropdownBoxEntry(Component fieldName, @NotNull Component resetButtonKey, @Nullable Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart, @Nullable Supplier<T> defaultValue, @Nullable Consumer<T> saveConsumer, @Nullable Iterable<T> selections, @NotNull BetterDropdownBoxEntry.SelectionTopCellElement<T> topRenderer, @NotNull BetterDropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.defaultValue = defaultValue;
        this.saveCallback = saveConsumer;
        this.resetButton = Button.builder(resetButtonKey, (widget) -> this.selectionElement.topRenderer.setValue(defaultValue.get())).bounds(0, 0, client.font.width(resetButtonKey) + 6, 20).build();
        this.selectionElement = new SelectionElement<>(this, new Rectangle(0, 0, fieldName.getString().isBlank() ? 300 : 150, 20), new DefaultDropdownMenuElement<>(selections == null ? ImmutableList.of() : ImmutableList.copyOf(selections)), topRenderer, cellCreator);
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = client.getWindow();
        this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() && (!this.defaultValue.get().equals(this.getValue()) || this.getConfigError().isPresent());
        this.resetButton.setY(y);
        this.selectionElement.active = this.isEditable();
        this.selectionElement.bounds.y = y;
        Component displayedFieldName = this.getDisplayedFieldName();
        boolean hasName = !displayedFieldName.getString().isBlank();
        if (client.font.isBidirectional()) {
            graphics.drawString(client.font, displayedFieldName.getVisualOrderText(), window.getGuiScaledWidth() - x - client.font.width(displayedFieldName), y + 6, this.getPreferredTextColor());
            this.resetButton.setX(x);
            this.selectionElement.bounds.x = x + this.resetButton.getWidth() + 1;
        } else {
            graphics.drawString(client.font, displayedFieldName.getVisualOrderText(), x, y + 6, this.getPreferredTextColor());
            this.resetButton.setX(x + entryWidth - this.resetButton.getWidth());
            this.selectionElement.bounds.x = x + (hasName ? entryWidth - 150 : 0) + 1;
        }

        this.selectionElement.bounds.width = (hasName ? 150 : entryWidth) - resetButton.getWidth() - 4;
        this.resetButton.render(graphics, mouseX, mouseY, delta);
        this.selectionElement.render(graphics, mouseX, mouseY, delta);
    }

    public boolean isEdited() {
        return this.selectionElement.topRenderer.isEdited();
    }

    public boolean isSuggestionMode() {
        return this.suggestionMode;
    }

    public void setSuggestionMode(boolean suggestionMode) {
        this.suggestionMode = suggestionMode;
    }

    public void updateSelected(boolean isSelected) {
        this.selectionElement.topRenderer.isSelected = isSelected;
        this.selectionElement.menu.isSelected = isSelected;
    }

    public @NotNull ImmutableList<T> getSelections() {
        return this.selectionElement.menu.getSelections();
    }

    public T getValue() {
        return this.selectionElement.getValue();
    }

    /** @deprecated */
    @Deprecated
    public BetterDropdownBoxEntry.SelectionElement<T> getSelectionElement() {
        return this.selectionElement;
    }

    public Optional<T> getDefaultValue() {
        return this.defaultValue == null ? Optional.empty() : Optional.ofNullable(this.defaultValue.get());
    }

    public List<? extends GuiEventListener> children() {
        return Lists.newArrayList(this.selectionElement, this.resetButton);
    }

    public List<? extends NarratableEntry> narratables() {
        return Collections.singletonList(this.resetButton);
    }

    public Optional<Component> getError() {
        return this.selectionElement.topRenderer.getError();
    }

    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.selectionElement.lateRender(graphics, mouseX, mouseY, delta);
    }

    public int getMorePossibleHeight() {
        return this.selectionElement.getMorePossibleHeight();
    }

    public boolean mouseScrolled(double double_1, double double_2, double amountX, double amountY) {
        return this.selectionElement.mouseScrolled(double_1, double_2, amountX, amountY);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || this.selectionElement.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean b = super.mouseClicked(event, doubleClick);
        if (dontReFocus) {
            setFocused(null);
            dontReFocus = false;
        }
        return b;
    }

    public static class SelectionElement<R> extends AbstractContainerEventHandler implements Renderable {
        protected Rectangle bounds;
        protected boolean active;
        protected BetterDropdownBoxEntry.SelectionTopCellElement<R> topRenderer;
        protected BetterDropdownBoxEntry<R> entry;
        protected BetterDropdownBoxEntry.DropdownMenuElement<R> menu;

        public SelectionElement(BetterDropdownBoxEntry<R> entry, Rectangle bounds, BetterDropdownBoxEntry.DropdownMenuElement<R> menu, BetterDropdownBoxEntry.SelectionTopCellElement<R> topRenderer, BetterDropdownBoxEntry.SelectionCellCreator<R> cellCreator) {
            this.bounds = bounds;
            this.entry = entry;
            this.menu = Objects.requireNonNull(menu);
            this.menu.entry = entry;
            this.menu.cellCreator = Objects.requireNonNull(cellCreator);
            this.menu.initCells();
            this.topRenderer = Objects.requireNonNull(topRenderer);
            this.topRenderer.entry = entry;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(this.bounds.x, this.bounds.y, this.bounds.x + this.bounds.width, this.bounds.y + this.bounds.height, this.topRenderer.isSelected ? -1 : -6250336);
            graphics.fill(this.bounds.x + 1, this.bounds.y + 1, this.bounds.x + this.bounds.width - 1, this.bounds.y + this.bounds.height - 1, -16777216);
            this.topRenderer.render(graphics, mouseX, mouseY, this.bounds.x, this.bounds.y, this.bounds.width, this.bounds.height, delta);
            this.topRenderer.updateBounds(this.bounds);
            if (this.menu.isExpanded()) {
                this.menu.render(graphics, mouseX, mouseY, this.bounds, delta);
            }

        }

        /** @deprecated */
        @Deprecated
        public BetterDropdownBoxEntry.SelectionTopCellElement<R> getTopRenderer() {
            return this.topRenderer;
        }

        public boolean mouseScrolled(double double_1, double double_2, double amountX, double amountY) {
            return this.menu.isExpanded() ? this.menu.mouseScrolled(double_1, double_2, amountX, amountY) : false;
        }

        public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (this.menu.isExpanded()) {
                this.menu.lateRender(graphics, mouseX, mouseY, delta);
            }

        }

        public int getMorePossibleHeight() {
            return this.menu.isExpanded() ? this.menu.getHeight() : -1;
        }

        public R getValue() {
            return this.topRenderer.getValue();
        }

        public List<? extends GuiEventListener> children() {
            return Lists.newArrayList(this.topRenderer, this.menu);
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.bounds.contains(mouseX, mouseY) || this.menu.isExpanded() && this.menu.isMouseOver(mouseX, mouseY);
        }

        public int getBoundsWidth() {
            return bounds.width;
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return super.mouseClicked(event, doubleClick);
        }
    }

    public abstract static class DropdownMenuElement<R> extends AbstractContainerEventHandler {
        /** @deprecated */
        @Deprecated
        private @NotNull BetterDropdownBoxEntry.SelectionCellCreator<R> cellCreator;
        /** @deprecated */
        @Deprecated
        private @NotNull BetterDropdownBoxEntry<R> entry;
        private boolean isSelected;

        public DropdownMenuElement() {
        }

        public @NotNull BetterDropdownBoxEntry.SelectionCellCreator<R> getCellCreator() {
            return this.cellCreator;
        }

        public int getCellWidth() {
            return getCellCreator().getCellWidth() > 0 ? getCellCreator().getCellWidth() : getEntry().getSelectionElement().getBoundsWidth();
        }

        public final @NotNull BetterDropdownBoxEntry<R> getEntry() {
            return this.entry;
        }

        public abstract @NotNull ImmutableList<R> getSelections();

        public abstract void initCells();

        public abstract void render(GuiGraphics var1, int var2, int var3, Rectangle var4, float var5);

        public abstract void lateRender(GuiGraphics var1, int var2, int var3, float var4);

        public abstract int getHeight();

        public final boolean isExpanded() {
            return this.isSelected && this.getEntry().getFocused() == this.getEntry().selectionElement;
        }

        public final boolean isSuggestionMode() {
            return this.entry.isSuggestionMode();
        }

        public abstract List<BetterDropdownBoxEntry.SelectionCellElement<R>> children();
    }

    public static class DefaultDropdownMenuElement<R> extends BetterDropdownBoxEntry.DropdownMenuElement<R> {
        protected @NotNull ImmutableList<R> selections;
        protected @NotNull List<BetterDropdownBoxEntry.SelectionCellElement<R>> cells;
        protected @NotNull List<BetterDropdownBoxEntry.SelectionCellElement<R>> currentElements;
        protected Component lastSearchKeyword = Component.empty();
        protected Rectangle lastRectangle;
        protected boolean scrolling;
        protected double scroll;
        protected double target;
        protected long start;
        protected long duration;

        public DefaultDropdownMenuElement(@NotNull ImmutableList<R> selections) {
            this.selections = selections;
            this.cells = Lists.newArrayList();
            this.currentElements = Lists.newArrayList();
        }

        public double getMaxScroll() {
            return (this.getCellCreator().getCellHeight() * this.currentElements.size());
        }

        protected double getMaxScrollPosition() {
            return Math.max(0.0F, this.getMaxScroll() - (double)this.getHeight());
        }

        public @NotNull ImmutableList<R> getSelections() {
            return this.selections;
        }

        public void initCells() {
            UnmodifiableIterator var1 = this.getSelections().iterator();

            while(var1.hasNext()) {
                R selection = (R)var1.next();
                this.cells.add(this.getCellCreator().create(selection));
            }

            for(BetterDropdownBoxEntry.SelectionCellElement<R> cell : this.cells) {
                cell.entry = this.getEntry();
            }

            this.search();
        }

        public void search() {
            if (this.isSuggestionMode()) {
                this.currentElements.clear();
                String keyword = this.lastSearchKeyword.getString().toLowerCase();

                for(BetterDropdownBoxEntry.SelectionCellElement<R> cell : this.cells) {
                    Component key = cell.getSearchKey();
                    if (key == null || key.getString().toLowerCase().contains(keyword)) {
                        this.currentElements.add(cell);
                    }
                }

                if (!keyword.isEmpty()) {
                    Comparator<BetterDropdownBoxEntry.SelectionCellElement<?>> c = Comparator.comparingDouble((i) -> i.getSearchKey() == null ? Double.MAX_VALUE : this.similarity(i.getSearchKey().getString(), keyword));
                    this.currentElements.sort(c.reversed());
                }

                this.scrollTo((double)0.0F, false);
            } else {
                this.currentElements.clear();
                this.currentElements.addAll(this.cells);
            }

        }

        protected int editDistance(String s1, String s2) {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();
            int[] costs = new int[s2.length() + 1];

            for(int i = 0; i <= s1.length(); ++i) {
                int lastValue = i;

                for(int j = 0; j <= s2.length(); ++j) {
                    if (i == 0) {
                        costs[j] = j;
                    } else if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }

                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }

                if (i > 0) {
                    costs[s2.length()] = lastValue;
                }
            }

            return costs[s2.length()];
        }

        protected double similarity(String s1, String s2) {
            String longer = s1;
            String shorter = s2;
            if (s1.length() < s2.length()) {
                longer = s2;
                shorter = s1;
            }

            int longerLength = longer.length();
            return longerLength == 0 ? (double)1.0F : (double)(longerLength - this.editDistance(longer, shorter)) / (double)longerLength;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, Rectangle rectangle, float delta) {
            if (!this.getEntry().selectionElement.topRenderer.getSearchTerm().equals(this.lastSearchKeyword)) {
                this.lastSearchKeyword = this.getEntry().selectionElement.topRenderer.getSearchTerm();
                this.search();
            }

            this.updatePosition(delta);
            this.lastRectangle = rectangle.clone();
            this.lastRectangle.translate(0, -1);
        }

        private void updatePosition(float delta) {
            double[] target = new double[]{this.target};
            this.scroll = ScrollingContainer.handleScrollingPosition(target, this.scroll, this.getMaxScrollPosition(), delta, (double)this.start, (double)this.duration);
            this.target = target[0];
        }

        public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int last10Height = this.getHeight();
            int cWidth = getCellWidth();
            graphics.fill(this.lastRectangle.x, this.lastRectangle.y + this.lastRectangle.height, this.lastRectangle.x + cWidth, this.lastRectangle.y + this.lastRectangle.height + last10Height + 1, this.isExpanded() ? -1 : -6250336);
            graphics.fill(this.lastRectangle.x + 1, this.lastRectangle.y + this.lastRectangle.height + 1, this.lastRectangle.x + cWidth - 1, this.lastRectangle.y + this.lastRectangle.height + last10Height, -16777216);
            graphics.enableScissor(this.lastRectangle.x, this.lastRectangle.y + this.lastRectangle.height + 1, this.lastRectangle.x + cWidth - 6, this.lastRectangle.y + this.lastRectangle.height + last10Height);
            double yy = (double)(this.lastRectangle.y + this.lastRectangle.height) - this.scroll + 1;

            for(BetterDropdownBoxEntry.SelectionCellElement<R> cell : this.currentElements) {
                if (yy + (double)this.getCellCreator().getCellHeight() >= (double)(this.lastRectangle.y + this.lastRectangle.height) && yy <= (double)(this.lastRectangle.y + this.lastRectangle.height + last10Height + 1)) {
                    graphics.fill(lastRectangle.x + 1, (int) yy, lastRectangle.x + cWidth, (int) yy + getCellCreator().getCellHeight(), 0xFF000000);
                    cell.bounds.setBounds(this.lastRectangle.x, (int)yy, this.getMaxScrollPosition() > (double)6.0F ? this.getCellCreator().getCellWidth() - 6 : this.getCellCreator().getCellWidth(), this.getCellCreator().getCellHeight());
                    cell.render(graphics, mouseX, mouseY, lastRectangle.x, (int) yy, getMaxScrollPosition() > 6 ? cWidth - 6 : cWidth, getCellCreator().getCellHeight(), delta);
                } else {
                    cell.bounds.setBounds(0, 0, 0, 0);
                    cell.dontRender(graphics, delta);
                }

                yy += this.getCellCreator().getCellHeight();
            }

            graphics.disableScissor();
            if (this.currentElements.isEmpty()) {
                Font font = client.font;
                Component text = Component.translatable("text.cloth-config.dropdown.value.unknown");
                graphics.drawString(font, text.getVisualOrderText(), (int)((float)this.lastRectangle.x + (float)cWidth / 2.0F - (float)font.width(text) / 2.0F), this.lastRectangle.y + this.lastRectangle.height + 3, -1);
            }

            if (this.getMaxScrollPosition() > (double)6.0F) {
                int scrollbarPositionMinX = this.lastRectangle.x + cWidth - 6;
                int scrollbarPositionMaxX = scrollbarPositionMinX + 6;
                int height = (int)((double)(last10Height * last10Height) / this.getMaxScrollPosition());
                height = Mth.clamp(height, 32, last10Height - 8);
                height = (int)((double)height - Math.min(this.scroll < (double)0.0F ? (double)((int)(-this.scroll)) : (this.scroll > this.getMaxScrollPosition() ? (double)((int)this.scroll) - this.getMaxScrollPosition() : (double)0.0F), (double)height * 0.95));
                height = Math.max(10, height);
                int minY = (int)Math.min(Math.max((double)((int)this.scroll * (last10Height - height)) / this.getMaxScrollPosition() + (double)(this.lastRectangle.y + this.lastRectangle.height + 1), (double)(this.lastRectangle.y + this.lastRectangle.height + 1)), (double)(this.lastRectangle.y + this.lastRectangle.height + 1 + last10Height - height));
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollbarPositionMinX, minY, 6, height);
            }

        }

        public int getHeight() {
            return Math.max(Math.min(this.getCellCreator().getDropBoxMaxHeight(), (int)this.getMaxScroll()), 14);
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.isExpanded() && mouseX >= (double)this.lastRectangle.x && mouseX <= (double)(this.lastRectangle.x + getCellWidth()) && mouseY >= (double)(this.lastRectangle.y + this.lastRectangle.height) && mouseY <= (double)(this.lastRectangle.y + this.lastRectangle.height + this.getHeight() + 1);
        }

        public boolean mouseDragged(MouseButtonEvent event, double double_3, double double_4) {
            if (!this.isExpanded()) {
                return false;
            } else if (event.button() == 0 && this.scrolling) {
                if (event.y() < (double)this.lastRectangle.y + (double)this.lastRectangle.height) {
                    this.scrollTo(0.0F, false);
                } else if (event.y() > (double)this.lastRectangle.y + (double)this.lastRectangle.height + (double)this.getHeight()) {
                    this.scrollTo(this.getMaxScrollPosition(), false);
                } else {
                    double double_5 = Math.max(1.0F, this.getMaxScrollPosition());
                    int int_2 = this.getHeight();
                    int int_3 = Mth.clamp((int)((float)(int_2 * int_2) / (float)this.getMaxScrollPosition()), 32, int_2 - 8);
                    double double_6 = Math.max(1.0F, double_5 / (double)(int_2 - int_3));
                    this.offset(double_4 * double_6, false);
                }

                this.target = Mth.clamp(this.target, 0.0F, this.getMaxScrollPosition());
                return true;
            } else {
                return false;
            }
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
            if (this.isMouseOver(mouseX, mouseY) && amountY != (double)0.0F) {
                this.offset(ClothConfigInitializer.getScrollStep() * -amountY, true);
                return true;
            } else {
                return false;
            }
        }

        protected void updateScrollingState(double double_1, double double_2, int int_1) {
            this.scrolling = isExpanded() && lastRectangle != null && int_1 == 0 && double_1 >= (double) lastRectangle.x + getCellWidth() - 6 && double_1 < (double) (lastRectangle.x + getCellWidth());
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.isExpanded()) {
                return false;
            } else {
                this.updateScrollingState(event.x(), event.y(), event.button());

                if(!isMouseOver(event.x(), event.y())) {
                    getEntry().dontReFocus = true;
                    getEntry().setFocused(null);
                    return true;
                } else {
                    boolean elementClicked = super.mouseClicked(event, doubleClick);
                    if(elementClicked) {
                        getEntry().dontReFocus = true;
                        getEntry().setFocused(null);
                    }
                    return elementClicked || scrolling;
                }
            }
        }

        public void offset(double value, boolean animated) {
            this.scrollTo(this.target + value, animated);
        }

        public void scrollTo(double value, boolean animated) {
            this.scrollTo(value, animated, ClothConfigInitializer.getScrollDuration());
        }

        public void scrollTo(double value, boolean animated, long duration) {
            this.target = ScrollingContainer.clampExtension(value, this.getMaxScrollPosition());
            if (animated) {
                this.start = System.currentTimeMillis();
                this.duration = duration;
            } else {
                this.scroll = this.target;
            }

        }

        public List<BetterDropdownBoxEntry.SelectionCellElement<R>> children() {
            return this.currentElements;
        }
    }

    public abstract static class SelectionCellCreator<R> {
        public SelectionCellCreator() {
        }

        public abstract BetterDropdownBoxEntry.SelectionCellElement<R> create(R var1);

        public abstract int getCellHeight();

        public abstract int getDropBoxMaxHeight();

        public int getCellWidth() {
            return -1;
        }
    }

    public static class DefaultSelectionCellCreator<R> extends BetterDropdownBoxEntry.SelectionCellCreator<R> {
        protected Function<R, Component> toTextFunction;

        public DefaultSelectionCellCreator(Function<R, Component> toTextFunction) {
            this.toTextFunction = toTextFunction;
        }

        public DefaultSelectionCellCreator() {
            this((r) -> Component.literal(r.toString()));
        }

        public BetterDropdownBoxEntry.SelectionCellElement<R> create(R selection) {
            return new BetterDropdownBoxEntry.DefaultSelectionCellElement<>(selection, this.toTextFunction);
        }

        public int getCellHeight() {
            return 14;
        }

        public int getDropBoxMaxHeight() {
            return this.getCellHeight() * 7;
        }
    }

    public abstract static class SelectionCellElement<R> extends AbstractContainerEventHandler {
        /** @deprecated */
        @Deprecated
        final Rectangle bounds = new Rectangle();
        /** @deprecated */
        @Deprecated
        private @NotNull BetterDropdownBoxEntry<R> entry;

        public SelectionCellElement() {
        }

        public final @NotNull BetterDropdownBoxEntry<R> getEntry() {
            return this.entry;
        }

        public abstract void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8);

        public abstract void dontRender(GuiGraphics var1, float var2);

        public abstract @Nullable Component getSearchKey();

        public abstract @Nullable R getSelection();

        public boolean isMouseOver(double d, double e) {
            return this.bounds.contains(d, e);
        }
    }

    public static class DefaultSelectionCellElement<R> extends BetterDropdownBoxEntry.SelectionCellElement<R> {
        protected R r;
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected boolean rendering;
        protected Function<R, Component> toTextFunction;

        public DefaultSelectionCellElement(R r, Function<R, Component> toTextFunction) {
            this.r = r;
            this.toTextFunction = toTextFunction;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
            this.rendering = true;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            boolean b = mouseX >= x && mouseX <= x + width -1 && mouseY >= y && mouseY <= y + height -1;
            if (b) {
                graphics.fill(x +1, y, x + width -1, y + height, -15132391);
            }

            graphics.drawString(client.font, ((Component)this.toTextFunction.apply(this.r)).getVisualOrderText(), x + 6, y + 3, b ? -1 : -7829368);
        }

        public void dontRender(GuiGraphics graphics, float delta) {
            this.rendering = false;
        }

        public @Nullable Component getSearchKey() {
            return (Component)this.toTextFunction.apply(this.r);
        }

        public @Nullable R getSelection() {
            return this.r;
        }

        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            boolean b = rendering && event.x() >= x && event.x() <= x + width && event.y() >= y && event.y() <= y + height;
            if (b) {
                getEntry().getSelectionElement().topRenderer.setValue(r);
                getEntry().setFocused(null);
                getEntry().dontReFocus = true;
                return true;
            } else {
                return false;
            }
        }

        public boolean isMouseOver(double d, double e) {
            return d >= x && d <= x + width -1 && e >= y && e <= y + height -1;
        }
    }

    public abstract static class SelectionTopCellElement<R> extends AbstractContainerEventHandler {
        /** @deprecated */
        @Deprecated
        private final Rectangle bounds = new Rectangle();
        /** @deprecated */
        @Deprecated
        private BetterDropdownBoxEntry<R> entry;
        protected boolean isSelected = false;

        public SelectionTopCellElement() {
        }

        public abstract R getValue();

        public abstract void setValue(R var1);

        public abstract Component getSearchTerm();

        public boolean isEdited() {
            return this.getConfigError().isPresent();
        }

        public abstract Optional<Component> getError();

        public final Optional<Component> getConfigError() {
            return this.entry.getConfigError();
        }

        public BetterDropdownBoxEntry<R> getParent() {
            return this.entry;
        }

        public final boolean hasConfigError() {
            return this.getConfigError().isPresent();
        }

        public final boolean hasError() {
            return this.getError().isPresent();
        }

        public final int getPreferredTextColor() {
            return this.getConfigError().isPresent() ? -43691 : -1;
        }

        public final boolean isSuggestionMode() {
            return this.getParent().isSuggestionMode();
        }

        public void selectFirstRecommendation() {
            for(BetterDropdownBoxEntry.SelectionCellElement<R> child : this.getParent().selectionElement.menu.children()) {
                if (child.getSelection() != null) {
                    this.setValue(child.getSelection());
                    this.getParent().selectionElement.setFocused(null);
                    break;
                }
            }

        }

        public abstract void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8);

        private void updateBounds(Rectangle bounds) {
            this.bounds.setBounds(bounds);
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.bounds.contains(mouseX, mouseY);
        }
    }

    public static class DefaultSelectionTopCellElement<R> extends BetterDropdownBoxEntry.SelectionTopCellElement<R> {
        protected EditBox textFieldWidget;
        protected Function<String, R> toObjectFunction;
        protected Function<R, Component> toTextFunction;
        protected final R original;
        protected R value;

        public DefaultSelectionTopCellElement(R value, Function<String, R> toObjectFunction, Function<R, Component> toTextFunction) {
            this.original = (R)Objects.requireNonNull(value);
            this.value = (R)Objects.requireNonNull(value);
            this.toObjectFunction = Objects.requireNonNull(toObjectFunction);
            this.toTextFunction = Objects.requireNonNull(toTextFunction);
            this.textFieldWidget = new EditBox(client.font, 0, 0, 148, 18, Component.empty()) {
                public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    this.setFocused(BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.isSuggestionMode() && BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.isSelected && BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.getParent().getFocused() == BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.getParent().selectionElement && BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.getParent().selectionElement.getFocused() == BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this && BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.getFocused() == this);
                    super.renderWidget(graphics, mouseX, mouseY, delta);
                }

                public boolean keyPressed(KeyEvent event) {
                    if (event.key() != GLFW.GLFW_KEY_ENTER && event.key() != GLFW.GLFW_KEY_KP_ENTER) {
                        return BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.isSuggestionMode() && super.keyPressed(event);
                    } else {
                        BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.selectFirstRecommendation();
                        return true;
                    }
                }

                public boolean charTyped(CharacterEvent event) {
                    return BetterDropdownBoxEntry.DefaultSelectionTopCellElement.this.isSuggestionMode() && super.charTyped(event);
                }
            };
            this.textFieldWidget.setBordered(false);
            this.textFieldWidget.setMaxLength(999999);
            this.textFieldWidget.setValue((toTextFunction.apply(value)).getString());
        }

        public boolean isEdited() {
            return super.isEdited() || !this.getValue().equals(this.original);
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
            this.textFieldWidget.setX(x + 4);
            this.textFieldWidget.setY(y + 6);
            this.textFieldWidget.setWidth(width - 8);
            this.textFieldWidget.setEditable(this.getParent().isEditable());
            this.textFieldWidget.setTextColor(this.getPreferredTextColor());
            this.textFieldWidget.render(graphics, mouseX, mouseY, delta);
        }

        public R getValue() {
            return this.hasError() ? this.value : this.toObjectFunction.apply(this.textFieldWidget.getValue());
        }

        public void setValue(R value) {
            this.textFieldWidget.setValue((this.toTextFunction.apply(value)).getString());
            this.textFieldWidget.moveCursorTo(0, false);
        }

        public Component getSearchTerm() {
            return Component.literal(this.textFieldWidget.getValue());
        }

        public Optional<Component> getError() {
            return this.toObjectFunction.apply(this.textFieldWidget.getValue()) != null ? Optional.empty() : Optional.of(Component.literal("Invalid Value!"));
        }

        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.textFieldWidget);
        }
    }
}

