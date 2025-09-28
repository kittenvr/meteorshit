/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.social;

import meteordevelopment.meteorclient.systems.alttracker.AltTracker;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPeople;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.WHITE;

public class SocialColorUtils {
    // Default social colors - moved from BetterTab
    public static final SettingColor DEFAULT_SELF_COLOR = new SettingColor(250, 130, 30);
    public static final SettingColor DEFAULT_FRIEND_COLOR = new SettingColor(0, 255, 180);
    public static final SettingColor DEFAULT_SCARY_COLOR = new SettingColor(100, 15, 175);
    public static final SettingColor DEFAULT_BLACKLISTED_COLOR = new SettingColor(255, 0, 255);
    public static final SettingColor DEFAULT_ALT_COLOR = new SettingColor(255, 165, 0);

    private static final Color color = new Color();

    private SocialColorUtils() {}

    /**
     * Gets the appropriate social color for a player based on various systems.
     * Priority order:
     * 1. Self (if enabled in BetterTab and player is self)
     * 2. Scary People (if enabled in BetterTab)
     * 3. Blacklisted People (if enabled in BetterTab)
     * 4. Alt accounts (if enabled in BetterTab)
     * 5. Friends (if enabled in BetterTab)
     * 6. Team color (if enabled in Config)
     * 7. Default color
     */
    public static Color getPlayerSocialColor(PlayerEntity entity, Color defaultColor) {
        if (entity == null) return defaultColor;

        String playerName = entity.getName().getString();
        BetterTab betterTab = Modules.get().get(BetterTab.class);

        // Check if player is self
        if (mc.player != null && betterTab.isActive() &&
            entity.getUuid().equals(mc.player.getUuid())) {
            return getSelfColor();
        }

        // Check scary people
        if (betterTab.isActive() && ScaryPeople.get().get(playerName) != null) {
            return getScaryPeopleColor();
        }

        // Check blacklisted people
        if (betterTab.isActive() && BlacklistedPeople.get().get(playerName) != null) {
            return getBlacklistedPeopleColor();
        }

        // Check alt accounts
        if (betterTab.isActive() && AltTracker.get().isTracked(playerName)) {
            return getAltColor();
        }

        // Check friends
        if (Friends.get().isFriend(entity)) {
            return color.set(Config.get().friendColor.get()).a(defaultColor.a);
        }

        // Check team color
        if (Config.get().useTeamColor.get() &&
            !color.set(TextUtils.getMostPopularColor(entity.getDisplayName())).equals(WHITE)) {
            return color.a(defaultColor.a);
        }

        return defaultColor;
    }

    /**
     * Gets color for a player name string (useful for tab list and other contexts)
     */
    public static Color getPlayerSocialColor(String playerName, Color defaultColor) {
        BetterTab betterTab = Modules.get().get(BetterTab.class);

        // Check if player is self
        if (mc.player != null && betterTab.isActive() &&
            playerName.equals(mc.player.getName().getString())) {
            return getSelfColor();
        }

        // Check scary people
        if (betterTab.isActive() && ScaryPeople.get().get(playerName) != null) {
            return getScaryPeopleColor();
        }

        // Check blacklisted people
        if (betterTab.isActive() && BlacklistedPeople.get().get(playerName) != null) {
            return getBlacklistedPeopleColor();
        }

        // Check alt accounts
        if (betterTab.isActive() && AltTracker.get().isTracked(playerName)) {
            return getAltColor();
        }

        // Check friends
        if (Friends.get().get(playerName) != null) {
            return color.set(Config.get().friendColor.get()).a(defaultColor.a);
        }

        return defaultColor;
    }

    // Helper methods to get colors from BetterTab settings or use defaults
    private static Color getSelfColor() {
        BetterTab betterTab = Modules.get().get(BetterTab.class);
        if (betterTab.isActive() && mc.player != null) {
            Color color = betterTab.getPlayerColor(mc.player.getName().getString());
            return color != null ? color : DEFAULT_SELF_COLOR;
        }
        return DEFAULT_SELF_COLOR;
    }

    private static Color getScaryPeopleColor() {
        BetterTab betterTab = Modules.get().get(BetterTab.class);
        if (betterTab.isActive()) {
            return betterTab.scaryPeopleColor.get();
        }
        return DEFAULT_SCARY_COLOR;
    }

    private static Color getBlacklistedPeopleColor() {
        BetterTab betterTab = Modules.get().get(BetterTab.class);
        if (betterTab.isActive()) {
            return betterTab.blacklistedPeopleColor.get();
        }
        return DEFAULT_BLACKLISTED_COLOR;
    }

    private static Color getAltColor() {
        BetterTab betterTab = Modules.get().get(BetterTab.class);
        if (betterTab.isActive()) {
            return betterTab.altColor.get();
        }
        return DEFAULT_ALT_COLOR;
    }

    /**
     * Checks if a player should be considered as having a special social status
     */
    public static boolean hasSpecialSocialStatus(PlayerEntity entity) {
        if (entity == null) return false;

        String playerName = entity.getName().getString();

        // Check if is self
        if (mc.player != null && entity.getUuid().equals(mc.player.getUuid())) {
            return true;
        }

        // Check various social systems
        return ScaryPeople.get().get(playerName) != null ||
               BlacklistedPeople.get().get(playerName) != null ||
               AltTracker.get().isTracked(playerName) ||
               Friends.get().isFriend(entity);
    }

    /**
     * Gets a descriptive name for the social status of a player
     */
    public static String getSocialStatus(PlayerEntity entity) {
        if (entity == null) return "Unknown";

        String playerName = entity.getName().getString();

        if (mc.player != null && entity.getUuid().equals(mc.player.getUuid())) {
            return "Self";
        }

        if (ScaryPeople.get().get(playerName) != null) {
            return "Scary";
        }

        if (BlacklistedPeople.get().get(playerName) != null) {
            return "Blacklisted";
        }

        if (AltTracker.get().isTracked(playerName)) {
            return "Alt";
        }

        if (Friends.get().isFriend(entity)) {
            return "Friend";
        }

        return "Player";
    }
}
