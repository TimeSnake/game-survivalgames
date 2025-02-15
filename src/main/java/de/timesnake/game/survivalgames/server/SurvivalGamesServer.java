/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.user.scoreboard.KeyedSideboard;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import org.bukkit.Location;

public class SurvivalGamesServer extends LoungeBridgeServer {

  public static SurvivalGamesMap getMap() {
    return server.getMap();
  }

  public static Location nextSpawn() {
    return server.nextSpawn();
  }

  public static Integer getRefillTime() {
    return server.getRefillTime();
  }

  public static void setRefillTime(Integer time) {
    server.setRefillTime(time);
  }

  public static KeyedSideboard getGameSideboard() {
    return server.getGameSideboard();
  }

  private static final SurvivalGamesServerManager server = SurvivalGamesServerManager.getInstance();
}
