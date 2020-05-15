package me.hydos.legacybrigadier;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.hydos.legacybrigadier.extras.CommandSourceExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Rect2i;
import net.minecraft.client.util.Window;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static me.hydos.legacybrigadier.LegacyBrigadier.DISPATCHER;

@Environment(EnvType.CLIENT)
public class CommandSuggestor {
    public Window screen;
    public static Pattern BACKSLASH_S_PATTERN = Pattern.compile("(\\s+)");
    public MinecraftClient client;
    public Screen owner;
    public TextFieldWidget textField;
    public TextRenderer textRenderer;
    public boolean slashRequired;
    public boolean suggestingWhenEmpty;
    public int inWindowIndexOffset;
    public int maxSuggestionSize;
    public boolean chatScreenSized;
    public int color;
    public List<String> messages = Lists.newArrayList();
    public int x;
    public int width;
    public ParseResults<CommandSource> parse;
    public CompletableFuture<Suggestions> pendingSuggestions;
    public CommandSuggestor.SuggestionWindow window;
    public boolean windowActive;
    public boolean completingSuggestions;

    public CommandSuggestor(MinecraftClient client, Screen owner, TextFieldWidget textField, TextRenderer textRenderer, boolean slashRequired, boolean suggestingWhenEmpty, int inWindowIndexOffset, int maxSuggestionSize, boolean chatScreenSized, int color) {
        this.screen = new Window(MinecraftClient.getInstance());
        this.client = client;
        this.owner = owner;
        this.textField = textField;
        this.textRenderer = textRenderer;
        this.slashRequired = slashRequired;
        this.suggestingWhenEmpty = suggestingWhenEmpty;
        this.inWindowIndexOffset = inWindowIndexOffset;
        this.maxSuggestionSize = maxSuggestionSize;
        this.chatScreenSized = chatScreenSized;
        this.color = color;
//        textField.setRenderTextProvider(this::provideRenderText);
    }

    public void setWindowActive(boolean windowActive) {
        this.windowActive = windowActive;
        if (!windowActive) {
            this.window = null;
        }

    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.showSuggestions(true);
        return true;
    }

    public boolean mouseScrolled(double amount) {
        return this.window != null && this.window.mouseScrolled(MathHelper.clamp(amount, -1.0D, 1.0D));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.window != null && this.window.mouseClicked((int)mouseX, (int)mouseY, button);
    }

