package net.vi.mobhealthindicator.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public record RenderData<T extends LivingEntity>(T livingEntity, MatrixStack matrixStack, Renderer renderer, double distance, float maxX, int heartsTotal, int heartsRed, boolean lastRedHalf, int heartsYellow, boolean lastYellowHalf, double heartDensity) {

}
