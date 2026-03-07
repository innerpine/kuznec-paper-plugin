package dev.mark.kuznec.upgrade;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum EquipmentType {
    SWORD("sword", "Меч") {
        @Override
        public ItemStack getPlayerItem(Player player) {
            return player.getInventory().getItemInMainHand();
        }

        @Override
        public void setPlayerItem(Player player, ItemStack item) {
            player.getInventory().setItemInMainHand(item);
        }

        @Override
        public boolean matches(ItemStack item) {
            return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_SWORD");
        }
    },
    HELMET("helmet", "Шлем") {
        @Override
        public ItemStack getPlayerItem(Player player) {
            return player.getInventory().getHelmet();
        }

        @Override
        public void setPlayerItem(Player player, ItemStack item) {
            player.getInventory().setHelmet(item);
        }

        @Override
        public boolean matches(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
            return item.getType().name().endsWith("_HELMET") || item.getType() == Material.TURTLE_HELMET;
        }
    },
    CHESTPLATE("chestplate", "Нагрудник") {
        @Override
        public ItemStack getPlayerItem(Player player) {
            return player.getInventory().getChestplate();
        }

        @Override
        public void setPlayerItem(Player player, ItemStack item) {
            player.getInventory().setChestplate(item);
        }

        @Override
        public boolean matches(ItemStack item) {
            return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_CHESTPLATE");
        }
    },
    LEGGINGS("leggings", "Поножи") {
        @Override
        public ItemStack getPlayerItem(Player player) {
            return player.getInventory().getLeggings();
        }

        @Override
        public void setPlayerItem(Player player, ItemStack item) {
            player.getInventory().setLeggings(item);
        }

        @Override
        public boolean matches(ItemStack item) {
            return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_LEGGINGS");
        }
    },
    BOOTS("boots", "Ботинки") {
        @Override
        public ItemStack getPlayerItem(Player player) {
            return player.getInventory().getBoots();
        }

        @Override
        public void setPlayerItem(Player player, ItemStack item) {
            player.getInventory().setBoots(item);
        }

        @Override
        public boolean matches(ItemStack item) {
            return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_BOOTS");
        }
    };

    private final String configKey;
    private final String displayName;

    EquipmentType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract ItemStack getPlayerItem(Player player);

    public abstract void setPlayerItem(Player player, ItemStack item);

    public abstract boolean matches(ItemStack item);

    public static EquipmentType fromItem(ItemStack item) {
        if (SWORD.matches(item)) {
            return SWORD;
        }
        if (HELMET.matches(item)) {
            return HELMET;
        }
        if (CHESTPLATE.matches(item)) {
            return CHESTPLATE;
        }
        if (LEGGINGS.matches(item)) {
            return LEGGINGS;
        }
        if (BOOTS.matches(item)) {
            return BOOTS;
        }
        return null;
    }

    public static boolean isArmor(ItemStack item) {
        if (item == null) {
            return false;
        }
        return HELMET.matches(item)
                || CHESTPLATE.matches(item)
                || LEGGINGS.matches(item)
                || BOOTS.matches(item);
    }

    public static EquipmentType fromConfigKey(String key) {
        for (EquipmentType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
