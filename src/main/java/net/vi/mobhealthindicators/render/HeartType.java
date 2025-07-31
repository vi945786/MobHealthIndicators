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
        none(""),
        poison("poisoned_"),
        wither("withered_"),
        absorption("absorbing_"),
        frozen("frozen_");

        public final String prefix;

        Effect(String prefix) {
            this.prefix = prefix;
        }

        public static boolean hasAbnormalHearts(LivingEntity entity) {
            return getEffect(entity) != none ||  entity.hasStatusEffect(StatusEffects.ABSORPTION) || entity.hasStatusEffect(StatusEffects.HEALTH_BOOST);
        }

        public static Effect getEffect(LivingEntity entity) {
            if (entity.hasStatusEffect(StatusEffects.POISON)) {
                return poison;
            } else if (entity.hasStatusEffect(StatusEffects.WITHER)) {
                return wither;
            } else if (entity.isFrozen()) {
                return frozen;
            } else {
                return none;
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