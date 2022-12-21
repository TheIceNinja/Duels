package net.theiceninja.duels.commands.subcommands;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.MessageUtils;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class DeleteSubCommand implements SubCommand {

    private final ArenaManager arenaManager;

    private final DuelsPlugin plugin;

    @Override
    public void execute(Player player, String[] args) {

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(MessageUtils.NO_PERMISSION);
            return;
        }

        if (args.length == 1) {
            player.sendMessage(ColorUtils.color("&cאתה צריך להקליד גם את שם הארנה כדי למחוק."));
            return;
        }

        if (plugin.getConfig().getString("arenas." + args[1]) == null) {
            player.sendMessage(ColorUtils.color("&cהארנה הזאת לא קיימת, לכן אי אפשר למחוק."));
            return;
        }

        arenaManager.removeArena(args[1], plugin);
        player.sendMessage(ColorUtils.color("&cאתה מחקת את הארנה &4&l" + args[1] + "&c."));
    }

    @Override
    public String getName() {
        return "delete";
    }
}
