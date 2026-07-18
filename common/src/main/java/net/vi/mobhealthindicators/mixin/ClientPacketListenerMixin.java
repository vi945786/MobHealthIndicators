package net.vi.mobhealthindicators.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.commands.HelpCommand;
import net.vi.mobhealthindicators.commands.RegisterCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;

import static net.vi.mobhealthindicators.ModInit.client;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {

    @Shadow
    private CommandDispatcher<SharedSuggestionProvider> commands;

    @Unique
    private static final CommandDispatcher<CommandSourceStack> mobHealthIndicators$mhiCommands = new CommandDispatcher<>();

    @Unique
    private static final CommandSourceStack mobHealthIndicators$source = new CommandSourceStack(new CommandSource() {
            public boolean acceptsSuccess() {
                return true;
            }
            public boolean acceptsFailure() {
                return true;
            }
            public boolean shouldInformAdmins() {
                return true;
            }

            public void sendSystemMessage(Component message) {
                client.getChatListener().handleSystemMessage(message, false);
            }
        }, null, null, null, null, "", Component.empty(), null, null);

    static {
        RegisterCommands.registerCommands(LiteralArgumentBuilder::<CommandSourceStack>literal).forEach(mobHealthIndicators$mhiCommands::register);
        HelpCommand.register(mobHealthIndicators$mhiCommands);
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void handleCommands(ClientboundCommandsPacket packet, CallbackInfo info) {
        RegisterCommands.registerCommands(LiteralArgumentBuilder::<SharedSuggestionProvider>literal).forEach(commands::register);
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    private void sendUnattendedCommand(String command, Screen screenAfterCommand, CallbackInfo info) {
        if(mobHealthIndicators$execute(command)) info.cancel();
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void sendCommand(String command, CallbackInfo info) {
        if(mobHealthIndicators$execute(command)) info.cancel();
    }

    @Unique
    private boolean mobHealthIndicators$execute(String command) {
        if(command.startsWith("help ") && mobHealthIndicators$mhiCommands.getRoot().getChildren().stream().noneMatch(node -> command.substring(5).startsWith(node.getName()) && !node.getName().equals("help"))) {
            return false;
        }

        try {
            mobHealthIndicators$mhiCommands.execute(command, mobHealthIndicators$source);
        } catch (CommandSyntaxException e) {
            if(e.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()) return false;

            Component message = ComponentUtils.fromMessage(e.getRawMessage());
            String context = e.getContext();

            message = context != null ? Component.translatable("command.context.parse_error", message, e.getCursor(), context) : message;
            client.getChatListener().handleSystemMessage(message.copy().withStyle(ChatFormatting.RED), false);
        }

        return true;
    }
}
