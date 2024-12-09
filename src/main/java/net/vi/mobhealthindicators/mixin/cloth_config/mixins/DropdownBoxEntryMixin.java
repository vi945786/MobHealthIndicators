package net.vi.mobhealthindicators.mixin.cloth_config.mixins;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Color;
import me.shedaniel.math.Rectangle;
import me.shedaniel.math.impl.PointHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInDropdownBoxEntry;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInDropdownMenuElement;
import net.vi.mobhealthindicators.mixin.cloth_config.addmethods.AddedMethodsInSelectionElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(value = DropdownBoxEntry.class)
public abstract class DropdownBoxEntryMixin<T> extends TooltipListEntry<T> implements AddedMethodsInDropdownBoxEntry {

    @Unique protected boolean dontReFocus = false;
    @Shadow protected DropdownBoxEntry.SelectionElement<T> selectionElement;
    @Shadow protected ButtonWidget resetButton;

    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        boolean b = super.mouseClicked(double_1, double_2, int_1);
        if (dontReFocus) {
            setFocused(null);
            dontReFocus = false;
        }
        return b;
    }

    public DropdownBoxEntryMixin(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
    }

    @Unique
    public boolean isMobhealthindicators() {
        return resetButton.getMessage().getString().equals("mobhealthindicators.hidebutton");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void changeSelectionElement(Text fieldName, Text resetButtonKey, Supplier tooltipSupplier, boolean requiresRestart, Supplier defaultValue, Consumer saveConsumer, Iterable selections, DropdownBoxEntry.SelectionTopCellElement topRenderer, DropdownBoxEntry.SelectionCellCreator cellCreator, CallbackInfo ci) {
        if(isMobhealthindicators()) resetButton.setWidth(0);
        this.selectionElement = new DropdownBoxEntry.SelectionElement<>((DropdownBoxEntry) (Object) this, new Rectangle(0, 0, fieldName.getString().isBlank() ? 300 : 150, 20), new DropdownBoxEntry.DefaultDropdownMenuElement<>(selections == null ? ImmutableList.of() : ImmutableList.copyOf(selections)), topRenderer, cellCreator);
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lme/shedaniel/math/Rectangle;x:I", ordinal = 1))
    public void setBoundsX(Rectangle instance, int value, @Local(ordinal = 2, argsOnly = true) int x, @Local(ordinal = 3, argsOnly = true) int entryWidth, @Local Text displayedFieldName) {
        instance.x = x + (!displayedFieldName.getString().isBlank() ? entryWidth - 150 : 0);
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lme/shedaniel/math/Rectangle;width:I", ordinal = 0))
    public void setBoundsWidth(Rectangle instance, int value, @Local(ordinal = 3, argsOnly = true) int entryWidth, @Local Text displayedFieldName) {
        instance.width = (!displayedFieldName.getString().isBlank() ? 150 : entryWidth) - (resetButton.getWidth() == 0 ? 0 : resetButton.getWidth() -4);
    }

    @Override
    public void setDontReFocus(boolean dontReFocus) {
        this.dontReFocus = dontReFocus;
    }

    @Mixin(value = DropdownBoxEntry.DropdownMenuElement.class)
    public static abstract class DropdownMenuElementMixin<R> extends AbstractParentElement implements AddedMethodsInDropdownMenuElement {

        @Shadow public abstract @NotNull DropdownBoxEntry.SelectionCellCreator<R> getCellCreator();
        @Shadow public abstract @NotNull DropdownBoxEntry<R> getEntry();

        @Override
        public int getCellWidth() {
            return getCellCreator().getCellWidth() > 0 ? getCellCreator().getCellWidth() : ((AddedMethodsInSelectionElement) getEntry().getSelectionElement()).getBoundsWidth();
        }
    }

    @Mixin(value = DropdownBoxEntry.DefaultDropdownMenuElement.class)
    public static abstract class DefaultDropdownMenuElementMixin<R> extends DropdownBoxEntry.DropdownMenuElement<R> {

        @Shadow
        protected Rectangle lastRectangle;

        @Shadow
        @NotNull
        protected List<DropdownBoxEntry.SelectionCellElement<R>> currentElements;

        @Shadow
        protected double scroll;

        @Shadow
        protected abstract double getMaxScrollPosition();

        @Shadow protected boolean scrolling;

        @Overwrite()
        public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {
            int last10Height = this.getHeight();
            int cWidth = ((AddedMethodsInDropdownMenuElement) this).getCellWidth();
            graphics.getMatrices().push();
            graphics.getMatrices().translate(0.0F, 0.0F, 300.0F);
            graphics.fill(lastRectangle.x, lastRectangle.y + lastRectangle.height, lastRectangle.x + cWidth, lastRectangle.y + lastRectangle.height + last10Height + 1, isExpanded() ? 0xFFFFFFFF : -6250336);
            graphics.getMatrices().translate(0, 0, 300f);
            graphics.enableScissor(lastRectangle.x +1, lastRectangle.y + lastRectangle.height +1, lastRectangle.x + cWidth -1, lastRectangle.y + lastRectangle.height + last10Height);
            graphics.fill(lastRectangle.x + 1, lastRectangle.y + lastRectangle.height + 1, lastRectangle.x + cWidth - 1, lastRectangle.y + lastRectangle.height + last10Height, 0xFF000000);
            double yy = (double) (this.lastRectangle.y + this.lastRectangle.height) - this.scroll;

            for (Iterator var9 = this.currentElements.iterator(); var9.hasNext(); yy += this.getCellCreator().getCellHeight()) {
                DropdownBoxEntry.SelectionCellElement<R> cell = (DropdownBoxEntry.SelectionCellElement) var9.next();
                if (yy + (double) this.getCellCreator().getCellHeight() >= (double) (this.lastRectangle.y + this.lastRectangle.height) && yy <= (double) (this.lastRectangle.y + this.lastRectangle.height + last10Height + 1)) {
                    graphics.fill(lastRectangle.x + 1, (int) yy, lastRectangle.x + cWidth, (int) yy + getCellCreator().getCellHeight(), 0xFF000000);
                    cell.render(graphics, mouseX, mouseY, lastRectangle.x, (int) yy, getMaxScrollPosition() > 6 ? cWidth -7 : cWidth, getCellCreator().getCellHeight(), delta);
                } else {
                    cell.dontRender(graphics, delta);
                }
            }

            graphics.disableScissor();
            graphics.drawBorder(lastRectangle.x, lastRectangle.y + lastRectangle.height, cWidth, last10Height + 1, 0xFFFFFFFF);
            if (this.currentElements.isEmpty()) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                Text text = Text.translatable("text.cloth-config.dropdown.value.unknown");
                graphics.drawTextWithShadow(textRenderer, text.asOrderedText(), (int) ((float) this.lastRectangle.x + cWidth / 2.0F - (float) textRenderer.getWidth(text) / 2.0F), this.lastRectangle.y + this.lastRectangle.height + 3, -1);
            }

            if (this.getMaxScrollPosition() > 6.0) {
                int scrollbarPositionMinX = this.lastRectangle.x + cWidth - 7;
                int scrollbarPositionMaxX = scrollbarPositionMinX + 6;
                int height = (int) ((double) (last10Height * last10Height) / this.getMaxScrollPosition());
                height = MathHelper.clamp(height, 32, last10Height - 8);
                height = (int) ((double) height - Math.min(this.scroll < 0.0 ? (double) ((int) (-this.scroll)) : (this.scroll > this.getMaxScrollPosition() ? (double) ((int) this.scroll) - this.getMaxScrollPosition() : 0.0), (double) height * 0.95));
                height = Math.max(10, height);
                int minY = (int) Math.min(Math.max((double) ((int) this.scroll * (last10Height - height)) / this.getMaxScrollPosition() + (double) (this.lastRectangle.y + this.lastRectangle.height + 1), this.lastRectangle.y + this.lastRectangle.height + 1), this.lastRectangle.y + this.lastRectangle.height + 1 + last10Height - height);
                int bottomc = (new Rectangle(scrollbarPositionMinX, minY +1, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse()) ? 168 : 128;
                int topc = (new Rectangle(scrollbarPositionMinX, minY +1, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse()) ? 222 : 172;

                graphics.fill(scrollbarPositionMinX, minY, scrollbarPositionMaxX, minY + height -1, Color.ofRGBA(bottomc, bottomc, bottomc, 255).getColor());
                graphics.fill(scrollbarPositionMinX, minY, scrollbarPositionMaxX -1, minY + height -2, Color.ofRGBA(topc, topc, topc, 255).getColor());
            }

            graphics.getMatrices().pop();
        }

        @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
        public void overwriteIsMouseOver(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
            int cWidth = ((AddedMethodsInDropdownMenuElement) this).getCellWidth();
            cir.setReturnValue(isExpanded() && mouseX >= lastRectangle.x && mouseX <= lastRectangle.x + cWidth && mouseY >= lastRectangle.y + lastRectangle.height && mouseY <= lastRectangle.y + lastRectangle.height + getHeight() + 1);
        }

        @Inject(method = "updateScrollingState", at = @At("HEAD"), remap = false)
        protected void overwriteUpdateScrollingState(double double_1, double double_2, int int_1, CallbackInfo ci) {
            int cWidth = ((AddedMethodsInDropdownMenuElement) this).getCellWidth();
            this.scrolling = isExpanded() && lastRectangle != null && int_1 == 0 && double_1 >= (double) lastRectangle.x + cWidth - 6 && double_1 < (double) (lastRectangle.x + cWidth);
        }

        @Inject(method = "mouseClicked", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
        public void mouseClicked(double double_1, double double_2, int int_1, CallbackInfoReturnable<Boolean> cir) {
            if(!isMouseOver(double_1, double_2)) {
                ((AddedMethodsInDropdownBoxEntry) getEntry()).setDontReFocus(true);
                getEntry().setFocused(null);
                cir.setReturnValue(true);
            } else {
                boolean elementClicked = super.mouseClicked(double_1, double_2, int_1);
                if(elementClicked) {
                    ((AddedMethodsInDropdownBoxEntry) getEntry()).setDontReFocus(true);
                    getEntry().setFocused(null);
                }
                cir.setReturnValue(elementClicked || scrolling);
            }
        }
    }

    @Mixin(value = DropdownBoxEntry.SelectionCellCreator.class)
    public static abstract class SelectionCellCreatorMixin<R> {

        @ModifyConstant(method = "getCellWidth", constant = @Constant(intValue = 132), remap = false)
        public int getCellWidth(int constant) {
            return -1;
        }
    }

    @Mixin(value = DropdownBoxEntry.SelectionElement.class)
    public abstract static class SelectionElementMixin<R> extends AbstractParentElement implements Drawable, AddedMethodsInSelectionElement<R> {

        @Shadow protected Rectangle bounds;
        @Shadow protected DropdownBoxEntry.SelectionTopCellElement<R> topRenderer;
        @Shadow protected DropdownBoxEntry.DropdownMenuElement<R> menu;

        @Override
        public int getBoundsWidth() {
            return bounds.width;
        }

        @Override
        public void setTopRendererValue(R value) {
            topRenderer.setValue(value);
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        public void mouseClicked(double double_1, double double_2, int int_1, CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(super.mouseClicked(double_1, double_2, int_1));
        }
    }

    @Mixin(value = DropdownBoxEntry.DefaultSelectionCellElement.class)
    public abstract static class DefaultSelectionCellElementMixin<R> extends DropdownBoxEntry.SelectionCellElement<R> {

        @Shadow protected boolean rendering;
        @Shadow protected int x;
        @Shadow protected int width;
        @Shadow protected int y;
        @Shadow protected int height;
        @Shadow protected R r;

        @Override
        public boolean isMouseOver(double d, double e) {
            return d >= x && d <= x + width -1 && e >= y && e <= y + height -1;
        }

        @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
        public void render(DrawContext graphics, int x1, int y1, int x2, int y2, int color, @Local(ordinal = 0, argsOnly = true) int mouseX, @Local(ordinal = 1, argsOnly = true) int mouseY) {
            boolean b = mouseX >= x && mouseX <= x + width -1 && mouseY >= y && mouseY <= y + height -1;
            if (b) {
                graphics.fill(x +1, y, x + width -1, y + height, -15132391);
            }
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        public void mouseClicked(double mouseX, double mouseY, int int_1, CallbackInfoReturnable<Boolean> cir) {
            boolean b = rendering && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            if (b) {
                ((AddedMethodsInSelectionElement) getEntry().getSelectionElement()).setTopRendererValue(r);
                getEntry().setFocused(null);
                ((AddedMethodsInDropdownBoxEntry) getEntry()).setDontReFocus(true);
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
