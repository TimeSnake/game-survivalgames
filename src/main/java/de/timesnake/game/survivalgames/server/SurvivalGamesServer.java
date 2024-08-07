/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboard;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
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

  public static ExSideboard getGameSideboard() {
    return server.getGameSideboard();
  }

  private static final SurvivalGamesServerManager server = SurvivalGamesServerManager.getInstance();
}
