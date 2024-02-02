/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.map;

import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.library.basic.util.Loggers;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SurvivalGamesMap extends Map {

  public static final Integer SPECTATOR_SPAWN_INDEX = 0;

  public static final Integer DEFAULT_RADIUS = 100;

  public static final Integer DEFAULT_MIN_ITEMS_PER_CHEST = 3;
  public static final Integer DEFAULT_MAX_ITEMS_PER_CHEST = 6;

  public static final Integer DEFAULT_PEACE_TIME = 60;
  public static final Integer DEFAULT_REFILL_TIME = 420;

  public static final Integer DEFAULT_PLAYER_BORDER_SHRINK = 2;
  public static final Integer DEFAULT_TIME_BORDER_SHRINK = 480;

  private static boolean isBlockAChest(ExBlock block) {
    return block.getBlock().getType().equals(Material.CHEST) || block.getBlock().getType()
        .equals(Material.TRAPPED_CHEST);
  }

  public static Inventory getInventoryOfChest(ExBlock block) {
    if (block == null || block.getBlock() == null || !isBlockAChest(block)) {
      return null;
    }

    if (!(block.getBlock().getState() instanceof InventoryHolder)) {
      return null;
    }

    return ((InventoryHolder) block.getBlock().getState()).getInventory();
  }

  private final Set<ExBlock> chests = new HashSet<>();
  protected final int radius;
  protected final int minItemsPerChest;
  protected final int maxItemsPerChest;
  protected final int peaceTime;
  protected final int refillTime;
  protected final int playerBorderShrink;
  protected final int timeBorderShrink;
  private final float chestChance;
  protected final boolean nightVision = false;
  protected final boolean noFallDamage = false;

  public SurvivalGamesMap(DbMap map) {
    super(map, true);

    this.radius = this.getProperty("radius", Integer.class, DEFAULT_RADIUS,
        v -> Loggers.GAME.warning("Can not load radius of map " + super.name + ", radius is not an integer"));

    this.minItemsPerChest = this.getProperty("min_items", Integer.class, DEFAULT_MIN_ITEMS_PER_CHEST,
        v -> Loggers.GAME.warning("Can not load minItems of map " + super.name + ", minItems is not an integer"));

    this.maxItemsPerChest = this.getProperty("max_items", Integer.class, DEFAULT_MAX_ITEMS_PER_CHEST,
        v -> Loggers.GAME.warning("Can not load maxItems of map " + super.name + ", maxItems is not an integer"));

    this.peaceTime = this.getProperty("peace_time", Integer.class, DEFAULT_PEACE_TIME,
        v -> Loggers.GAME.warning("Can not load peace time of map " + super.name + ", peace time is not an integer"));

    this.refillTime = this.getProperty("refill_time", Integer.class, DEFAULT_REFILL_TIME,
        v -> Loggers.GAME.warning("Can not load refill time of map " + super.name + ", refill time is not an integer"));

    this.playerBorderShrink = this.getProperty("border_player", Integer.class, DEFAULT_PLAYER_BORDER_SHRINK,
        v -> Loggers.GAME.warning("Can not load player border shrink of map " + super.name + ", border player is not " +
            "an integer"));

    this.timeBorderShrink = this.getProperty("border_time", Integer.class, DEFAULT_TIME_BORDER_SHRINK,
        v -> Loggers.GAME.warning("Can not load time border shrink of map " + super.name + ", border time is not an " +
            "integer"));

    this.chestChance = this.getProperty("chest_chance", Float.class, 1f,
        v -> Loggers.GAME.warning("Can not load time chest chance of map " + super.name + ", chest chance is not a " +
            "float"));


    Loggers.GAME.info("Map " + super.name +
        " radius: " + this.radius +
        " minItems: " + this.minItemsPerChest +
        " maxItems: " + this.maxItemsPerChest +
        " peace time: " + this.peaceTime +
        " refill time: " + this.refillTime +
        " player border shrink: " + this.playerBorderShrink +
        " time border shrink: " + this.timeBorderShrink +
        " night vision: " + this.nightVision +
        " no fall-damage: " + this.noFallDamage +
        " chest chance: " + this.chestChance);

    if (this.getWorld() != null) {
      this.world.restrict(ExWorld.Restriction.BLOCK_BREAK, true);
      this.world.restrict(ExWorld.Restriction.ENTITY_BLOCK_BREAK, true);
      this.world.restrict(ExWorld.Restriction.BLOCK_PLACE, true);
      this.world.setExceptService(true);
      this.world.restrict(ExWorld.Restriction.FIRE_SPREAD_SPEED, 0f);
      this.world.restrict(ExWorld.Restriction.TNT_PRIME, true);
      this.world.restrict(ExWorld.Restriction.ENTITY_EXPLODE, true);
      this.world.restrict(ExWorld.Restriction.BLOCK_BURN_UP, true);
      this.world.restrict(ExWorld.Restriction.FLINT_AND_STEEL, false);
      this.world.restrict(ExWorld.Restriction.LIGHT_UP_INTERACTION, false);
      this.world.restrict(ExWorld.Restriction.FLUID_PLACE, true);
      this.world.restrict(ExWorld.Restriction.FLUID_COLLECT, true);
      this.world.restrict(ExWorld.Restriction.ENTITY_EXPLODE, true);
      this.world.restrict(ExWorld.Restriction.PLACE_IN_BLOCK, true);
      this.world.restrict(ExWorld.Restriction.OPEN_INVENTORIES, List.of(Material.ANVIL, Material.DAMAGED_ANVIL,
          Material.CHIPPED_ANVIL));

      this.world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
      this.world.setGameRule(GameRule.DO_FIRE_TICK, false);
      this.world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
      this.world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
      this.world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
      this.world.setAutoSave(false);

      for (Entity entity : this.world.getEntities()) {
        if (entity instanceof LivingEntity) {
          entity.remove();
        }
      }
    }

    this.initChests();
  }

  public void initChests() {
    if (super.world == null) {
      Loggers.GAME.warning("Can not load chests in map " + super.name + ", world is null");
      return;
    }

    if (this.getSpawn() == null) {
      Loggers.GAME.warning("Can not load chests in map " + super.name + ", spawn (number: 0) is null");
    }

    ExLocation spawn = this.getSpawn();
    Loggers.GAME.info("Map center: " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ());

    int xStart = spawn.getBlockX() - this.radius;
    int zStart = spawn.getBlockZ() - this.radius;

    int xEnd = spawn.getBlockX() + this.radius;
    int zEnd = spawn.getBlockZ() + this.radius;

    for (int x = xStart; x < xEnd; x++) {
      for (int z = zStart; z < zEnd; z++) {
        for (int y = this.world.getMinHeight(); y < this.world.getMaxHeight(); y++) {
          ExBlock block = spawn.getExWorld().getExBlockAt(x, y, z);
          if (getInventoryOfChest(block) != null) {
            this.chests.add(block);
          }
        }
      }
    }

    Loggers.GAME.info("Map: " + super.name + " chests: " + this.chests.size());
  }

  public ExLocation getSpawn() {
    return super.getLocation(0);
  }

  public Integer getRadius() {
    return radius;
  }

  public Integer getMinItemsPerChest() {
    return minItemsPerChest;
  }

  public Integer getMaxItemsPerChest() {
    return maxItemsPerChest;
  }

  public Collection<ExBlock> getChests() {
    return chests;
  }

  public ExLocation getSpectatorSpawn() {
    return super.getLocation(SPECTATOR_SPAWN_INDEX);
  }

  public Integer getPeaceTime() {
    return peaceTime;
  }

  public Integer getRefillTime() {
    return refillTime;
  }

  public Integer getPlayerBorderShrink() {
    return playerBorderShrink;
  }

  public Integer getTimeBorderShrink() {
    return timeBorderShrink;
  }

  public boolean isNightVision() {
    return nightVision;
  }

  public boolean isNoFallDamage() {
    return noFallDamage;
  }

  public float getChestChance() {
    return chestChance;
  }
}
