package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExWorldBorder;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;

public class SurvivalGamesServer extends LoungeBridgeServer {

    public static SurvivalGamesMap getMap() {
        return server.getMap();
    }

    public static Sideboard getSideboard() {
        return server.getSideboard();
    }

    public static Location nextSpawn() {
        return server.nextSpawn();
    }

    public static void updateSideboardPlayerAmount() {
        server.updateSideboardPlayerAmount();
    }

    public static ExWorldBorder getWorldBorder() {
        return server.getWorldBorder();
    }

    public static void shrinkBorder() {
        server.shrinkBorder();
    }

    public static Integer getRefillTime() {
        return server.getRefillTime();
    }

    public static void setRefillTime(Integer time) {
        server.setRefillTime(time);
    }

    public static Double getShrinkSpeed() {
        return server.getShrinkSpeed();
    }

    public static void setShrinkSpeed(Double speed) {
        server.setShrinkSpeed(speed);
    }

    public static BossBar getPeaceTimeBar() {
        return server.getPeaceTimeBar();
    }

    private static final SurvivalGamesServerManager server = SurvivalGamesServerManager.getInstance();
}
