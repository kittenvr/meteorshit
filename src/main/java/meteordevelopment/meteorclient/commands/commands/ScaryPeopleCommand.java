/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPerson;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class ScaryPeopleCommand extends Command {
    public ScaryPeopleCommand() {
        super("scarypeople", "Manages scary people.");
    }

    // Suggest online player names, filtered by input
    private static final SuggestionProvider<CommandSource> ONLINE_PLAYERS = (context, builder) -> {
        if (MeteorClient.mc.getNetworkHandler() != null) {
            String remaining = builder.getRemaining().toLowerCase();
            for (PlayerListEntry entry : MeteorClient.mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().getName();
                if (name.toLowerCase().startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
        }
        return builder.buildFuture();
    };

    // Suggest players already marked scary, filtered by input
    private static final SuggestionProvider<CommandSource> SCARY_PLAYERS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (ScaryPerson scaryPerson : ScaryPeople.get()) {
            String name = scaryPerson.getName();
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
                    ScaryPerson scaryPerson = new ScaryPerson(name);
                    if (ScaryPeople.get().add(scaryPerson)) {
                        ChatUtils.sendMsg(name.hashCode(), Formatting.RED, "Added (highlight)%s (default)to scary people.".formatted(name));
                    } else error("Already marked as scary.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("remove")
            .then(argument("player", StringArgumentType.word())
                .suggests(SCARY_PLAYERS)
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "player");
                    ScaryPerson scaryPerson = ScaryPeople.get().get(name);
                    if (scaryPerson != null && ScaryPeople.get().remove(scaryPerson)) {
                        ChatUtils.sendMsg(name.hashCode(), Formatting.RED, "Removed (highlight)%s (default)from scary people.".formatted(name));
                    } else error("Not marked as scary.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("list").executes(context -> {
            info("--- Scary People ((highlight)%s(default)) ---", ScaryPeople.get().count());
            ScaryPeople.get().forEach(scaryPerson -> ChatUtils.info("(highlight)%s".formatted(scaryPerson.getName())));
            return SINGLE_SUCCESS;
        }));
    }
}
