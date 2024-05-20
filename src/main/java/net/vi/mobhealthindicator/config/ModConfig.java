package net.vi.mobhealthindicator.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.vi.mobhealthindicator.MobHealthIndicator;

import java.util.Arrays;

import static me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON;

@Config(name = MobHealthIndicator.modId)
public class ModConfig implements ConfigData {

    public boolean showHearts = true;
    public boolean dynamicBrightness = true;
    @ConfigEntry.Gui.EnumHandler(option=BUTTON) public WhiteOrBlackList filteringMechanism = WhiteOrBlackList.BLACK_LIST;
    public String[] blackList = new String[] {"minecraft:armor_stand"};
    public String[] whiteList = new String[] {"minecraft:player"};


    public boolean shouldRender(LivingEntity livingEntity) {
        if(!showHearts) return false;

        return switch (filteringMechanism) {
            case BLACK_LIST -> Arrays.stream(blackList).noneMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case WHITE_LIST -> Arrays.stream(whiteList).anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case NONE -> true;
        };
    }

    public enum WhiteOrBlackList {
        BLACK_LIST("BLACK_LIST"),
        WHITE_LIST("WHITE_LIST"),
        NONE("NONE");

        public final String name;

        WhiteOrBlackList(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return I18n.translate("text.autoconfig.mobhealthindicator.WhiteOrBlackList." + this.name());
        }
    }
}
