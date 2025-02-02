/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.loungebridge.util.tool.Timeable;
import de.timesnake.database.util.game.DbMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class SurvivalGamesMap extends Map implements Timeable {

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

  private final Logger logger = LogManager.getLogger("survival-games.map");

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
        v -> this.logger.warn("Can not load radius of map {}, radius is not an integer", super.name));

    this.minItemsPerChest = this.getProperty("min_items", Integer.class, DEFAULT_MIN_ITEMS_PER_CHEST,
        v -> this.logger.warn("Can not load minItems of map {}, minItems is not an integer", super.name));

    this.maxItemsPerChest = this.getProperty("max_items", Integer.class, DEFAULT_MAX_ITEMS_PER_CHEST,
        v -> this.logger.warn("Can not load maxItems of map {}, maxItems is not an integer", super.name));

    this.peaceTime = this.getProperty("peace_time", Integer.class, DEFAULT_PEACE_TIME,
        v -> this.logger.warn("Can not load peace time of map {}, peace time is not an integer", super.name));

    this.refillTime = this.getProperty("refill_time", Integer.class, DEFAULT_REFILL_TIME,
        v -> this.logger.warn("Can not load refill time of map {}, refill time is not an integer", super.name));

    this.playerBorderShrink = this.getProperty("border_player", Integer.class, DEFAULT_PLAYER_BORDER_SHRINK,
        v -> this.logger.warn("Can not load player border shrink of map {}, border player is not an integer", super.name));

    this.timeBorderShrink = this.getProperty("border_time", Integer.class, DEFAULT_TIME_BORDER_SHRINK,
        v -> this.logger.warn("Can not load time border shrink of map {}, border time is not an integer", super.name));

    this.chestChance = this.getProperty("chest_chance", Float.class, 1f,
        v -> this.logger.warn("Can not load time chest chance of map {}, chest chance is not a float", super.name));


    this.logger.info("Map {}: radius: {}, minItems: {}, maxItems: {}, peace time: {}, refill time: {}, " +
            "player border shrink: {}, time border shrink: {}, night vision: {}, no fall-damage: {}, chest chance: {}",
        super.name, this.radius, this.minItemsPerChest, this.maxItemsPerChest, this.peaceTime, this.refillTime,
        this.playerBorderShrink, this.timeBorderShrink, this.nightVision, this.noFallDamage, this.chestChance);

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
      this.logger.warn("Can not load chests in map '{}', world is null", super.name);
      return;
    }

    if (this.getSpawn() == null) {
      this.logger.warn("Can not load chests in map {}, spawn (number: 0) is null", super.name);
    }

    ExLocation spawn = this.getSpawn();
    this.logger.info("Map center: {} {} {}", spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());

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

    this.logger.info("Map: '{}' chests: {}", super.name, this.chests.size());
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

  @Override
  public int getTime() {
    return this.getPeaceTime();
  }
}
