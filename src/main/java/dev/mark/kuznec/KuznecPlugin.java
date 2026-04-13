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
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public final class KuznecPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private VaultHook vaultHook;
    private UpgradeManager upgradeManager;
    private GuiManager guiManager;
    private BukkitTask passiveEffectTask;

    @Override
    public void onEnable() {
        try {
            this.configManager = new ConfigManager(this);
            configManager.reload();

            this.vaultHook = new VaultHook(this);
            if (!vaultHook.setup()) {
                getLogger().severe("Vault или поставщик экономики не найдены. Плагин отключен.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            this.upgradeManager = new UpgradeManager(this, configManager);
            upgradeManager.load();

            this.guiManager = new GuiManager(configManager, upgradeManager);

            PluginCommand pluginCommand = getCommand("kuznec");
            if (pluginCommand == null) {
                throw new IllegalStateException("Команда /kuznec не объявлена в plugin.yml");
            }
            pluginCommand.setExecutor(new KuznecCommand(this, configManager, guiManager));

            PluginManager pluginManager = getServer().getPluginManager();
            pluginManager.registerEvents(new InventoryListener(guiManager, configManager, upgradeManager, vaultHook), this);
            pluginManager.registerEvents(new UpgradeEffectListener(configManager, upgradeManager), this);
            startPassiveEffectTask();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Не удалось запустить Kuznec. Проверьте конфиги и зависимости.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public void reloadPluginState() {
        configManager.reload();
        upgradeManager.load();
        startPassiveEffectTask();
    }

    @Override
    public void onDisable() {
        if (passiveEffectTask != null) {
            passiveEffectTask.cancel();
            passiveEffectTask = null;
        }
    }

    private void startPassiveEffectTask() {
        if (passiveEffectTask != null) {
            passiveEffectTask.cancel();
        }

        passiveEffectTask = getServer().getScheduler().runTaskTimer(
                this,
                new UpgradeEffectListener.PassiveEffectTask(configManager, upgradeManager, getLogger()),
                20L,
                Math.max(20L, configManager.getPassiveRefreshTicks())
        );
    }
}
