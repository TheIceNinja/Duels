package net.theiceninja.duels.listeners;

import lombok.AllArgsConstructor;
import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import net.theiceninja.duels.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

@AllArgsConstructor
public class GuiListener implements Listener {

    private ArenaManager arenaManager;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equalsIgnoreCase(ColorUtils.color("&8ארנות"))) {
            if (event.getCurrentItem() == null) return;
            if (!event.getCurrentItem().hasItemMeta()) return;

            String item = event.getCurrentItem().getItemMeta().getDisplayName();

            for (Arena arena : arenaManager.getArenas()) {
                if (item.equalsIgnoreCase(ColorUtils.color("&bארנה&8: &6" + arena.getName()))) {

           if (event.getClick() == ClickType.LEFT) {
               player.performCommand("duelpanel join " + item.substring(12));
            } else if (event.getClick() == ClickType.RIGHT) {
             player.performCommand("duelpanel spectate " + item.substring(12));
           }

             player.closeInventory();
           }
      }

            if (item.equalsIgnoreCase(ColorUtils.color("&cסגירה"))) {
                player.closeInventory();
                player.sendMessage(ColorUtils.color("&cסגרת את תפריט הקרבות."));
            } else if (item.equalsIgnoreCase(ColorUtils.color("&#E8BD2Aכניסה מהירה"))) {
                player.closeInventory();
                player.performCommand("duelpanel randomJoin");
            }

            event.setCancelled(true);
        }
    }
}
