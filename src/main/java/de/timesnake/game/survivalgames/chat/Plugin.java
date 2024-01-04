/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.chat;

import de.timesnake.library.basic.util.LogHelper;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends de.timesnake.library.chat.Plugin {

  public static final Plugin SURVIVAL_GAMES = new Plugin("SurvivalGames", "GSG",
      LogHelper.getLogger("SurvivalGames", Level.INFO));

  protected Plugin(String name, String code, Logger logger) {
    super(name, code);
  }
}
