/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.blacklistedpeople;

import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BlacklistedPeople extends System<BlacklistedPeople> implements Iterable<BlacklistedPerson> {
    private final List<BlacklistedPerson> blacklistedPeople = new ArrayList<>();

    public BlacklistedPeople() {
        super("blacklistedpeople");
    }

    public static BlacklistedPeople get() {
        return Systems.get(BlacklistedPeople.class);
    }

    public boolean add(BlacklistedPerson blacklistedPerson) {
        if (blacklistedPerson.name.isEmpty() || blacklistedPerson.name.contains(" ")) return false;

        if (!blacklistedPeople.contains(blacklistedPerson)) {
            blacklistedPeople.add(blacklistedPerson);
            save();

            return true;
        }

        return false;
    }

    public boolean remove(BlacklistedPerson blacklistedPerson) {
        if (blacklistedPeople.remove(blacklistedPerson)) {
            save();
            return true;
        }

        return false;
    }

    public BlacklistedPerson get(String name) {
        for (BlacklistedPerson blacklistedPerson : blacklistedPeople) {
            if (blacklistedPerson.name.equals(name)) {
                return blacklistedPerson;
            }
        }

        return null;
    }

    public BlacklistedPerson get(PlayerEntity player) {
        return get(player.getName().getString());
    }

    public BlacklistedPerson get(PlayerListEntry player) {
        return get(player.getProfile().getName());
    }

    public boolean isBlacklisted(PlayerEntity player) {
        return player != null && get(player) != null;
    }

    public boolean isBlacklisted(PlayerListEntry player) {
        return get(player) != null;
    }

    public boolean shouldIgnore(PlayerEntity player) {
        return isBlacklisted(player);
    }

    public int count() {
        return blacklistedPeople.size();
    }

    public boolean isEmpty() {
        return blacklistedPeople.isEmpty();
    }

    @Override
    public @NotNull Iterator<BlacklistedPerson> iterator() {
        return blacklistedPeople.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("blacklistedpeople", NbtUtils.listToTag(blacklistedPeople));

        return tag;
    }

    @Override
    public BlacklistedPeople fromTag(NbtCompound tag) {
        blacklistedPeople.clear();

        for (NbtElement itemTag : tag.getList("blacklistedpeople", 10)) {
            NbtCompound blacklistedPersonTag = (NbtCompound) itemTag;
            if (!blacklistedPersonTag.contains("name")) continue;

            String name = blacklistedPersonTag.getString("name");
            if (get(name) != null) continue;

            String uuid = blacklistedPersonTag.getString("id");
            BlacklistedPerson blacklistedPerson = !uuid.isBlank()
                ? new BlacklistedPerson(name, UndashedUuid.fromStringLenient(uuid))
                : new BlacklistedPerson(name);

            blacklistedPeople.add(blacklistedPerson);
        }

        Collections.sort(blacklistedPeople);

        MeteorExecutor.execute(() -> blacklistedPeople.forEach(BlacklistedPerson::updateInfo));

        return this;
    }
}