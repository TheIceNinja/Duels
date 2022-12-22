package net.theiceninja.duels.commands.subcommands;

import lombok.AllArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@AllArgsConstructor
public class JoinSubCommand implements SubCommand {

    private ArenaManager arenaManager;

    @Override
    public void execute(Player player, String[] args) {

        if (args.length == 1) {
            player.sendMessage(ColorUtils.color("&cאתה צריך להקליד את שם הארנה שאתה רוצה להכנס."));
            return;
        }

        Optional<Arena> optionalArena = arenaManager.findArena(args[1]);

        if (optionalArena.isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאין שום ארנות עם השם הזה."));
            return;
        }

        if (optionalArena.get().getArenaState() != ArenaState.DEFAULT) {
            player.sendMessage(ColorUtils.color("&cמשחק זה במצב פעיל, אז אי אפשר להכנס."));
            return;
        }

        for (Arena arena : arenaManager.getArenas()) {
            if (arena.isPlaying(player) || arena.isSpectating(player)) {
                player.sendMessage(ColorUtils.color("&cאתה במשחק אחר, אתה צריך לצאת כדי להכנס לכאן."));
                return;
            }
        }

        optionalArena.get().join(player, optionalArena);
        player.sendMessage(ColorUtils.color("&aנכנסת לארנה &2&l" + optionalArena.get().getName() + "&a."));

    }

    @Override
    public String getName() {
        return "join";
    }
}
