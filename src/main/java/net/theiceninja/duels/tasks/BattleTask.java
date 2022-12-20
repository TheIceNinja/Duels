package net.theiceninja.duels.tasks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public class BattleTask extends BukkitRunnable {

    @Getter
    private int timer = 60 * 3;

    private final Arena arena;


    @Override
    public void run() {

        timer--;

        if (timer <= 0) {
            cancel();
            arena.sendTitle("&#0FF36Cהמשחק נגמר!");
            arena.sendMessage("&cאין מנצח? המשחק נגמר!");
            arena.cleanup();
        }

        arena.updateScoreBoard();
    }
}
