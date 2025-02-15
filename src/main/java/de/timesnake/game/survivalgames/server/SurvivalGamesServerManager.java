/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.server.ExTime;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.scoreboard.KeyedSideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.KeyedSideboard.LineId;
import de.timesnake.basic.bukkit.util.user.scoreboard.KeyedSideboardBuilder;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.loungebridge.util.game.TmpGame;
import de.timesnake.basic.loungebridge.util.server.EndMessage;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServerManager;
import de.timesnake.basic.loungebridge.util.tool.ToolWatcher;
import de.timesnake.basic.loungebridge.util.tool.advanced.BossBarMapTimerTool;
import de.timesnake.basic.loungebridge.util.tool.advanced.PlayerNumberTool;
import de.timesnake.basic.loungebridge.util.tool.advanced.WorldBorderTool;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.game.survivalgames.item.SurvivalGamesItemManager;
import de.timesnake.game.survivalgames.main.GameSurvivalGames;
import de.timesnake.game.survivalgames.user.SurvivalGamesUser;
import de.timesnake.library.basic.util.TimeCoins;
import de.timesnake.library.basic.util.statistics.IntegerStat;
import de.timesnake.library.basic.util.statistics.PercentStat;
import de.timesnake.library.basic.util.statistics.StatPeriod;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.Plugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

