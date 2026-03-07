package dev.mark.kuznec.gui;

import dev.mark.kuznec.config.ConfigManager;
import dev.mark.kuznec.config.FillerItemConfig;
import dev.mark.kuznec.config.FillerMenuType;
import dev.mark.kuznec.upgrade.EquipmentType;
import dev.mark.kuznec.upgrade.UpgradeDefinition;
import dev.mark.kuznec.upgrade.UpgradeManager;
import dev.mark.kuznec.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GuiManager {

    private static final int MENU_SIZE = 54;

    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;

    public GuiManager(ConfigManager configManager, UpgradeManager upgradeManager) {
        this.configManager = configManager;
        this.upgradeManager = upgradeManager;
    }

    public void openMainMenu(Player player) {
        MainMenuHolder holder = new MainMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, configManager.getMainMenuTitle());
        holder.setInventory(inventory);

        fillWithFiller(inventory, FillerMenuType.MAIN);
        for (Map.Entry<MenuSlotType, Integer> entry : configManager.getMainMenuSlots().entrySet()) {
            inventory.setItem(entry.getValue(), buildMainMenuItem(player, entry.getKey()));
        }

        player.openInventory(inventory);
    }

    public void openUpgradeMenu(Player player, EquipmentType type) {
        UpgradeMenuHolder holder = new UpgradeMenuHolder(type);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, configManager.getUpgradeMenuTitle(type.getDisplayName()));
        holder.setInventory(inventory);

        fillWithFiller(inventory, FillerMenuType.UPGRADE);
        inventory.setItem(configManager.getBackButtonSlot(), configManager.createBackButton());

        List<UpgradeDefinition> upgrades = upgradeManager.getUpgrades(type);
        List<Integer> slots = configManager.getUpgradeSlots();
        for (int index = 0; index < slots.size() && index < upgrades.size(); index++) {
            placeUpgradeItem(inventory, player, type, upgrades.get(index), slots.get(index));
        }

        player.openInventory(inventory);
    }

    private void fillWithFiller(Inventory inventory, FillerMenuType menuType) {
        if (!configManager.isFillerEnabled(menuType)) {
            return;
        }

        for (FillerItemConfig fillerItem : configManager.getFillerItems(menuType)) {
            for (Integer slot : fillerItem.getSlots()) {
                if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                    continue;
                }
                inventory.setItem(slot, fillerItem.getItemStack().clone());
            }
        }
    }

    private ItemStack buildMainMenuItem(Player player, MenuSlotType slotType) {
        ItemStack playerItem = slotType.getMenuItem(player);
        if (playerItem == null || playerItem.getType() == Material.AIR) {
            return configManager.createPlaceholderItem();
        }

        ItemStack clone = playerItem.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<String>(meta.getLore())
                : new ArrayList<String>();
        lore.add("");
        if (slotType.resolveUpgradeType(clone) != null) {
            lore.addAll(configManager.getPreviewLoreSupported());
        } else {
            lore.addAll(configManager.getPreviewLoreUnsupported());
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        clone.setItemMeta(meta);
        return clone;
    }

    private ItemStack buildUpgradeItem(Player player, EquipmentType type, UpgradeDefinition definition) {
        ItemBuilder builder = new ItemBuilder(definition.getIconMaterial())
                .name(definition.getName())
                .lore(definition.getDescription())
                .flags(ItemFlag.values())
                .addLoreLine("");

        ItemStack actualItem = type.getPlayerItem(player);
        if (actualItem != null && upgradeManager.hasUpgrade(actualItem, definition)) {
            builder.lore(configManager.getUpgradePurchasedLore());
        } else {
            builder.lore(configManager.getUpgradeAvailableLore(definition.getPrice()));
        }
        return builder.build();
    }

    private void placeUpgradeItem(Inventory inventory, Player player, EquipmentType type, UpgradeDefinition definition, int slot) {
        inventory.setItem(slot, buildUpgradeItem(player, type, definition));
    }
}
