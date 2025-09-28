/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.alttracker;

import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class AltAccount implements ISerializable<AltAccount>, Comparable<AltAccount> {
    public volatile String mainAccount;
    public volatile Set<String> altAccounts;
    private volatile @Nullable PlayerHeadTexture headTexture;
    private volatile boolean updating;

    public AltAccount(String mainAccount) {
        this.mainAccount = mainAccount;
        this.altAccounts = new HashSet<>();
        this.headTexture = null;
    }

    public AltAccount(String mainAccount, Set<String> altAccounts) {
        this.mainAccount = mainAccount;
        this.altAccounts = new HashSet<>(altAccounts);
        this.headTexture = null;
    }

    public AltAccount(PlayerEntity player) {
        this(player.getName().getString());
    }

    public String getMainAccount() {
        return mainAccount;
    }

    public Set<String> getAltAccounts() {
        return new HashSet<>(altAccounts);
    }

    public void addAlt(String altName) {
        altAccounts.add(altName);
    }

    public void removeAlt(String altName) {
        altAccounts.remove(altName);
    }

    public boolean hasAlt(String altName) {
        return altAccounts.contains(altName);
    }

    public boolean isMainOrAlt(String playerName) {
        return mainAccount.equals(playerName) || altAccounts.contains(playerName);
    }

    public String getDisplayText(String currentPlayer) {
        if (mainAccount.equals(currentPlayer)) {
            if (altAccounts.isEmpty()) return mainAccount;
            return mainAccount + " (+" + altAccounts.size() + " alts)";
        } else if (altAccounts.contains(currentPlayer)) {
            return currentPlayer + " (" + mainAccount + ")";
        }
        return currentPlayer;
    }

    public String getDisplayText(String currentPlayer, Collection<String> onlinePlayers) {
        if (mainAccount.equals(currentPlayer)) {
            if (altAccounts.isEmpty()) return mainAccount;
            // Count only online alts
            long onlineAlts = altAccounts.stream().filter(onlinePlayers::contains).count();
            if (onlineAlts == 0) return mainAccount;
            return mainAccount + " (+" + onlineAlts + ")";
        } else if (altAccounts.contains(currentPlayer)) {
            return currentPlayer + " (" + mainAccount + ")";
        }
        return currentPlayer;
    }

    public List<String> getAllAccounts() {
        List<String> all = new ArrayList<>();
        all.add(mainAccount);
        all.addAll(altAccounts);
        return all;
    }

    public PlayerHeadTexture getHead() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void updateInfo() {
        updating = true;
        APIResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + mainAccount).sendJson(APIResponse.class);
        if (res == null || res.name == null || res.id == null) return;
        mainAccount = res.name;
        UUID id = UndashedUuid.fromStringLenient(res.id);
        headTexture = PlayerHeadUtils.fetchHead(id);
        updating = false;
    }

    public boolean headTextureNeedsUpdate() {
        return !this.updating && headTexture == null;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("mainAccount", mainAccount);

        NbtList altList = new NbtList();
        for (String alt : altAccounts) {
            altList.add(NbtString.of(alt));
        }
        tag.put("altAccounts", altList);

        return tag;
    }

    @Override
    public AltAccount fromTag(NbtCompound tag) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AltAccount altAccount = (AltAccount) o;
        return Objects.equals(mainAccount, altAccount.mainAccount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainAccount);
    }

    @Override
    public int compareTo(@NotNull AltAccount altAccount) {
        return mainAccount.compareTo(altAccount.mainAccount);
    }

    private static class APIResponse {
        String name, id;
    }
}
