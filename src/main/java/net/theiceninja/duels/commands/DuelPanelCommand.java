package net.theiceninja.duels.commands;

import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.commands.subcommands.*;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DuelPanelCommand implements CommandExecutor, TabCompleter {

    // subcommands - add all the subcommands.
    private List<SubCommand> subCommands = new ArrayList<>();

    private final ArenaManager arenaManager;

    private final DuelsPlugin plugin;

    public DuelPanelCommand(ArenaManager arenaManager, DuelsPlugin plugin) {
        this.arenaManager = arenaManager;
        this.plugin = plugin;
        subCommands.add(new CreateSubCommand(arenaManager, plugin));
        subCommands.add(new ListSubCommand(arenaManager));
        subCommands.add(new DeleteSubCommand(arenaManager, plugin));
        subCommands.add(new RandomJoinSubCommand(arenaManager));
        subCommands.add(new QuitSubCommand(arenaManager));
        subCommands.add(new MenuSubCommand(arenaManager));
        subCommands.add(new JoinSubCommand(arenaManager));
        subCommands.add(new SpectatorSubCommand(arenaManager));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.MUST_BE_PLAYER_ERROR);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("duels.admin")) {
                player.sendMessage(ColorUtils.color("&eUsage: /duelpanel <randomJoin|quit|join>"));
                return true;
            }
            player.sendMessage(ColorUtils.color("&eUsage: /duelpanel <create|list|delete|randomJoin|quit|join>"));
            return true;
        }

        for (SubCommand subCommand : subCommands) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                subCommand.execute(player, args);
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> complete = new ArrayList<>();
        if (complete.isEmpty() && args.length == 1 && sender.hasPermission("duels.admin")) {
            complete.add("create");
            complete.add("list");
            complete.add("delete");
            complete.add("randomJoin");
            complete.add("quit");
            complete.add("arenas");
            complete.add("join");
            complete.add("spectate");
        } else if (complete.isEmpty() && args.length == 1) {
            complete.add("randomJoin");
            complete.add("quit");
            complete.add("arenas");
            complete.add("join");
            complete.add("spectate");
        }
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String a : complete) {
                if (a.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    result.add(a);
            }
            return result;
        }
        return null;
    }
}