    public void showSuggestions(boolean e) {
        if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
            Suggestions suggestions = this.pendingSuggestions.join();
            if (!suggestions.isEmpty()) {
                int i = 0;

                Suggestion suggestion;
                for(Iterator var4 = suggestions.getList().iterator(); var4.hasNext(); i = Math.max(i, this.textRenderer.getStringWidth(suggestion.getText()))) {
                    suggestion = (Suggestion)var4.next();
                }

                int x = textField.getInnerWidth();//MathHelper.clamp(this.textField.getCharacterX(suggestions.getRange().getStart()), 0, this.textField.getCharacterX(0) + this.textField.getInnerWidth() - i);
                int y = this.chatScreenSized ? this.owner.height - 12 : 72;
                this.window = new CommandSuggestor.SuggestionWindow(x, y, i, suggestions, false);
            }
        }

    }

    public void refresh() {
        String string = this.textField.getText();
        if (this.parse != null && !this.parse.getReader().getString().equals(string)) {
            this.parse = null;
        }

        if (!this.completingSuggestions) {
//            this.textField.setSuggestion((String)null); FIXME: later
            this.window = null;
        }

        this.messages.clear();
        StringReader stringReader = new StringReader(string);
        boolean bl = stringReader.canRead() && stringReader.peek() == '/';
        if (bl) {
            stringReader.skip();
        }

        boolean bl2 = this.slashRequired || bl;
        int i = this.textField.getCursor();
        int j;
        if (bl2) {
            CommandDispatcher<CommandSource> commandDispatcher = DISPATCHER;
            if (this.parse == null) {
                this.parse = commandDispatcher.parse(stringReader, MinecraftClient.getInstance().player);
            }

            j = this.suggestingWhenEmpty ? stringReader.getCursor() : 1;
            if (i >= j && (this.window == null || !this.completingSuggestions)) {
                this.pendingSuggestions = commandDispatcher.getCompletionSuggestions(this.parse, i);
                this.pendingSuggestions.thenRun(() -> {
                    if (this.pendingSuggestions.isDone()) {
                        this.show();
                    }
                });
            }
        } else {
            String string2 = string.substring(0, i);
            j = getLastPlayerNameStart(string2);
            Collection<String> collection = Arrays.asList(MinecraftServer.getServer().getPlayerManager().getPlayerNames());
            this.pendingSuggestions = CommandSourceExtra.suggestMatching((Iterable)collection, new SuggestionsBuilder(string2, j));
        }

    }

    public static int getLastPlayerNameStart(String input) {
        if (Strings.isNullOrEmpty(input)) {
            return 0;
        } else {
            int i = 0;

            return i;
        }
    }

    public void show() {
        if (this.textField.getCursor() == this.textField.getText().length()) {
            if (this.pendingSuggestions.join().isEmpty() && !this.parse.getExceptions().isEmpty()) {
                int i = 0;
                Iterator var2 = this.parse.getExceptions().entrySet().iterator();

                while(var2.hasNext()) {
                    Entry<CommandNode<CommandSource>, CommandSyntaxException> entry = (Entry)var2.next();
                    CommandSyntaxException commandSyntaxException = (CommandSyntaxException)entry.getValue();
                    if (commandSyntaxException.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                        ++i;
                    } else {
                        this.messages.add(commandSyntaxException.getMessage());
                    }
                }

                if (i > 0) {
                    this.messages.add(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create().getMessage());
                }
            } else if (this.parse.getReader().canRead()) {
//                this.messages.add(CommandManager.getException(this.parse).getMessage());
//                FIXME: e
            }
        }

        this.x = 0;
        this.width = this.owner.width;
        if (this.messages.isEmpty()) {
            this.showUsages(Formatting.GRAY);
        }

        this.window = null;

    }

    public void showUsages(Formatting formatting) {
        CommandContextBuilder<CommandSource> commandContextBuilder = this.parse.getContext();
        SuggestionContext<CommandSource> suggestionContext = commandContextBuilder.findSuggestionContext(this.textField.getCursor());
        Map<CommandNode<CommandSource>, String> map = DISPATCHER.getSmartUsage(suggestionContext.parent, MinecraftClient.getInstance().player);
        List<String> list = Lists.newArrayList();
        int i = 0;

        for (Entry<CommandNode<CommandSource>, String> commandNodeStringEntry : map.entrySet()) {
            Entry entry = (Entry) commandNodeStringEntry;
            if (!(entry.getKey() instanceof LiteralCommandNode)) {
                list.add(formatting + (String) entry.getValue());
                i = Math.max(i, this.textRenderer.getStringWidth((String) entry.getValue()));
            }
        }

        if (!list.isEmpty()) {
            this.messages.addAll(list);
//            this.x = MathHelper.clamp(this.textField.getCharacterX(suggestionContext.startPos), 0, this.textField.getCharacterX(0) + this.textField.getInnerWidth() - i);
            this.x = this.textField.getInnerWidth();
            this.width = i;
        }

    }

    public String provideRenderText(String original, int firstCharacterIndex) {
        return this.parse != null ? highlight(this.parse, original, firstCharacterIndex) : original;
    }

    @Nullable
    public static String getSuggestionSuffix(String original, String suggestion) {
        return suggestion.startsWith(original) ? suggestion.substring(original.length()) : null;
    }

    public static String highlight(ParseResults<CommandSource> parse, String original, int firstCharacterIndex) {
        Formatting[] formattings = new Formatting[]{Formatting.AQUA, Formatting.YELLOW, Formatting.GREEN, Formatting.LIGHT_PURPLE, Formatting.GOLD};
        String string = Formatting.GRAY.toString();
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        int j = -1;
        CommandContextBuilder<CommandSource> commandContextBuilder = parse.getContext().getLastChild();

        for (ParsedArgument<CommandSource, ?> commandSourceParsedArgument : commandContextBuilder.getArguments().values()) {
            ParsedArgument parsedArgument = commandSourceParsedArgument;
            ++j;
            if (j >= formattings.length) {
                j = 0;
            }

            int k = Math.max(parsedArgument.getRange().getStart() - firstCharacterIndex, 0);
            if (k >= original.length()) {
                break;
            }

            int l = Math.min(parsedArgument.getRange().getEnd() - firstCharacterIndex, original.length());
            if (l > 0) {
                stringBuilder.append(original, i, k);
                stringBuilder.append(formattings[j]);
                stringBuilder.append(original, k, l);
                stringBuilder.append(string);
                i = l;
            }
        }

        if (parse.getReader().canRead()) {
            int m = Math.max(parse.getReader().getCursor() - firstCharacterIndex, 0);
            if (m < original.length()) {
                int n = Math.min(m + parse.getReader().getRemainingLength(), original.length());
                stringBuilder.append(original, i, m);
                stringBuilder.append(Formatting.RED);
                stringBuilder.append(original, m, n);
                i = n;
            }
        }

        stringBuilder.append(original, i, original.length());
        return stringBuilder.toString();
    }

    public void render(int mouseX, int mouseY) {
        if (this.window != null) {
            this.window.render(mouseX, mouseY);
        } else {
            int i = 0;

            for(Iterator var4 = this.messages.iterator(); var4.hasNext(); ++i) {
                String string = (String)var4.next();
                int j = this.chatScreenSized ? this.owner.height - 14 - 13 - 12 * i : 72 + 12 * i;
                DrawableHelper.fill(this.x - 1, j, this.x + this.width + 1, j + 12, this.color);
                this.textRenderer.drawWithShadow(string, (float)this.x, (float)(j + 2), -1);
            }
        }

    }

    public String method_23958() {
        return this.window != null ? "\n" + this.window.getNarration() : "";
    }

    @Environment(EnvType.CLIENT)
    public class SuggestionWindow {
        public Rect2i area;
        public Suggestions suggestions;
        public String typedText;
        public int inWindowIndex;
        public int selection;
        public Vec2f mouse;
        public boolean completed;
        public int lastNarrationIndex;

        private SuggestionWindow(int x, int y, int width, Suggestions suggestions, boolean narrateFirstSuggestion) {
            this.mouse = Vec2f.ZERO;
            int i = x - 1;
            int j = CommandSuggestor.this.chatScreenSized ? y - 3 - Math.min(suggestions.getList().size(), CommandSuggestor.this.maxSuggestionSize) * 12 : y;
            this.area = new Rect2i(i, j, width + 1, Math.min(suggestions.getList().size(), CommandSuggestor.this.maxSuggestionSize) * 12);
            this.suggestions = suggestions;
            this.typedText = CommandSuggestor.this.textField.getText();
            this.lastNarrationIndex = narrateFirstSuggestion ? -1 : 0;
            this.select(0);
        }

        public void render(int mouseX, int mouseY) {
            int i = Math.min(this.suggestions.getList().size(), CommandSuggestor.this.maxSuggestionSize);
            int j = -5592406;
            boolean bl = this.inWindowIndex > 0;
            boolean bl2 = this.suggestions.getList().size() > this.inWindowIndex + i;
            boolean bl3 = bl || bl2;
            boolean bl4 = this.mouse.x != (float)mouseX || this.mouse.y != (float)mouseY;
            if (bl4) {
                this.mouse = new Vec2f((float)mouseX, (float)mouseY);
            }

            if (bl3) {
                DrawableHelper.fill(this.area.getX(), this.area.getY() - 1, this.area.getX() + this.area.getWidth(), this.area.getY(), CommandSuggestor.this.color);
                DrawableHelper.fill(this.area.getX(), this.area.getY() + this.area.getHeight(), this.area.getX() + this.area.getWidth(), this.area.getY() + this.area.getHeight() + 1, CommandSuggestor.this.color);
                int l;
                if (bl) {
                    for(l = 0; l < this.area.getWidth(); ++l) {
                        if (l % 2 == 0) {
                            DrawableHelper.fill(this.area.getX() + l, this.area.getY() - 1, this.area.getX() + l + 1, this.area.getY(), -1);
                        }
                    }
                }

                if (bl2) {
                    for(l = 0; l < this.area.getWidth(); ++l) {
                        if (l % 2 == 0) {
                            DrawableHelper.fill(this.area.getX() + l, this.area.getY() + this.area.getHeight(), this.area.getX() + l + 1, this.area.getY() + this.area.getHeight() + 1, -1);
                        }
                    }
                }
            }

            boolean bl5 = false;

            for(int m = 0; m < i; ++m) {
                Suggestion suggestion = (Suggestion)this.suggestions.getList().get(m + this.inWindowIndex);
                DrawableHelper.fill(this.area.getX(), this.area.getY() + 12 * m, this.area.getX() + this.area.getWidth(), this.area.getY() + 12 * m + 12, CommandSuggestor.this.color);
                if (mouseX > this.area.getX() && mouseX < this.area.getX() + this.area.getWidth() && mouseY > this.area.getY() + 12 * m && mouseY < this.area.getY() + 12 * m + 12) {
                    if (bl4) {
                        this.select(m + this.inWindowIndex);
                    }

                    bl5 = true;
                }

                CommandSuggestor.this.textRenderer.drawWithShadow(suggestion.getText(), (float)(this.area.getX() + 1), (float)(this.area.getY() + 2 + 12 * m), m + this.inWindowIndex == this.selection ? -256 : -5592406);
            }

            if (bl5) {
                Message message = this.suggestions.getList().get(this.selection).getTooltip();
                if (message != null) {
//                    CommandSuggestor.this.owner.renderTooltip(Texts.toText(message).asFormattedString(), mouseX, mouseY);
                    //FIXME: send help
                }
            }

        }

        public boolean mouseClicked(int x, int y, int button) {
            if (!this.area.contains(x, y)) {
                return false;
            } else {
                int i = (y - this.area.getY()) / 12 + this.inWindowIndex;
                if (i >= 0 && i < this.suggestions.getList().size()) {
                    this.select(i);
                    this.complete();
                }

                return true;
            }
        }

        public boolean mouseScrolled(double amount) {
            int i = (int)(CommandSuggestor.this.client.mouse.x* (double)screen.getScaledWidth() / screen.method_2467());
            int j = (int)(CommandSuggestor.this.client.mouse.y * (double)screen.getScaledHeight() / screen.method_2468());
            if (this.area.contains(i, j)) {
                this.inWindowIndex = MathHelper.clamp((int)((double)this.inWindowIndex - amount), 0, Math.max(this.suggestions.getList().size() - CommandSuggestor.this.maxSuggestionSize, 0));
                return true;
            } else {
                return false;
            }
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 265) {
                this.scroll(-1);
                this.completed = false;
                return true;
            } else if (keyCode == 264) {
                this.scroll(1);
                this.completed = false;
                return true;
            } else if (keyCode == 258) {
                if (this.completed) {
                    this.scroll(Screen.hasShiftDown() ? -1 : 1);
                }

                this.complete();
                return true;
            } else if (keyCode == 256) {
                this.discard();
                return true;
            } else {
                return false;
            }
        }

        public void scroll(int offset) {
            this.select(this.selection + offset);
            int i = this.inWindowIndex;
            int j = this.inWindowIndex + CommandSuggestor.this.maxSuggestionSize - 1;
            if (this.selection < i) {
                this.inWindowIndex = MathHelper.clamp(this.selection, 0, Math.max(this.suggestions.getList().size() - CommandSuggestor.this.maxSuggestionSize, 0));
            } else if (this.selection > j) {
                this.inWindowIndex = MathHelper.clamp(this.selection + CommandSuggestor.this.inWindowIndexOffset - CommandSuggestor.this.maxSuggestionSize, 0, Math.max(this.suggestions.getList().size() - CommandSuggestor.this.maxSuggestionSize, 0));
            }

        }

        public void select(int index) {
            this.selection = index;
            if (this.selection < 0) {
                this.selection += this.suggestions.getList().size();
            }

            if (this.selection >= this.suggestions.getList().size()) {
                this.selection -= this.suggestions.getList().size();
            }

            Suggestion suggestion = this.suggestions.getList().get(this.selection);
//            CommandSuggestor.this.textField.setSuggestion(CommandSuggestor.getSuggestionSuffix(CommandSuggestor.this.textField.getText(), suggestion.apply(this.typedText)));
                //FIXME: send help
        }

        public void complete() {
            Suggestion suggestion = this.suggestions.getList().get(this.selection);
            CommandSuggestor.this.completingSuggestions = true;
            CommandSuggestor.this.textField.setText(suggestion.apply(this.typedText));
            int i = suggestion.getRange().getStart() + suggestion.getText().length();
//            CommandSuggestor.this.textField.setSelectionStart(i); //FIXME: send help
            CommandSuggestor.this.textField.setSelectionEnd(i);
            this.select(this.selection);
            CommandSuggestor.this.completingSuggestions = false;
            this.completed = true;
        }

        public String getNarration() {
            this.lastNarrationIndex = this.selection;
            List<Suggestion> list = this.suggestions.getList();
            Suggestion suggestion = list.get(this.selection);
            Message message = suggestion.getTooltip();
            return message != null ? I18n.translate("narration.suggestion.tooltip", this.selection + 1, list.size(), suggestion.getText(), message.getString()) : I18n.translate("narration.suggestion", this.selection + 1, list.size(), suggestion.getText());
        }

        public void discard() {
            CommandSuggestor.this.window = null;
        }
    }
}
