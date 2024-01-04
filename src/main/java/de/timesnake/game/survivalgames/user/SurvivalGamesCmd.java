/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.user;

import de.timesnake.basic.bukkit.util.chat.cmd.Argument;
import de.timesnake.basic.bukkit.util.chat.cmd.CommandListener;
import de.timesnake.basic.bukkit.util.chat.cmd.Completion;
import de.timesnake.basic.bukkit.util.chat.cmd.Sender;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.chat.Code;
import net.kyori.adventure.text.Component;

public class SurvivalGamesCmd implements CommandListener {

  private final Code perm = Plugin.SURVIVAL_GAMES.createPermssionCode("survivalgames");
  private final Code borderPerm = Plugin.SURVIVAL_GAMES.createPermssionCode("survivalgames.border");
  private final Code refillPerm = Plugin.SURVIVAL_GAMES.createPermssionCode("survivalgames.refill");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!args.isLengthHigherEquals(1, true)) {
      return;
    }

    switch (args.getString(0).toLowerCase()) {
      case "border" -> {
        if (!sender.hasPermission(this.borderPerm)) {
          return;
        }
        if (!args.isLengthHigherEquals(2, true)) {
          return;
        }
        String task = args.getString(1);
        if (task.equalsIgnoreCase("begin")) {
          SurvivalGamesServer.shrinkBorder();
          sender.sendPluginMessage(
              Component.text("Forced border shrink", ExTextColor.PERSONAL));
        } else if (task.equalsIgnoreCase("speed")) {
          if (!(args.isLengthEquals(3, true) && args.get(2).isDouble(true))) {
            return;
          }

          Double speed = args.get(2).toDouble();

          SurvivalGamesServer.setShrinkSpeed(speed);
          sender.sendPluginMessage(
              Component.text("Updated shrink speed to ", ExTextColor.PERSONAL)
                  .append(Component.text(speed, ExTextColor.VALUE)));

        }
      }
      case "refill" -> {
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
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion()
        .addArgument(new Completion(this.refillPerm, "refill"))
        .addArgument(new Completion(this.borderPerm, "border")
            .addArgument(new Completion("begin"))
            .addArgument(new Completion("speed")
                .addArgument(new Completion("1", "2", "4"))));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
