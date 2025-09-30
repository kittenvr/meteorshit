/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.alttracker.AltAccount;
import meteordevelopment.meteorclient.systems.alttracker.AltTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A command to count how many online players are tracked as alt accounts.
 * Command: .online-alts
 */
public class OnlineAltsCommand extends Command {
    // Reference to MinecraftClient instance, standard practice in Meteor Client
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public OnlineAltsCommand() {
        super("online-alts", "Counts how many online players are tracked as alt accounts.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            // Ensure we are connected to a server and the player list is available
            if (mc.getNetworkHandler() == null) {
                error("You must be connected to a server to run this command.");
                return 1; // Return failure
            }

            AltTracker altTracker = AltTracker.get();
            int onlineAltCount = 0;

            // A Set to track unique main accounts whose alts are currently online
            Set<String> uniqueMainAccountsWithOnlineAlt = new HashSet<>();

            // Get all online players from the tab list
            // FIX: mc.getNetworkHandler().getPlayerList() returns a Collection, which must be converted to a List.
            List<PlayerListEntry> playerList = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
            // Get the total number of players currently in the tab list
            int totalPlayers = playerList.size();

            for (PlayerListEntry entry : playerList) {
                String playerName = entry.getProfile().getName();

                // Exclude the current client player from the alt count
                ClientPlayerEntity localPlayer = mc.player;
                if (localPlayer != null && localPlayer.getName().getString().equals(playerName)) {
                    continue;
                }

                AltAccount group = altTracker.getGroupByPlayer(playerName);

                if (group != null) {
                    // This player is a tracked account. Check if they are the alt (i.e., their
                    // name is in the altAccounts set, not the mainAccount field).
                    if (group.altAccounts.contains(playerName)) {
                        onlineAltCount++;
                        uniqueMainAccountsWithOnlineAlt.add(group.mainAccount);
                    }
                }
            }

            // Report the result in the requested "alts/total" format
            if (onlineAltCount == 0) {
                info("(highlight)0/%d(default) players online are tracked as alt accounts.", totalPlayers);
            } else {
                info("Found (highlight)%d/%d(default) players online are tracked as alt accounts.", onlineAltCount, totalPlayers);

                // Check if there are any unique main accounts identified using !isEmpty()
                if (!uniqueMainAccountsWithOnlineAlt.isEmpty()) {
                    // Report the unique main accounts identified
                    info("These belong to (highlight)%d unique main account(s)(default): %s",
                        uniqueMainAccountsWithOnlineAlt.size(),
                        String.join(", ", uniqueMainAccountsWithOnlineAlt));
                }
            }

            return SINGLE_SUCCESS;
        });
    }
}
