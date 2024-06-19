package net.vi.mobhealthindicator.commands;

import net.minecraft.command.argument.EnumArgumentType;
import net.vi.mobhealthindicator.config.Config.WhiteOrBlackList;

public class WhiteOrBlackListArgumentType extends EnumArgumentType<WhiteOrBlackList> {

    private WhiteOrBlackListArgumentType() {
        super(WhiteOrBlackList.CODEC, WhiteOrBlackList::values);
    }

    public static EnumArgumentType<WhiteOrBlackList> whiteOrBlackList() {
        return new WhiteOrBlackListArgumentType();
    }
}
