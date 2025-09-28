/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPeople;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPerson;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import com.mojang.brigadier.context.CommandContext;
import java.util.concurrent.CompletableFuture;

public class BlacklistedPeopleCommand extends Command {
    public BlacklistedPeopleCommand() {
        super("blacklistedpeople", "Manages blacklisted people.", "blacklist", "bl");
    }

    // Suggest online player names, filtered by input
    private static final SuggestionProvider<CommandSource> ONLINE_PLAYERS = (CommandContext<CommandSource> context, SuggestionsBuilder builder) -> {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            String remaining = builder.getRemaining().toLowerCase();
            for (PlayerListEntry entry : client.player.networkHandler.getPlayerList()) {
                String name = entry.getProfile().getName();
                if (name.toLowerCase().startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
        }
        return builder.buildFuture();
    };

    // Suggest only blacklisted player names, filtered by input
    private static final SuggestionProvider<CommandSource> BLACKLISTED_PLAYERS = (CommandContext<CommandSource> context, SuggestionsBuilder builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (BlacklistedPerson person : BlacklistedPeople.get()) {
            String name = person.getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
            .then(argument("player", StringArgumentType.word())
                .suggests(ONLINE_PLAYERS)
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "player");
                    BlacklistedPerson blacklistedPerson = new BlacklistedPerson(name);
                    if (BlacklistedPeople.get().add(blacklistedPerson)) {
                        ChatUtils.sendMsg(name.hashCode(), Formatting.RED, "Added (highlight)%s (default)to blacklisted people.".formatted(name));
                    } else error("Already blacklisted.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("remove")
            .then(argument("player", StringArgumentType.word())
                .suggests(BLACKLISTED_PLAYERS)
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "player");
                    BlacklistedPerson blacklistedPerson = BlacklistedPeople.get().get(name);
                    if (blacklistedPerson != null && BlacklistedPeople.get().remove(blacklistedPerson)) {
                        ChatUtils.sendMsg(name.hashCode(), Formatting.GREEN, "Removed (highlight)%s (default)from blacklisted people.".formatted(name));
                    } else error("Not blacklisted.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("list").executes(context -> {
            info("--- Blacklisted People ((highlight)%s(default)) ---", BlacklistedPeople.get().count());
            BlacklistedPeople.get().forEach(blacklistedPerson -> ChatUtils.info("(highlight)%s".formatted(blacklistedPerson.getName())));
            return SINGLE_SUCCESS;
        }));
    }
}
