package de.timesnake.game.hungergames.user;

import de.timesnake.basic.bukkit.util.chat.Argument;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.chat.CommandListener;
import de.timesnake.basic.bukkit.util.chat.Sender;
import de.timesnake.game.hungergames.server.HungerGamesServer;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class HungerGamesCmd implements CommandListener {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!args.isLengthHigherEquals(1, true)) {
            return;
        }

        switch (args.getString(0).toLowerCase()) {
            case "border":
                if (!sender.hasPermission("hungergames.border", 2409)) {
                    return;
                }

                if (!args.isLengthHigherEquals(2, true)) {
                    return;
                }

                String task = args.getString(1);

                if (task.equalsIgnoreCase("begin")) {
                    HungerGamesServer.shrinkBorder();
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Forced border shrink");
                } else if (task.equalsIgnoreCase("speed")) {
                    if (!(args.isLengthEquals(3, true) && args.get(2).isDouble(true))) {
                        return;
                    }

                    Double speed = args.get(2).toDouble();

                    HungerGamesServer.setShrinkSpeed(speed);
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Updated shrink speed to " + speed);

                }
                break;
            case "refill":
                if (!sender.hasPermission("hungergames.refill", 2410)) {
                    return;
                }

                if (HungerGamesServer.getRefillTime() < 60) {
                    HungerGamesServer.setRefillTime(10);
                } else {
                    HungerGamesServer.setRefillTime(60);
                }

                sender.sendPluginMessage(ChatColor.PERSONAL + "Forced chest refill in " + ChatColor.VALUE + HungerGamesServer.getRefillTime() + "s");
                break;
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.getLength() == 1) {
            return List.of("border", "refill");
        } else if (args.getLength() == 2) {
            if (args.get(0).equalsIgnoreCase("border")) {
                return List.of("begin", "speed");
            }
        } else if (args.getLength() == 3) {
            if (args.get(0).equalsIgnoreCase("border")) {
                if (args.get(1).equalsIgnoreCase("speed")) {
                    List.of("1", "2", "0.5");
                }
            }
        }
        return List.of();
    }
}
