package dev.mark.kuznec.gui;

import dev.mark.kuznec.config.ConfigManager;
import dev.mark.kuznec.upgrade.EquipmentType;
import dev.mark.kuznec.upgrade.PurchaseResult;
import dev.mark.kuznec.upgrade.UpgradeDefinition;
import dev.mark.kuznec.upgrade.UpgradeManager;
import dev.mark.kuznec.util.TextUtil;
import dev.mark.kuznec.vault.VaultHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class InventoryListener implements Listener {

    private final GuiManager guiManager;
    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;
    private final VaultHook vaultHook;

    public InventoryListener(GuiManager guiManager,
                             ConfigManager configManager,
                             UpgradeManager upgradeManager,
                             VaultHook vaultHook) {
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.upgradeManager = upgradeManager;
        this.vaultHook = vaultHook;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof MainMenuHolder) && !(holder instanceof UpgradeMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (holder instanceof MainMenuHolder) {
            handleMainMenuClick(player, event.getRawSlot());
            return;
        }

        handleUpgradeMenuClick(player, (UpgradeMenuHolder) holder, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MainMenuHolder || holder instanceof UpgradeMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        MenuSlotType slotType = configManager.getMenuSlotTypeForSlot(slot);
        if (slotType == null) {
            return;
        }

        ItemStack item = slotType.getMenuItem(player);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        EquipmentType type = slotType.resolveUpgradeType(item);
        if (type == null) {
            sendError(player, configManager.getMessage("unsupported-item"));
            return;
        }

        guiManager.openUpgradeMenu(player, type);
    }

    private void handleUpgradeMenuClick(Player player, UpgradeMenuHolder holder, int slot) {
        if (slot == configManager.getBackButtonSlot()) {
            guiManager.openMainMenu(player);
            return;
        }

        List<Integer> upgradeSlots = configManager.getUpgradeSlots();
        int index = upgradeSlots.indexOf(slot);
        if (index < 0) {
            return;
        }

        List<UpgradeDefinition> upgrades = upgradeManager.getUpgrades(holder.getType());
        if (index >= upgrades.size()) {
            return;
        }

        Economy economy = vaultHook.getEconomy();
        if (economy == null) {
            sendError(player, configManager.getMessage("no-economy"));
            return;
        }

        UpgradeDefinition definition = upgrades.get(index);
        PurchaseResult result = upgradeManager.purchase(player, holder.getType(), definition, economy);
        handlePurchaseResult(player, holder, definition, result);
    }

    private void handlePurchaseResult(Player player,
                                      UpgradeMenuHolder holder,
                                      UpgradeDefinition definition,
                                      PurchaseResult result) {
        switch (result) {
            case SUCCESS:
                player.sendMessage(configManager.getMessage(
                        "purchase-success",
                        "%upgrade%", definition.getName(),
                        "%price%", TextUtil.formatPrice(definition.getPrice())
                ));
                playSound(player, configManager.getSuccessSound(), configManager.getSuccessVolume(), configManager.getSuccessPitch());
                guiManager.openUpgradeMenu(player, holder.getType());
                break;
            case ALREADY_PURCHASED:
                sendError(player, configManager.getMessage("already-purchased"));
                break;
            case NOT_ENOUGH_MONEY:
                sendError(player, configManager.getMessage("not-enough-money", "%price%", TextUtil.formatPrice(definition.getPrice())));
                break;
            case NO_ITEM:
                sendError(player, configManager.getMessage("no-item"));
                guiManager.openMainMenu(player);
                break;
            case UNSUPPORTED_ITEM:
                sendError(player, configManager.getMessage("unsupported-item"));
                guiManager.openMainMenu(player);
                break;
            default:
                sendError(player, configManager.getMessage("generic-error"));
                return;
        }
    }

    private void sendError(Player player, String message) {
        player.sendMessage(message);
        playSound(player, configManager.getErrorSound(), configManager.getErrorVolume(), configManager.getErrorPitch());
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
