/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.user;

import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboard.LineId;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import de.timesnake.game.survivalgames.server.SurvivalGamesServerManager;
import de.timesnake.library.basic.util.Loggers;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SurvivalGamesUser extends GameUser {


  public SurvivalGamesUser(Player player) {
    super(player);
  }

  @Override
  public void onGameJoin() {
    super.onGameJoin();

    SurvivalGamesServer.updateSideboardPlayerAmount();
    this.setDefault();
    this.teleport(SurvivalGamesServer.nextSpawn());
    this.lockLocation();
    this.setBossBar(SurvivalGamesServer.getPeaceTimeBar());

    if (LoungeBridgeServer.getServerTeamAmount() > 0) {
      this.setSideboardTeam();
    }
    this.updateSideboardKills();
    if (SurvivalGamesServer.getMap().isNightVision()) {
      this.addPotionEffect(PotionEffectType.NIGHT_VISION, 1);
    } else {
      this.removePotionEffects();
    }

    if (SurvivalGamesServer.getWorldBorder() != null) {
      SurvivalGamesServer.getWorldBorder().addUser(this);
    } else {
      Loggers.GAME.warning("Unable to set world border for user " + this.getName());
    }

  }

  public void setSideboardTeam() {
    if (this.getTeam() != null) {
      SurvivalGamesServer.getGameSideboard().updateScore4User(this, LineId.TEAM,
          this.getTeam().getChatColor() + this.getTeam().getDisplayName());
    }
  }

  public void updateSideboardKills() {
    SurvivalGamesServer.getGameSideboard()
        .updateScore4User(this, LineId.KILLS, super.getKills());
  }

  @Override
  public void addKill() {
    super.addKill();
    this.updateSideboardKills();
    this.addCoins(SurvivalGamesServerManager.KILL_COINS, true);
  }

  @Override
  public void setKills(Integer kills) {
    super.setKills(kills);
    this.updateSideboardKills();
  }

  @Override
  public void joinSpectator() {
    super.joinSpectator();
    SurvivalGamesServer.getWorldBorder().removeUser(this);
    SurvivalGamesServer.getWorldBorder().addSpectator(this);

    if (SurvivalGamesServer.getMap().isNightVision()) {
      this.addPotionEffect(PotionEffectType.NIGHT_VISION, 1);
    } else {
      this.removePotionEffects();
    }
  }

  public void startGame() {
    this.unlockLocation();
  }

}
