package net.theiceninja.duels.arena;

import lombok.Getter;
import lombok.Setter;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.listeners.ArenaListeners;
import net.theiceninja.duels.arena.manager.*;
import net.theiceninja.duels.tasks.BattleTask;
import net.theiceninja.duels.tasks.CooldownTask;
import net.theiceninja.duels.utils.ColorUtils;

import net.theiceninja.duels.utils.ItemCreator;
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
    private ArenaState arenaState;

    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectating = new ArrayList<>();

    private PlayerRollBackManager rollBackManager;
    private ArenaSetupManager arenaSetupManager;
    private final ArenaManager arenaManager;
    private final DuelsPlugin plugin;

    private BattleTask battleTask;
    private CooldownTask cooldownTask;

    // loading arena(onEnable)
    public Arena(String name, Location spawnLocationOne, Location spawnLocationTwo, ArenaManager arenaManager, DuelsPlugin plugin) {
        this.arenaManager = arenaManager;
        this.plugin = plugin;
        this.name = name;
        this.spawnLocationOne = spawnLocationOne;
        this.spawnLocationTwo = spawnLocationTwo;
        setState(ArenaState.DEFAULT);
        rollBackManager = new PlayerRollBackManager();
    }

    // loading all the from the config(arena creation)
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
        // sending messages to all players in the arena
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
            case COOLDOWN -> {

                // updates the scoreboard and showing cooldown
                updateScoreBoard();
                if (cooldownTask != null) cooldownTask.cancel();
                cooldownTask = new CooldownTask(this);
                cooldownTask.runTaskTimer(plugin, 0, 20);
            }
            case ACTIVE -> {
                // teleporting to the locations
                Player player1 = Bukkit.getPlayer(players.get(0));
                if (player1 == null) return;
                player1.teleport(getLocationOne());
                Player player2 = Bukkit.getPlayer(players.get(1));
                if (player2 == null) return;
                player2.teleport(getLocationTwo());
                // update the scoreboard to the state
                updateScoreBoard();
                sendMessage("&#14A045המשחק עכשיו מופעל! תלחמו!");
                // giving the duel items
                giveItems();
                // run task that will check if the game cooldown ends
                if (cooldownTask != null) cooldownTask.cancel();
                if (battleTask != null) battleTask.cancel();
                battleTask = new BattleTask(this);
                battleTask.runTaskTimer(plugin, 0, 20);
            }
        }
    }

    // saving the locations in the arena setup manager
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
        // saving the player(inv and all of those stuff) and adding to the list
        rollBackManager.save(player);
        players.add(player.getUniqueId());
        // update the scoreboard with out flicker to be 1/2 or 2/2
        updateScoreBoard();
        playsound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // adding items
        player.setHealth(20);
        player.setFoodLevel(20);
        sendMessage("&7[&a+&7] &2" + player.getDisplayName());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setItem(8, ItemCreator.createItem(
                Material.RED_DYE,
                1,
                "&cעזיבת משחק &7(לחיצה ימנית)"
        ));
    }

    public void removePlayer(Player player) {

        // restore the inventory (rollback) and removing from the list
        sendMessage("&7[&c-&7] &4" + player.getDisplayName());
        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        playsound(Sound.ENTITY_BLAZE_HURT);
        rollBackManager.restore(player);
        players.remove(player.getUniqueId());

        // if the state is active do stuff
        if (arenaState == ArenaState.ACTIVE) {

            sendMessage("&c" + player.getDisplayName() + " &edied!");
            player.sendMessage(ColorUtils.color("&cאתה מתת!"));

            Player playerOne = Bukkit.getPlayer(players.get(0));

            // removing the spec and show players
            for (UUID playerUUID : spectating) {
                Player spectators = Bukkit.getPlayer(playerUUID);
                if (spectators == null) return;
                playerOne.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), spectators);
                player.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class),spectators);
            }

            // if the players size is one do stuff
            if (players.size() == 1) {
                // cleanup and announce the winner
                Player winner = Bukkit.getPlayer(players.get(0));
                player.sendMessage(ColorUtils.color("&6" +  winner.getDisplayName() + " &bis the winner!"));
                sendMessage("&6" +  winner.getDisplayName() + " &bis the winner!");
                cleanup();
            } else if (players.isEmpty()) {
                // no winner with cleanup
                player.sendMessage(ColorUtils.color("&cאין שחקנים חיים? נו טוב בכל מקרה נגמר.."));
                sendMessage("&cאין שחקנים חיים? נו טוב בכל מקרה נגמר..");
                cleanup();
            }

            // if the player quits the game will be in the cooldown mode(no active)
        } else if (arenaState == ArenaState.COOLDOWN) {
            if (cooldownTask != null) cooldownTask.cancel();
            if (battleTask != null) battleTask.cancel();

            updateScoreBoard();
            setState(ArenaState.DEFAULT);
            sendMessage("&cצריך עוד שחקנים כדי להתחיל את המשחק..");

            // clear there scoreboards
        } else if (arenaState == ArenaState.DEFAULT) {
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            updateScoreBoard();
        }
    }

    public void addSpectator(Player player, Optional<Arena> optionalArena) {
        // set the player in the spectator mode to watch all the arena
        optionalArena.get().spectating.add(player.getUniqueId());
        // save player and teleport to the locations
        rollBackManager.save(player);
        player.teleport(optionalArena.get().getLocationOne());

        // unshow players (spec)
        for (UUID playerUUID : optionalArena.get().players) {
            Player battle = Bukkit.getPlayer(playerUUID);
            battle.hidePlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), player);
        }

        // adding items and doing spec things
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGlowing(true);
        player.setAllowFlight(true);
        player.getInventory().setItem(8, ItemCreator.createItem(
                Material.RED_DYE,
                1,
                "&cעזיבת משחק &7(לחיצה ימנית)"
        ));

        /*
                player.getInventory().setItem(0, ItemBuilder.createItem(
                Material.COMPASS,
                1,
                "&eמציאת שחקן &7(לחיצה ימנית)"
        ));
         */

        player.sendMessage(ColorUtils.color("&aאתה עכשיו צופה באנשים שבארנה &2" + optionalArena.get().getName() + "&a."));

    }

    public void removeSpectator(Player player, Optional<Arena> optionalArena) {
        // restoring player
        optionalArena.get().spectating.remove(player.getUniqueId());
        rollBackManager.restore(player);

        // make the players in the arena show those players
        for (UUID playerUUID : players) {
            Player arenaPlayer = Bukkit.getPlayer(playerUUID);
            if (arenaPlayer == null) return;
            arenaPlayer.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), player);
        }

        // removing scoreboard and all spec ability
        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        player.setGlowing(false);
        player.setAllowFlight(false);
    }

    public boolean isInGame(Player player) {
        return isPlaying(player) || isSpectating(player);
    }

    // add player to the arena with the locations
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

    private Location getLocationOne() {
        return plugin.getConfig().getLocation("arenas." + name + ".locationOne");
    }

    private Location getLocationTwo() {
        return plugin.getConfig().getLocation("arenas." + name + ".locationTwo");
    }

    // cleanup system
    public void cleanup() {
        // ending cooldown if its not equals to null
        if (cooldownTask != null) cooldownTask.cancel();
        if (battleTask != null) battleTask.cancel();

        setState(ArenaState.DEFAULT);

        // rollback players
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
            player.setAllowFlight(false);
        }

        // clear the list
        players.clear();
        spectating.clear();
    }

    public void sendTitle(String str) {
        // sending titles
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

    private void giveItems() {
        // give players
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            player.getInventory().clear();

            player.getInventory().setItem(3, ItemCreator.createItem(
                    Material.DIAMOND_SWORD,
                    1,
                    "&bחרב יהלום"
            ));

            player.getInventory().setItem(0, ItemCreator.createItem(
                    Material.DIAMOND_AXE,
                    1,
                    "&bגרזן יהלום"
            ));

            player.getInventory().setItem(1, ItemCreator.createItem(
                    Material.BOW,
                    1,
                    "&6קשת"
            ));
            player.getInventory().setItem(2, ItemCreator.createItem(
                    Material.ARROW,
                    10,
                    "&fחצים"
            ));
            player.getInventory().setBoots(ItemCreator.createItem(
                    Material.DIAMOND_BOOTS,
                    1,
                    "&bמגפי יהלום"
            ));
            player.getInventory().setHelmet(ItemCreator.createItem(
                    Material.DIAMOND_HELMET,
                    1,
                    "&bקסדת יהלום"
            ));
            player.getInventory().setChestplate(ItemCreator.createItem(
                    Material.DIAMOND_CHESTPLATE,
                    1,
                    "&bשיריון יהלום"
            ));
            player.getInventory().setLeggings(ItemCreator.createItem(
                    Material.DIAMOND_LEGGINGS,
                    1,
                    "&bמכנסי יהלום"
            ));

            player.getInventory().setItemInOffHand(ItemCreator.createItem(
                    Material.SHIELD,
                    1,
                    "&6מגן"
            ));

        }
    }

    private void setScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        List<String> scoreboardLines = new ArrayList<>();
        Objective objective = scoreboard.registerNewObjective("ice",
                "dummy",
                ColorUtils.color("&#855010&lNutellaClub &7| &fקרבות"));
        scoreboardLines.add("&f");

        // players scoreboard
        if (isPlaying(player)) {

            if (arenaState == ArenaState.DEFAULT) {
            // adding those lines if the state is default
                scoreboardLines.add("&r ");
                scoreboardLines.add("&fשחקנים&8: &a" + players.size() + "/2");
                scoreboardLines.add("&cמחכה לעוד שחקנים...");

            } else if (arenaState == ArenaState.COOLDOWN) {
                // adding those lines if the state is cooldown
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
            // spectators scoreboard

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
                scoreboardLines.add("&fהמשחק שלהם נגמר בעוד&8: &e" + battleTask.getTimer() / 60 + "&8:&e" + battleTask.getTimer() % 60);

        }

        scoreboardLines.add("&r ");
        scoreboardLines.add("&7play.nutellaclub.ml");

        for (int i = 0; i < scoreboardLines.size(); i++) {
            String line = ColorUtils.color(scoreboardLines.get(i));
            objective.getScore(line).setScore(scoreboardLines.size() - i);
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    public void updateScoreBoard() {
        // setting the scoreboard too
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
        // playsound to players
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