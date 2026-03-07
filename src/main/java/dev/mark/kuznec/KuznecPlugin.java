package dev.mark.kuznec;

import dev.mark.kuznec.command.KuznecCommand;
import dev.mark.kuznec.config.ConfigManager;
import dev.mark.kuznec.gui.GuiManager;
import dev.mark.kuznec.gui.InventoryListener;
import dev.mark.kuznec.upgrade.UpgradeEffectListener;
import dev.mark.kuznec.upgrade.UpgradeManager;
import dev.mark.kuznec.vault.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class KuznecPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.reload();

        VaultHook vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().severe("Vault или поставщик экономики не найдены. Плагин отключен.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        UpgradeManager upgradeManager = new UpgradeManager(this, configManager);
        upgradeManager.load();

        GuiManager guiManager = new GuiManager(configManager, upgradeManager);

        PluginCommand pluginCommand = getCommand("kuznec");
        if (pluginCommand == null) {
            throw new IllegalStateException("Команда /kuznec не объявлена в plugin.yml");
        }
        pluginCommand.setExecutor(new KuznecCommand(guiManager));

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new InventoryListener(guiManager, configManager, upgradeManager, vaultHook), this);
        pluginManager.registerEvents(new UpgradeEffectListener(configManager, upgradeManager), this);
        getServer().getScheduler().runTaskTimer(
                this,
                new UpgradeEffectListener.PassiveEffectTask(configManager, upgradeManager),
                20L,
                Math.max(20L, configManager.getPassiveRefreshTicks())
        );
    }
}
