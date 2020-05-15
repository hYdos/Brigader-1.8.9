package me.hydos.legacybrigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.concurrent.ExecutionException;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class LegacyBrigadier implements ModInitializer {

	public static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();;

	@Override
	public void onInitialize() {
		DISPATCHER.register(
				LiteralArgumentBuilder.<CommandSource>literal("foo")
						.then(
								RequiredArgumentBuilder.<CommandSource, Integer>argument("bar", integer())
										.executes(c -> {
											c.getSource().sendMessage(new LiteralText("Bar is " + getInteger(c, "bar")));
											return 1;
										})
						)
						.executes(c -> {
							c.getSource().sendMessage(new LiteralText("Called foo with no arguments"));
							return 1;
						})
		);
	}
}
