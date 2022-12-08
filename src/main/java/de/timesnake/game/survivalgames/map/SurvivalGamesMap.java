/*
 * workspace.game-survivalgames.main
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

package de.timesnake.game.survivalgames.map;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.game.survivalgames.chat.Plugin;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class SurvivalGamesMap extends Map {

    public static final Integer SPECTATOR_SPAWN_INDEX = 0;

    public static final Integer DEFAULT_RADIUS = 100;

    public static final Integer DEFAULT_MIN_ITEMS_PER_CHEST = 3;
    public static final Integer DEFAULT_MAX_ITEMS_PER_CHEST = 6;

    public static final Integer DEFAULT_PEACE_TIME = 60;
    public static final Integer DEFAULT_REFILL_TIME = 420;

    public static final Integer DEFAULT_PLAYER_BORDER_SHRINK = 2;
    public static final Integer DEFAULT_TIME_BORDER_SHRINK = 480;

    public static final String RADIUS_TOKEN = "radius=";
    public static final String MIN_ITEMS_TOKEN = "minItems=";
    public static final String MAX_ITEMS_TOKEN = "maxItems=";
    public static final String PEACE_TIME_TOKEN = "peace=";
    public static final String REFILL_TIME_TOKEN = "refill=";
    public static final String PLAYER_BORDER_SHRINK_TOKEN = "playerborder=";
    public static final String TIME_BORDER_SHRINK_TOKEN = "timeborder=";
    public static final String NIGHT_VISION_TOKEN = "nightvision";
    public static final String NO_FALL_DAMAGE_TOKEN = "nofalldamage";
    public static final String CHEST_CHANCE_TOKEN = "chestChance=";

    private static boolean isBlockAChest(ExBlock block) {
        return block.getBlock().getType().equals(Material.CHEST) || block.getBlock().getType().equals(Material.TRAPPED_CHEST);
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
    protected Integer radius = DEFAULT_RADIUS;
    protected Integer minItemsPerChest = DEFAULT_MIN_ITEMS_PER_CHEST;
    protected Integer maxItemsPerChest = DEFAULT_MAX_ITEMS_PER_CHEST;
    protected Integer peaceTime = DEFAULT_PEACE_TIME;
    protected Integer refillTime = DEFAULT_REFILL_TIME;
    protected Integer playerBorderShrink = DEFAULT_PLAYER_BORDER_SHRINK;
    protected Integer timeBorderShrink = DEFAULT_TIME_BORDER_SHRINK;
    protected boolean nightVision = false;
    protected boolean noFallDamage = false;
    private float chestChance = 1f;

    public SurvivalGamesMap(DbMap map) {
        super(map, true);

        ArrayList<String> infos = map.getInfo();
        if (info != null) {

            for (String info : infos) {

                if (info.contains(RADIUS_TOKEN)) {
                    info = info.replace(RADIUS_TOKEN, "");
                    try {
                        this.radius = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load radius of map " + super.name + ", " +
                                "info-radius is not an integer");
                    }
                } else if (info.contains(MIN_ITEMS_TOKEN)) {
                    info = info.replace(MIN_ITEMS_TOKEN, "");
                    try {
                        this.minItemsPerChest = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load minItems of map " + super.name + ", " +
                                "info-minItems is not an integer");
                    }
                } else if (info.contains(MAX_ITEMS_TOKEN)) {
                    info = info.replace(MAX_ITEMS_TOKEN, "");
                    try {
                        this.maxItemsPerChest = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load maxItems of map " + super.name + ", " +
                                "info-maxItems is not an integer");
                    }
                } else if (info.contains(PEACE_TIME_TOKEN)) {
                    info = info.replace(PEACE_TIME_TOKEN, "");
                    try {
                        this.peaceTime = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load peace time of map " + super.name +
                                ", " +
                                "info peace time is not an integer");
                    }
                } else if (info.contains(REFILL_TIME_TOKEN)) {
                    info = info.replace(REFILL_TIME_TOKEN, "");
                    try {
                        this.refillTime = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load refill time of map " + super.name +
                                "," +
                                " info refill time is not an integer");
                    }
                } else if (info.contains(PLAYER_BORDER_SHRINK_TOKEN)) {
                    info = info.replace(PLAYER_BORDER_SHRINK_TOKEN, "");
                    try {
                        this.playerBorderShrink = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES,
                                "Can not load player border shrink of map " + super.name + ", info player border " +
                                        "shrink is not an integer");
                    }
                } else if (info.contains(TIME_BORDER_SHRINK_TOKEN)) {
                    info = info.replace(TIME_BORDER_SHRINK_TOKEN, "");
                    try {
                        this.timeBorderShrink = Integer.parseInt(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES,
                                "Can not load time border shrink of map " + super.name + ", info time border shrink " +
                                        "is not an integer");
                    }
                } else if (info.contains(NIGHT_VISION_TOKEN)) {
                    this.nightVision = true;
                } else if (info.contains(NO_FALL_DAMAGE_TOKEN)) {
                    this.noFallDamage = true;
                } else if (info.contains(CHEST_CHANCE_TOKEN)) {
                    info = info.replace(CHEST_CHANCE_TOKEN, "");
                    try {
                        this.chestChance = Float.parseFloat(info);
                    } catch (NumberFormatException e) {
                        Server.printWarning(Plugin.SURVIVAL_GAMES,
                                "Can not load time chest chance of map " + super.name + ", info time chest chance is " +
                                        "not a float");
                    }
                }
            }


            Server.printText(Plugin.SURVIVAL_GAMES, "Map " + super.name +
                    " radius: " + this.radius +
                    " minItems: " + this.minItemsPerChest +
                    " maxItems: " + this.maxItemsPerChest +
                    " peace time: " + this.peaceTime +
                    " refill time: " + this.refillTime +
                    " player border shrink: " + this.playerBorderShrink +
                    " time border shrink: " + this.timeBorderShrink +
                    " night vision: " + this.nightVision +
                    " no fall-damage: " + this.noFallDamage +
                    " chest chance: " + this.chestChance, "Map");


        } else {
            Server.printWarning(Plugin.SURVIVAL_GAMES, "Info of map " + super.name + " is null, " + "loaded standard " +
                    "values");
        }

        if (this.getWorld() != null) {
            this.world.restrict(ExWorld.Restriction.BLOCK_BREAK, true);
            this.world.restrict(ExWorld.Restriction.ENTITY_BLOCK_BREAK, true);
            this.world.restrict(ExWorld.Restriction.BLOCK_PLACE, true);
            this.world.setExceptService(true);
            this.world.restrict(ExWorld.Restriction.FIRE_SPREAD, true);
            this.world.restrict(ExWorld.Restriction.BLOCK_IGNITE, true);
            this.world.restrict(ExWorld.Restriction.ENTITY_EXPLODE, true);
            this.world.restrict(ExWorld.Restriction.BLOCK_BURN_UP, true);
            this.world.restrict(ExWorld.Restriction.FLINT_AND_STEEL, false);
            this.world.restrict(ExWorld.Restriction.LIGHT_UP_INTERACTION, false);
            this.world.restrict(ExWorld.Restriction.FLUID_PLACE, true);
            this.world.restrict(ExWorld.Restriction.FLUID_COLLECT, true);
            this.world.restrict(ExWorld.Restriction.ENTITY_EXPLODE, true);
            this.world.restrict(ExWorld.Restriction.PLACE_IN_BLOCK, true);

            this.world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            this.world.setGameRule(GameRule.DO_FIRE_TICK, false);
            this.world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            this.world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            this.world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            this.world.setAutoSave(false);

            for (Entity entity : this.world.getEntities()) {
                if (entity instanceof LivingEntity || entity instanceof Monster) {
                    entity.remove();
                }
            }

            Server.getWorldManager().backupWorld(this.getWorld());
        }


        this.initChests();
    }

    public void initChests() {
        if (super.world == null) {
            Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load chests in map " + super.name + ", world is null");
            return;
        }

        if (this.getSpawn() == null) {
            Server.printWarning(Plugin.SURVIVAL_GAMES, "Can not load chests in map " + super.name + ", spawn (number:" +
                    " " +
                    "0) is null");
        }

        ExLocation spawn = this.getSpawn();
        Server.printText(Plugin.SURVIVAL_GAMES,
                "Map center: " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ());

        Random random = new Random();

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

        Server.printText(Plugin.SURVIVAL_GAMES, "Map: " + super.name + " chests: " + this.chests.size(), "Map");
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
