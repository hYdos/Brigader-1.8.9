package me.hydos.legacybrigadier.mixin;

import me.hydos.legacybrigadier.CommandSuggestor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
@Environment(EnvType.CLIENT)
public class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;
    private CommandSuggestor commandSuggestor;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setText(Ljava/lang/String;)V"))
    public void init(CallbackInfo ci){
        commandSuggestor = new CommandSuggestor(MinecraftClient.getInstance(), (ChatScreen)(Object)this, this.chatField, MinecraftClient.getInstance().textRenderer, false, false, 1, 10, true, -805306368);
        commandSuggestor.refresh();
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(int mouseX, int mouseY, float delta, CallbackInfo ci){
        commandSuggestor.render(mouseX, mouseY);
        commandSuggestor.setWindowActive(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPress(char character, int code, CallbackInfo ci){
        commandSuggestor.refresh();
        this.commandSuggestor.keyPressed(code, 1, 1);
    }


}
