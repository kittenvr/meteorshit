/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.alttracker.AltAccount;
import meteordevelopment.meteorclient.systems.alttracker.AltTracker;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPeople;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.social.SocialColorUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class BetterTab extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> tabSize = sgGeneral.add(new IntSetting.Builder()
        .name("tablist-size")
        .description("How many players in total to display in the tablist.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 1000)
        .build()
    );

    public final Setting<Integer> tabHeight = sgGeneral.add(new IntSetting.Builder()
        .name("column-height")
        .description("How many players to display in each column.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 1000)
        .build()
    );

    private final Setting<Boolean> self = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-self")
        .description("Highlights yourself in the tablist.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> selfColor = sgGeneral.add(new ColorSetting.Builder()
        .name("self-color")
        .description("The color to highlight your name with.")
        .defaultValue(SocialColorUtils.DEFAULT_SELF_COLOR)
        .visible(self::get)
        .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-friends")
        .description("Highlights friends in the tablist.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> scaryPeople = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-scary-people")
        .description("Highlights scary people in the tablist.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> scaryPeopleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("scary-people-color")
        .description("The color to highlight scary people with.")
        .defaultValue(SocialColorUtils.DEFAULT_SCARY_COLOR)
        .visible(scaryPeople::get)
        .build()
    );

    private final Setting<Boolean> blacklistedPeople = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-blacklisted-people")
        .description("Highlights blacklisted people in the tablist.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> blacklistedPeopleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("blacklisted-people-color")
        .description("The color to highlight blacklisted people with.")
        .defaultValue(SocialColorUtils.DEFAULT_BLACKLISTED_COLOR)
        .visible(blacklistedPeople::get)
        .build()
    );

    public final Setting<Boolean> accurateLatency = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate-latency")
        .description("Shows latency as a number in the tablist.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> gamemode = sgGeneral.add(new BoolSetting.Builder()
        .name("gamemode")
        .description("Display gamemode next to the nick.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> altIndicators = sgGeneral.add(new BoolSetting.Builder()
        .name("alt-indicators")
        .description("Shows alt account indicators in the tablist.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> altColor = sgGeneral.add(new ColorSetting.Builder()
        .name("alt-color")
        .description("The color to highlight alt accounts with.")
        .defaultValue(SocialColorUtils.DEFAULT_ALT_COLOR)
        .visible(altIndicators::get)
        .build()
    );


    public BetterTab() {
        super(Categories.Render, "better-tab", "Various improvements to the tab list.");
    }

    public Text getPlayerName(PlayerListEntry playerListEntry) {
        Text name;
        Color color = null;

        name = playerListEntry.getDisplayName();
        if (name == null) name = Text.literal(playerListEntry.getProfile().getName());

        String playerName = playerListEntry.getProfile().getName();

        if (playerListEntry.getProfile().getId().toString().equals(mc.player.getGameProfile().getId().toString()) && self.get()) {
            color = selfColor.get();
        }
        else if (scaryPeople.get() && ScaryPeople.get().get(playerName) != null) {
            color = scaryPeopleColor.get();
        }
        else if (blacklistedPeople.get() && BlacklistedPeople.get().get(playerName) != null) {
            color = blacklistedPeopleColor.get();
        }
        else if (altIndicators.get() && AltTracker.get().isTracked(playerName)) {
            color = altColor.get();
            // Show alt relationship information, only for online alts
            AltAccount group = AltTracker.get().getGroupByPlayer(playerName);
            if (group != null) {
                // Get list of online player names from tablist
                java.util.Collection<PlayerListEntry> onlineEntries = mc.getNetworkHandler().getPlayerList();
                java.util.Set<String> onlineNames = new java.util.HashSet<>();
                for (PlayerListEntry entry : onlineEntries) {
                    onlineNames.add(entry.getProfile().getName());
                }
                name = Text.literal(group.getDisplayText(playerName, onlineNames));
            }
        }
        else if (friends.get() && Friends.get().isFriend(playerListEntry)) {
            Friend friend = Friends.get().get(playerListEntry);
            if (friend != null) color = Config.get().friendColor.get();
        }

        if (color != null) {
            String nameString = name.getString();

            for (Formatting format : Formatting.values()) {
                if (format.isColor()) nameString = nameString.replace(format.toString(), "");
            }

            name = Text.literal(nameString).setStyle(name.getStyle().withColor(TextColor.fromRgb(color.getPacked())));
        }

        if (gamemode.get()) {
            GameMode gm = playerListEntry.getGameMode();
            String gmText = "?";
            if (gm != null) {
                gmText = switch (gm) {
                    case SPECTATOR -> "Sp";
                    case SURVIVAL -> "S";
                    case CREATIVE -> "C";
                    case ADVENTURE -> "A";
                };
            }
            MutableText text = Text.literal("");
            text.append(name);
            text.append(" [" + gmText + "]");
            name = text;
        }

        return name;
    }

    public Color getPlayerColor(String playerName) {
        if (mc.player != null && playerName.equals(mc.player.getName().getString()) && self.get()) {
            return selfColor.get();
        }
        else if (scaryPeople.get() && ScaryPeople.get().get(playerName) != null) {
            return scaryPeopleColor.get();
        }
        else if (blacklistedPeople.get() && BlacklistedPeople.get().get(playerName) != null) {
            return blacklistedPeopleColor.get();
        }
        else if (altIndicators.get() && AltTracker.get().isTracked(playerName)) {
            return altColor.get();
        }
        else if (friends.get() && Friends.get().get(playerName) != null) {
            return Config.get().friendColor.get();
        }
        return null;
    }

}
