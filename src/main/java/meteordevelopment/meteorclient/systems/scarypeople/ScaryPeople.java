/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.scarypeople;

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

public class ScaryPeople extends System<ScaryPeople> implements Iterable<ScaryPerson> {
    private final List<ScaryPerson> scaryPeople = new ArrayList<>();

    public ScaryPeople() {
        super("scarypeople");
    }

    public static ScaryPeople get() {
        return Systems.get(ScaryPeople.class);
    }

    public boolean add(ScaryPerson scaryPerson) {
        if (scaryPerson.name.isEmpty() || scaryPerson.name.contains(" ")) return false;

        if (!scaryPeople.contains(scaryPerson)) {
            scaryPeople.add(scaryPerson);
            save();

            return true;
        }

        return false;
    }

    public boolean remove(ScaryPerson scaryPerson) {
        if (scaryPeople.remove(scaryPerson)) {
            save();
            return true;
        }

        return false;
    }

    public ScaryPerson get(String name) {
        for (ScaryPerson scaryPerson : scaryPeople) {
            if (scaryPerson.name.equals(name)) {
                return scaryPerson;
            }
        }

        return null;
    }

    public ScaryPerson get(PlayerEntity player) {
        return get(player.getName().getString());
    }

    public ScaryPerson get(PlayerListEntry player) {
        return get(player.getProfile().getName());
    }

    public boolean isScary(PlayerEntity player) {
        return player != null && get(player) != null;
    }

    public boolean isScary(PlayerListEntry player) {
        return get(player) != null;
    }

    public boolean shouldAttack(PlayerEntity player) {
        return isScary(player);
    }

    public int count() {
        return scaryPeople.size();
    }

    public boolean isEmpty() {
        return scaryPeople.isEmpty();
    }

    @Override
    public @NotNull Iterator<ScaryPerson> iterator() {
        return scaryPeople.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("scarypeople", NbtUtils.listToTag(scaryPeople));

        return tag;
    }

    @Override
    public ScaryPeople fromTag(NbtCompound tag) {
        scaryPeople.clear();

        for (NbtElement itemTag : tag.getList("scarypeople", 10)) {
            NbtCompound scaryPersonTag = (NbtCompound) itemTag;
            if (!scaryPersonTag.contains("name")) continue;

            String name = scaryPersonTag.getString("name");
            if (get(name) != null) continue;

            String uuid = scaryPersonTag.getString("id");
            ScaryPerson scaryPerson = !uuid.isBlank()
                ? new ScaryPerson(name, UndashedUuid.fromStringLenient(uuid))
                : new ScaryPerson(name);

            scaryPeople.add(scaryPerson);
        }

        Collections.sort(scaryPeople);

        MeteorExecutor.execute(() -> scaryPeople.forEach(ScaryPerson::updateInfo));

        return this;
    }
}
