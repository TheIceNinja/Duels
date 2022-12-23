package net.theiceninja.duels.commands.subcommands;

import lombok.AllArgsConstructor;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.utils.ColorUtils;
import net.theiceninja.duels.utils.Gui;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class MenuSubCommand implements SubCommand {

    private ArenaManager arenaManager;

    @Override
    public void execute(Player player, String[] args) {

        player.openInventory(Gui.arenasList(arenaManager));
        player.sendMessage(ColorUtils.color("&aפתחת את תפריט הקרבות!"));

    }

    @Override
    public String getName() {
        return "arenas";
    }
}
