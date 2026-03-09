package dev.mark.kuznec.config;

import dev.mark.kuznec.gui.MenuSlotType;
import dev.mark.kuznec.upgrade.EquipmentType;
import dev.mark.kuznec.upgrade.UpgradeEffectType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ConfigValidator {

    private static final int MENU_SIZE = 54;

    private ConfigValidator() {
    }

    public static ConfigValidationResult validate(FileConfiguration guiConfig,
                                                  FileConfiguration messagesConfig,
                                                  FileConfiguration effectsConfig,
                                                  Map<MenuSlotType, Integer> mainMenuSlots,
                                                  List<Integer> upgradeSlots,
                                                  Map<FillerMenuType, List<FillerItemConfig>> fillerItemsByMenu) {
        ConfigValidationResult result = new ConfigValidationResult();

        validateGui(guiConfig, mainMenuSlots, upgradeSlots, fillerItemsByMenu, result);
        validateMessages(messagesConfig, result);
        validateEffects(effectsConfig, upgradeSlots, result);

        return result;
    }

    private static void validateGui(FileConfiguration guiConfig,
                                    Map<MenuSlotType, Integer> mainMenuSlots,
                                    List<Integer> upgradeSlots,
                                    Map<FillerMenuType, List<FillerItemConfig>> fillerItemsByMenu,
                                    ConfigValidationResult result) {
        requireNonBlank(guiConfig.getString("menu.main-title"), "gui.yml: menu.main-title", result);
        requireNonBlank(guiConfig.getString("menu.upgrade-title"), "gui.yml: menu.upgrade-title", result);
        requireNonBlank(guiConfig.getString("item-lore.upgrade-format"), "gui.yml: item-lore.upgrade-format", result);

        String upgradeLoreFormat = guiConfig.getString("item-lore.upgrade-format", "");
        if (!upgradeLoreFormat.contains("%upgrade%")) {
            result.addWarning("gui.yml: item-lore.upgrade-format does not contain %upgrade%.");
        }

        validateMainMenuSlots(mainMenuSlots, result);
        validateUpgradeSlots(upgradeSlots, guiConfig.getInt("menu.back-button.slot", 49), result);
        validateMaterial(guiConfig.getString("menu.placeholder.material"), "gui.yml: menu.placeholder.material", result);
        validateMaterial(guiConfig.getString("menu.back-button.material"), "gui.yml: menu.back-button.material", result);

        for (FillerMenuType menuType : FillerMenuType.values()) {
            List<FillerItemConfig> fillerItems = fillerItemsByMenu.get(menuType);
            if (fillerItems == null) {
                continue;
            }
            validateFillerItems(menuType, fillerItems, result);
        }
    }

    private static void validateMessages(FileConfiguration messagesConfig, ConfigValidationResult result) {
        validateSound(messagesConfig.getString("sounds.success"), "messages.yml: sounds.success", result);
        validateSound(messagesConfig.getString("sounds.error"), "messages.yml: sounds.error", result);

        requireNonBlank(messagesConfig.getString("messages.no-economy"), "messages.yml: messages.no-economy", result);
        requireNonBlank(messagesConfig.getString("messages.unsupported-item"), "messages.yml: messages.unsupported-item", result);
        requireNonBlank(messagesConfig.getString("messages.not-enough-money"), "messages.yml: messages.not-enough-money", result);
        requireNonBlank(messagesConfig.getString("messages.already-purchased"), "messages.yml: messages.already-purchased", result);
        requireNonBlank(messagesConfig.getString("messages.no-item"), "messages.yml: messages.no-item", result);
        requireNonBlank(messagesConfig.getString("messages.generic-error"), "messages.yml: messages.generic-error", result);
        requireNonBlank(messagesConfig.getString("messages.purchase-success"), "messages.yml: messages.purchase-success", result);
        requireNonBlank(messagesConfig.getString("messages.reload-success"), "messages.yml: messages.reload-success", result);
        requireNonBlank(messagesConfig.getString("messages.reload-failed"), "messages.yml: messages.reload-failed", result);
        requireNonBlank(messagesConfig.getString("messages.no-permission"), "messages.yml: messages.no-permission", result);
        requireNonBlank(messagesConfig.getString("messages.player-only"), "messages.yml: messages.player-only", result);
    }

    private static void validateEffects(FileConfiguration effectsConfig,
                                        List<Integer> upgradeSlots,
                                        ConfigValidationResult result) {
        long passiveRefreshTicks = effectsConfig.getLong("effects.passive-refresh-ticks", 100L);
        if (passiveRefreshTicks < 20L) {
            result.addWarning("effects.yml: effects.passive-refresh-ticks is below 20. Runtime will clamp it to 20.");
        }

        ConfigurationSection upgradesSection = effectsConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null || upgradesSection.getKeys(false).isEmpty()) {
            result.addError("effects.yml: upgrades section is missing or empty.");
            return;
        }

        for (String typeKey : upgradesSection.getKeys(false)) {
            EquipmentType type = EquipmentType.fromConfigKey(typeKey);
            if (type == null) {
                result.addError("effects.yml: unknown upgrade type '" + typeKey + "'.");
                continue;
            }

            ConfigurationSection typeSection = upgradesSection.getConfigurationSection(typeKey);
            if (typeSection == null || typeSection.getKeys(false).isEmpty()) {
                result.addError("effects.yml: upgrades." + typeKey + " is empty.");
                continue;
            }

            if (typeSection.getKeys(false).size() > upgradeSlots.size()) {
                result.addWarning("effects.yml: upgrades." + typeKey + " contains more upgrades than available GUI slots.");
            }

            for (String upgradeId : typeSection.getKeys(false)) {
                validateUpgrade(typeSection.getConfigurationSection(upgradeId), typeKey, upgradeId, result);
            }
        }
    }

    private static void validateUpgrade(ConfigurationSection section,
                                        String typeKey,
                                        String upgradeId,
                                        ConfigValidationResult result) {
        String prefix = "effects.yml: upgrades." + typeKey + "." + upgradeId;
        if (section == null) {
            result.addError(prefix + " section is missing.");
            return;
        }

        validateMaterial(section.getString("icon"), prefix + ".icon", result);
        requireNonBlank(section.getString("name"), prefix + ".name", result);

        double price = section.getDouble("price", 0.0D);
        if (price < 0.0D) {
            result.addError(prefix + ".price must be >= 0.");
        }

        ConfigurationSection effectSection = section.getConfigurationSection("effect");
        if (effectSection == null) {
            result.addError(prefix + ".effect section is missing.");
            return;
        }

        String rawType = effectSection.getString("type");
        requireNonBlank(rawType, prefix + ".effect.type", result);
        UpgradeEffectType effectType = UpgradeEffectType.from(rawType);
        if (effectType == UpgradeEffectType.NONE) {
            result.addError(prefix + ".effect.type is invalid: " + rawType);
            return;
        }

        validateChance(effectSection, prefix, result);
        validateEffectSpecifics(effectSection, prefix, effectType, result);
        validateCauses(effectSection.getStringList("causes"), prefix, result);
    }

    private static void validateEffectSpecifics(ConfigurationSection section,
                                                String prefix,
                                                UpgradeEffectType effectType,
                                                ConfigValidationResult result) {
        switch (effectType) {
            case LIFESTEAL:
            case DAMAGE_BONUS:
            case DAMAGE_REDUCTION:
            case THORNS:
                if (section.getDouble("amount", 0.0D) <= 0.0D) {
                    result.addError(prefix + ".effect.amount must be > 0 for " + effectType.name().toLowerCase(Locale.ROOT) + ".");
                }
                break;
            case FIRE:
                if (section.getInt("fire-ticks", 0) <= 0) {
                    result.addError(prefix + ".effect.fire-ticks must be > 0.");
                }
                break;
            case TARGET_POTION:
            case PASSIVE_POTION:
                validatePotion(section.getString("potion"), prefix + ".effect.potion", result);
                if (section.getInt("duration-ticks", 0) <= 0) {
                    result.addError(prefix + ".effect.duration-ticks must be > 0.");
                }
                if (section.getInt("amplifier", 0) < 0) {
                    result.addError(prefix + ".effect.amplifier must be >= 0.");
                }
                break;
            case CANCEL_CHANCE:
                break;
            default:
                break;
        }
    }

    private static void validateMainMenuSlots(Map<MenuSlotType, Integer> mainMenuSlots, ConfigValidationResult result) {
        Set<Integer> usedSlots = new HashSet<Integer>();
        for (Map.Entry<MenuSlotType, Integer> entry : mainMenuSlots.entrySet()) {
            int slot = entry.getValue().intValue();
            validateSlot(slot, "gui.yml: menu.main-slots." + entry.getKey().getConfigKey(), result);
            if (!usedSlots.add(Integer.valueOf(slot))) {
                result.addError("gui.yml: duplicate main menu slot " + slot + ".");
            }
        }
    }

    private static void validateUpgradeSlots(List<Integer> upgradeSlots, int backButtonSlot, ConfigValidationResult result) {
        if (upgradeSlots.isEmpty()) {
            result.addError("gui.yml: menu.upgrade-slots must not be empty.");
            return;
        }

        Set<Integer> usedSlots = new HashSet<Integer>();
        for (Integer slot : upgradeSlots) {
            if (slot == null) {
                result.addError("gui.yml: menu.upgrade-slots contains a null slot.");
                continue;
            }
            validateSlot(slot.intValue(), "gui.yml: menu.upgrade-slots", result);
            if (!usedSlots.add(slot)) {
                result.addError("gui.yml: duplicate upgrade slot " + slot + ".");
            }
        }

        validateSlot(backButtonSlot, "gui.yml: menu.back-button.slot", result);
        if (usedSlots.contains(Integer.valueOf(backButtonSlot))) {
            result.addError("gui.yml: menu.back-button.slot overlaps with menu.upgrade-slots.");
        }
    }

    private static void validateFillerItems(FillerMenuType menuType,
                                            List<FillerItemConfig> fillerItems,
                                            ConfigValidationResult result) {
        for (int index = 0; index < fillerItems.size(); index++) {
            FillerItemConfig fillerItem = fillerItems.get(index);
            String prefix = "gui.yml: menu.filler." + menuType.getConfigKey() + ".items[" + index + "]";
            if (fillerItem.getItemStack() == null || fillerItem.getItemStack().getType() == Material.AIR) {
                result.addError(prefix + " has an invalid material.");
            }
            for (Integer slot : fillerItem.getSlots()) {
                if (slot == null) {
                    result.addError(prefix + " contains a null slot.");
                    continue;
                }
                validateSlot(slot.intValue(), prefix + ".slots", result);
            }
        }
    }

    private static void validateSlot(int slot, String path, ConfigValidationResult result) {
        if (slot < 0 || slot >= MENU_SIZE) {
            result.addError(path + " must be between 0 and " + (MENU_SIZE - 1) + ".");
        }
    }

    private static void validateMaterial(String materialName, String path, ConfigValidationResult result) {
        if (materialName == null || materialName.trim().isEmpty()) {
            result.addError(path + " is missing.");
            return;
        }

        if (Material.matchMaterial(materialName.trim().toUpperCase(Locale.ROOT)) == null) {
            result.addError(path + " has invalid material '" + materialName + "'.");
        }
    }

    private static void validateSound(String soundName, String path, ConfigValidationResult result) {
        if (soundName == null || soundName.trim().isEmpty()) {
            result.addError(path + " is missing.");
            return;
        }

        try {
            Sound.valueOf(soundName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            result.addError(path + " has invalid sound '" + soundName + "'.");
        }
    }

    private static void validatePotion(String potionName, String path, ConfigValidationResult result) {
        if (potionName == null || potionName.trim().isEmpty()) {
            result.addError(path + " is missing.");
            return;
        }

        if (PotionEffectType.getByName(potionName.trim().toUpperCase(Locale.ROOT)) == null) {
            result.addError(path + " has invalid potion '" + potionName + "'.");
        }
    }

    private static void validateChance(ConfigurationSection effectSection, String prefix, ConfigValidationResult result) {
        if (!effectSection.contains("chance")) {
            return;
        }

        double chance = effectSection.getDouble("chance");
        if (chance < 0.0D || chance > 1.0D) {
            result.addError(prefix + ".effect.chance must be between 0 and 1.");
        }
    }

    private static void validateCauses(List<String> causes, String prefix, ConfigValidationResult result) {
        for (String causeName : causes) {
            if (causeName == null || causeName.trim().isEmpty()) {
                continue;
            }

            try {
                EntityDamageEvent.DamageCause.valueOf(causeName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                result.addError(prefix + ".effect.causes contains invalid cause '" + causeName + "'.");
            }
        }
    }

    private static void requireNonBlank(String value, String path, ConfigValidationResult result) {
        if (value == null || value.trim().isEmpty()) {
            result.addError(path + " must not be empty.");
        }
    }
}
