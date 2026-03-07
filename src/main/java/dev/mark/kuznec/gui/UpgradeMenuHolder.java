package dev.mark.kuznec.gui;

import dev.mark.kuznec.upgrade.EquipmentType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class UpgradeMenuHolder implements InventoryHolder {

    private final EquipmentType type;
    private Inventory inventory;

    public UpgradeMenuHolder(EquipmentType type) {
        this.type = type;
    }

    public EquipmentType getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
