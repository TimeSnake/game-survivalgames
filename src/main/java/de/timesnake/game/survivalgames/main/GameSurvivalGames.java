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

package de.timesnake.game.survivalgames.main;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.server.SurvivalGamesServerManager;
import de.timesnake.game.survivalgames.user.SurvivalGamesCmd;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GameSurvivalGames extends JavaPlugin {

    public static GameSurvivalGames getPlugin() {
        return plugin;
    }

    private static GameSurvivalGames plugin;

    @Override
    public void onLoad() {
        ServerManager.setInstance(new SurvivalGamesServerManager());
    }

    @Override
    public void onEnable() {
        GameSurvivalGames.plugin = this;

        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(SurvivalGamesServerManager.getInstance(), this);

        Server.getCommandManager().addCommand(this, "hg", List.of("hungergames"), new SurvivalGamesCmd(),
                Plugin.SURVIVAL_GAMES);

        SurvivalGamesServerManager.getInstance().onHungerGamesEnable();
    }
}
