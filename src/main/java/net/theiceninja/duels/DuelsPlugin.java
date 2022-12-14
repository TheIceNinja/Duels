package net.theiceninja.duels;

import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.commands.DuelPanelCommand;
import net.theiceninja.duels.listeners.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelsPlugin extends JavaPlugin {

    private ArenaManager arenaManager;

   // @Getter
   // private Database database;

  //  @Getter
   // private PlayerStats playerStats;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(false);
        saveDefaultConfig();

        getLogger().info("The plugin is enabled.");
       // connect();

        // playerStats = new PlayerStats();
       // this.getServer().getPluginManager().registerEvents(playerStats, this);

        // register listeners and commands

        arenaManager = new ArenaManager();
        registerCommands();
        registerListeners();

        // check if there are any arenas in the config, if there is arena load.
        if (getConfig().getConfigurationSection("arenas") != null) arenaManager.load(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("The plugin is disabled.");
    }

    /*
        private void connect() {
        database = new Database("localhost", "root", "", "statistics", 3306);
        database.createTable("playerStats", "uuid VARCHAR(36) primary key, wins int, loses int");
    }
     */

    private void registerCommands() {
        getCommand("duelpanel").setExecutor(new DuelPanelCommand(arenaManager, this));
        getCommand("duelpanel").setTabCompleter(new DuelPanelCommand(arenaManager, this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(arenaManager), this);
    }
}
