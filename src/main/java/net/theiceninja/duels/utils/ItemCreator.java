package net.theiceninja.duels.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ItemCreator {

    public static ItemStack createItem(Material material, int amount, String displayName, String... lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemStack.setAmount(amount);
        itemMeta.setDisplayName(ColorUtils.color(displayName));
        itemMeta.setUnbreakable(true);
        itemMeta.setLore(Arrays.stream(lore).map(ColorUtils::color).collect(Collectors.toList()));

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
