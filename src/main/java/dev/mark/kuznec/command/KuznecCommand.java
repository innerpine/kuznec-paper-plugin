package dev.mark.kuznec.command;

import dev.mark.kuznec.gui.GuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KuznecCommand implements CommandExecutor {

    private final GuiManager guiManager;

    public KuznecCommand(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }

        guiManager.openMainMenu((Player) sender);
        return true;
    }
}
