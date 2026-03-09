package dev.mark.kuznec.config;

import dev.mark.kuznec.gui.MenuSlotType;
import dev.mark.kuznec.util.ItemBuilder;
import dev.mark.kuznec.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private final YamlFile guiFile;
    private final YamlFile messagesFile;
    private final YamlFile effectsFile;

    private FileConfiguration guiConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration effectsConfig;
    private Map<MenuSlotType, Integer> mainMenuSlots;
    private List<Integer> upgradeSlots;
    private Map<FillerMenuType, List<FillerItemConfig>> fillerItemsByMenu;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guiFile = new YamlFile(plugin, "gui.yml");
        this.messagesFile = new YamlFile(plugin, "messages.yml");
        this.effectsFile = new YamlFile(plugin, "effects.yml");
        this.mainMenuSlots = new EnumMap<MenuSlotType, Integer>(MenuSlotType.class);
        this.upgradeSlots = Collections.emptyList();
        this.fillerItemsByMenu = new EnumMap<FillerMenuType, List<FillerItemConfig>>(FillerMenuType.class);
    }

    public void reload() {
        guiFile.reload();
        messagesFile.reload();
        effectsFile.reload();

        FileConfiguration loadedGuiConfig = guiFile.getConfiguration();
        FileConfiguration loadedMessagesConfig = messagesFile.getConfiguration();
        FileConfiguration loadedEffectsConfig = effectsFile.getConfiguration();
        Map<MenuSlotType, Integer> loadedMainMenuSlots = loadMainMenuSlots(loadedGuiConfig);
        List<Integer> loadedUpgradeSlots = loadUpgradeSlots(loadedGuiConfig);
        Map<FillerMenuType, List<FillerItemConfig>> loadedFillerItemsByMenu = loadAllFillerItems(loadedGuiConfig);

        ConfigValidationResult validationResult = ConfigValidator.validate(
                loadedGuiConfig,
                loadedMessagesConfig,
                loadedEffectsConfig,
                loadedMainMenuSlots,
                loadedUpgradeSlots,
                loadedFillerItemsByMenu
        );
        logValidationWarnings(validationResult);
        if (!validationResult.isValid()) {
            throw new ConfigValidationException(validationResult);
        }

        this.guiConfig = loadedGuiConfig;
        this.messagesConfig = loadedMessagesConfig;
        this.effectsConfig = loadedEffectsConfig;
        this.mainMenuSlots = loadedMainMenuSlots;
        this.upgradeSlots = loadedUpgradeSlots;
        this.fillerItemsByMenu = loadedFillerItemsByMenu;
    }

    public ConfigurationSection getUpgradesSection() {
        return effectsConfig.getConfigurationSection("upgrades");
    }

    public String getMainMenuTitle() {
        return TextUtil.colorize(guiConfig.getString("menu.main-title", "&8Кузнец"));
    }

    public String getUpgradeMenuTitle(String typeName) {
        return TextUtil.colorize(TextUtil.replace(
                guiConfig.getString("menu.upgrade-title", "&8Улучшения: %type%"),
                "%type%",
                typeName
        ));
    }

    public Map<MenuSlotType, Integer> getMainMenuSlots() {
        return Collections.unmodifiableMap(mainMenuSlots);
    }

    public MenuSlotType getMenuSlotTypeForSlot(int slot) {
        for (Map.Entry<MenuSlotType, Integer> entry : mainMenuSlots.entrySet()) {
            if (entry.getValue().intValue() == slot) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<Integer> getUpgradeSlots() {
        return Collections.unmodifiableList(upgradeSlots);
    }

    public int getBackButtonSlot() {
        return guiConfig.getInt("menu.back-button.slot", 49);
    }

    public boolean isFillerEnabled(FillerMenuType menuType) {
        return !getFillerItems(menuType).isEmpty();
    }

    public ItemStack createPlaceholderItem() {
        return new ItemBuilder(parseMaterial(guiConfig.getString("menu.placeholder.material"), Material.BARRIER))
                .name(guiConfig.getString("menu.placeholder.name", "&cНет предмета"))
                .lore(guiConfig.getStringList("menu.placeholder.lore"))
                .flags(ItemFlag.values())
                .build();
    }

    public List<FillerItemConfig> getFillerItems(FillerMenuType menuType) {
        List<FillerItemConfig> fillerItems = fillerItemsByMenu.get(menuType);
        if (fillerItems == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(fillerItems);
    }

    public ItemStack createBackButton() {
        return new ItemBuilder(parseMaterial(guiConfig.getString("menu.back-button.material"), Material.ARROW))
                .name(guiConfig.getString("menu.back-button.name", "&eНазад"))
                .lore(guiConfig.getStringList("menu.back-button.lore"))
                .flags(ItemFlag.values())
                .build();
    }

    public List<String> getPreviewLoreSupported() {
        return TextUtil.colorize(guiConfig.getStringList("menu.preview-lore.supported"));
    }

    public List<String> getPreviewLoreUnsupported() {
        return TextUtil.colorize(guiConfig.getStringList("menu.preview-lore.unsupported"));
    }

    public List<String> getUpgradePurchasedLore() {
        return TextUtil.colorize(guiConfig.getStringList("menu.upgrade-status.purchased"));
    }

    public List<String> getUpgradeAvailableLore(double price) {
        List<String> lines = TextUtil.colorize(guiConfig.getStringList("menu.upgrade-status.available"));
        List<String> result = new ArrayList<String>(lines.size());
        for (String line : lines) {
            result.add(TextUtil.replace(line, "%price%", TextUtil.formatPrice(price)));
        }
        return result;
    }

    public boolean shouldInsertEmptyLineBeforeUpgradeLore() {
        return guiConfig.getBoolean("item-lore.empty-line-before-upgrades", true);
    }

    public String formatPurchasedUpgradeLoreLine(String upgradeName) {
        String format = TextUtil.colorize(guiConfig.getString("item-lore.upgrade-format", "&a+ %upgrade%"));
        return TextUtil.replace(format, "%upgrade%", upgradeName);
    }

    public String getMessage(String path) {
        return TextUtil.colorize(messagesConfig.getString("messages." + path, "&cСообщение не найдено: " + path));
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = TextUtil.replace(message, replacements[index], replacements[index + 1]);
        }
        return message;
    }

    public Sound getSuccessSound() {
        return parseSound(messagesConfig.getString("sounds.success"), Sound.ENTITY_PLAYER_LEVELUP);
    }

    public float getSuccessVolume() {
        return (float) messagesConfig.getDouble("sounds.success-volume", 1.0D);
    }

    public float getSuccessPitch() {
        return (float) messagesConfig.getDouble("sounds.success-pitch", 1.0D);
    }

    public Sound getErrorSound() {
        return parseSound(messagesConfig.getString("sounds.error"), Sound.ENTITY_VILLAGER_NO);
    }

    public float getErrorVolume() {
        return (float) messagesConfig.getDouble("sounds.error-volume", 1.0D);
    }

    public float getErrorPitch() {
        return (float) messagesConfig.getDouble("sounds.error-pitch", 1.0D);
    }

    public long getPassiveRefreshTicks() {
        return effectsConfig.getLong("effects.passive-refresh-ticks", 100L);
    }

    private Map<MenuSlotType, Integer> loadMainMenuSlots(FileConfiguration loadedGuiConfig) {
        Map<MenuSlotType, Integer> slots = new EnumMap<MenuSlotType, Integer>(MenuSlotType.class);
        slots.put(MenuSlotType.MAIN_HAND, loadedGuiConfig.getInt("menu.main-slots.main-hand", 20));
        slots.put(MenuSlotType.HELMET, loadedGuiConfig.getInt("menu.main-slots.helmet", 21));
        slots.put(MenuSlotType.CHESTPLATE, loadedGuiConfig.getInt("menu.main-slots.chestplate", 22));
        slots.put(MenuSlotType.LEGGINGS, loadedGuiConfig.getInt("menu.main-slots.leggings", 23));
        slots.put(MenuSlotType.BOOTS, loadedGuiConfig.getInt("menu.main-slots.boots", 24));
        return slots;
    }

    private List<Integer> loadUpgradeSlots(FileConfiguration loadedGuiConfig) {
        List<Integer> slots = new ArrayList<Integer>(loadedGuiConfig.getIntegerList("menu.upgrade-slots"));
        if (slots.isEmpty()) {
            return Arrays.asList(20, 21, 22, 23, 24, 30, 31, 32);
        }
        return slots;
    }

    private Map<FillerMenuType, List<FillerItemConfig>> loadAllFillerItems(FileConfiguration loadedGuiConfig) {
        Map<FillerMenuType, List<FillerItemConfig>> itemsByMenu = new EnumMap<FillerMenuType, List<FillerItemConfig>>(FillerMenuType.class);
        List<FillerItemConfig> sharedItems = loadFillerItemsFromPath(loadedGuiConfig, "menu.filler.items");
        List<FillerItemConfig> legacyItems = sharedItems.isEmpty() ? loadLegacyFillerItems(loadedGuiConfig) : Collections.<FillerItemConfig>emptyList();

        for (FillerMenuType menuType : FillerMenuType.values()) {
            List<FillerItemConfig> specificItems = loadFillerItemsFromPath(loadedGuiConfig, "menu.filler." + menuType.getConfigKey() + ".items");
            if (!specificItems.isEmpty()) {
                itemsByMenu.put(menuType, specificItems);
                continue;
            }

            if (!sharedItems.isEmpty()) {
                itemsByMenu.put(menuType, sharedItems);
                continue;
            }

            itemsByMenu.put(menuType, legacyItems);
        }

        return itemsByMenu;
    }

    private List<FillerItemConfig> loadFillerItemsFromPath(FileConfiguration loadedGuiConfig, String path) {
        ConfigurationSection itemsSection = loadedGuiConfig.getConfigurationSection(path);
        if (itemsSection == null) {
            return Collections.emptyList();
        }

        List<FillerItemConfig> items = new ArrayList<FillerItemConfig>();
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            List<Integer> slots = parseSlots(itemSection.get("slots"));
            if (slots.isEmpty()) {
                continue;
            }

            ItemStack itemStack = new ItemBuilder(parseMaterial(itemSection.getString("material"), Material.BLACK_STAINED_GLASS_PANE))
                    .name(parseDisplayName(itemSection.getString("display_name")))
                    .flags(ItemFlag.values())
                    .build();
            items.add(new FillerItemConfig(itemStack, slots));
        }
        return items;
    }

    private List<FillerItemConfig> loadLegacyFillerItems(FileConfiguration loadedGuiConfig) {
        if (!loadedGuiConfig.getBoolean("menu.filler.enabled", true)) {
            return Collections.emptyList();
        }

        List<Integer> allSlots = new ArrayList<Integer>();
        for (int slot = 0; slot < 54; slot++) {
            allSlots.add(slot);
        }

        ItemStack itemStack = new ItemBuilder(parseMaterial(loadedGuiConfig.getString("menu.filler.material"), Material.BLACK_STAINED_GLASS_PANE))
                .name(loadedGuiConfig.getString("menu.filler.name", " "))
                .lore(loadedGuiConfig.getStringList("menu.filler.lore"))
                .flags(ItemFlag.values())
                .build();

        return Collections.singletonList(new FillerItemConfig(itemStack, allSlots));
    }

    private void logValidationWarnings(ConfigValidationResult validationResult) {
        for (String warning : validationResult.getWarnings()) {
            plugin.getLogger().warning(warning);
        }
    }

    private List<Integer> parseSlots(Object rawSlots) {
        if (rawSlots == null) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<Integer>();
        if (rawSlots instanceof Collection) {
            for (Object value : (Collection<?>) rawSlots) {
                addSlot(result, value);
            }
            return result;
        }

        String[] parts = String.valueOf(rawSlots).split(",");
        for (String part : parts) {
            addSlot(result, part);
        }
        return result;
    }

    private void addSlot(List<Integer> result, Object rawValue) {
        if (rawValue == null) {
            return;
        }

        String value = String.valueOf(rawValue).trim();
        if (value.isEmpty()) {
            return;
        }

        try {
            result.add(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Некорректный слот filler: " + value);
        }
    }

    private String parseDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        String trimmed = displayName.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none")) {
            return null;
        }
        return displayName;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }

        Material material = Material.matchMaterial(name.trim().toUpperCase());
        return material == null ? fallback : material;
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }

        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Неизвестный звук в конфиге: " + name + ". Используется " + fallback.name());
            return fallback;
        }
    }
}
