package net.theiceninja.duels.commands.subcommands;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.MessageUtils;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class ListSubCommand implements SubCommand {

    private final ArenaManager arenaManager;

    @Override
    public void execute(Player player, String[] args) {

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(MessageUtils.NO_PERMISSION);
            return;
        }

        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאין שום ארנות, אתה צריך ליצור אחד."));
            return;
        }

        for (Arena arena : arenaManager.getArenas()) {
            player.sendMessage(ColorUtils.color(
                    "&b===================" + "\n"
                    +   "&fשם הארנה&8: &6" + arena.getName() + "\n"
                    +   "&fמצב הארנה&8: " + arenaManager.getArenaStateToString(arena) + "\n"
            ));
            player.sendMessage(ColorUtils.color("&b==================="));
        }
    }

    @Override
    public String getName() {
        return "list";
    }
}
