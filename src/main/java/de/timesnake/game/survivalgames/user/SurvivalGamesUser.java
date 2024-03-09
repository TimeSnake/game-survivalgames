/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.user;

import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboard.LineId;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.server.SurvivalGamesServer;
import de.timesnake.game.survivalgames.server.SurvivalGamesServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SurvivalGamesUser extends GameUser {

  private final Logger logger = LogManager.getLogger("survival-games.user");

  public SurvivalGamesUser(Player player) {
    super(player);
  }

  @Override
  public void onGameJoin() {
    super.onGameJoin();

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
      this.logger.warn("Unable to set world border for user {}", this.getName());
    }

  }

  public void setSideboardTeam() {
    if (this.getTeam() != null) {
      SurvivalGamesServer.getGameSideboard().updateScore4User(this, LineId.TEAM,
          this.getTeam().getTDColor() + this.getTeam().getDisplayName());
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
    SurvivalGamesServer.getWorldBorder().addSpectator(this);

    if (SurvivalGamesServer.getMap().isNightVision()) {
      this.addPotionEffect(PotionEffectType.NIGHT_VISION, 1);
    } else {
      this.removePotionEffects();
    }
  }

  @Override
  public void onGameStart() {
    super.onGameStart();

    this.unlockLocation();
  }

  @Override
  public @Nullable ExLocation getRespawnLocation() {
    SurvivalGamesServer.getWorldBorder().removeUser(this);
    this.joinSpectator();
    return SurvivalGamesServer.getSpectatorSpawn();
  }

  @Override
  public List<ItemStack> onGameDeath() {
    if (this.getLastDamager() != null) {
      User damager = this.getLastDamager().getDamager();
      this.sendPluginTDMessage(Plugin.SURVIVAL_GAMES, damager.getTDChatName() + " §shealth: §v"
          + ((GameUser) damager).getHealthDisplay());
      this.logger.info("{}: {}", damager.getName(), ((GameUser) damager).getHealthDisplay());
    }

    return super.onGameDeath();
  }
}
