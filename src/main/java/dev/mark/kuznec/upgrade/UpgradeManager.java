package dev.mark.kuznec.upgrade;

import dev.mark.kuznec.config.ConfigManager;
import dev.mark.kuznec.util.TextUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UpgradeManager {

    private static final String LORE_DELIMITER = "\u0000";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<EquipmentType, List<UpgradeDefinition>> upgradesByType;
    private final Map<String, NamespacedKey> keysByUpgrade;
    private final NamespacedKey baseLoreKey;

    public UpgradeManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.upgradesByType = new EnumMap<EquipmentType, List<UpgradeDefinition>>(EquipmentType.class);
        this.keysByUpgrade = new HashMap<String, NamespacedKey>();
        this.baseLoreKey = new NamespacedKey(plugin, "base_item_lore");
    }

    public void load() {
        upgradesByType.clear();
        keysByUpgrade.clear();

        ConfigurationSection upgradesSection = configManager.getUpgradesSection();
        if (upgradesSection == null) {
            plugin.getLogger().warning("Секция upgrades не найдена в effects.yml");
            return;
        }

        for (String typeKey : upgradesSection.getKeys(false)) {
            EquipmentType type = EquipmentType.fromConfigKey(typeKey);
            if (type == null) {
                plugin.getLogger().warning("Пропущен неизвестный тип улучшений: " + typeKey);
                continue;
            }

            ConfigurationSection typeSection = upgradesSection.getConfigurationSection(typeKey);
            if (typeSection == null) {
                continue;
            }

            List<UpgradeDefinition> definitions = new ArrayList<UpgradeDefinition>();
            for (String upgradeId : typeSection.getKeys(false)) {
                ConfigurationSection upgradeSection = typeSection.getConfigurationSection(upgradeId);
                if (upgradeSection == null) {
                    continue;
                }

                UpgradeDefinition definition = new UpgradeDefinition(
                        upgradeId,
                        type,
                        parseMaterial(upgradeSection.getString("icon"), defaultIcon(type)),
                        TextUtil.colorize(upgradeSection.getString("name", upgradeId)),
                        TextUtil.colorize(upgradeSection.getStringList("description")),
                        upgradeSection.getDouble("price", 0.0D),
                        UpgradeEffect.fromConfig(upgradeSection.getConfigurationSection("effect"))
                );
                definitions.add(definition);
                keysByUpgrade.put(definition.getUniqueKey(), new NamespacedKey(plugin, definition.getUniqueKey().toLowerCase()));
            }

            upgradesByType.put(type, definitions);
        }
    }

    public List<UpgradeDefinition> getUpgrades(EquipmentType type) {
        List<UpgradeDefinition> definitions = upgradesByType.get(type);
        if (definitions == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(definitions);
    }

    public boolean hasUpgrade(ItemStack item, UpgradeDefinition definition) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        NamespacedKey key = keysByUpgrade.get(definition.getUniqueKey());
        if (key == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.BYTE);
    }

    public List<UpgradeDefinition> getPurchasedUpgrades(ItemStack item, EquipmentType type) {
        List<UpgradeDefinition> purchased = new ArrayList<UpgradeDefinition>();
        for (UpgradeDefinition definition : getUpgrades(type)) {
            if (hasUpgrade(item, definition)) {
                purchased.add(definition);
            }
        }
        return purchased;
    }

    public PurchaseResult purchase(Player player, EquipmentType type, UpgradeDefinition definition, Economy economy) {
        ItemStack item = type.getPlayerItem(player);
        if (item == null || item.getType() == Material.AIR) {
            return PurchaseResult.NO_ITEM;
        }

        if (!type.matches(item)) {
            return PurchaseResult.UNSUPPORTED_ITEM;
        }

        if (hasUpgrade(item, definition)) {
            return PurchaseResult.ALREADY_PURCHASED;
        }

        if (item.getItemMeta() == null) {
            return PurchaseResult.UNSUPPORTED_ITEM;
        }

        NamespacedKey key = keysByUpgrade.get(definition.getUniqueKey());
        if (key == null) {
            return PurchaseResult.UNSUPPORTED_ITEM;
        }

        if (!economy.has(player, definition.getPrice())) {
            return PurchaseResult.NOT_ENOUGH_MONEY;
        }

        EconomyResponse response = economy.withdrawPlayer(player, definition.getPrice());
        if (!response.transactionSuccess()) {
            return PurchaseResult.NOT_ENOUGH_MONEY;
        }

        ItemStack updated = item.clone();
        ItemMeta meta = updated.getItemMeta();
        if (meta == null) {
            economy.depositPlayer(player, definition.getPrice());
            return PurchaseResult.UNSUPPORTED_ITEM;
        }

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        updated.setItemMeta(meta);
        applyPurchasedUpgradeLore(updated, type);
        type.setPlayerItem(player, updated);
        player.updateInventory();

        if (configManager.isDebugMode()) {
            plugin.getLogger().info("[Debug] " + player.getName()
                    + " купил улучшение \"" + definition.getName()
                    + "\" (" + definition.getUniqueKey() + ")"
                    + " за " + definition.getPrice());
        }

        return PurchaseResult.SUCCESS;
    }

    private void applyPurchasedUpgradeLore(ItemStack item, EquipmentType type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        List<String> baseLore = readStoredBaseLore(container);
        if (baseLore == null) {
            baseLore = meta.hasLore() && meta.getLore() != null
                    ? new ArrayList<String>(meta.getLore())
                    : new ArrayList<String>();
            container.set(baseLoreKey, PersistentDataType.STRING, serializeLore(baseLore));
        }

        List<String> finalLore = new ArrayList<String>(baseLore);
        List<UpgradeDefinition> purchasedUpgrades = getPurchasedUpgrades(item, type);
        if (!purchasedUpgrades.isEmpty()) {
            if (!finalLore.isEmpty() && configManager.shouldInsertEmptyLineBeforeUpgradeLore()) {
                finalLore.add("");
            }

            for (UpgradeDefinition purchasedUpgrade : purchasedUpgrades) {
                finalLore.add(configManager.formatPurchasedUpgradeLoreLine(purchasedUpgrade.getName()));
            }
        }

        meta.setLore(finalLore.isEmpty() ? null : finalLore);
        item.setItemMeta(meta);
    }

    private List<String> readStoredBaseLore(PersistentDataContainer container) {
        if (!container.has(baseLoreKey, PersistentDataType.STRING)) {
            return null;
        }

        String raw = container.get(baseLoreKey, PersistentDataType.STRING);
        if (raw == null) {
            return new ArrayList<String>();
        }

        return deserializeLore(raw);
    }

    private String serializeLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return "";
        }
        return joinLore(lore, LORE_DELIMITER);
    }

    private List<String> deserializeLore(String raw) {
        List<String> lore = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) {
            return lore;
        }

        String[] parts = raw.split(LORE_DELIMITER, -1);
        Collections.addAll(lore, parts);
        return lore;
    }

    private String joinLore(List<String> lines, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase());
        return material == null ? fallback : material;
    }

    private Material defaultIcon(EquipmentType type) {
        switch (type) {
            case SWORD:
                return Material.DIAMOND_SWORD;
            case HELMET:
                return Material.DIAMOND_HELMET;
            case CHESTPLATE:
                return Material.DIAMOND_CHESTPLATE;
            case LEGGINGS:
                return Material.DIAMOND_LEGGINGS;
            case BOOTS:
                return Material.DIAMOND_BOOTS;
            default:
                return Material.PAPER;
        }
    }
}
