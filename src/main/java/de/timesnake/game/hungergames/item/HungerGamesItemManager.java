package de.timesnake.game.hungergames.item;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.hungergames.DbHungerGamesItem;
import de.timesnake.game.hungergames.chat.Plugin;
import de.timesnake.game.hungergames.map.HungerGamesMap;
import de.timesnake.game.hungergames.server.HungerGamesServer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class HungerGamesItemManager {

    private static final Double LEVEL_CHANCE = 0.5;

    private final Random random = new Random();

    private final HashMap<Integer, LinkedList<HungerGamesItem>> itemsByLevel = new HashMap<>();
    private final HashMap<Integer, Float> chanceSumPerLevel = new HashMap<>();

    private final HashSet<ExBlock> filledChests = new HashSet<>();

    public HungerGamesItemManager() {

        for (DbHungerGamesItem dbItem : Database.getHungerGames().getItems()) {
            HungerGamesItem item;
            try {
                item = new HungerGamesItem(dbItem);
            } catch (InvalidHungerGamesItemTypeException e) {
                Server.printWarning(Plugin.HUNGER_GAMES, e.getMessage(), "Item");
                continue;
            }

            LinkedList<HungerGamesItem> items = this.itemsByLevel.get(item.getLevel());

            if (items == null) {
                items = new LinkedList<>();
            }

            items.add(item);
            this.itemsByLevel.put(item.getLevel(), items);
            Server.printText(Plugin.HUNGER_GAMES,
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

    public HashMap<Integer, LinkedList<HungerGamesItem>> getItemsByLevel() {
        return itemsByLevel;
    }

    public LinkedList<HungerGamesItem> getItems(Integer level) {
        return itemsByLevel.get(level);
    }

    public void fillMapChests(int chestLevel) {
        HungerGamesMap map = HungerGamesServer.getMap();
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

        HungerGamesMap map = HungerGamesServer.getMap();

        Inventory chestInventory = HungerGamesMap.getInventoryOfChest(block);

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
                Server.printError(Plugin.HUNGER_GAMES, "Too few items", "Item");
                return null;
            } else {
                return this.getRandomItem(level - 1);
            }
        }
        double random = this.random.nextFloat() * this.chanceSumPerLevel.get(level);
        for (HungerGamesItem item : this.getItems(level)) {
            if (random < item.getChance()) {
                return item.getItem();
            }
            random -= item.getChance();
        }
        return null;
    }
}
