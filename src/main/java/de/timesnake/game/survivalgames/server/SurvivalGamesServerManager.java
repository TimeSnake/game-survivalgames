/*
 * workspace.game-survivalgames.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.game.survivalgames.server;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorldBorder;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.game.util.game.TmpGame;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServerManager;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.game.survivalgames.chat.Plugin;
import de.timesnake.game.survivalgames.item.SurvivalGamesItemManager;
import de.timesnake.game.survivalgames.main.GameSurvivalGames;
import de.timesnake.game.survivalgames.map.SurvivalGamesMap;
import de.timesnake.game.survivalgames.user.SurvivalGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.TimeCoins;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.basic.util.statistics.IntegerStat;
import de.timesnake.library.basic.util.statistics.PercentStat;
import de.timesnake.library.basic.util.statistics.StatPeriod;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.extension.util.chat.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SurvivalGamesServerManager extends LoungeBridgeServerManager<TmpGame> implements Listener {

    public static final String SIDEBOARD_TITLE = "§6§lSurvivalGames";
    public static final Integer START_TIME = 1000;
    public static final Integer WORLD_BORDER_WARNING = 5;
    public static final Double WORLD_BORDER_DAMAGE = 1d;
    public static final Double MIN_BORDER_SIZE = 30d;
    public static final Double BORDER_SHRINKING_TIME_MULTIPLIER = 1.5d;
    public static final Integer BORDER_DELAY_MIN_TO_0 = 2 * 60;
    public static final double BOW_DAMAGE_MULTIPLIER = 0.7;
    public static final StatType<Integer> WINS = new IntegerStat("wins", "Wins", 0, 10, 2, true, 0, 1);
    public static final StatType<Float> WIN_CHANCE = new PercentStat("win_chance", "Win Chance", 0f, 10, 3,
            false, 0, 0);
    public static final StatType<Integer> KILLS = new IntegerStat("kills", "Kills", 0, 10, 4, true, 0, 2);
    public static final float WIN_COINS = 10 * TimeCoins.MULTIPLIER;
    public static final float KILL_COINS = 3 * TimeCoins.MULTIPLIER;

    public static SurvivalGamesServerManager getInstance() {
        return (SurvivalGamesServerManager) ServerManager.getInstance();
    }

    private boolean isRunning = false;
    private SurvivalGamesItemManager itemManager;
    private Integer chestLevel = 0;
    private boolean stopAfterStart = false;
    private boolean stopped = false;
    private Sideboard sideboard;
    private Sideboard spectatorSideboard;
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

        this.sideboard = Server.getScoreboardManager().registerSideboard("survivalgames", SIDEBOARD_TITLE);

        if (LoungeBridgeServer.getServerTeamAmount() > 0) {
            this.sideboard.setScore(7, "§3§lTeam");
            // team
            this.sideboard.setScore(5, "§f-----------");
        }

        this.sideboard.setScore(4, "§9§lPlayers");
        // player amount
        this.sideboard.setScore(2, "§r§f-----------");
        this.sideboard.setScore(1, "§c§lKills");
        // kill amount

        this.spectatorSideboard = Server.getScoreboardManager().registerSideboard("sgSpec", SIDEBOARD_TITLE);
        this.spectatorSideboard.setScore(4, "§9§lPlayers");
        // player amount
        this.spectatorSideboard.setScore(2, "§r§f-----------");
        this.spectatorSideboard.setScore(1, "§c§lMap");
        // map

        this.peaceTimeBar = Server.createBossBar("", BarColor.RED, BarStyle.SOLID);

        this.updateSideboardPlayerAmount();

        if (LoungeBridgeServer.getServerTeamAmount() > 0) {
            LoungeBridgeServer.setTeamMateDamage(false);
        }
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
                Server.printWarning(Plugin.SURVIVAL_GAMES, "Too few spawns in map " + this.getMap().getName());
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
    public boolean isGameRunning() {
        return this.isRunning;
    }

    @Override
    @Deprecated
    public void broadcastGameMessage(String message) {
        Server.broadcastMessage(Plugin.SURVIVAL_GAMES, message);
    }

    @Override
    public void broadcastGameMessage(Component message) {
        Server.broadcastMessage(Plugin.SURVIVAL_GAMES, message);
    }

    @Override
    public void onMapLoad() {
        SurvivalGamesMap map = this.getMap();
        this.itemManager.fillMapChests(this.chestLevel);
        this.chestLevel++;
        Server.printText(Plugin.SURVIVAL_GAMES, "Chests filled in map " + map.getName());
        map.getWorld().setTime(1000);

        if (this.worldBorder != null) {
            this.worldBorder.destroy();
        }

        for (Entity entity : map.getWorld().getEntitiesByClass(Item.class)) {
            entity.remove();
        }

        map.getWorld().getWorldBorder().reset();
        this.worldBorder = new ExWorldBorder(map.getWorld(), map.getSpectatorSpawn().getX(),
                map.getSpectatorSpawn().getZ(), map.getRadius() * 2, WORLD_BORDER_WARNING, 0, WORLD_BORDER_DAMAGE,
                true);

        this.peaceTime = this.getMap().getPeaceTime();

        this.peaceTimeBar.setTitle("Peace time ends in " + ChatColor.RED + this.peaceTime + ChatColor.WHITE + " " +
                "seconds");
        this.peaceTimeBar.setProgress(1);

        this.updateMapOnSideboard();
    }

    @Override
    public void onGameStart() {
        if (this.stopAfterStart) {
            this.stopGame();
        }
        this.isRunning = true;
        for (User user : Server.getInGameUsers()) {
            ((SurvivalGamesUser) user).startGame();
        }
        this.getMap().getWorld().setTime(START_TIME);
        this.startPeaceTime();
        this.startRefillTask();
        this.startBorderTask();
        this.startPvPHintTask();
    }

    public void startPeaceTime() {
        if (!(peaceTime.equals(60) || peaceTime.equals(30) || peaceTime.equals(10) || peaceTime.equals(5) || peaceTime.equals(0))) {
            this.broadcastGameMessage(Component.text("Peace time ends in ", ExTextColor.PUBLIC)
                    .append(Component.text(this.peaceTime + "s", ExTextColor.VALUE)));
        }

        if (peaceTime > 0) {
            this.peaceTimeTask = Server.runTaskTimerSynchrony(() -> {
                switch (peaceTime) {
                    case 60, 30, 10, 5 -> broadcastGameMessage(Component.text("Peace time ends in ", ExTextColor.PUBLIC)
                            .append(Component.text(peaceTime + "s", ExTextColor.VALUE)));
                    case 0 -> {
                        broadcastGameMessage(Component.text("Peace time ends ", ExTextColor.PUBLIC)
                                .append(Component.text("now", ExTextColor.WARNING)));
                        Server.broadcastTitle(Component.empty(),
                                Component.text("Peace time has ended!", ExTextColor.WARNING),
                                Duration.ofSeconds(3));
                        Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
                        Server.getGameUsers().forEach(u -> u.removeBossBar(this.peaceTimeBar));
                        this.peaceTimeTask.cancel();
                    }
                }

                this.peaceTimeBar.setTitle("Peace time ends in " + ChatColor.RED + peaceTime + ChatColor.WHITE + " " +
                        "seconds");
                this.peaceTimeBar.setProgress(peaceTime / ((double) this.getMap().getPeaceTime()));

                peaceTime--;
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
                case 60, 30, 10, 5 -> broadcastGameMessage(Component.text("Chest refill in ", ExTextColor.PUBLIC)
                        .append(Component.text(refillTime + "s", ExTextColor.VALUE)));
                case 0 -> {
                    broadcastGameMessage(Component.text("Chests were refilled", ExTextColor.WARNING));
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
        this.borderTask = Server.runTaskLaterSynchrony(this::shrinkBorder, this.getMap().getTimeBorderShrink() * 20,
                GameSurvivalGames.getPlugin());
    }

    public void checkPlayerBorderShrink() {
        if (Server.getInGameUsers().size() <= this.getMap().getPlayerBorderShrink() || (LoungeBridgeServer.getNotEmptyGameTeams().size() <= this.getMap().getPlayerBorderShrink() && LoungeBridgeServer.getServerTeamAmount() > 0)) {
            this.shrinkBorder();
        }
    }

    public void shrinkBorder() {
        if (this.shrinkingBorder) {
            return;
        }

        if (!this.isRunning) {
            return;
        }

        this.shrinkingBorder = true;

        Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
        Server.runTaskLaterSynchrony(() -> Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A)), 10,
                GameSurvivalGames.getPlugin());
        this.broadcastGameMessage(Component.text("The border begins to shrink. Watch out!", ExTextColor.WARNING));
        this.worldBorder.setSize(MIN_BORDER_SIZE, (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20), false);

        if (this.borderTask != null) {
            this.borderTask.cancel();
        }

        this.borderTask = Server.runTaskLaterSynchrony(() -> {
                    this.worldBorder.setSize(0, (int) (MIN_BORDER_SIZE * 20 * 2 * this.shrinkSpeed), false);
                    this.broadcastGameMessage(Component.text("The border begins to shrink. Watch out!", ExTextColor.WARNING));
                }, (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20) + BORDER_DELAY_MIN_TO_0 * 20,
                GameSurvivalGames.getPlugin());
    }

    public void startPvPHintTask() {
        this.pvpHintTask = Server.runTaskTimerSynchrony(Server::broadcastPvPTypeMessage, 0, 20 * 60 * 3,
                GameSurvivalGames.getPlugin());
    }

    @Override
    public void onGameUserQuit(GameUser user) {
        this.updateSideboardPlayerAmount();
        if (this.isStop()) {
            this.stopGame();
        } else {
            this.checkPlayerBorderShrink();
        }
    }

    @Override
    public void onGameStop() {
        if (this.stopped) {
            return;
        }

        this.stopped = true;
        this.chestLevel = 0;

        this.isRunning = false;

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

        this.broadcastGameMessage(Chat.getLongLineSeparator());

        if (LoungeBridgeServer.getServerTeamAmount() == 0) {
            Iterator<User> it = Server.getInGameUsers().iterator();
            this.winnerUser = it.hasNext() ? it.next() : null;
            if (winnerUser != null) {
                Server.broadcastTitle(winnerUser.getChatNameComponent()
                                .append(Component.text(" wins", ExTextColor.PUBLIC)), Component.empty(),
                        Duration.ofSeconds(5));
                this.broadcastGameMessage(winnerUser.getChatNameComponent()
                        .append(Component.text(" wins", ExTextColor.PUBLIC)));
                winnerUser.addCoins(WIN_COINS, true);
            } else {
                this.broadcastGameMessage(Component.text("Game ended", ExTextColor.PUBLIC));
            }
        } else {
            Iterator<Team> it = LoungeBridgeServer.getNotEmptyGameTeams().iterator();
            this.winnerTeam = it.hasNext() ? it.next() : null;
            if (winnerTeam != null) {
                Server.broadcastTitle(Component.text(winnerTeam.getDisplayName(), winnerTeam.getTextColor())
                                .append(Component.text(" wins", ExTextColor.PUBLIC)), Component.empty(),
                        Duration.ofSeconds(5));
                this.broadcastGameMessage(Component.text(winnerTeam.getDisplayName(), winnerTeam.getTextColor())
                        .append(Component.text(" wins", ExTextColor.PUBLIC)));
                for (User user : winnerTeam.getUsers()) {
                    user.addCoins(WIN_COINS, true);
                }
            } else {
                this.broadcastGameMessage(Component.text("Game ended", ExTextColor.PUBLIC));
            }
        }

        this.broadcastGameMessage(Chat.getLongLineSeparator());
        this.broadcastHighscore("Kills", ((Collection) Server.getInOutGameUsers()), 3, GameUser::getKills);
        this.broadcastHighscore("Longest Shot", ((Collection) Server.getInOutGameUsers()), 3,
                u -> u.getLongestShot() > 0, GameUser::getLongestShot);
        this.broadcastGameMessage(Chat.getLongLineSeparator());
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
    public void onUserDeath(UserDeathEvent e) {
        SpectatorUser user = (SpectatorUser) e.getUser();
        if (user.getStatus().equals(Status.User.IN_GAME)) {
            e.setAutoRespawn(true);
        }

        if (user.isInGame() && user.getLastDamager() != null) {
            User damager = user.getLastDamager().getDamager();
            user.sendPluginMessage(Plugin.SURVIVAL_GAMES, damager.getChatNameComponent()
                    .append(Component.text(" health: ", ExTextColor.PERSONAL))
                    .append(((GameUser) damager).getHealthDisplay().color(ExTextColor.VALUE)));
            Server.printText(Plugin.SURVIVAL_GAMES,
                    PlainTextComponentSerializer.plainText().serialize(damager.getChatNameComponent()
                            .append(Component.text(": ", ExTextColor.PERSONAL))
                            .append(((GameUser) damager).getHealthDisplay().color(ExTextColor.VALUE))));
        }
    }

    @EventHandler
    public void onUserRespawn(UserRespawnEvent e) {
        SurvivalGamesUser user = ((SurvivalGamesUser) e.getUser());
        e.setRespawnLocation(this.getSpectatorSpawn());
        user.joinSpectator();

        this.worldBorder.removeUser(user);

        this.updateSideboardPlayerAmount();

        if (this.isStop()) {
            this.stopGame();
        } else {
            this.checkPlayerBorderShrink();
        }

    }

    @EventHandler
    public void onUserDamageByUser(UserDamageByUserEvent e) {
        SurvivalGamesUser user = ((SurvivalGamesUser) e.getUser());
        if (this.peaceTime == null || this.peaceTime > 0 || !this.isGameRunning()) {
            if (!LoungeBridgeServer.isTeamMateDamage()) {
                if (!user.isTeamMate(((SurvivalGamesUser) e.getUserDamager()))) {
                    if (this.isGameRunning()) {
                        e.getUserDamager().sendPluginMessage(Plugin.SURVIVAL_GAMES,
                                Component.text("Peace time is not over", ExTextColor.WARNING));
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

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getView().getTopInventory().getType().equals(InventoryType.ANVIL)) {
            e.setCancelled(true);
            e.getPlayer().closeInventory();
        }
    }

    @Override
    public void onGameUserQuitBeforeStart(GameUser user) {
        this.updateSideboardPlayerAmount();
        if (this.isStop()) {
            this.stopAfterStart = true;
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
        this.stopped = false;
        this.stopAfterStart = false;
        this.spawnIndex = 1;
        this.shrinkSpeed = BORDER_SHRINKING_TIME_MULTIPLIER;
        Server.getWorldManager().reloadWorld(this.getMap().getWorld());
        Server.printText(Plugin.SURVIVAL_GAMES, "Reloaded world " + this.getMap().getWorld().getName());
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

    public boolean isStop() {
        return Server.getInGameUsers().size() <= 1 || (LoungeBridgeServer.getNotEmptyGameTeams().size() <= 1
                && LoungeBridgeServer.getServerTeamAmount() > 0);
    }

    public void updateSideboardPlayerAmount() {
        Integer amount = Server.getInGameUsers().size() + Server.getPreGameUsers().size();
        Integer max = this.getMaxPlayers();
        this.sideboard.setScore(3, amount + " §7/§f " + max);
        this.spectatorSideboard.setScore(3, amount + " §7/§f " + max);
    }

    public void updateMapOnSideboard() {
        this.spectatorSideboard.setScore(0, "§f" + this.getMap().getDisplayName());
    }

    public Sideboard getSideboard() {
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
                this.worldBorder.setSize(MIN_BORDER_SIZE, (int) (this.worldBorder.getSize() * this.shrinkSpeed * 20),
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
