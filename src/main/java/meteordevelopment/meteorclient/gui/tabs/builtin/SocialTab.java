/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPeople;
import meteordevelopment.meteorclient.systems.blacklistedpeople.BlacklistedPerson;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPerson;
import meteordevelopment.meteorclient.systems.scarypeople.ScaryPeople;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;

public class SocialTab extends Tab {
    public SocialTab() {
        super("Social");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new SocialScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof SocialScreen;
    }

    private static class SocialScreen extends WindowTabScreen {
        public SocialScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            // Create main horizontal container for side-by-side layout
            WHorizontalList mainContainer = add(theme.horizontalList()).expandX().widget();
            
            // Scary People Section (left side)
            WVerticalList scarySection = mainContainer.add(theme.verticalList()).expandX().widget();
            scarySection.add(theme.label("Scary People")).expandX().center();
            scarySection.add(theme.horizontalSeparator()).expandX();
            
            WTable scaryTable = scarySection.add(theme.table()).expandX().minWidth(200).widget();
            initScaryTable(scaryTable);

            scarySection.add(theme.horizontalSeparator()).expandX();

            // Scary People input section
            WHorizontalList scaryInputList = scarySection.add(theme.horizontalList()).expandX().widget();
            WTextBox scaryNameW = scaryInputList.add(theme.textBox("", (text, c) -> c != ' ')).expandX().widget();
            scaryNameW.setFocused(true);

            WPlus scaryAdd = scaryInputList.add(theme.plus()).widget();
            scaryAdd.action = () -> {
                String name = scaryNameW.get().trim();
                ScaryPerson scaryPerson = new ScaryPerson(name);

                if (ScaryPeople.get().add(scaryPerson)) {
                    scaryNameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        scaryPerson.updateInfo();
                        reload();
                    });
                }
            };

            // Vertical divider between sections
            mainContainer.add(theme.verticalSeparator()).centerY();

            // Blacklisted People Section (right side)  
            WVerticalList blacklistedSection = mainContainer.add(theme.verticalList()).expandX().widget();
            blacklistedSection.add(theme.label("Blacklisted People")).expandX().center();
            blacklistedSection.add(theme.horizontalSeparator()).expandX();
            
            WTable blacklistedTable = blacklistedSection.add(theme.table()).expandX().minWidth(200).widget();
            initBlacklistedTable(blacklistedTable);

            blacklistedSection.add(theme.horizontalSeparator()).expandX();

            // Blacklisted People input section
            WHorizontalList blacklistedInputList = blacklistedSection.add(theme.horizontalList()).expandX().widget();
            WTextBox blacklistedNameW = blacklistedInputList.add(theme.textBox("", (text, c) -> c != ' ')).expandX().widget();

            WPlus blacklistedAdd = blacklistedInputList.add(theme.plus()).widget();
            blacklistedAdd.action = () -> {
                String name = blacklistedNameW.get().trim();
                BlacklistedPerson blacklistedPerson = new BlacklistedPerson(name);

                if (BlacklistedPeople.get().add(blacklistedPerson)) {
                    blacklistedNameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        blacklistedPerson.updateInfo();
                        reload();
                    });
                }
            };

            enterAction = scaryAdd.action;
        }

        private void initScaryTable(WTable table) {
            table.clear();
            if (ScaryPeople.get().isEmpty()) return;

            ScaryPeople.get().forEach(scaryPerson ->
                MeteorExecutor.execute(() -> {
                    if (scaryPerson.headTextureNeedsUpdate()) {
                        scaryPerson.updateInfo();
                        reload();
                    }
                })
            );

            for (ScaryPerson scaryPerson : ScaryPeople.get()) {
                table.add(theme.texture(32, 32, scaryPerson.getHead().needsRotate() ? 90 : 0, scaryPerson.getHead()));
                
                // Get color from BetterTab if it's active
                BetterTab betterTab = Modules.get().get(BetterTab.class);
                Color nameColor = betterTab != null && betterTab.isActive() ? betterTab.getPlayerColor(scaryPerson.getName()) : null;
                
                if (nameColor != null) {
                    table.add(theme.label(scaryPerson.getName())).widget().color = nameColor;
                } else {
                    table.add(theme.label(scaryPerson.getName()));
                }

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    ScaryPeople.get().remove(scaryPerson);
                    reload();
                };

                table.row();
            }
        }

        private void initBlacklistedTable(WTable table) {
            table.clear();
            if (BlacklistedPeople.get().isEmpty()) return;

            BlacklistedPeople.get().forEach(blacklistedPerson ->
                MeteorExecutor.execute(() -> {
                    if (blacklistedPerson.headTextureNeedsUpdate()) {
                        blacklistedPerson.updateInfo();
                        reload();
                    }
                })
            );

            for (BlacklistedPerson blacklistedPerson : BlacklistedPeople.get()) {
                table.add(theme.texture(32, 32, blacklistedPerson.getHead().needsRotate() ? 90 : 0, blacklistedPerson.getHead()));
                
                // Get color from BetterTab if it's active
                BetterTab betterTab = Modules.get().get(BetterTab.class);
                Color nameColor = betterTab != null && betterTab.isActive() ? betterTab.getPlayerColor(blacklistedPerson.getName()) : null;
                
                if (nameColor != null) {
                    table.add(theme.label(blacklistedPerson.getName())).widget().color = nameColor;
                } else {
                    table.add(theme.label(blacklistedPerson.getName()));
                }

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    BlacklistedPeople.get().remove(blacklistedPerson);
                    reload();
                };

                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(ScaryPeople.get()) && NbtUtils.toClipboard(BlacklistedPeople.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(ScaryPeople.get()) && NbtUtils.fromClipboard(BlacklistedPeople.get());
        }
    }
}