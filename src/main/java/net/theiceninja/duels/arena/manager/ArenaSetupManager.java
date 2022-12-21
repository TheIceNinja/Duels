package net.theiceninja.duels.arena.manager;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.ItemBuilder;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ArenaSetupManager implements Listener {

    private Map<UUID, Arena> setup = new HashMap<>();
    private final PlayerRollBackManager rollBackManager;
    private final DuelsPlugin plugin;
    private final ArenaManager arenaManager;

    private ItemStack setLocationItem = ItemBuilder.createItem(Material.BLAZE_ROD, 1, ColorUtils.color("&#F1CA16קביעת מיקומים &7(לחיצה ימנית/שמאלית)"));
    private ItemStack save = ItemBuilder.createItem(Material.GREEN_WOOL, 1, ColorUtils.color("&#12A459שמירת ארנה &7(לחיצה ימנית)"));

    private ItemStack cancel = ItemBuilder.createItem(Material.BARRIER, 1, ColorUtils.color("&#F03D15מחיקת ארנה &7(לחיצה ימנית)"));


    public void addToSetup(Player player, Arena arena) {
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
        HandlerList.unregisterAll(this);
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

            if (arena.getSpawnLocationOne() == null || arena.getSpawnLocationTwo() == null) {
                player.sendMessage(ColorUtils.color("&cאתה לא יכול לצאת ממצב יצירת הארנות כיוון שלא סיימת את מלאכתך."));
                return;
            }

            player.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            player.getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);

            plugin.saveConfig();
            removeFromSetup(player);
        } else if (item.isSimilar(cancel)) {
            arenaManager.getArenas().remove(setup.get(event.getPlayer().getUniqueId()));
            removeFromSetup(event.getPlayer());
            plugin.reloadConfig();
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (!isOnSetup(event.getPlayer())) return;
        arenaManager.getArenas().remove(setup.get(event.getPlayer().getUniqueId()));
        removeFromSetup(event.getPlayer());
        plugin.reloadConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlace(BlockPlaceEvent event) {
        if (!isOnSetup(event.getPlayer())) return;
        if (event.getBlock().getType() == Material.GREEN_WOOL) event.setBuild(false);
    }

    @EventHandler
    private void onBreak(BlockBreakEvent event) {
        if (!isOnSetup(event.getPlayer())) return;
        event.setCancelled(true);
    }
}
