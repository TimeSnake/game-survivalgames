package de.timesnake.game.hungergames.server;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.chat.ChatColor;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExWorldBorder;
import de.timesnake.basic.game.util.Game;
import de.timesnake.basic.game.util.Map;
import de.timesnake.basic.game.util.Team;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServerManager;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.basic.loungebridge.util.user.SpectatorUser;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbMap;
import de.timesnake.database.util.object.Status;
import de.timesnake.game.hungergames.chat.Plugin;
import de.timesnake.game.hungergames.item.HungerGamesItemManager;
import de.timesnake.game.hungergames.main.GameHungerGames;
import de.timesnake.game.hungergames.map.HungerGamesMap;
import de.timesnake.game.hungergames.user.HungerGamesUser;
import de.timesnake.library.basic.util.statistics.Stat;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class HungerGamesServerManager extends LoungeBridgeServerManager implements Listener {

    public static HungerGamesServerManager getInstance() {
        return (HungerGamesServerManager) ServerManager.getInstance();
    }

    public static final String SIDEBOARD_TITLE = "§6HungerGames";
    public static final Integer START_TIME = 1000;

    public static final Integer WORLD_BORDER_WARNING = 5;
    public static final Double WORLD_BORDER_DAMAGE = 1d;
    public static final Double MIN_BORDER_SIZE = 30d;
    public static final Double BORDER_SHRINKING_TIME_MULTIPLIER = 1.5d;
    public static final Integer BORDER_DELAY_MIN_TO_0 = 3 * 60;

    public static final double BOW_DAMAGE_MULTIPLIER = 0.5;

    public static final Stat<Integer> WINS = Stat.Type.INTEGER.asStat("wins", "Wins", 0, 0, 2);
    public static final Stat<Float> WIN_CHANCE = Stat.Type.PERCENT.asStat("win_chance", "Win Chance", 0f, 0, 3);
    public static final Stat<Integer> KILLS = Stat.Type.INTEGER.asStat("kills", "Kills", 0, 1, 1);

    public static final float WIN_COINS = 10;
    public static final float KILL_COINS = 2;

    private boolean isRunning = false;

    private HungerGamesItemManager itemManager;

    private Integer chestLevel = 0;

    private boolean stopAfterStart = false;
    private boolean stopped = false;

    private Sideboard sideboard;
    private Sideboard spectatorSideboard;

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
        this.itemManager = new HungerGamesItemManager();

        this.sideboard = Server.getScoreboardManager().registerNewSideboard("hungergames", SIDEBOARD_TITLE);

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

        this.spectatorSideboard = Server.getScoreboardManager().registerNewSideboard("hgSpec", SIDEBOARD_TITLE);
        this.spectatorSideboard.setScore(4, "§9§lPlayers");
        // player amount
        this.spectatorSideboard.setScore(2, "§r§f-----------");
        this.spectatorSideboard.setScore(1, "§c§lMap");
        // map

        this.updateSideboardPlayerAmount();

        if (LoungeBridgeServer.getServerTeamAmount() > 0) {
            LoungeBridgeServer.setTeamMateDamage(false);
        }
    }

    @Override
    public HungerGamesUser loadUser(Player player) {
        return new HungerGamesUser(player);
    }

    @Override
    protected Game loadGame(DbGame dbGame, boolean loadWorlds) {
        return new Game(dbGame, true) {
            @Override
            public Map loadMap(DbMap dbMap, boolean loadWorld) {
                return new HungerGamesMap(dbMap);
            }
        };
    }

    public Location nextSpawn() {
        Location spawn = this.getMap().getLocation(this.spawnIndex);
        if (spawn == null) {
            if (this.spawnIndex == 1) {
                Server.printError(Plugin.HUNGER_GAMES, "Too few spawns in map " + this.getMap().getName());
                return this.getMap().getSpectatorSpawn();
            }
            this.spawnIndex = 1;
            return this.nextSpawn();
        }
        this.spawnIndex++;
        return spawn;
    }

    @Override
    public HungerGamesMap getMap() {
        return (HungerGamesMap) super.getMap();
    }

    @Override
    public Plugin getGamePlugin() {
        return Plugin.HUNGER_GAMES;
    }

    @Override
    public boolean isGameRunning() {
        return this.isRunning;
    }

    @Override
    public void broadcastGameMessage(String message) {
        Server.broadcastMessage(Server.getChat().getSenderPlugin(Plugin.HUNGER_GAMES) + message);
    }

    @Override
    public void prepareGame() {
        this.updateMapOnSideboard();
    }

    @Override
    public void loadMap() {
        HungerGamesMap map = this.getMap();
        this.itemManager.fillMapChests(this.chestLevel);
        this.chestLevel++;
        Server.printText(Plugin.HUNGER_GAMES, "Chests filled in map " + map.getName());
        map.getWorld().setTime(1000);

        if (this.worldBorder != null) {
            this.worldBorder.destroy();
        }

        for (Entity entity : map.getWorld().getEntitiesByClass(Item.class)) {
            entity.remove();
        }

        map.getWorld().getWorldBorder().reset();
        this.worldBorder = new ExWorldBorder(map.getWorld(), map.getSpectatorSpawn().getX(), map.getSpectatorSpawn().getZ(), map.getRadius() * 2, WORLD_BORDER_WARNING, 0, WORLD_BORDER_DAMAGE, true);
    }

    @Override
    public void startGame() {
        if (this.stopAfterStart) {
            this.stopGame();
        }
        this.isRunning = true;
        for (User user : Server.getInGameUsers()) {
            ((HungerGamesUser) user).startGame();
        }
        this.getMap().getWorld().setTime(START_TIME);
        this.startPeaceTime();
        this.startRefillTask();
        this.startBorderTask();
        this.startPvPHintTask();
    }

    public void startPeaceTime() {
        this.peaceTime = this.getMap().getPeaceTime();

        if (!(peaceTime.equals(60) || peaceTime.equals(30) || peaceTime.equals(10) || peaceTime.equals(5) || peaceTime.equals(0))) {
            this.broadcastGameMessage(ChatColor.PUBLIC + "Peace time ends in " + ChatColor.VALUE + this.peaceTime + ChatColor.PUBLIC + " seconds");
        }

        if (peaceTime > 0) {
            this.peaceTimeTask = new BukkitRunnable() {
                @Override
                public void run() {
                    switch (peaceTime) {
                        case 60:
                        case 30:
                        case 10:
                        case 5:
                            broadcastGameMessage(ChatColor.PUBLIC + "Peace time ends in " + ChatColor.VALUE + peaceTime + ChatColor.PUBLIC + " seconds");
                            break;
                        case 0:
                            broadcastGameMessage(ChatColor.PUBLIC + "Peace time ends " + ChatColor.WARNING + "now");
                            Server.broadcastTitle("", "§cPeace time has ended!", Duration.ofSeconds(3));
                            Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
                            this.cancel();
                            break;
                    }
                    peaceTime--;
                }
            }.runTaskTimer(GameHungerGames.getPlugin(), 0, 20);
        }

    }

    public void startRefillTask() {

        this.refillTime = this.getMap().getRefillTime();

        if (this.refillTask != null) {
            this.refillTask.cancel();
        }

        this.refillTask = Server.runTaskTimerSynchrony(() -> {
            switch (refillTime) {
                case 60:
                case 30:
                case 10:
                case 5:
                    broadcastGameMessage(ChatColor.PUBLIC + "Chest refill in " + ChatColor.VALUE + refillTime + ChatColor.PUBLIC + " seconds");
                    break;
                case 0:
                    broadcastGameMessage(ChatColor.WARNING + "Chests were refilled");
                    Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));
                    this.itemManager.fillMapChests(this.chestLevel);
                    this.chestLevel++;
                    refillTime = getMap().getRefillTime();
                    break;
            }

            refillTime--;
        }, 0, 20, GameHungerGames.getPlugin());
    }

    public void startBorderTask() {
        this.borderTask = Server.runTaskLaterSynchrony(this::shrinkBorder, this.getMap().getTimeBorderShrink() * 20, GameHungerGames.getPlugin());
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
        Server.runTaskLaterSynchrony(() -> Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A)), 10, GameHungerGames.getPlugin());
        this.broadcastGameMessage(ChatColor.WARNING + "The border begins to shrink. Watch out!");
        this.worldBorder.setSize(MIN_BORDER_SIZE, (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20), false);

        if (this.borderTask != null) {
            this.borderTask.cancel();
        }

        this.borderTask = Server.runTaskLaterSynchrony(() -> {
            this.worldBorder.setSize(0, (int) (MIN_BORDER_SIZE * 20 * 2 * this.shrinkSpeed), false);
            this.broadcastGameMessage(ChatColor.WARNING + "The border begins to shrink. Watch out!");
        }, (int) (this.getMap().getRadius() * 2 * this.shrinkSpeed * 20) + BORDER_DELAY_MIN_TO_0 * 20, GameHungerGames.getPlugin());
    }

    public void startPvPHintTask() {
        this.pvpHintTask = Server.runTaskTimerSynchrony(Server::broadcastPvPTypeMessage, 0, 20 * 60 * 3, GameHungerGames.getPlugin());
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

    public void stopGame() {
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

        this.broadcastGameMessage(Server.getChat().getLongLineSeparator());

        if (LoungeBridgeServer.getServerTeamAmount() == 0) {
            Iterator<User> it = Server.getInGameUsers().iterator();
            this.winnerUser = it.hasNext() ? it.next() : null;
            if (winnerUser != null) {
                Server.broadcastTitle(winnerUser.getChatName() + ChatColor.PUBLIC + " wins", "", Duration.ofSeconds(5));
                this.broadcastGameMessage(ChatColor.PUBLIC + winnerUser.getChatName() + ChatColor.PUBLIC + " wins");
                winnerUser.addCoins(WIN_COINS, true);
            } else {
                this.broadcastGameMessage(ChatColor.PUBLIC + "Game ended");
            }
        } else {
            Iterator<Team> it = LoungeBridgeServer.getNotEmptyGameTeams().iterator();
            this.winnerTeam = it.hasNext() ? it.next() : null;
            if (winnerTeam != null) {
                Server.broadcastTitle(winnerTeam.getChatColor() + winnerTeam.getDisplayName() + ChatColor.PUBLIC + " wins", "", Duration.ofSeconds(5));
                this.broadcastGameMessage(ChatColor.PUBLIC + "" + winnerTeam.getChatColor() + winnerTeam.getDisplayName() + ChatColor.PUBLIC + " wins");
                for (User user : winnerTeam.getUsers()) {
                    user.addCoins(WIN_COINS, true);
                }
            } else {
                this.broadcastGameMessage(ChatColor.PUBLIC + "Game ended");
            }
        }

        this.broadcastGameMessage(Server.getChat().getLongLineSeparator());

        GameUser userKills = LoungeBridgeServer.getMostKills(((Collection) Server.getGameNotServiceUsers()));
        GameUser userLongestShot = LoungeBridgeServer.getLongestShot(((Collection) Server.getGameNotServiceUsers()));

        if (userKills != null) {
            this.broadcastGameMessage(ChatColor.WHITE + "Kills: " + ChatColor.GOLD + userKills.getKills() + ChatColor.WHITE + " by " + userKills.getChatName());
        }

        if (userLongestShot != null && userLongestShot.getLongestShot() > 0) {
            this.broadcastGameMessage(ChatColor.WHITE + "Longest Shot: " + ChatColor.GOLD + userLongestShot.getLongestShot() + ChatColor.WHITE + " by " + userLongestShot.getChatName());
        }

        this.broadcastGameMessage(Server.getChat().getLongLineSeparator());

        LoungeBridgeServer.closeGame();
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
            user.sendPluginMessage(Plugin.HUNGER_GAMES, damager.getChatName() + ChatColor.PERSONAL + " health: " + ((GameUser) damager).getHealthDisplay());
            Server.printText(Plugin.HUNGER_GAMES, damager.getChatName() + ": " + ((GameUser) damager).getHealthDisplay());
        }
    }

    @EventHandler
    public void onUserRespawn(UserRespawnEvent e) {
        HungerGamesUser user = ((HungerGamesUser) e.getUser());
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
        HungerGamesUser user = ((HungerGamesUser) e.getUser());
        if (this.peaceTime == null || this.peaceTime > 0 || !this.isGameRunning()) {
            if (!LoungeBridgeServer.isTeamMateDamage()) {
                if (!user.isTeamMate(((HungerGamesUser) e.getUserDamager()))) {
                    if (this.isGameRunning()) {
                        e.getUserDamager().sendPluginMessage(Plugin.HUNGER_GAMES, "Peace time is not over");
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
    public void d(InventoryOpenEvent e) {
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
    public void resetGame() {
        this.stopped = false;
        this.stopAfterStart = false;
        this.spawnIndex = 1;
        Server.getWorldManager().reloadWorld(this.getMap().getWorld());
        Server.printText(Plugin.HUNGER_GAMES, "Reloaded world " + this.getMap().getWorld().getName());
    }

    @Override
    public Location getSpectatorSpawn() {
        return this.getMap().getSpectatorSpawn();
    }

    @Override
    public void saveGameUserStats(GameUser user) {
        super.saveGameUserStats(user);

        if (user.equals(this.winnerUser)) {
            user.increaseStat(WINS, 1);
        }
        else if (this.winnerTeam != null && this.winnerTeam.getUsers().contains(user)) {
            user.increaseStat(WINS, 1);
        }

        user.increaseStat(KILLS, user.getKills());
        user.setStat(WIN_CHANCE, user.getStat(WINS) / ((float) user.getStat(GAMES_PLAYED)));
    }

    public boolean isStop() {
        return Server.getInGameUsers().size() <= 1 || (LoungeBridgeServer.getNotEmptyGameTeams().size() <= 1 && LoungeBridgeServer.getServerTeamAmount() > 0);
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
                this.worldBorder.setSize(MIN_BORDER_SIZE, (int) (this.worldBorder.getSize() * this.shrinkSpeed * 20), true);
            }
        }
    }

    @Override
    public Set<Stat<?>> getStats() {
        return Set.of(WINS, WIN_CHANCE, KILLS);
    }
}
