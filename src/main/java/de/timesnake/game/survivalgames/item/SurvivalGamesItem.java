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
