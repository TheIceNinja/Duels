package net.theiceninja.duels.commands.subcommands;

import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.arena.manager.ArenaState;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
public class RandomJoinSubCommand implements SubCommand {

    private final ArenaManager arenaManager;

    @Override
    public void execute(Player player, String[] args) {

        Optional<Arena> optionalArena = arenaManager.getArenas()
                .stream()
                .filter(arena -> arena.getArenaState() == ArenaState.DEFAULT)
                .findAny();

        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאין שום ארנות."));
            return;
        }

        if (optionalArena.isEmpty()) {
            player.sendMessage(ColorUtils.color("&cאין ארנות שתוכל לשחק בהם."));
            return;
        }

        for (Arena arena : arenaManager.getArenas()) {
            if (arena.isPlaying(player) || arena.isSpectating(player)) {
                player.sendMessage(ColorUtils.color("&cאתה במשחק אחר, אתה צריך לצאת כדי לשחק."));
                return;
            }
        }

        optionalArena.get().join(player, optionalArena);
        player.sendMessage(ColorUtils.color("&aנכנסת לארנה &2&l" + optionalArena.get().getName() + "&a."));
    }

    @Override
    public String getName() {
        return "randomJoin";
    }
}
