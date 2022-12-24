package net.theiceninja.duels.arena.listeners;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.Gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ArenaListeners implements Listener {

    @Getter
    private final Arena arena;

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // check if the player is playing or spectating and remove from the list.
        if (getArena().isPlaying(player)) {
            arena.removePlayer(player);
        } else if (getArena().isSpectating(player)) {
            getArena().removeSpectator(player, Optional.of(getArena()));
        }
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // if arena is not active cancel the damage(so the players will not die..
        if (getArena().isPlaying(player)) {
            if (arena.getArenaState() != ArenaState.ACTIVE)
                event.setCancelled(true);
        } else if (arena.isSpectating(player)) {
            // cancel damage if he is spec
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (arena.getArenaState() != ArenaState.ACTIVE) return;
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        // cancel pvp between players.
        if (arena.isPlaying(damager) && arena.isSpectating(damaged)) event.setCancelled(true);
        else if (arena.isPlaying(damaged) && arena.isSpectating(damager)) event.setCancelled(true);
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        // kill messages will be empty
        if (arena.getArenaState() == ArenaState.ACTIVE) event.setDeathMessage(null);
    }

    @EventHandler
    private void onRespawn(PlayerRespawnEvent event) {
        if (!arena.isPlaying(event.getPlayer())) return;

        Player player = event.getPlayer();
        // removing the player from the list
        new BukkitRunnable() {
            @Override
            public void run() {
                arena.removePlayer(player);
            }
        }.runTaskLater(arena.getPlugin(), 29);
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);
    }

    // soon
    @EventHandler(priority = EventPriority.LOWEST)
    private void onInvClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // if he is in game he cant click
        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);

        if (event.getView().getTitle().equalsIgnoreCase("&8מצב צופה")) {
            for (UUID playerUUID : arena.getPlayers()) {
                Player alivePlayer = Bukkit.getPlayer(playerUUID);
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                if (itemName.equalsIgnoreCase(ColorUtils.color(
                        "&a&l" + alivePlayer.getDisplayName() + " &c" + player.getHealth()
                ))) {
                    // soon
                    player.sendMessage(itemName.substring(10));
                }
            }
        }
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!event.getItem().hasItemMeta()) return;

        String itemName = event.getItem().getItemMeta().getDisplayName();
        Player player = event.getPlayer();

        if (itemName.equalsIgnoreCase(ColorUtils.color("&cעזיבת משחק &7(לחיצה ימנית)"))) {

            // check if the player is on the list and remove if spec or in game
            if (arena.getArenaState() != ArenaState.ACTIVE) {
                if (arena.isPlaying(player)) arena.removePlayer(player);
            } else if (arena.getArenaState() == ArenaState.ACTIVE) {

                if (getArena().isSpectating(player)) {
                    arena.sendMessage("&c" + player.getDisplayName() + " &6יצא מצפייה מהארנה שלכם.");
                    arena.removeSpectator(player, Optional.of(arena));
                }
            }
        } else if (itemName.equalsIgnoreCase(ColorUtils.color("&eמציאת שחקן &7(לחיצה ימנית)"))) {
            // soon
            player.openInventory(Gui.spectatingGUI(arena));
        }
    }

    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        Player player = (Player) event.getEntity();

        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!arena.isInGame(player)) return;

        // cancel all the commands accept one
        if (!event.getMessage().equalsIgnoreCase("/duelpanel quit")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.color(
                    "&cאתה לא יכול לבצע את הפקודה הזאת במהלך משחק."
            ));
        }
    }

    @EventHandler
    private void onProjectileDrop(ProjectileHitEvent event) {
        if (arena.getArenaState() != ArenaState.ACTIVE) return;
        if (event.getHitBlock() == null) return;

        event.getEntity().remove();
    }

    @EventHandler
    private void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onInteractOnEntity(PlayerInteractEntityEvent event) {
        if (!arena.isPlaying(event.getPlayer())) return;

        if (event.getRightClicked() instanceof ItemFrame)
            event.setCancelled(true);
        else if (event.getRightClicked() instanceof GlowItemFrame)
            event.setCancelled(true);
    }

    @EventHandler
    private void onSweet(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        if (!arena.isInGame(player)) return;

        if (event.getHarvestedBlock().getType() == Material.SWEET_BERRIES) event.setCancelled(true);
        if (event.getHarvestedBlock().getType() == Material.SWEET_BERRY_BUSH) event.setCancelled(true);
    }

    @EventHandler
    private void onPickUp(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onSwap(PlayerSwapHandItemsEvent event) {
        if (!getArena().isInGame(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!getArena().isInGame(player)) return;
        // sending the arena messages
        arena.sendMessage(getStatePlayerEnglish(player) + player.getDisplayName() + "&8: &f" + event.getMessage());
        event.setCancelled(true);
    }

    // gets the player state
    private String getStatePlayerEnglish(Player player) {
        if (arena.isPlaying(player)) return "&#0FE716&lAlive &#2AE886";

        if (getArena().isSpectating(player)) return "&#B2C2B3&lSpectator &#B2C2B3";

        return null;
    }
}