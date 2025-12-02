package net.vi.mobhealthindicators.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static net.vi.mobhealthindicators.ModInit.client;

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
            return getEffect(entity) != none ||  entity.hasEffect(MobEffects.ABSORPTION) || entity.hasEffect(MobEffects.HEALTH_BOOST);
        }

        public static Effect getEffect(LivingEntity entity) {
            if (entity.hasEffect(MobEffects.POISON)) {
                return poison;
            } else if (entity.hasEffect(MobEffects.WITHER)) {
                return wither;
            } else if (entity.isFullyFrozen()) {
                return frozen;
            } else {
                return none;
            }
        }
    }

    public BufferedImage getTexture(Effect effect) {
        try {
            return ImageIO.read(client.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/" + effect.prefix + heartIcon + ".png")).open());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}