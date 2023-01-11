package net.theiceninja.duels.utils;

import net.theiceninja.duels.arena.Arena;
import net.theiceninja.duels.arena.manager.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class Gui {

    public static Inventory arenasList(ArenaManager arenaManager) {
        Inventory inventory = createInv(36, "&8ארנות");

        // TODO: do max arenas in inventory(26)
        for (Arena arena : arenaManager.getArenas()) {
          ItemStack arenaItem = ItemCreator.createItem(
                    Material.BLUE_ICE,
                    1,
                    "&bארנה&8: &6" + arena.getName(),
                    "&r",
                    "&fמצב ארנה&8: " + arenaManager.getArenaStateToString(arena),
                    "&fכמות האנשים בארנה&8: &a" + arena.getPlayers().size() + "/2",
                    "&r",
                    "&7לחיצה ימנית לצפייה במשחק!",
                    "&7לחיצה שמאלית לכניסה למשחק!"
            );

            inventory.addItem(arenaItem);
        }
        inventory.setItem(31, ItemCreator.createItem(
                Material.BARRIER,
                1,
                "&cסגירה"
        ));

        inventory.setItem(32, ItemCreator.createItem(
                Material.ENDER_PEARL,
                1,
                "&#E8BD2Aכניסה מהירה"
        ));

        return inventory;
    }

    // soon
    public static Inventory spectatingGUI(Arena arena) {
        Inventory inventory = createInv(9, "&8מצב צופה");

            if (arena == null || arena.getPlayers().isEmpty()) return null;
            Player playerOne = Bukkit.getPlayer(arena.getPlayers().get(0));
            Player playerTwo = Bukkit.getPlayer(arena.getPlayers().get(1));


            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (playerOne == null) return null;
            skullMeta.setOwningPlayer(playerOne);
            skullMeta.setDisplayName(ColorUtils.color("&a&l" + playerOne.getDisplayName()));
            skull.setItemMeta(skullMeta);

            inventory.setItem(0, skull);

            ItemStack skullTwo = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMetaTwo = (SkullMeta) skullTwo.getItemMeta();
             if (playerTwo == null) return null;
            skullMetaTwo.setOwningPlayer(playerTwo);
            skullMetaTwo.setDisplayName(ColorUtils.color("&a&l" + playerTwo.getDisplayName()));
            skullTwo.setItemMeta(skullMetaTwo);

            inventory.setItem(1, skullTwo);


        inventory.setItem(8, ItemCreator.createItem(
                Material.BARRIER,
                1,
                "&cסגירה"
        ));

        return inventory;
    }

    private static Inventory createInv(int invSize, String title) {
        Inventory inv = Bukkit.createInventory(null, invSize, ColorUtils.color(title));
        return inv;
    }
}
