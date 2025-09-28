/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.alttracker;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class AltTracker extends System<AltTracker> implements Iterable<AltAccount> {
    private final List<AltAccount> altAccounts = new ArrayList<>();

    public AltTracker() {
        super("alttracker");
    }

    public static AltTracker get() {
        return Systems.get(AltTracker.class);
    }

    public boolean addAltGroup(AltAccount altAccount) {
        if (altAccount.mainAccount.isEmpty() || altAccount.mainAccount.contains(" ")) return false;

        // Check if main account already exists
        AltAccount existing = getByMainAccount(altAccount.mainAccount);
        if (existing != null) {
            // Merge alt accounts
            existing.altAccounts.addAll(altAccount.altAccounts);
            save();
            return true;
        }

        // Check if any account in this group is already tracked elsewhere
        for (String account : altAccount.getAllAccounts()) {
            AltAccount existingGroup = getGroupByPlayer(account);
            if (existingGroup != null) {
                // Merge groups
                Set<String> allAccounts = new HashSet<>(altAccount.getAllAccounts());
                allAccounts.addAll(existingGroup.getAllAccounts());
                allAccounts.remove(existingGroup.mainAccount); // Remove main from alt list

                existingGroup.altAccounts.clear();
                existingGroup.altAccounts.addAll(allAccounts);
                existingGroup.altAccounts.remove(existingGroup.mainAccount); // Ensure main isn't in alt list
                save();
                return true;
            }
        }

        altAccounts.add(altAccount);
        save();
        return true;
    }

    public boolean remove(AltAccount altAccount) {
        if (altAccounts.remove(altAccount)) {
            save();
            return true;
        }
        return false;
    }

    public boolean removePlayer(String playerName) {
        AltAccount group = getGroupByPlayer(playerName);
        if (group == null) return false;

        if (group.mainAccount.equals(playerName)) {
            // Removing main account - either delete group or promote an alt
            if (group.altAccounts.isEmpty()) {
                return remove(group);
            } else {
                // Promote first alt to main
                String newMain = group.altAccounts.iterator().next();
                group.altAccounts.remove(newMain);
                group.mainAccount = newMain;
                save();
                return true;
            }
        } else {
            // Removing alt account
            group.removeAlt(playerName);
            save();
            return true;
        }
    }

    public AltAccount getByMainAccount(String mainAccount) {
        for (AltAccount altAccount : altAccounts) {
            if (altAccount.mainAccount.equals(mainAccount)) {
                return altAccount;
            }
        }
        return null;
    }

    public AltAccount getGroupByPlayer(String playerName) {
        for (AltAccount altAccount : altAccounts) {
            if (altAccount.isMainOrAlt(playerName)) {
                return altAccount;
            }
        }
        return null;
    }

    public AltAccount get(PlayerEntity player) {
        return getGroupByPlayer(player.getName().getString());
    }

    public AltAccount get(PlayerListEntry player) {
        return getGroupByPlayer(player.getProfile().getName());
    }

    public boolean isTracked(String playerName) {
        return getGroupByPlayer(playerName) != null;
    }

    public boolean isTracked(PlayerEntity player) {
        return getGroupByPlayer(player.getName().getString()) != null;
    }

    public boolean isTracked(PlayerListEntry player) {
        return getGroupByPlayer(player.getProfile().getName()) != null;
    }

    public String getMainAccountFor(String playerName) {
        AltAccount group = getGroupByPlayer(playerName);
        return group != null ? group.mainAccount : playerName;
    }

    public List<String> getAltsFor(String playerName) {
        AltAccount group = getGroupByPlayer(playerName);
        if (group == null) return Collections.emptyList();
        
        List<String> alts = new ArrayList<>(group.altAccounts);
        if (!group.mainAccount.equals(playerName)) {
            alts.add(0, group.mainAccount); // Add main account if current player is an alt
        }
        return alts;
    }

    public String getDisplayName(String playerName) {
        AltAccount group = getGroupByPlayer(playerName);
        if (group == null) return playerName;
        return group.getDisplayText(playerName);
    }

    public boolean linkAccounts(String player1, String player2) {
        AltAccount group1 = getGroupByPlayer(player1);
        AltAccount group2 = getGroupByPlayer(player2);

        if (group1 != null && group2 != null && group1 == group2) {
            return false; // Already linked
        }

        if (group1 == null && group2 == null) {
            // Create new group with player1 as main
            AltAccount newGroup = new AltAccount(player1);
            newGroup.addAlt(player2);
            return addAltGroup(newGroup);
        } else if (group1 == null) {
            // Add player1 to group2
            group2.addAlt(player1);
            save();
            return true;
        } else if (group2 == null) {
            // Add player2 to group1
            group1.addAlt(player2);
            save();
            return true;
        } else {
            // Merge two existing groups
            group1.altAccounts.addAll(group2.altAccounts);
            group1.altAccounts.add(group2.mainAccount);
            remove(group2);
            save();
            return true;
        }
    }

    public boolean unlinkAccount(String playerName) {
        return removePlayer(playerName);
    }

    public int count() {
        return altAccounts.size();
    }

    public boolean isEmpty() {
        return altAccounts.isEmpty();
    }

    public int getTotalTrackedPlayers() {
        return altAccounts.stream().mapToInt(group -> 1 + group.altAccounts.size()).sum();
    }

    @Override
    public @NotNull Iterator<AltAccount> iterator() {
        return altAccounts.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("altAccounts", NbtUtils.listToTag(altAccounts));

        return tag;
    }

    @Override
    public AltTracker fromTag(NbtCompound tag) {
        altAccounts.clear();

        for (NbtElement itemTag : tag.getList("altAccounts", 10)) {
            NbtCompound altAccountTag = (NbtCompound) itemTag;
            if (!altAccountTag.contains("mainAccount")) continue;

            String mainAccount = altAccountTag.getString("mainAccount");
            if (getByMainAccount(mainAccount) != null) continue;

            AltAccount altAccount = new AltAccount(mainAccount);
            
            if (altAccountTag.contains("altAccounts")) {
                NbtList altList = altAccountTag.getList("altAccounts", 8); // 8 = String type
                for (NbtElement altElement : altList) {
                    altAccount.addAlt(altElement.asString());
                }
            }

            altAccounts.add(altAccount);
        }

        Collections.sort(altAccounts);

        MeteorExecutor.execute(() -> altAccounts.forEach(AltAccount::updateInfo));

        return this;
    }
}
