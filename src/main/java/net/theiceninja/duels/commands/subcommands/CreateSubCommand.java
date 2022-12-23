package net.theiceninja.duels.commands.subcommands;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.Messages;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CreateSubCommand implements SubCommand {

    private final ArenaManager arenaManager;

    private final DuelsPlugin plugin;

    @Override
    public void execute(Player player, String[] args) {

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return;
        }

        if (args.length == 1) {
            player.sendMessage(ColorUtils.color("&cאתה צריך להקליד גם את שם הארנה."));
            return;
        }

        if (plugin.getConfig().getString("arenas." + args[1]) != null) {
            player.sendMessage(ColorUtils.color("&cיש כבר ארנה שקיימת עם השם הזה!"));
            return;
        }

        // adding the arena and add to the setupmode
        Arena arena = new Arena(args[1], ArenaState.DEFAULT, plugin, arenaManager);
        arenaManager.addArena(arena, plugin);
        player.sendMessage(ColorUtils.color("&aאתה יצרת את הארנה &2&l" + args[1] + "&a."));

        arena.getArenaSetupManager().addToSetup(player, arena);
    }

    @Override
    public String getName() {
        return "create";
    }
}
