/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.main;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.game.survivalgames.server.SurvivalGamesServerManager;
import de.timesnake.game.survivalgames.user.SurvivalGamesCmd;
import de.timesnake.library.chat.Plugin;
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

    Server.getCommandManager().addCommand(this, "hg", List.of("hungergames"), new SurvivalGamesCmd(), Plugin.GAME);

    SurvivalGamesServerManager.getInstance().onHungerGamesEnable();
  }
}
