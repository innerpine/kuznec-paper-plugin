package dev.mark.kuznec.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class YamlFile {

    private final JavaPlugin plugin;
    private final String fileName;
    private FileConfiguration configuration;

    public YamlFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void reload() {
        saveDefaultIfMissing();

        File file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(file);

        InputStream resource = plugin.getResource(fileName);
        if (resource == null) {
            return;
        }

        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(resource, StandardCharsets.UTF_8);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            configuration.setDefaults(defaults);
        } finally {
            closeQuietly(reader);
            closeQuietly(resource);
        }
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }

    private void saveDefaultIfMissing() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
            // Closing a default config stream must not interrupt plugin startup.
        }
    }
}
