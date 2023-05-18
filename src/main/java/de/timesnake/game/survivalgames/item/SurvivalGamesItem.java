/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.item;

import de.timesnake.database.util.hungergames.DbHungerGamesItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SurvivalGamesItem {

  private final ItemStack item;
  private final Float chance;
  private final Integer level;

  public SurvivalGamesItem(ItemStack item, Float chance, Integer level) {
    this.item = item;
    this.chance = chance;
    this.level = level;
  }

  public SurvivalGamesItem(DbHungerGamesItem item) throws InvalidSurvivalGamesItemTypeException {
    if (item.getType() == null) {
      throw new InvalidSurvivalGamesItemTypeException(null);
    }

    Material type = Material.matchMaterial(item.getType());

    if (type == null) {
      throw new InvalidSurvivalGamesItemTypeException(item.getType());
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
