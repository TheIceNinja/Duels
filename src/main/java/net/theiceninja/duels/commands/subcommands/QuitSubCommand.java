package net.theiceninja.duels.commands.subcommands;

import lombok.AllArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@AllArgsConstructor
public class QuitSubCommand implements SubCommand {

    private ArenaManager arenaManager;

    @Override
    public void execute(Player player, String[] args) {

        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאין ארנות"));
            return;
        }

        Optional<Arena> optionalArena = arenaManager.getArenas().stream().filter(arena ->
                arena.isPlaying(player) || arena.isSpectating(player)).findAny();

        if (optionalArena.isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאתה לא משחק!"));
            return;
        }

        if (optionalArena.get().isSpectating(player)) {
            optionalArena.get().removeSpectator(player, optionalArena);
            optionalArena.get().sendMessage("&c" + player.getDisplayName() + " &6יצא מצפייה מהארנה שלכם.");

        } else if (optionalArena.get().isPlaying(player)) {
            optionalArena.get().removePlayer(player);
        }
    }

    @Override
    public String getName() {
        return "quit";
    }
}
