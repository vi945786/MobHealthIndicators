package net.vi.mobhealthindicators.commands;

import net.minecraft.command.argument.EnumArgumentType;
import net.vi.mobhealthindicators.config.Config.FilteringMechanism;

public class WhiteOrBlackListArgumentType extends EnumArgumentType<FilteringMechanism> {

    private WhiteOrBlackListArgumentType() {
        super(FilteringMechanism.CODEC, FilteringMechanism::values);
    }

    public static EnumArgumentType<FilteringMechanism> whiteOrBlackList() {
        return new WhiteOrBlackListArgumentType();
    }
}
