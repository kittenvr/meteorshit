/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;

public class ScaryAlertModule extends Module {
    public ScaryAlertModule() {
        super(Categories.Misc, "scary-alert", "Warns you when a scary person is nearby.");
    }

    @Override
    public void onActivate() {
        if (mc.world == null) return;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (ScaryPeople.get().isScary(player)) {
                ChatUtils.warning("Scary person detected: " + player.getName().getString());
            }
        }
    }
}
