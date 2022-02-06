package de.timesnake.game.hungergames.main;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.game.hungergames.chat.Plugin;
import de.timesnake.game.hungergames.server.HungerGamesServerManager;
import de.timesnake.game.hungergames.user.HungerGamesCmd;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GameHungerGames extends JavaPlugin {

    private static GameHungerGames plugin;


    @Override
    public void onLoad() {
        ServerManager.setInstance(new HungerGamesServerManager());
    }

    @Override
    public void onEnable() {
        GameHungerGames.plugin = this;

        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(HungerGamesServerManager.getInstance(), this);

        Server.getCommandManager().addCommand(this, "hg", List.of("hungergames"), new HungerGamesCmd(), Plugin.HUNGER_GAMES);

        HungerGamesServerManager.getInstance().onHungerGamesEnable();
    }

    public static GameHungerGames getPlugin() {
        return plugin;
    }
}