public class SurvivalGamesServerManager extends LoungeBridgeServerManager<TmpGame> implements
    Listener {

  public static final Double MIN_BORDER_SIZE = 30d;
  public static final Double BORDER_SHRINKING_TIME_MULTIPLIER = 1.5d;
  public static final double BOW_DAMAGE_MULTIPLIER = 0.7;

  public static final StatType<Integer> WINS = new IntegerStat("wins", "Wins", 0, 10, 2, true, 0,
      1);
  public static final StatType<Float> WIN_CHANCE = new PercentStat("win_chance", "Win Chance", 0f,
      10, 3,
      false, 0, 0);
  public static final StatType<Integer> KILLS = new IntegerStat("kills", "Kills", 0, 10, 4, true,
      0, 2);

  public static final float KILL_COINS = 3 * TimeCoins.MULTIPLIER;

  public static SurvivalGamesServerManager getInstance() {
    return (SurvivalGamesServerManager) ServerManager.getInstance();
  }

  private final Logger logger = LogManager.getLogger("survival-games.server");

  private SurvivalGamesItemManager itemManager;
  private Integer chestLevel = 0;
  private boolean stopAfterStart = false;
  private KeyedSideboard sideboard;
  private KeyedSideboard spectatorSideboard;
  private Integer spawnIndex = 1;
  private BukkitTask refillTask;
  private Integer refillTime;

  private WorldBorderTool worldBorderTool;
  private BossBarMapTimerTool peaceBarTimer;

  private BukkitTask pvpHintTask;

  public void onHungerGamesEnable() {
    super.onLoungeBridgeEnable();
    this.itemManager = new SurvivalGamesItemManager();

    KeyedSideboardBuilder sideboardBuilder = new KeyedSideboardBuilder()
        .name("survivalgames")
        .title("§6§lSurvivalGames")
        .lineSpacer();

    if (LoungeBridgeServer.getServerTeamAmount() > 0) {
      sideboardBuilder.addLine(LineId.TEAM);
    }

    sideboardBuilder.addLine(LineId.PLAYERS_OF)
        .addLine(LineId.KILLS);

    this.sideboard = Server.getScoreboardManager().registerExSideboard(sideboardBuilder);

    KeyedSideboardBuilder spectatorSideboardBuilder = new KeyedSideboardBuilder()
        .name("sg_spec")
        .title("§6§lSurvivalGames")
        .lineSpacer()
        .addLine(LineId.PLAYERS_OF)
        .addLine(LineId.MAP);

    this.spectatorSideboard = Server.getScoreboardManager()
        .registerExSideboard(spectatorSideboardBuilder);

    this.peaceBarTimer = new BossBarMapTimerTool() {
      @Override
      public String getTitle(String time) {
        return "Peace time ends in §c" + time;
      }

      @Override
      public ToolWatcher getWatchers() {
        return ToolWatcher.ALL;
      }

      @Override
      public String getChatMessage(String time) {
        return "§pPeace time ends in §v" + time;
      }

      @Override
      public String getEndChatMessage() {
        return "§pPeace time ends §wnow";
      }
    };
    this.getToolManager().add(this.peaceBarTimer);

    if (LoungeBridgeServer.getServerTeamAmount() > 0) {
      LoungeBridgeServer.setTeamMateDamage(false);
    }

    this.getToolManager().add(new PlayerNumberTool() {
      @Override
      public void onPlayerUpdate() {
        Integer amount = Server.getInGameUsers().size() + Server.getPreGameUsers().size();
        Integer max = SurvivalGamesServerManager.this.getMaxPlayers();
        SurvivalGamesServerManager.this.sideboard.updateScore(LineId.PLAYERS_OF, amount + " §7/§f " + max);
        SurvivalGamesServerManager.this.spectatorSideboard.updateScore(LineId.PLAYERS_OF, amount + " §7/§f " + max);

        if (SurvivalGamesServerManager.this.checkGameEnd()) {
          SurvivalGamesServerManager.this.stopGame();
        } else {
          SurvivalGamesServerManager.this.checkPlayerBorderShrink();
        }
      }
    });

    this.worldBorderTool = new WorldBorderTool() {
      @Override
      public Location getBorderCenter() {
        return SurvivalGamesServerManager.this.getMap().getSpectatorSpawn();
      }

      @Override
      public double getBorderSize() {
        return SurvivalGamesServerManager.this.getMap().getRadius() * 2;
      }

      @Override
      public double getBorderDamagePerSec() {
        return 1;
      }

      @Override
      public void onShrink() {
        Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
        Server.runTaskLaterSynchrony(
            () -> Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A)), 10,
            GameSurvivalGames.getPlugin());
        SurvivalGamesServer.broadcastGameTDMessage("§wThe border begins to shrink. Watch out!");
      }
    };
    this.getToolManager().add(this.worldBorderTool);
  }

  @Override
  public SurvivalGamesUser loadUser(Player player) {
    return new SurvivalGamesUser(player);
  }

  @Override
  protected TmpGame loadGame(DbGame dbGame, boolean loadWorlds) {
    return new TmpGame((DbTmpGame) dbGame, true) {
      @Override
      public Map loadMap(DbMap dbMap, boolean loadWorld) {
        return new SurvivalGamesMap(dbMap);
      }
    };
  }

  public Location nextSpawn() {
    Location spawn = this.getMap().getLocation(this.spawnIndex);
    if (spawn == null) {
      if (this.spawnIndex == 1) {
        this.logger.warn("Too few spawns in map '{}'", this.getMap().getName());
        return this.getMap().getSpectatorSpawn();
      }
      this.spawnIndex = 1;
      return this.nextSpawn();
    }
    this.spawnIndex++;
    return spawn;
  }

  @Override
  public SurvivalGamesMap getMap() {
    return (SurvivalGamesMap) super.getMap();
  }

  @Override
  public void onMapLoad() {
    SurvivalGamesMap map = this.getMap();
    this.itemManager.fillMapChests(this.chestLevel);
    this.chestLevel++;
    this.logger.info("Chests filled in map '{}'", map.getName());
    map.getWorld().setTime(1000);

    for (Entity entity : map.getWorld().getEntitiesByClass(Item.class)) {
      entity.remove();
    }

    this.peaceBarTimer.setTime(map.getPeaceTime());
    this.updateMapOnSideboard();
  }

  @Override
  public void onGameStart() {
    if (this.stopAfterStart) {
      this.stopGame();
    }

    this.getMap().getWorld().setTime(1000);
    this.startRefillTask();
    this.worldBorderTool.shrinkBorder(MIN_BORDER_SIZE,
        ExTime.ofSeconds((int) (this.getMap().getRadius() * 2 * BORDER_SHRINKING_TIME_MULTIPLIER)),
        ExTime.ofSeconds(this.getMap().getTimeBorderShrink()));
    this.startPvPHintTask();
  }

  public void startRefillTask() {

    this.refillTime = this.getMap().getRefillTime();

    if (this.refillTask != null) {
      this.refillTask.cancel();
    }

    this.refillTask = Server.runTaskTimerSynchrony(() -> {
      switch (refillTime) {
        case 60, 30, 10, 5 -> broadcastGameTDMessage("Chest refill in §v" + Chat.getTimeString(refillTime));
        case 0 -> {
          broadcastGameTDMessage("§wChests were refilled");
          Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
          this.itemManager.fillMapChests(this.chestLevel);
          this.chestLevel++;
          refillTime = getMap().getRefillTime();
        }
      }

      refillTime--;
    }, 0, 20, GameSurvivalGames.getPlugin());
  }

  public void checkPlayerBorderShrink() {
    if (Server.getInGameUsers().size() <= this.getMap().getPlayerBorderShrink() || (
        LoungeBridgeServer.getNotEmptyGameTeams().size() <= this.getMap()
            .getPlayerBorderShrink() && LoungeBridgeServer.getServerTeamAmount() > 0)) {
      this.shrinkBorder();
    }
  }

  public void shrinkBorder() {
    if (!this.isGameRunning() || this.worldBorderTool.getBorder().isShrinking()) {
      return;
    }
    this.worldBorderTool.shrinkBorder(MIN_BORDER_SIZE,
        ExTime.ofSeconds((int) (this.getMap().getRadius() * 2 * BORDER_SHRINKING_TIME_MULTIPLIER)));
  }

  public void startPvPHintTask() {
    this.pvpHintTask = Server.runTaskTimerSynchrony(Server::broadcastPvPTypeMessage, 0, 20 * 60 * 3,
        GameSurvivalGames.getPlugin());
  }

  @Override
  public void onGameStop() {
    this.chestLevel = 0;

    for (Entity current : this.getMap().getWorld().getEntities()) {
      if (current instanceof Item) {
        current.remove();
      }
    }

    if (this.refillTask != null) {
      this.refillTask.cancel();
    }

    if (this.pvpHintTask != null) {
      this.pvpHintTask.cancel();
    }

    for (User user : Server.getInGameUsers()) {
      user.getPlayer().setInvulnerable(true);
    }

    EndMessage endMessage = new EndMessage();

    if (LoungeBridgeServer.getServerTeamAmount() == 0) {
      GameUser user = (GameUser) Server.getInGameUsers().stream().findAny().orElse(null);
      if (user != null) {
        endMessage.winner(user);
        user.setGameWinner();
      }
    } else {
      Team team = LoungeBridgeServer.getNotEmptyGameTeams().stream().findAny().orElse(null);

      if (team != null) {
        endMessage.winner(team);
        team.getUsers().forEach(u -> ((GameUser) u).setGameWinner());
      }
    }

    endMessage.addStat("Kills", Server.getInOutGameUsers(), 3, GameUser::getKills)
        .addStat("Longest Shot", Server.getInOutGameUsers(), 3, u -> u.getLongestShot() > 0, GameUser::getLongestShot);

    endMessage.send();
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (!e.getDamageCause().equals(EntityDamageEvent.DamageCause.FALL)) {
      return;
    }

    if (!this.getMap().isNoFallDamage()) {
      return;
    }

    e.setCancelDamage(true);
    e.setCancelled(true);
  }

  @EventHandler
  public void onUserDamageByUser(UserDamageByUserEvent e) {
    SurvivalGamesUser user = ((SurvivalGamesUser) e.getUser());
    if (this.peaceBarTimer.getTime() > 0 || !this.isGameRunning()) {
      if (!LoungeBridgeServer.allowTeamMateDamage()) {
        if (!user.isTeamMate(((SurvivalGamesUser) e.getUserDamager()))) {
          if (this.isGameRunning()) {
            e.getUserDamager().sendPluginTDMessage(Plugin.GAME, "§wPeace time is not over");
          }
        }
      }
      e.setCancelled(true);
      e.setCancelDamage(true);
    }

    if (e.getDamageCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
      if (Server.isOldPvP()) {
        e.setDamage(e.getDamage() * BOW_DAMAGE_MULTIPLIER);
      }
    }
  }

  @Override
  public boolean isRejoiningAllowed() {
    return false;
  }

  @Override
  public Sideboard getSpectatorSideboard() {
    return this.spectatorSideboard;
  }

  @Override
  public void onGameReset() {
    this.stopAfterStart = false;
    this.spawnIndex = 1;
    Server.getWorldManager().reloadWorld(this.getMap().getWorld());
    this.logger.info("Reloaded world '{}'", this.getMap().getWorld().getName());
  }

  @Override
  public ExLocation getSpectatorSpawn() {
    return this.getMap().getSpectatorSpawn();
  }

  @Override
  public void saveGameUserStats(GameUser user) {
    super.saveGameUserStats(user);

    if (user.isGameWinner()) {
      user.getStat(WINS).increaseAllBy(1);
    }

    user.getStat(KILLS).increaseAllBy(user.getKills());

    for (StatPeriod period : StatPeriod.values()) {
      Integer wins = user.getStat(WINS).get(period);
      Integer gamesPlayed = user.getStat(GAMES_PLAYED).get(period);
      user.getStat(WIN_CHANCE).set(period, wins / ((float) gamesPlayed));
    }

  }

  @Override
  public boolean checkGameEnd() {
    return Server.getInGameUsers().size() <= 1 || (LoungeBridgeServer.getNotEmptyGameTeams().size() <= 1
                                                   && LoungeBridgeServer.getServerTeamAmount() > 0);
  }

  public void updateMapOnSideboard() {
    this.spectatorSideboard.updateScore(LineId.MAP, "§f" + this.getMap().getDisplayName());
  }

  @Override
  public KeyedSideboard getGameSideboard() {
    return sideboard;
  }

  public Integer getRefillTime() {
    return refillTime;
  }

  public void setRefillTime(Integer refillTime) {
    this.refillTime = refillTime;
  }

  @Override
  public Set<StatType<?>> getStats() {
    return Set.of(WINS, WIN_CHANCE, KILLS);
  }
}
