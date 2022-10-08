/*
 * game-survivalgames.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.game.survivalgames.item;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.hungergames.DbHungerGamesItem;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SurvivalGamesItemManager {

    private static final Double LEVEL_CHANCE = 0.5;

    private final Random random = new Random();

    private final HashMap<Integer, LinkedList<SurvivalGamesItem>> itemsByLevel = new HashMap<>();
    private final HashMap<Integer, Float> chanceSumPerLevel = new HashMap<>();

    private final HashSet<ExBlock> filledChests = new HashSet<>();

    public SurvivalGamesItemManager() {

        for (DbHungerGamesItem dbItem : Database.getHungerGames().getItems()) {
            SurvivalGamesItem item;
            try {
                item = new SurvivalGamesItem(dbItem);
            } catch (InvalidSurvivalGamesItemTypeException e) {
                Server.printWarning(Plugin.SURVIVAL_GAMES, e.getMessage(), "Item");
                continue;
            }

            LinkedList<SurvivalGamesItem> items = this.itemsByLevel.get(item.getLevel());

            if (items == null) {
                items = new LinkedList<>();
            }

            items.add(item);
            this.itemsByLevel.put(item.getLevel(), items);
            Server.printText(Plugin.SURVIVAL_GAMES,
                    "Added item " + item.getItem().getType().name().toLowerCase() + " " + item.getItem().getAmount() + " chance " + item.getChance() + " level " + item.getLevel(), "Item");

            Float chanceSum = this.chanceSumPerLevel.get(item.getLevel());

            if (chanceSum == null) {
                chanceSum = item.getChance();
            } else {
                chanceSum += item.getChance();
            }

            this.chanceSumPerLevel.put(item.getLevel(), chanceSum);

        }

    }

    public HashMap<Integer, LinkedList<SurvivalGamesItem>> getItemsByLevel() {
        return itemsByLevel;
    }

    public LinkedList<SurvivalGamesItem> getItems(Integer level) {
        return itemsByLevel.get(level);
    }

    public void fillMapChests(int chestLevel) {
        SurvivalGamesMap map = SurvivalGamesServer.getMap();
        if (map != null) {
            if (chestLevel == 0) {
                this.filledChests.clear();

                for (ExBlock block : map.getChests()) {
                    if (random.nextFloat() < map.getChestChance() || block.getBlock().getType().equals(Material.TRAPPED_CHEST)) {
                        this.fillInventory(chestLevel, block);
                        this.filledChests.add(block);
                    } else {
                        block.getBlock().setType(Material.AIR);
                    }
                }
            } else {
                for (ExBlock block : this.filledChests) {
                    if (!block.getBlock().getType().equals(Material.TRAPPED_CHEST) || this.random.nextFloat() < map.getChestChance()) {
                        this.fillInventory(chestLevel, block);
                    }
                }
            }
        }
    }

    public void fillInventory(Integer level, ExBlock block) {

        SurvivalGamesMap map = SurvivalGamesServer.getMap();

        Inventory chestInventory = SurvivalGamesMap.getInventoryOfChest(block);

        chestInventory.clear();

        int amount =
                this.random.nextInt(map.getMaxItemsPerChest() - map.getMinItemsPerChest() + 1) + map.getMinItemsPerChest();

        Set<Integer> usedSlots = new HashSet<>();

        int chestSize = chestInventory.getSize();

        for (int size = 0; size < chestSize; size += 27) {
            for (int i = 0; i < amount; i++) {
                int itemLevel;

                if (level == 0) {
                    itemLevel = 0;
                } else {
                    itemLevel = Math.random() > LEVEL_CHANCE ? level : level - 1;
                }

                int slot;
                if (usedSlots.size() >= chestSize) {
                    break;
                }
                do {
                    slot = this.random.nextInt(27) + size;
                } while (usedSlots.contains(slot));

                ItemStack item = this.getRandomItem(itemLevel);
                if (item != null) {
                    chestInventory.setItem(slot, item);
                    usedSlots.add(size);
                } else {
                    i--;
                }
            }
        }

    }

    private ItemStack getRandomItem(Integer level) {
        if (this.chanceSumPerLevel.get(level) == null) {
            if (level.equals(0)) {
                Server.printError(Plugin.SURVIVAL_GAMES, "Too few items", "Item");
                return null;
            } else {
                return this.getRandomItem(level - 1);
            }
        }
        double random = this.random.nextFloat() * this.chanceSumPerLevel.get(level);
        for (SurvivalGamesItem item : this.getItems(level)) {
            if (random < item.getChance()) {
                return item.getItem();
            }
            random -= item.getChance();
        }
        return null;
    }
}
