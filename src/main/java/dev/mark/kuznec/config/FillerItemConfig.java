package dev.mark.kuznec.config;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public final class FillerItemConfig {

    private final ItemStack itemStack;
    private final List<Integer> slots;

    public FillerItemConfig(ItemStack itemStack, List<Integer> slots) {
        this.itemStack = itemStack;
        this.slots = slots;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public List<Integer> getSlots() {
        return Collections.unmodifiableList(slots);
    }
}
