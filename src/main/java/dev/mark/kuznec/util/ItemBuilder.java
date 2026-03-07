package dev.mark.kuznec.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder name(String value) {
        if (itemMeta != null && value != null) {
            itemMeta.setDisplayName(TextUtil.colorize(value));
        }
        return this;
    }

    public ItemBuilder lore(List<String> values) {
        if (itemMeta != null && values != null) {
            List<String> lore = itemMeta.hasLore() && itemMeta.getLore() != null
                    ? new ArrayList<String>(itemMeta.getLore())
                    : new ArrayList<String>();
            lore.addAll(TextUtil.colorize(values));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String value) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.hasLore() && itemMeta.getLore() != null
                    ? new ArrayList<String>(itemMeta.getLore())
                    : new ArrayList<String>();
            lore.add(TextUtil.colorize(value));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (itemMeta != null && flags != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
