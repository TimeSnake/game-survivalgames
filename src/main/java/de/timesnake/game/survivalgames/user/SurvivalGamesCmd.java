/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.user;

import de.timesnake.basic.bukkit.util.chat.cmd.Argument;
import de.timesnake.basic.bukkit.util.chat.cmd.CommandListener;
import de.timesnake.basic.bukkit.util.chat.cmd.Completion;
import de.timesnake.basic.bukkit.util.chat.cmd.Sender;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;

public class SurvivalGamesCmd implements CommandListener {

  private final Code perm = Plugin.GAME.createPermssionCode("survivalgames");
  private final Code refillPerm = Plugin.GAME.createPermssionCode("survivalgames.refill");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!args.isLengthHigherEquals(1, true)) {
      return;
    }

    if (args.get(0).equalsIgnoreCase("refill")) {
      if (!sender.hasPermission(this.refillPerm)) {
        return;
      }
      if (SurvivalGamesServer.getRefillTime() < 60) {
        SurvivalGamesServer.setRefillTime(10);
      } else {
        SurvivalGamesServer.setRefillTime(60);
      }
      sender.sendPluginMessage(
          Component.text("Forced chest refill in ", ExTextColor.PERSONAL)
              .append(Component.text(SurvivalGamesServer.getRefillTime() + "s",
                  ExTextColor.VALUE)));
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion()
        .addArgument(new Completion(this.refillPerm, "refill"));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
