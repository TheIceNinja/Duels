package net.theiceninja.duels.arena.manager;

import lombok.Getter;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArenaManager {

    @Getter
    private List<Arena> arenas = new ArrayList<>();

    public void addArena(Arena arena, DuelsPlugin plugin) {
        arenas.add(arena);
        plugin.getConfig().set("arenas." + arena.getName() + ".name", arena.getName());
    }

    public void removeArena(String arenaName, DuelsPlugin plugin) {
        arenas.removeIf(arena1 ->
                arena1.getName().equalsIgnoreCase(arenaName));

        plugin.getConfig().set("arenas." + arenaName, null);
        plugin.saveConfig();
    }

    public void load(DuelsPlugin plugin) {
        for (String key : plugin.getConfig().getConfigurationSection("arenas").getKeys(false)) {
            ConfigurationSection configSection = plugin.getConfig().getConfigurationSection("arenas." + key);
            if (configSection == null) return;

            Location spawnLocationOne = configSection.getLocation("spawnLocationOne");
            Location spawnLocationTwo = configSection.getLocation("spawnLocationTwo");
            String name = configSection.getString("name");
            Arena arena = new Arena(name, spawnLocationOne, spawnLocationTwo, plugin);
            plugin.getServer().getPluginManager().registerEvents(new ArenaListeners(arena), plugin);

             arenas.add(arena);
        }
    }

    public String getArenaStateToString(Arena arena) {
        if (arena.getArenaState() == ArenaState.DEFAULT) return "&#F3190Fמצב מכובה";

        if (arena.getArenaState() == ArenaState.COOLDOWN) return "&#F3C30Fמצב כוננות";

        if (arena.getArenaState() == ArenaState.ACTIVE) return "&#0FF319מצב פעיל";

        return null;
    }

    public Optional<Arena> findArena(String arenaName) {
        return getArenas().stream().filter(arena1 ->
                arena1.getName().equalsIgnoreCase(arenaName)).findAny();
    }
}