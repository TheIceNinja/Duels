package net.theiceninja.duels;

import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.commands.DuelPanelCommand;
import net.theiceninja.duels.listeners.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelsPlugin extends JavaPlugin {

    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        super.onEnable();
        getConfig().options().copyDefaults(false);
        saveDefaultConfig();
        arenaManager = new ArenaManager();
        getCommand("duelpanel").setExecutor(new DuelPanelCommand(arenaManager, this));
        getCommand("duelpanel").setTabCompleter(new DuelPanelCommand(arenaManager, this));
        getServer().getPluginManager().registerEvents(new GuiListener(arenaManager), this);
        if (getConfig().getConfigurationSection("arenas") != null)
        arenaManager.load(this);

    }


    @Override
    public void onDisable() {
        super.onDisable();
    }
}
