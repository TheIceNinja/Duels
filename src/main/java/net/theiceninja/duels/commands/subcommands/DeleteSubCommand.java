package net.theiceninja.duels.commands.subcommands;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.Messages;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
public class DeleteSubCommand implements SubCommand {

    private final ArenaManager arenaManager;
    private final DuelsPlugin plugin;

    @Override
    public void execute(Player player, String[] args) {

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return;
        }

        if (args.length == 1) {
            player.sendMessage(ColorUtils.color("&cאתה צריך להקליד גם את שם הארנה כדי למחוק."));
            return;
        }

        Optional<Arena> optionalArena = arenaManager.findArena(args[1]);

        if (optionalArena.isEmpty()) {
            player.sendMessage(ColorUtils.color("&cהארנה הזאת לא קיימת, לכן אי אפשר למחוק."));
            return;
        }

        if (optionalArena.get().getArenaState() != ArenaState.DEFAULT) {
            player.sendMessage(ColorUtils.color("&cהארנה כרגע במצב פעיל, לכן לא תוכל למחוק את הארנה."));
            return;
        }

        // delete arena
        arenaManager.removeArena(args[1], plugin);
        player.sendMessage(ColorUtils.color("&cאתה מחקת את הארנה &4&l" + args[1] + "&c."));
    }

    @Override
    public String getName() {
        return "delete";
    }
}
