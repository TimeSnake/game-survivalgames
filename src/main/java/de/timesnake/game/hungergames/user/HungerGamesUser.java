package de.timesnake.game.hungergames.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.game.hungergames.chat.Plugin;
import de.timesnake.game.hungergames.server.HungerGamesServer;
import de.timesnake.game.hungergames.server.HungerGamesServerManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class HungerGamesUser extends GameUser {


    public HungerGamesUser(Player player) {
        super(player);
    }

    @Override
    public void joinGame() {
        HungerGamesServer.updateSideboardPlayerAmount();
        this.setDefault();
        this.teleport(HungerGamesServer.nextSpawn());
        this.lockLocation(true);
        this.setSideboard(HungerGamesServer.getSideboard());
        if (LoungeBridgeServer.getServerTeamAmount() > 0) {
            this.setSideboardTeam();
        }
        this.updateSideboardKills();
        if (HungerGamesServer.getMap().isNightVision()) {
            this.addPotionEffect(PotionEffectType.NIGHT_VISION, 1);
        } else {
            this.removePotionEffects();
        }

        if (HungerGamesServer.getWorldBorder() != null) {
            HungerGamesServer.getWorldBorder().addUser(this);
        } else {
            Server.printWarning(Plugin.HUNGER_GAMES, "Unable to set world border for user " + this.getName());
        }

    }

    public void setSideboardTeam() {
        if (this.getTeam() != null) {
            this.setSideboardScore(6, this.getTeam().getDisplayName());
        }
    }

    public void updateSideboardKills() {
        this.setSideboardScore(0, super.getKills() + "");
    }

    @Override
    public void addKill() {
        super.addKill();
        this.updateSideboardKills();
        this.addCoins(HungerGamesServerManager.KILL_COINS, true);
    }

    @Override
    public void setKills(Integer kills) {
        super.setKills(kills);
        this.updateSideboardKills();
    }

    @Override
    public void joinSpectator() {
        super.joinSpectator();
        HungerGamesServer.getWorldBorder().removeUser(this);
        HungerGamesServer.getWorldBorder().addSpectator(this);
    }

    public void startGame() {
        this.lockLocation(false);
    }

}
