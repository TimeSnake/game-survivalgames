package de.timesnake.game.hungergames.item;

import de.timesnake.database.util.hungergames.DbHungerGamesItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class HungerGamesItem {

    private final ItemStack item;
    private final Float chance;
    private final Integer level;

    public HungerGamesItem(ItemStack item, Float chance, Integer level) {
        this.item = item;
        this.chance = chance;
        this.level = level;
    }

    public HungerGamesItem(DbHungerGamesItem item) throws InvalidHungerGamesItemTypeException {
        if (item.getType() == null) {
            throw new InvalidHungerGamesItemTypeException(null);
        }

        Material type = Material.matchMaterial(item.getType());

        if (type == null) {
            throw new InvalidHungerGamesItemTypeException(item.getType());
        }

        this.item = new ItemStack(type, item.getAmount());
        this.chance = item.getChance();
        this.level = item.getLevel();
    }

    public ItemStack getItem() {
        return item;
    }

    public Float getChance() {
        return chance;
    }

    public Integer getLevel() {
        return level;
    }
}
