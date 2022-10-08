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

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExWorldBorder;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;

public class SurvivalGamesServer extends LoungeBridgeServer {

    public static SurvivalGamesMap getMap() {
        return server.getMap();
    }

    public static Sideboard getSideboard() {
        return server.getSideboard();
    }

    public static Location nextSpawn() {
        return server.nextSpawn();
    }

    public static void updateSideboardPlayerAmount() {
        server.updateSideboardPlayerAmount();
    }

    public static ExWorldBorder getWorldBorder() {
        return server.getWorldBorder();
    }

    public static void shrinkBorder() {
        server.shrinkBorder();
    }

    public static Integer getRefillTime() {
        return server.getRefillTime();
    }

    public static void setRefillTime(Integer time) {
        server.setRefillTime(time);
    }

    public static Double getShrinkSpeed() {
        return server.getShrinkSpeed();
    }

    public static void setShrinkSpeed(Double speed) {
        server.setShrinkSpeed(speed);
    }

    public static BossBar getPeaceTimeBar() {
        return server.getPeaceTimeBar();
    }

    private static final SurvivalGamesServerManager server = SurvivalGamesServerManager.getInstance();
}
