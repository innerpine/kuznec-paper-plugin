package dev.mark.kuznec.command;

import dev.mark.kuznec.KuznecPlugin;
import dev.mark.kuznec.config.ConfigManager;
import dev.mark.kuznec.gui.GuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KuznecCommand implements CommandExecutor {

    private final KuznecPlugin plugin;
    private final ConfigManager configManager;
    private final GuiManager guiManager;

    public KuznecCommand(KuznecPlugin plugin, ConfigManager configManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        guiManager.openMainMenu((Player) sender);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("kuznec.reload")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        plugin.reloadPluginState();
        sender.sendMessage(configManager.getMessage("reload-success"));
        return true;
    }
}
