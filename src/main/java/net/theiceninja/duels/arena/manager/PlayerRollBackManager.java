package net.theiceninja.duels.arena.manager;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRollBackManager {

    private final Map<UUID, ItemStack[]> playerInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> playerArmor = new HashMap<>();
    private final Map<UUID, Location> playerLoc = new HashMap<>();
    private final Map<UUID, GameMode> playerGameMode = new HashMap<>();
    private final Map<UUID, Double> playerHealth = new HashMap<>();

    /**
     *
     * @param player save the player stuff
     */

    public void save(Player player) {
        playerArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
        playerInventory.put(player.getUniqueId(), player.getInventory().getContents());
        playerLoc.put(player.getUniqueId(), player.getLocation());
        playerGameMode.put(player.getUniqueId(), player.getGameMode());
        playerHealth.put(player.getUniqueId(), player.getHealth());
        player.getInventory().clear();
    }

    /**
     *
     * @param player restoring player stuff from the beginning
     */
    public void restore(Player player) {
        player.getInventory().clear();
        ItemStack[] previousInventory = playerInventory.get(player.getUniqueId());
        if (previousInventory != null) player.getInventory().setContents(previousInventory);

        ItemStack[] previousArmor = playerArmor.get(player.getUniqueId());
        if (previousArmor != null) player.getInventory().setArmorContents(previousArmor);

        Location previousLocation = playerLoc.get(player.getUniqueId());
        if (previousLocation != null) player.teleport(previousLocation);

        GameMode previousGameMode = playerGameMode.get(player.getUniqueId());
        if (previousGameMode != null)  player.setGameMode(previousGameMode);

        double previousHealth = playerHealth.get(player.getUniqueId());
        if (previousHealth != 0) player.setHealth(previousHealth);

        playerInventory.remove(player.getUniqueId());
        playerHealth.remove(player.getUniqueId());
        playerArmor.remove(player.getUniqueId());
        playerLoc.remove(player.getUniqueId());
        playerGameMode.remove(player.getUniqueId());

    }
}
