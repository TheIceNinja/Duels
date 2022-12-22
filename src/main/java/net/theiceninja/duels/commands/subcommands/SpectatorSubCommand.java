package net.theiceninja.duels.commands.subcommands;

import lombok.AllArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@AllArgsConstructor
public class SpectatorSubCommand implements SubCommand {

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

        for (Arena arena : arenaManager.getArenas()) {
            if (arena.isPlaying(player) || arena.isSpectating(player)) {
                player.sendMessage(ColorUtils.color("&cאתה במשחק אחר, אתה צריך לצאת כדי להכנס לכאן."));
                return;
            }
        }

        if (optionalArena.get().getArenaState() != ArenaState.ACTIVE) {
            player.sendMessage(ColorUtils.color("&cהארנה לא פעילה, אז אתה לא יכול לצפות באף אחד."));
            return;
        }

        optionalArena.get().addSpectator(player, optionalArena);
        optionalArena.get().sendMessage("&a" + player.getDisplayName() + " &6נכנס לצפות בקרב שלכם!");

    }

    @Override
    public String getName() {
        return "spectate";
    }
}
