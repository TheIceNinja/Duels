package net.theiceninja.duels.arena.manager;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.ItemCreator;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ArenaSetupManager implements Listener {

    private final Map<UUID, Arena> setup = new HashMap<>();
    private final PlayerRollBackManager rollBackManager;

    private final DuelsPlugin plugin;
    private final ArenaManager arenaManager;

    // setup items
    private ItemStack setLocationItem = ItemCreator.createItem(Material.BLAZE_ROD, 1, ColorUtils.color("&#F1CA16קביעת מיקומים &7(לחיצה ימנית/שמאלית)"));
    private ItemStack save = ItemCreator.createItem(Material.GREEN_WOOL, 1, ColorUtils.color("&#12A459שמירת ארנה &7(לחיצה ימנית)"));
    private ItemStack cancel = ItemCreator.createItem(Material.BARRIER, 1, ColorUtils.color("&#F03D15מחיקת ארנה &7(לחיצה ימנית)"));

    public void addToSetup(Player player, Arena arena) {
        // save the players and register the event (and put on the list with the arena)
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        rollBackManager.save(player);
        setup.put(player.getUniqueId(), arena);

        // add items
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().setItem(0, setLocationItem);
        player.getInventory().setItem(1, save);
        player.getInventory().setItem(2, cancel);
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        player.sendMessage(ColorUtils.color("&#1CE446אתה עכשיו במצב יצירת ארנה!"));
    }

    public void removeFromSetup(Player player) {
        // unregister the event (no double messages)
        HandlerList.unregisterAll(this);

        // restore players and remove from the list
        rollBackManager.restore(player);
        setup.remove(player.getUniqueId());

        player.playSound(player, Sound.ENTITY_WITHER_DEATH, 1, 1);
        player.sendMessage(ColorUtils.color("&#F03D15ביטלת את מצב יצירת ארנה!"));
    }

    public boolean isOnSetup(Player player) {
        return setup.containsKey(player.getUniqueId());
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasItem()) return;
        if (!event.getItem().hasItemMeta()) return;
        if (!isOnSetup(player)) return;

        Arena arena = setup.get(player.getUniqueId());
        ItemStack item = event.getItem();

        // items check
        if (item.isSimilar(setLocationItem)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                arena.saveLocation(plugin, player.getLocation(), "locationOne");
                player.sendMessage(ColorUtils.color("&aמיקום ראשון של הארנה נשמר!"));
            } else if (event.getAction() == Action.LEFT_CLICK_AIR) {
                arena.saveLocation(plugin, player.getLocation(), "locationTwo");
                player.sendMessage(ColorUtils.color("&aמיקום שני של הארנה נשמר!"));
            }

        } else if (item.isSimilar(save)) {
            if (!(event.getAction() == Action.RIGHT_CLICK_AIR)) return;

            // check if the locations are null, if null player need to set them
            if (arena.getSpawnLocationOne() == null || arena.getSpawnLocationTwo() == null) {
                player.sendMessage(ColorUtils.color("&cאתה לא יכול לצאת ממצב יצירת הארנות כיוון שלא סיימת את מלאכתך."));
                return;
            }

            // worlds need to have keepInventory(No dropping items) and immediate spawn.
            player.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            player.getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);

            // save config(the arena will be saved)  and remove from the list
            plugin.saveConfig();
            removeFromSetup(player);
        } else if (item.isSimilar(cancel)) {
            // cancel all the event and remove the arena

            arenaManager.getArenas().remove(setup.get(event.getPlayer().getUniqueId()));
            removeFromSetup(event.getPlayer());

            // reload the config(so the arena will not be on the config)
            plugin.reloadConfig();
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (!isOnSetup(event.getPlayer())) return;

        // if player quits the arena will be deleted
        arenaManager.getArenas().remove(setup.get(event.getPlayer().getUniqueId()));
        removeFromSetup(event.getPlayer());

        // reload the config(so the arena will not be on the config)
        plugin.reloadConfig();
    }

    @EventHandler
    private void onPlace(BlockPlaceEvent event) {
        if (!isOnSetup(event.getPlayer())) return;

        if (event.getBlock().getType() == Material.GREEN_WOOL) event.setBuild(false);
    }

    @EventHandler
    private void onBreak(BlockBreakEvent event) {
        if (!isOnSetup(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isOnSetup(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (!item.isSimilar(cancel) || !item.isSimilar(save) || !item.isSimilar(setLocationItem)) return;

        event.setCancelled(true);
    }

}
