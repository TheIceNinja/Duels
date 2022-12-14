package net.theiceninja.duels.arena;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.w3c.dom.Text;

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
    private DuelsPlugin plugin;
    private final ArenaManager arenaManager;

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

    // loading all from the config(arena creation)
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
                sendMessage("&#14A045?????????? ?????????? ??????????! ??????????!");
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
        // update the scoreboard without flicker to be 1/2 or 2/2
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
                "&c?????????? ???????? &7(?????????? ??????????)"
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
            player.sendMessage(ColorUtils.color("&c?????? ??????!"));

            Player playerOne = Bukkit.getPlayer(players.get(0));

            // removing the spec and show players
            for (UUID playerUUID : spectating) {
                Player spectators = Bukkit.getPlayer(playerUUID);
                if (spectators == null) return;
                playerOne.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), spectators);
                player.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), spectators);
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
                player.sendMessage(ColorUtils.color("&c?????? ???????????? ????????? ???? ?????? ?????? ???????? ????????.."));
                sendMessage("&c?????? ???????????? ????????? ???? ?????? ?????? ???????? ????????..");
                cleanup();
            }

            // if the player quits the game will be in the cooldown mode(no active)
        } else if (arenaState == ArenaState.COOLDOWN) {
            if (cooldownTask != null) cooldownTask.cancel();
            if (battleTask != null) battleTask.cancel();

            updateScoreBoard();
            setState(ArenaState.DEFAULT);
            sendMessage("&c???????? ?????? ???????????? ?????? ???????????? ???? ??????????..");

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

        // unshod players (spec)
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
                "&c?????????? ???????? &7(?????????? ??????????)"
        ));


        player.getInventory().setItem(0, ItemCreator.createItem(
                Material.COMPASS,
                1,
                "&e?????????? ???????? &7(?????????? ??????????)"
        ));


        player.sendMessage(ColorUtils.color("&a?????? ?????????? ???????? ???????????? ???????????? &2" + optionalArena.get().getName() + "&a."));

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
        // ending cooldown if it's not equals to null
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

        players.clear();
        spectating.clear();
    }

    public void sendTitle(String str) {
        // sending titles
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            player.sendTitle(ColorUtils.color("&#F3120F&l??????????"), ColorUtils.color(str), 0, 40, 0);
        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            player.sendTitle(ColorUtils.color("&#F3120F&l??????????"), ColorUtils.color(str), 0, 40, 0);
        }
    }

    private void giveItems() {
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            player.getInventory().clear();

            player.getInventory().setItem(3, ItemCreator.createItem(
                    Material.DIAMOND_SWORD,
                    1,
                    "&#2AB1E8?????? ??????????"
            ));

            player.getInventory().setItem(0, ItemCreator.createItem(
                    Material.DIAMOND_AXE,
                    1,
                    "&#2AB1E8???????? ??????????"
            ));

            player.getInventory().setItem(1, ItemCreator.createItem(
                    Material.BOW,
                    1,
                    "&6??????"
            ));
            player.getInventory().setItem(2, ItemCreator.createItem(
                    Material.ARROW,
                    10,
                    "&f????????"
            ));
            player.getInventory().setBoots(ItemCreator.createItem(
                    Material.DIAMOND_BOOTS,
                    1,
                    "&#2AB1E8???????? ??????????"
            ));
            player.getInventory().setHelmet(ItemCreator.createItem(
                    Material.DIAMOND_HELMET,
                    1,
                    "&#2AB1E8???????? ??????????"
            ));
            player.getInventory().setChestplate(ItemCreator.createItem(
                    Material.DIAMOND_CHESTPLATE,
                    1,
                    "&#2AB1E8???????????? ??????????"
            ));
            player.getInventory().setLeggings(ItemCreator.createItem(
                    Material.DIAMOND_LEGGINGS,
                    1,
                    "&#2AB1E8?????????? ??????????"
            ));
            player.getInventory().setItemInOffHand(ItemCreator.createItem(
                    Material.SHIELD,
                    1,
                    "&6??????"
            ));

        }
    }

    private void setScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        List<String> scoreboardLines = new ArrayList<>();
        Objective objective = scoreboard.registerNewObjective("ice",
                "dummy",
                ColorUtils.color("&#3bb6fb&lN&#4bbce7&li&#5bc3d3&ln&#6bc9be&lj&#7bd0aa&la&#8bd696&lN&#9bdd82&le&#abe36e&lt&#bbea5a&lw&#cbf045&lo&#dbf731&lr&#ebfd1d&lk &7| &f??????????"));
        scoreboardLines.add("&f");

        // players scoreboard
        if (isPlaying(player)) {

            if (arenaState == ArenaState.DEFAULT) {
            // adding those lines if the state is default
                scoreboardLines.add("&r ");
                scoreboardLines.add("&f????????????&8: &a" + players.size() + "/2");
                scoreboardLines.add("&c???????? ???????? ????????????...");

            } else if (arenaState == ArenaState.COOLDOWN) {
                // adding those lines if the state is cooldown
                scoreboardLines.add("&r ");
                scoreboardLines.add("&f????????????&8: &a" + players.size() + "/2");

                if (cooldownTask != null)
                    scoreboardLines.add("&f?????????? ?????????? ????????&8: &e" + cooldownTask.getTimer());

            } else if (arenaState == ArenaState.ACTIVE) {
                scoreboardLines.add("&r ");
                Player opponent = Bukkit.getPlayer(players.get(0));

                if (opponent == null) return;
                if (opponent == player)
                    opponent = Bukkit.getPlayer(players.get(1));

                scoreboardLines.add("&f?????????? ??????&8: &6" + opponent.getDisplayName());
                scoreboardLines.add("&r ");

                if (battleTask != null)
                    scoreboardLines.add("&f?????????? ???????? ????????&8: &c" + battleTask.getTimer() / 60 + "&8:&c" + battleTask.getTimer() % 60);
            }
            // spectators scoreboard

        } else if (isSpectating(player)) {
            scoreboardLines.add("&r");

            Player opponentOne = Bukkit.getPlayer(players.get(0));
            Player opponentTwo = Bukkit.getPlayer(players.get(1));

            if (opponentTwo == null || opponentOne == null) return;

            scoreboardLines.add("&f???????????? ??????????&8:");
            scoreboardLines.add("&r");
            scoreboardLines.add("&f???? ?????? ??????????&8: &6" + opponentOne.getDisplayName() + " &c" + (int) opponentOne.getHealth() + " hearts");
            scoreboardLines.add("&f???? ?????? ??????&8: &e" + opponentTwo.getDisplayName() + " &c" + (int) opponentTwo.getHealth() + " hearts");
            scoreboardLines.add("&r");

            if (battleTask != null)
                scoreboardLines.add("&f?????????? ???????? ???????? ????????&8: &b" + battleTask.getTimer() / 60 + "&8:&b" + battleTask.getTimer() % 60);

        }

        scoreboardLines.add("&r ");
        scoreboardLines.add("&7play.iceninja.us.to");

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

    public void sendActionBar(String str) {
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorUtils.color(str)));
        }

        for (UUID playerUUID : spectating) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorUtils.color(str)));
        }

    }
}