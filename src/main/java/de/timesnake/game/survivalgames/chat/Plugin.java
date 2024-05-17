/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.chat;

public class Plugin extends de.timesnake.library.chat.Plugin {

  public static final Plugin SURVIVAL_GAMES = new Plugin("SurvivalGames", "GSG");

  protected Plugin(String name, String code) {
    super(name, code);
  }
}
