package net.theiceninja.duels.arena;

import lombok.Getter;
import lombok.Setter;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.manager.*;
import net.theiceninja.duels.tasks.BattleTask;
import net.theiceninja.duels.tasks.CooldownTask;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter @Setter
public class  Arena {

    private String name;

    private Location spawnLocationOne;
    private Location spawnLocationTwo;

    private ArenaSetupManager arenaSetupManager;

    private List<UUID> players = new ArrayList<>();

    private List<UUID> spectating = new ArrayList<>();

    private PlayerRollBackManager rollBackManager;

    private ArenaState arenaState;

    private ArenaManager arenaManager;

    private DuelsPlugin plugin;

    private BattleTask battleTask;

    private CooldownTask cooldownTask;


    public Arena(String name, Location spawnLocationOne, Location spawnLocationTwo, DuelsPlugin plugin) {
        this.plugin = plugin;
        this.name = name;
        this.spawnLocationOne = spawnLocationOne;
        this.spawnLocationTwo = spawnLocationTwo;
        setState(ArenaState.DEFAULT);
        rollBackManager = new PlayerRollBackManager();
    }

    public Arena(String name, ArenaState arenaState, DuelsPlugin plugin, ArenaManager arenaManager) {
        this.name = name;
        this.arenaState = arenaState;
        this.arenaManager = arenaManager;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new ArenaListeners(this), plugin);
        rollBackManager = new PlayerRollBackManager();
        arenaSetupManager = new ArenaSetupManager(rollBackManager, plugin, arenaManager);
    }

    public void sendMessage(String message) {
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.sendMessage(ColorUtils.color(message));
        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.sendMessage(ColorUtils.color(message));
        }
    }

    public void setState(ArenaState arenaState) {
        this.arenaState = arenaState;

        switch (arenaState) {
            case COOLDOWN:
                updateScoreBoard();
                if (cooldownTask != null) cooldownTask.cancel();
                cooldownTask = new CooldownTask(this);
                cooldownTask.runTaskTimer(plugin, 0, 20);

                break;
            case ACTIVE:
                Player player1 = Bukkit.getPlayer(players.get(0));
                if (player1 == null) return;
                player1.teleport(getLocationOne());
                Player player2 = Bukkit.getPlayer(players.get(1));
                if (player2 == null) return;
                player2.teleport(getLocationTwo());
                updateScoreBoard();
                sendMessage("&#14A045המשחק עכשיו מופעל! תלחמו!");
                giveItems();
                if (cooldownTask != null) cooldownTask.cancel();
                if (battleTask != null) battleTask.cancel();
                battleTask = new BattleTask(this);
                battleTask.runTaskTimer(plugin, 0, 20);
                break;
        }
    }

    public void saveLocation(DuelsPlugin plugin, Location location, String locationName) {
        if (locationName.equalsIgnoreCase("locationOne")) {
            setSpawnLocationOne(location);
            plugin.getConfig().set("arenas." + getName() + "." + "locationOne", location);
        } else if (locationName.equalsIgnoreCase("locationTwo")) {
            setSpawnLocationTwo(location);
            plugin.getConfig().set("arenas." + getName() + "." + "locationTwo", location);
        }
    }

    public void addPlayer(Player player) {
        rollBackManager.save(player);
        players.add(player.getUniqueId());
        updateScoreBoard();
        playsound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        player.setHealth(20);
        player.setFoodLevel(20);
        sendMessage("&7[&a+&7] &2" + player.getDisplayName());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setItem(8, ItemBuilder.createItem(
                Material.RED_DYE,
                1,
                "&cעזיבת משחק &7(לחיצה ימנית)"
        ));
    }

    public void removePlayer(Player player) {

        sendMessage("&7[&c-&7] &4" + player.getDisplayName());
        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        playsound(Sound.ENTITY_BLAZE_HURT);
        rollBackManager.restore(player);
        players.remove(player.getUniqueId());

        if (arenaState == ArenaState.ACTIVE) {

            sendMessage("&c" + player.getDisplayName() + " &edied!");
            player.sendMessage(ColorUtils.color("&cאתה מתת!"));

            Player playerOne = Bukkit.getPlayer(players.get(0));

            for (UUID playerUUID : spectating) {
                Player spectators = Bukkit.getPlayer(playerUUID);
                if (spectators == null) return;
                playerOne.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), spectators);
                player.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class),spectators);
            }

            if (players.size() == 1) {
                Player winner = Bukkit.getPlayer(players.get(0));

                player.sendMessage(ColorUtils.color("&6" +  winner.getDisplayName() + " &bis the winner!"));
                sendMessage("&6" +  winner.getDisplayName() + " &bis the winner!");
                cleanup();

            } else if (players.isEmpty()) {
                player.sendMessage(ColorUtils.color("&cאין שחקנים חיים? נו טוב בכל מקרה נגמר.."));
                sendMessage("&cאין שחקנים חיים? נו טוב בכל מקרה נגמר..");
                cleanup();

            }

        } else if (arenaState == ArenaState.COOLDOWN) {

            if (cooldownTask != null) cooldownTask.cancel();
            if (battleTask != null) battleTask.cancel();

            updateScoreBoard();
            setState(ArenaState.DEFAULT);
            sendMessage("&cצריך עוד שחקנים כדי להתחיל את המשחק..");

        } else if (arenaState == ArenaState.DEFAULT) {
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            updateScoreBoard();
        }
    }

    public void addSpectator(Player player, Optional<Arena> optionalArena) {

        optionalArena.get().spectating.add(player.getUniqueId());

        rollBackManager.save(player);

        player.teleport(optionalArena.get().getLocationOne());

        for (UUID playerUUID : optionalArena.get().players) {
            Player battle = Bukkit.getPlayer(playerUUID);
            battle.hidePlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), player);
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGlowing(true);
        player.setAllowFlight(true);
        player.getInventory().setItem(8, ItemBuilder.createItem(
                Material.RED_DYE,
                1,
                "&cעזיבת משחק &7(לחיצה ימנית)"
        ));

        player.getInventory().setItem(0, ItemBuilder.createItem(
                Material.COMPASS,
                1,
                "&eמציאת שחקן &7(לחיצה ימנית)"
        ));

        player.sendMessage(ColorUtils.color("&aאתה עכשיו צופה באנשים שבארנה &2" + optionalArena.get().getName() + "&a."));

    }

    public void removeSpectator(Player player, Optional<Arena> optionalArena) {

        optionalArena.get().spectating.remove(player.getUniqueId());
        rollBackManager.restore(player);

        for (UUID playerUUID : players) {
            Player arenaPlayer = Bukkit.getPlayer(playerUUID);
            if (arenaPlayer == null) return;
            arenaPlayer.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), player);
        }

        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        player.setGlowing(false);
    }


    public boolean isInGame(Player player) {
        return isPlaying(player) || isSpectating(player);
    }

    public void join(Player player, Optional<Arena> arena) {

        arena.get().addPlayer(player);

        if (arena.get().players.size() == 1) {
            player.teleport(arena.get().getLocationOne());

        } else if (arena.get().players.size() == 2) {
            player.teleport(arena.get().getLocationTwo());
            arena.get().setState(ArenaState.COOLDOWN);
        }
    }

    public boolean isPlaying(Player player) {
        return players.contains(player.getUniqueId());
    }

    public boolean isSpectating(Player player) {
        return spectating.contains(player.getUniqueId());
    }

    public Location getLocationOne() {
        return plugin.getConfig().getLocation("arenas." + name + ".locationOne");
    }

    public Location getLocationTwo() {
        return plugin.getConfig().getLocation("arenas." + name + ".locationTwo");
    }


    public void cleanup() {

        if (cooldownTask != null) cooldownTask.cancel();
        if (battleTask != null) battleTask.cancel();

        setState(ArenaState.DEFAULT);

        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            rollBackManager.restore(player);

        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            rollBackManager.restore(player);

            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            player.setGlowing(false);
        }

        players.clear();
        spectating.clear();
    }

    public void sendTitle(String str) {

        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.sendTitle(ColorUtils.color("&#F3120F&lקרבות"), ColorUtils.color(str), 0, 40, 0);
        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.sendTitle(ColorUtils.color("&#F3120F&lקרבות"), ColorUtils.color(str), 0, 40, 0);
        }

    }

    public void giveItems() {

        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            player.getInventory().clear();

            player.getInventory().setItem(3, ItemBuilder.createItem(
                    Material.DIAMOND_SWORD,
                    1,
                    "&bחרב יהלום"
            ));

            player.getInventory().setItem(0, ItemBuilder.createItem(
                    Material.DIAMOND_AXE,
                    1,
                    "&bגרזן יהלום"
            ));

            player.getInventory().setItem(1, ItemBuilder.createItem(
                    Material.BOW,
                    1,
                    "&6קשת"
            ));
            player.getInventory().setItem(2, ItemBuilder.createItem(
                    Material.ARROW,
                    10,
                    "&fחצים"
            ));
            player.getInventory().setBoots(ItemBuilder.createItem(
                    Material.DIAMOND_BOOTS,
                    1,
                    "&bמגפי יהלום"
            ));
            player.getInventory().setHelmet(ItemBuilder.createItem(
                    Material.DIAMOND_HELMET,
                    1,
                    "&bקסדת יהלום"
            ));
            player.getInventory().setChestplate(ItemBuilder.createItem(
                    Material.DIAMOND_CHESTPLATE,
                    1,
                    "&bשיריון יהלום"
            ));
            player.getInventory().setLeggings(ItemBuilder.createItem(
                    Material.DIAMOND_LEGGINGS,
                    1,
                    "&bמכנסי יהלום"
            ));

            player.getInventory().setItemInOffHand(ItemBuilder.createItem(
                    Material.SHIELD,
                    1,
                    "&6מגן"
            ));

        }
    }

    public void setScoreboard(Player player) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        List<String> scoreboardLines = new ArrayList<>();
        Objective objective = scoreboard.registerNewObjective("ice", "dummy", ColorUtils.color("&#d49c4a&lᴛ&#d79745&lɪ&#db9340&lɢ&#de8e3c&lᴇ&#e18937&lʀ &#e58432&lɴ&#e8802d&lᴇ&#ec7b28&lᴛ&#ef7623&lᴡ&#f2711f&lᴏ&#f66d1a&lʀ&#f96815&lᴋ &7| &fקרבות"));
        scoreboardLines.add("&f");

        if (isPlaying(player)) {

            if (arenaState == ArenaState.DEFAULT) {

                scoreboardLines.add("&r ");
                scoreboardLines.add("&fשחקנים&8: &a" + players.size() + "/2");
                scoreboardLines.add("&cמחכה לעוד שחקנים...");

            } else if (arenaState == ArenaState.COOLDOWN) {

                scoreboardLines.add("&r ");
                scoreboardLines.add("&fשחקנים&8: &a" + players.size() + "/2");
                if (cooldownTask != null)
                    scoreboardLines.add("&fהמשחק מתחיל בעוד&8: &e" + cooldownTask.getTimer());

            } else if (arenaState == ArenaState.ACTIVE) {
                scoreboardLines.add("&r ");
                Player opponent = Bukkit.getPlayer(players.get(0));
                if (opponent == null) return;
                if (opponent == player)
                    opponent = Bukkit.getPlayer(players.get(1));
                scoreboardLines.add("&fהיריב שלך&8: &6" + opponent.getDisplayName());
                scoreboardLines.add("&r ");

                if (battleTask != null)
                    scoreboardLines.add("&fהמשחק נגמר בעוד&8: &c" + battleTask.getTimer() / 60 + "&8:&c" + battleTask.getTimer() % 60);
            }

        } else if (isSpectating(player)) {
            scoreboardLines.add("&r");
            Player opponentOne = Bukkit.getPlayer(players.get(0));
            Player opponentTwo = Bukkit.getPlayer(players.get(1));
            if (opponentTwo == null) return;
            if (opponentOne == null) return;
            scoreboardLines.add("&fהאנשים במשחק&8:");
            scoreboardLines.add("&r");
            scoreboardLines.add("&fבן אדם ראשון&8: &6" + opponentOne.getDisplayName());
                scoreboardLines.add("&fבן אדם שני&8: &e" + opponentTwo.getDisplayName());
            scoreboardLines.add("&r");
            if (battleTask != null)
                scoreboardLines.add("&fהמשחק שלהם נגמר בעוד&8: &c" + battleTask.getTimer() / 60 + "&8:&c" + battleTask.getTimer() % 60);

        }

        scoreboardLines.add("&r ");
        scoreboardLines.add("&7play.tigernetwork.cf");

        for (int i = 0; i < scoreboardLines.size(); i++) {
            String line = ColorUtils.color(scoreboardLines.get(i));
            objective.getScore(line).setScore(scoreboardLines.size() - i);
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    public void updateScoreBoard() {

        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            setScoreboard(player);
        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            setScoreboard(player);
        }

    }

    public void playsound(Sound sound) {

        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.playSound(player, sound, 1, 1);

        }
        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;
            player.playSound(player, sound, 1, 1);
        }
    }

}
