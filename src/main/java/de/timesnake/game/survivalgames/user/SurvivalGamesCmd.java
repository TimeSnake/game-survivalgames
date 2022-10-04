package de.timesnake.game.survivalgames.user;

import de.timesnake.basic.bukkit.util.chat.Argument;
import de.timesnake.basic.bukkit.util.chat.CommandListener;
import de.timesnake.basic.bukkit.util.chat.Sender;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class SurvivalGamesCmd implements CommandListener {

    private Code.Permission borderPerm;
    private Code.Permission refillPerm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
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
                    sender.sendPluginMessage(Component.text("Forced border shrink", ExTextColor.PERSONAL));
                } else if (task.equalsIgnoreCase("speed")) {
                    if (!(args.isLengthEquals(3, true) && args.get(2).isDouble(true))) {
                        return;
                    }

                    Double speed = args.get(2).toDouble();

                    SurvivalGamesServer.setShrinkSpeed(speed);
                    sender.sendPluginMessage(Component.text("Updated shrink speed to ", ExTextColor.PERSONAL)
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
                sender.sendPluginMessage(Component.text("Forced chest refill in ", ExTextColor.PERSONAL)
                        .append(Component.text(SurvivalGamesServer.getRefillTime() + "s", ExTextColor.VALUE)));
            }
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

    @Override
    public void loadCodes(Plugin plugin) {
        this.borderPerm = plugin.createPermssionCode("svg", "survivalgames.border");
        this.refillPerm = plugin.createPermssionCode("svg", "survivalgames.refill");
    }
}
