package me.hydos.legacybrigadier.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.class_1581;
import net.minecraft.client.util.Texts;
import net.minecraft.command.Command;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.hydos.legacybrigadier.LegacyBrigadier.DISPATCHER;

@Mixin(class_1581.class)
public abstract class CommandManagerMixin {

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void onCommand(CommandSource commandSource, String command, CallbackInfoReturnable<Integer> cir){
        StringReader stringReader = new StringReader(command);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        MinecraftServer.getServer().profiler.push(command);

        byte var19 = 0;
        try {
            LiteralText var10000;
            byte var18 = 0;
            try {
                int var4 = DISPATCHER.execute(stringReader, commandSource);
                cir.setReturnValue((int) var4);
            } catch (CommandSyntaxException var14) {
                commandSource.sendMessage(new LiteralText(ChatFormatting.RED + var14.getMessage()));
                if (var14.getInput() != null && var14.getCursor() >= 0) {
                    int i = Math.min(var14.getInput().length(), var14.getCursor());
                    Text text = (new LiteralText(""));
                    Style style = new Style();
                    style.setColor(Formatting.GRAY);
                    style.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
                    text.setStyle(style);
                    if (i > 10) {
                        text.append("...");
                    }

                    text.append(var14.getInput().substring(Math.max(0, i - 10), i));
                    if (i < var14.getInput().length()) {
//                        Text text2 = (new LiteralText(var14.getInput().substring(i))).formatted(new Formatting[]{Formatting.RED, Formatting.UNDERLINE});
//                        text.append(text2);
                    }

//                    text.append((new TranslatableText("command.context.here", new Object[0])).formatted(new Formatting[]{Formatting.RED, Formatting.ITALIC}));
//                    commandSource.sendMessage(text);
                }

                cir.setReturnValue((int) var18);
            } catch (Exception var15) {
                var10000 = new LiteralText(var15.getMessage() == null ? var15.getClass().getName() : var15.getMessage());
                Text text3 = var10000;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("Command exception: {}", command, var15);
                    StackTraceElement[] stackTraceElements = var15.getStackTrace();

                    for(int j = 0; j < Math.min(stackTraceElements.length, 3); ++j) {
                        text3.append("\n\n").append(stackTraceElements[j].getMethodName()).append("\n ").append(stackTraceElements[j].getFileName()).append(":").append(String.valueOf(stackTraceElements[j].getLineNumber()));
                    }
                }

//                commandSource.sendMessage((new TranslatableText("command.failed", new Object[0])).styled((style) -> {
//                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text3));
//                }));
                var19 = 0;
            }


        } finally {
            MinecraftServer.getServer().profiler.pop();
        }

        cir.setReturnValue((int) var19);
    }

}
