package net.theiceninja.duels.arena.manager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    private final ArenaManager arenaManager;


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!getArena().isInGame(player)) return;
        if (player == null) return;
        if (getArena().isPlaying(player)) {
            arena.removePlayer(player);

        } else if (getArena().isSpectating(player)) {
            getArena().removeSpectator(player, Optional.of(getArena()));

        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (getArena().isPlaying(player)) {
            if (arena.getArenaState() != ArenaState.ACTIVE)
                event.setCancelled(true);
        } else if (arena.isSpectating(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (arena.getArenaState() != ArenaState.ACTIVE) return;
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (arena.isPlaying(damager) && arena.isSpectating(damaged)) event.setCancelled(true);
        else if (arena.isPlaying(damaged) && arena.isSpectating(damager)) event.setCancelled(true);
    }


    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (arena.getArenaState() == ArenaState.ACTIVE) event.setDeathMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!arena.isPlaying(event.getPlayer())) return;

        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                cancel();
                arena.removePlayer(player);
            }
        }.runTaskTimer(arena.getPlugin(),0,  29);
    }


    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);

        if (event.getView().getTitle().equalsIgnoreCase("&8מצב צופה")) {
            for (UUID playerUUID : arena.getPlayers()) {
                Player alivePlayer = Bukkit.getPlayer(playerUUID);
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                if (itemName.equalsIgnoreCase(ColorUtils.color("&a&l" + alivePlayer.getDisplayName() + " &c" + player.getHealth()))) {
                    player.sendMessage(itemName.substring(10));
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!event.getItem().hasItemMeta()) return;
        String itemName = event.getItem().getItemMeta().getDisplayName();
        Player player = event.getPlayer();
        if (itemName.equalsIgnoreCase(ColorUtils.color("&cעזיבת משחק &7(לחיצה ימנית)"))) {


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
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        Player player = (Player) event.getEntity();
        if (!getArena().isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (!arena.isInGame(player)) return;

        if (!event.getMessage().equalsIgnoreCase("/duelpanel quit")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.color(
                    "&cאתה לא יכול לבצע את הפקודה הזאת במהלך משחק."
            ));
        }
    }

    @EventHandler
    public void onProjectileDrop(ProjectileHitEvent event) {
        if (arena.getArenaState() != ArenaState.ACTIVE) return;
        if (event.getHitBlock() == null) return;
        event.getEntity().remove();
    }
    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractOnEntity(PlayerInteractEntityEvent event) {
        if (!arena.isPlaying(event.getPlayer())) return;
        if (event.getRightClicked() instanceof ItemFrame)
            event.setCancelled(true);
        else if (event.getRightClicked() instanceof GlowItemFrame)
            event.setCancelled(true);
    }

    @EventHandler
    public void onSweet(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        if (!arena.isInGame(player)) return;
        if (event.getHarvestedBlock().getType() == Material.SWEET_BERRIES) event.setCancelled(true);
        if (event.getHarvestedBlock().getType() == Material.SWEET_BERRY_BUSH) event.setCancelled(true);
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent event) {

        Player player = event.getPlayer();

        if (!arena.isInGame(player)) return;
        event.setCancelled(true);
    }
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!getArena().isInGame(event.getPlayer())) return;
        event.setCancelled(true);
    }
}