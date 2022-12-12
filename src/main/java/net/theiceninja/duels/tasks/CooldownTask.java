package net.theiceninja.duels.tasks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaState;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public class CooldownTask extends BukkitRunnable {

    @Getter
    private int timer = 6;

    private final Arena arena;


    @Override
    public void run() {

        timer--;

        if (timer <= 0) {
            cancel();
            arena.setState(ArenaState.ACTIVE);
            arena.playsound(Sound.BLOCK_NOTE_BLOCK_BIT);
            return;
        }

        arena.sendTitle("&#0FBCF3המשחק יתחיל בעוד&8: &e" + timer);
        arena.playsound(Sound.BLOCK_NOTE_BLOCK_PLING);
        arena.updateScoreBoard();
    }
}
