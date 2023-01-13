package net.theiceninja.duels.tasks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.DuelsPlugin;
import net.theiceninja.duels.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@RequiredArgsConstructor
public class BattleTask extends BukkitRunnable {

    @Getter
    private int timer = 120;
    private final Arena arena;

    @Override
    public void run() {
        timer--;

        if (timer <= 0) {
            cancel();
            // game over
            arena.sendTitle("&#0FF36Cהמשחק נגמר!");
            arena.sendMessage("&cאין מנצח? המשחק נגמר!");

            Player playerOne = Bukkit.getPlayer(arena.getPlayers().get(0));
            Player playerTwo = Bukkit.getPlayer(arena.getPlayers().get(1));

            // removing the spec and show players
            for (UUID playerUUID : arena.getSpectating()) {
                Player spectators = Bukkit.getPlayer(playerUUID);
                if (spectators == null) return;
                playerOne.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class), spectators);
                playerTwo.showPlayer(DuelsPlugin.getPlugin(DuelsPlugin.class),spectators);
            }

            arena.cleanup();
            return;
        }

        // update the scoreboard if the cooldown is not 0
        arena.updateScoreBoard();

        if (timer == 10 || timer <= 5) {
            arena.sendActionBar("&cהמשחק נגמר בעוד&8: &e" + timer);
            arena.playsound(Sound.BLOCK_NOTE_BLOCK_PLING);
        }
    }
}
