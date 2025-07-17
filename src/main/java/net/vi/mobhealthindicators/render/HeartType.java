package net.vi.mobhealthindicators.render;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;

public enum HeartType {
    EMPTY("container"),
    FULL("full"),
    HALF("half");

    public final String heartIcon;

    HeartType(String heartIcon) {
        this.heartIcon = heartIcon;
    }

    public record HeartColor(BufferedImage fullHeartTexture, BufferedImage halfHeartTexture) {}

    public enum Effect {
        NONE(""),
        POISON("poisoned_"),
        WITHER("withered_"),
        ABSORPTION("absorbing_"),
        FROZEN("frozen_");

        public final String prefix;

        Effect(String prefix) {
            this.prefix = prefix;
        }

        public static boolean hasAbnormalHearts(LivingEntity entity) {
            return getEffect(entity) != NONE ||  entity.hasStatusEffect(StatusEffects.ABSORPTION) || entity.hasStatusEffect(StatusEffects.HEALTH_BOOST);
        }

        public static Effect getEffect(LivingEntity entity) {
            if (entity.hasStatusEffect(StatusEffects.POISON)) {
                return POISON;
            } else if (entity.hasStatusEffect(StatusEffects.WITHER)) {
                return WITHER;
            } else if (entity.isFrozen()) {
                return FROZEN;
            } else {
                return NONE;
            }
        }
    }

    public BufferedImage getTexture(Effect effect) {
        try {
            return ImageIO.read(client.getResourceManager().getResourceOrThrow(Identifier.of("minecraft", "textures/gui/sprites/hud/heart/" + effect.prefix + heartIcon + ".png")).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}