package dev.mark.kuznec.gui;

import dev.mark.kuznec.upgrade.EquipmentType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum MenuSlotType {
    MAIN_HAND("main-hand") {
        @Override
        public ItemStack getMenuItem(Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR || EquipmentType.isArmor(item)) {
                return null;
            }
            return item;
        }

        @Override
        public EquipmentType resolveUpgradeType(ItemStack item) {
            return EquipmentType.fromItem(item);
        }
    },
    HELMET("helmet") {
        @Override
        public ItemStack getMenuItem(Player player) {
            return player.getInventory().getHelmet();
        }

        @Override
        public EquipmentType resolveUpgradeType(ItemStack item) {
            return EquipmentType.HELMET.matches(item) ? EquipmentType.HELMET : null;
        }
    },
    CHESTPLATE("chestplate") {
        @Override
        public ItemStack getMenuItem(Player player) {
            return player.getInventory().getChestplate();
        }

        @Override
        public EquipmentType resolveUpgradeType(ItemStack item) {
            return EquipmentType.CHESTPLATE.matches(item) ? EquipmentType.CHESTPLATE : null;
        }
    },
    LEGGINGS("leggings") {
        @Override
        public ItemStack getMenuItem(Player player) {
            return player.getInventory().getLeggings();
        }

        @Override
        public EquipmentType resolveUpgradeType(ItemStack item) {
            return EquipmentType.LEGGINGS.matches(item) ? EquipmentType.LEGGINGS : null;
        }
    },
    BOOTS("boots") {
        @Override
        public ItemStack getMenuItem(Player player) {
            return player.getInventory().getBoots();
        }

        @Override
        public EquipmentType resolveUpgradeType(ItemStack item) {
            return EquipmentType.BOOTS.matches(item) ? EquipmentType.BOOTS : null;
        }
    };

    private final String configKey;

    MenuSlotType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public abstract ItemStack getMenuItem(Player player);

    public abstract EquipmentType resolveUpgradeType(ItemStack item);
}
