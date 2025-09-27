/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.alttracker.AltTracker;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPeople;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.social.SocialColorUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerRadarHud extends HudElement {
    public static final HudElementInfo<PlayerRadarHud> INFO = new HudElementInfo<>(Hud.GROUP, "player-radar", "Displays players in your visual range.", PlayerRadarHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("The max number of players to show.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance to the player next to their name.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
        .name("display-friends")
        .description("Whether to show friends or not.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showScaryPeople = sgGeneral.add(new BoolSetting.Builder()
        .name("show-scary-people")
        .description("Whether to show scary people in the radar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showBlacklistedPeople = sgGeneral.add(new BoolSetting.Builder()
        .name("show-blacklisted-people")
        .description("Whether to show blacklisted people in the radar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showAlts = sgGeneral.add(new BoolSetting.Builder()
        .name("show-alts")
        .description("Whether to show alt accounts in the radar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> socialStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("social-status")
        .description("Shows social status (Friend, Scary, etc.) next to player names.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> useBetterTabColors = sgGeneral.add(new BoolSetting.Builder()
        .name("use-bettertab-colors")
        .description("Use colors from BetterTab module instead of primary color.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hideBasedOnSocialStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("filter-by-social-status")
        .description("Only show players with special social status (friends, scary, blacklisted, etc.).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
        .name("primary-color")
        .description("Primary color.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
        .name("secondary-color")
        .description("Secondary color.")
        .defaultValue(new SettingColor(175, 175, 175))
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .build()
    );

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private final List<AbstractClientPlayerEntity> players = new ArrayList<>();

    public PlayerRadarHud() {
        super(INFO);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    @Override
    public void tick(HudRenderer renderer) {
        double width = renderer.textWidth("Players:", shadow.get(), getScale());
        double height = renderer.textHeight(shadow.get(), getScale());

        if (mc.world == null) {
            setSize(width, height);
            return;
        }

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!shouldShowPlayer(entity)) continue;

            String text = getPlayerDisplayName(entity);
            if (distance.get()) text += String.format(" (%sm)", Math.round(mc.getCameraEntity().distanceTo(entity)));

            width = Math.max(width, renderer.textWidth(text, shadow.get(), getScale()));
            height += renderer.textHeight(shadow.get(), getScale()) + 2;
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double y = this.y + border.get();

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.text("Players:", x + border.get() + alignX(renderer.textWidth("Players:", shadow.get(), getScale()), alignment.get()), y, secondaryColor.get(), shadow.get(), getScale());

        if (mc.world == null) return;
        double spaceWidth = renderer.textWidth(" ", shadow.get(), getScale());

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!shouldShowPlayer(entity)) continue;

            String text = getPlayerDisplayName(entity);
            Color color = getPlayerColor(entity);
            String distanceText = null;

            double width = renderer.textWidth(text, shadow.get(), getScale());
            if (distance.get()) width += spaceWidth;

            if (distance.get()) {
                distanceText = String.format(" (%sm)", Math.round(mc.getCameraEntity().distanceTo(entity)));
                width += renderer.textWidth(distanceText, shadow.get(), getScale());
            }

            double x = this.x + border.get() + alignX(width, alignment.get());
            y += renderer.textHeight(shadow.get(), getScale()) + 2;

            x = renderer.text(text, x, y, color, shadow.get(), getScale());
            if (distance.get()) renderer.text(distanceText, x, y, secondaryColor.get(), shadow.get(), getScale());
        }
    }

    private List<AbstractClientPlayerEntity> getPlayers() {
        players.clear();
        players.addAll(mc.world.getPlayers());
        if (players.size() > limit.get()) players.subList(limit.get() - 1, players.size() - 1).clear();
        players.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.getCameraEntity())));

        return players;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }

    /**
     * Determines whether a player should be shown in the radar based on current settings
     */
    private boolean shouldShowPlayer(PlayerEntity entity) {
        String playerName = entity.getName().getString();
        
        // Check basic friend filter
        if (!friends.get() && Friends.get().isFriend(entity)) return false;
        
        // Check specific social system filters
        if (!showScaryPeople.get() && ScaryPeople.get().get(playerName) != null) return false;
        if (!showBlacklistedPeople.get() && BlacklistedPeople.get().get(playerName) != null) return false;
        if (!showAlts.get() && AltTracker.get().isTracked(playerName)) return false;
        
        // If filtering by social status, only show players with special status
        if (hideBasedOnSocialStatus.get()) {
            return SocialColorUtils.hasSpecialSocialStatus(entity);
        }
        
        return true;
    }
    
    /**
     * Gets the display name for a player, optionally including social status
     */
    private String getPlayerDisplayName(PlayerEntity entity) {
        String name = entity.getName().getString();
        
        if (socialStatus.get()) {
            String status = SocialColorUtils.getSocialStatus(entity);
            if (!status.equals("Player")) {
                name += " [" + status + "]";
            }
        }
        
        return name;
    }
    
    /**
     * Gets the appropriate color for a player based on settings
     */
    private Color getPlayerColor(PlayerEntity entity) {
        if (useBetterTabColors.get()) {
            return SocialColorUtils.getPlayerSocialColor(entity, primaryColor.get());
        }
        
        return primaryColor.get();
    }
}
