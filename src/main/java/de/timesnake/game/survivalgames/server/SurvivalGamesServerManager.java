/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboard.LineId;
import de.timesnake.basic.bukkit.util.user.scoreboard.ExSideboardBuilder;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorldBorder;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.loungebridge.util.game.TmpGame;
import de.timesnake.basic.loungebridge.util.server.EndMessage;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServerManager;
import de.timesnake.basic.loungebridge.util.tool.advanced.PlayerNumberTool;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.item.SurvivalGamesItemManager;
import de.timesnake.game.survivalgames.main.GameSurvivalGames;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
import de.timesnake.game.survivalgames.user.SurvivalGamesUser;
import de.timesnake.library.basic.util.TimeCoins;
import de.timesnake.library.basic.util.statistics.IntegerStat;
import de.timesnake.library.basic.util.statistics.PercentStat;
import de.timesnake.library.basic.util.statistics.StatPeriod;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.chat.Chat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Set;

public class SurvivalGamesServerManager extends LoungeBridgeServerManager<TmpGame> implements
    Listener {

  public static final String SIDEBOARD_TITLE = "§6§lSurvivalGames";
  public static final Integer START_TIME = 1000;
  public static final Integer WORLD_BORDER_WARNING = 5;
  public static final Double WORLD_BORDER_DAMAGE = 1d;
  public static final Double MIN_BORDER_SIZE = 30d;
  public static final Double BORDER_SHRINKING_TIME_MULTIPLIER = 1.5d;
  public static final Integer BORDER_DELAY_MIN_TO_0 = 2 * 60;
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

  private Logger logger = LogManager.getLogger("survival-games.server");

  private SurvivalGamesItemManager itemManager;
  private Integer chestLevel = 0;
  private boolean stopAfterStart = false;
  private ExSideboard sideboard;
  private ExSideboard spectatorSideboard;
  private BossBar peaceTimeBar;
  private Integer spawnIndex = 1;
  private BukkitTask refillTask;
  private Integer refillTime;
  private BukkitTask peaceTimeTask;
  private Integer peaceTime;
  private BukkitTask borderTask;
  private boolean shrinkingBorder;
  private Double shrinkSpeed = BORDER_SHRINKING_TIME_MULTIPLIER;
  private ExWorldBorder worldBorder;
  private BukkitTask pvpHintTask;
  private User winnerUser;
  private Team winnerTeam;

  public void onHungerGamesEnable() {
    super.onLoungeBridgeEnable();
    this.itemManager = new SurvivalGamesItemManager();

    ExSideboardBuilder sideboardBuilder = new ExSideboardBuilder()
        .name("survivalgames")
        .title(SIDEBOARD_TITLE)
        .lineSpacer();

    if (LoungeBridgeServer.getServerTeamAmount() > 0) {
      sideboardBuilder.addLine(LineId.TEAM);
    }

    sideboardBuilder.addLine(LineId.PLAYERS_OF)
        .addLine(LineId.KILLS);

    this.sideboard = Server.getScoreboardManager().registerExSideboard(sideboardBuilder);

    ExSideboardBuilder spectatorSideboardBuilder = new ExSideboardBuilder()
        .name("sg_spec")
        .title(SIDEBOARD_TITLE)
        .lineSpacer()
        .addLine(LineId.PLAYERS_OF)
        .addLine(LineId.MAP);

    this.spectatorSideboard = Server.getScoreboardManager()
        .registerExSideboard(spectatorSideboardBuilder);

    this.peaceTimeBar = Server.createBossBar("", BarColor.RED, BarStyle.SOLID);

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
  public Plugin getGamePlugin() {
    return Plugin.SURVIVAL_GAMES;
  }

  @Override
  public void onMapLoad() {
    SurvivalGamesMap map = this.getMap();
    this.itemManager.fillMapChests(this.chestLevel);
    this.chestLevel++;
    this.logger.info("Chests filled in map '{}'", map.getName());
    map.getWorld().setTime(1000);

    if (this.worldBorder != null) {
      this.worldBorder.destroy();
    }

    for (Entity entity : map.getWorld().getEntitiesByClass(Item.class)) {
      entity.remove();
    }

    map.getWorld().getWorldBorder().reset();
    this.worldBorder = new ExWorldBorder.Builder()
        .world(map.getWorld())
        .centerX(map.getSpectatorSpawn().getX())
        .centerZ(map.getSpectatorSpawn().getZ())
        .size(map.getRadius() * 2)
        .warningDistance(WORLD_BORDER_WARNING)
        .damagePerSec(WORLD_BORDER_DAMAGE)
        .sound(true).build();

    this.peaceTime = this.getMap().getPeaceTime();

    this.peaceTimeBar.setTitle("Peace time ends in " + ChatColor.RED + Chat.getTimeString(this.peaceTime));
    this.peaceTimeBar.setProgress(1);

    this.updateMapOnSideboard();
  }

  @Override
  public void onGameStart() {
    if (this.stopAfterStart) {
      this.stopGame();
    }

    this.getMap().getWorld().setTime(START_TIME);
    this.startPeaceTime();
    this.startRefillTask();
    this.startBorderTask();
    this.startPvPHintTask();
  }

  public void startPeaceTime() {
    if (!(peaceTime.equals(60) || peaceTime.equals(30) || peaceTime.equals(10)
        || peaceTime.equals(5) || peaceTime.equals(0))) {
      this.broadcastGameTDMessage("Peace time ends in §v" + Chat.getTimeString(this.peaceTime));
    }

    if (peaceTime > 0) {
      this.peaceTimeTask = Server.runTaskTimerSynchrony(() -> {
        switch (peaceTime) {
          case 60, 30, 10, 5 ->
              this.broadcastGameTDMessage("§pPeace time ends in §v" + Chat.getTimeString(this.peaceTime));
          case 0 -> {
            this.broadcastGameTDMessage("Peace time ends §wnow");
            Server.broadcastTDTitle("", "§wPeace time has ended!", Duration.ofSeconds(3));
            Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
            Server.getGameUsers().forEach(u -> u.removeBossBar(this.peaceTimeBar));
            this.peaceTimeTask.cancel();
          }
        }

        this.peaceTimeBar.setTitle("Peace time ends in §w" + ChatColor.RED + Chat.getTimeString(this.peaceTime));
        this.peaceTimeBar.setProgress(peaceTime / ((double) this.getMap().getPeaceTime()));

        this.peaceTime--;
      }, 0, 20, GameSurvivalGames.getPlugin());
    }
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

  public void startBorderTask() {
    this.borderTask = Server.runTaskLaterSynchrony(this::shrinkBorder,
        this.getMap().getTimeBorderShrink() * 20,
        GameSurvivalGames.getPlugin());
  }

  public void checkPlayerBorderShrink() {
    if (Server.getInGameUsers().size() <= this.getMap().getPlayerBorderShrink() || (
        LoungeBridgeServer.getNotEmptyGameTeams().size() <= this.getMap()
            .getPlayerBorderShrink() && LoungeBridgeServer.getServerTeamAmount() > 0)) {
      this.shrinkBorder();
    }
  }

  public void shrinkBorder() {
    if (this.shrinkingBorder) {
      return;
    }

    if (!this.isGameRunning()) {
      return;
    }

    this.shrinkingBorder = true;

    Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
    Server.runTaskLaterSynchrony(
        () -> Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A)), 10,
        GameSurvivalGames.getPlugin());
    this.broadcastGameTDMessage("§wThe border begins to shrink. Watch out!");
    this.worldBorder.setSize(MIN_BORDER_SIZE, (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20), false);

    if (this.borderTask != null) {
      this.borderTask.cancel();
    }

    this.borderTask = Server.runTaskLaterSynchrony(() -> {
          this.worldBorder.setSize(0, (int) (MIN_BORDER_SIZE * 20 * 2 * this.shrinkSpeed), false);
          this.broadcastGameTDMessage("§wThe border begins to shrink. Watch out!");
        },
        (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20) + BORDER_DELAY_MIN_TO_0 * 20,
        GameSurvivalGames.getPlugin());
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

    if (this.peaceTimeTask != null) {
      this.peaceTimeTask.cancel();
    }

    if (this.pvpHintTask != null) {
      this.pvpHintTask.cancel();
    }

    if (this.borderTask != null) {
      this.borderTask.cancel();
    }

    this.shrinkingBorder = false;

    this.worldBorder.setSize(this.getMap().getRadius() * 2);

    for (User user : Server.getInGameUsers()) {
      user.getPlayer().setInvulnerable(true);
    }

    EndMessage endMessage = new EndMessage();

    if (LoungeBridgeServer.getServerTeamAmount() == 0) {
      endMessage.winner(Server.getInGameUsers().stream().findAny().orElse(null));
    } else {
      endMessage.winner(LoungeBridgeServer.getNotEmptyGameTeams().stream().findAny().orElse(null));
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
    if (this.peaceTime == null || this.peaceTime > 0 || !this.isGameRunning()) {
      if (!LoungeBridgeServer.allowTeamMateDamage()) {
        if (!user.isTeamMate(((SurvivalGamesUser) e.getUserDamager()))) {
          if (this.isGameRunning()) {
            e.getUserDamager().sendPluginTDMessage(Plugin.SURVIVAL_GAMES, "§wPeace time is not over");
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
    this.shrinkSpeed = BORDER_SHRINKING_TIME_MULTIPLIER;
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

    if (user.equals(this.winnerUser)) {
      user.getStat(WINS).increaseAll(1);
    } else if (this.winnerTeam != null && this.winnerTeam.getUsers().contains(user)) {
      user.getStat(WINS).increaseAll(1);
    }

    user.getStat(KILLS).increaseAll(user.getKills());

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
  public ExSideboard getGameSideboard() {
    return sideboard;
  }

  public ExWorldBorder getWorldBorder() {
    return worldBorder;
  }

  public Integer getRefillTime() {
    return refillTime;
  }

  public void setRefillTime(Integer refillTime) {
    this.refillTime = refillTime;
  }

  public Double getShrinkSpeed() {
    return shrinkSpeed;
  }

  public void setShrinkSpeed(Double shrinkSpeed) {
    this.shrinkSpeed = shrinkSpeed;
    if (this.shrinkingBorder) {
      if (this.worldBorder.getSize() >= MIN_BORDER_SIZE) {
        this.worldBorder.setSize(MIN_BORDER_SIZE,
            (int) (this.worldBorder.getSize() * this.shrinkSpeed * 20),
            true);
      }
    }
  }

  public BossBar getPeaceTimeBar() {
    return peaceTimeBar;
  }

  @Override
  public Set<StatType<?>> getStats() {
    return Set.of(WINS, WIN_CHANCE, KILLS);
  }
}
