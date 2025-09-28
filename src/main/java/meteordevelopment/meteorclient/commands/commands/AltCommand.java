/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.alttracker.AltAccount;
import meteordevelopment.meteorclient.systems.alttracker.AltTracker;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;

public class AltCommand extends Command {
    public AltCommand() {
        super("alt", "Manages alt accounts tracking. Available commands: link, unlink, list");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Link two players as alt accounts
        builder.then(literal("link")
            .then(argument("main", StringArgumentType.string())
                .suggests((CommandContext<CommandSource> context, SuggestionsBuilder suggestionsBuilder) -> {
                    if (client.player != null && client.world != null) {
                        String remaining = suggestionsBuilder.getRemaining().toLowerCase();
                        for (PlayerListEntry entry : client.player.networkHandler.getPlayerList()) {
                            String name = entry.getProfile().getName();
                            if (name.toLowerCase().startsWith(remaining)) {
                                suggestionsBuilder.suggest(name);
                            }
                        }
                    }
                    return suggestionsBuilder.buildFuture();
                })
                .then(argument("alt", StringArgumentType.string())
                    .suggests((CommandContext<CommandSource> context, SuggestionsBuilder suggestionsBuilder) -> {
                        if (client.player != null && client.world != null) {
                            String remaining = suggestionsBuilder.getRemaining().toLowerCase();
                            for (PlayerListEntry entry : client.player.networkHandler.getPlayerList()) {
                                String name = entry.getProfile().getName();
                                if (name.toLowerCase().startsWith(remaining)) {
                                    suggestionsBuilder.suggest(name);
                                }
                            }
                        }
                        return suggestionsBuilder.buildFuture();
                    })
                    .executes(context -> {
                        String player1 = StringArgumentType.getString(context, "main");
                        String player2 = StringArgumentType.getString(context, "alt");

                        var tracker = AltTracker.get();
                        var group1 = tracker.getGroupByPlayer(player1);
                        var group2 = tracker.getGroupByPlayer(player2);

                        if (group1 != null && group2 != null && group1 == group2) {
                            warning("Players (highlight)%s (default)and (highlight)%s (default)are already linked.", player1, player2);
                            return SINGLE_SUCCESS;
                        }

                        if (group1 == null && group2 == null) {
                            if (tracker.linkAccounts(player1, player2)) {
                                info("(highlight)%s (default)has been linked as an alt of (highlight)%s", player2, player1);
                            }
                        } else if (group1 == null) {
                            if (tracker.linkAccounts(player1, player2)) {
                                info("(highlight)%s (default)has been linked as an alt of (highlight)%s", player1, group2.mainAccount);
                            }
                        } else if (group2 == null) {
                            if (tracker.linkAccounts(player1, player2)) {
                                info("(highlight)%s (default)has been linked as an alt of (highlight)%s", player2, group1.mainAccount);
                            }
                        } else {
                            if (tracker.linkAccounts(player1, player2)) {
                                info("Alt groups of (highlight)%s (default)and (highlight)%s (default)have been merged", group1.mainAccount, group2.mainAccount);
                            }
                        }

                        return SINGLE_SUCCESS;
                    })
                )
            )
        );

        // Unlink a specific player
        builder.then(literal("unlink")
            .then(argument("player", StringArgumentType.string())
                .suggests((CommandContext<CommandSource> context, SuggestionsBuilder suggestionsBuilder) -> {
                    String remaining = suggestionsBuilder.getRemaining().toLowerCase();
                    for (AltAccount group : AltTracker.get()) {
                        String main = group.getMainAccount();
                        if (main.toLowerCase().startsWith(remaining)) suggestionsBuilder.suggest(main);
                        for (String alt : group.getAltAccounts()) {
                            if (alt.toLowerCase().startsWith(remaining)) suggestionsBuilder.suggest(alt);
                        }
                    }
                    return suggestionsBuilder.buildFuture();
                })
                .executes(context -> {
                    String player = StringArgumentType.getString(context, "player");

                    if (AltTracker.get().unlinkAccount(player)) {
                        info("Unlinked (highlight)%s (default)from its alt group", player);
                    } else {
                        error("Player (highlight)%s (default)is not being tracked", player);
                    }

                    return SINGLE_SUCCESS;
                })
            )
        );

        // List all alt accounts
        builder.then(literal("list")
            .executes(context -> {
                if (AltTracker.get().isEmpty()) {
                    info("No alt accounts are being tracked.");
                    return SINGLE_SUCCESS;
                }

                info("--- Alt Groups ((highlight)%d (default)groups, (highlight)%d (default)total players) ---",
                    AltTracker.get().count(), AltTracker.get().getTotalTrackedPlayers());

                for (AltAccount group : AltTracker.get()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("(highlight)").append(group.getMainAccount()).append("(default)");

                    if (!group.getAltAccounts().isEmpty()) {
                        sb.append(" -> ");
                        sb.append(String.join(", ", group.getAltAccounts()));
                    }

                    ChatUtils.info(sb.toString());
                }

                return SINGLE_SUCCESS;
            })
        );
    }
}
