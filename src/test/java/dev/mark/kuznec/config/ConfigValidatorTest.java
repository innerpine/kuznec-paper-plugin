package dev.mark.kuznec.config;

import dev.mark.kuznec.gui.MenuSlotType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConfigValidatorTest {

    @Test
    void validateAcceptsMinimalValidConfiguration() {
        ConfigValidationResult result = ConfigValidator.validate(
                yaml(
                        "menu:\n" +
                                "  main-title: \"&8Кузнец\"\n" +
                                "  upgrade-title: \"&8Улучшения: %type%\"\n" +
                                "  main-slots:\n" +
                                "    main-hand: 20\n" +
                                "    helmet: 21\n" +
                                "    chestplate: 22\n" +
                                "    leggings: 23\n" +
                                "    boots: 24\n" +
                                "  upgrade-slots: [20, 21, 22, 23, 24, 30, 31, 32]\n" +
                                "  placeholder:\n" +
                                "    material: BARRIER\n" +
                                "  back-button:\n" +
                                "    slot: 49\n" +
                                "    material: ARROW\n" +
                                "item-lore:\n" +
                                "  upgrade-format: \"&a+ %upgrade%\"\n"
                ),
                yaml(
                        "messages:\n" +
                                "  no-economy: a\n" +
                                "  unsupported-item: a\n" +
                                "  not-enough-money: a\n" +
                                "  already-purchased: a\n" +
                                "  no-item: a\n" +
                                "  generic-error: a\n" +
                                "  purchase-success: a\n" +
                                "  reload-success: a\n" +
                                "  reload-failed: a\n" +
                                "  no-permission: a\n" +
                                "  player-only: a\n" +
                                "sounds:\n" +
                                "  success: ENTITY_PLAYER_LEVELUP\n" +
                                "  error: ENTITY_VILLAGER_NO\n"
                ),
                yaml(
                        "effects:\n" +
                                "  passive-refresh-ticks: 100\n" +
                                "upgrades:\n" +
                                "  sword:\n" +
                                "    vampirism:\n" +
                                "      icon: RED_DYE\n" +
                                "      name: '&cВампиризм'\n" +
                                "      price: 40000\n" +
                                "      effect:\n" +
                                "        type: LIFESTEAL\n" +
                                "        chance: 0.25\n" +
                                "        amount: 2.0\n"
                ),
                defaultMainSlots(),
                defaultUpgradeSlots(),
                new EnumMap<FillerMenuType, List<FillerItemConfig>>(FillerMenuType.class)
        );

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void validateRejectsOverlappingBackButtonAndUpgradeSlots() {
        ConfigValidationResult result = ConfigValidator.validate(
                yaml(
                        "menu:\n" +
                                "  main-title: test\n" +
                                "  upgrade-title: test\n" +
                                "  main-slots:\n" +
                                "    main-hand: 20\n" +
                                "    helmet: 21\n" +
                                "    chestplate: 22\n" +
                                "    leggings: 23\n" +
                                "    boots: 24\n" +
                                "  upgrade-slots: [20, 21, 22, 23, 24, 30, 31, 32]\n" +
                                "  placeholder:\n" +
                                "    material: BARRIER\n" +
                                "  back-button:\n" +
                                "    slot: 20\n" +
                                "    material: ARROW\n" +
                                "item-lore:\n" +
                                "  upgrade-format: '%upgrade%'\n"
                ),
                validMessages(),
                validEffects(),
                defaultMainSlots(),
                defaultUpgradeSlots(),
                Collections.<FillerMenuType, List<FillerItemConfig>>emptyMap()
        );

        assertFalse(result.isValid());
    }

    @Test
    void validateRejectsInvalidUpgradeMaterial() {
        ConfigValidationResult result = ConfigValidator.validate(
                validGui(),
                validMessages(),
                yaml(
                        "effects:\n" +
                                "  passive-refresh-ticks: 100\n" +
                                "upgrades:\n" +
                                "  sword:\n" +
                                "    vampirism:\n" +
                                "      icon: NOT_A_REAL_MATERIAL\n" +
                                "      name: test\n" +
                                "      price: 1\n" +
                                "      effect:\n" +
                                "        type: LIFESTEAL\n" +
                                "        amount: 1\n"
                ),
                defaultMainSlots(),
                defaultUpgradeSlots(),
                Collections.<FillerMenuType, List<FillerItemConfig>>emptyMap()
        );

        assertFalse(result.isValid());
    }

    private static YamlConfiguration validGui() {
        return yaml(
                "menu:\n" +
                        "  main-title: test\n" +
                        "  upgrade-title: test\n" +
                        "  main-slots:\n" +
                        "    main-hand: 20\n" +
                        "    helmet: 21\n" +
                        "    chestplate: 22\n" +
                        "    leggings: 23\n" +
                        "    boots: 24\n" +
                        "  upgrade-slots: [20, 21, 22, 23, 24, 30, 31, 32]\n" +
                        "  placeholder:\n" +
                        "    material: BARRIER\n" +
                        "  back-button:\n" +
                        "    slot: 49\n" +
                        "    material: ARROW\n" +
                        "item-lore:\n" +
                        "  upgrade-format: '%upgrade%'\n"
        );
    }

    private static YamlConfiguration validMessages() {
        return yaml(
                "messages:\n" +
                        "  no-economy: a\n" +
                        "  unsupported-item: a\n" +
                        "  not-enough-money: a\n" +
                        "  already-purchased: a\n" +
                        "  no-item: a\n" +
                        "  generic-error: a\n" +
                        "  purchase-success: a\n" +
                        "  reload-success: a\n" +
                        "  reload-failed: a\n" +
                        "  no-permission: a\n" +
                        "  player-only: a\n" +
                        "sounds:\n" +
                        "  success: ENTITY_PLAYER_LEVELUP\n" +
                        "  error: ENTITY_VILLAGER_NO\n"
        );
    }

    private static YamlConfiguration validEffects() {
        return yaml(
                "effects:\n" +
                        "  passive-refresh-ticks: 100\n" +
                        "upgrades:\n" +
                        "  sword:\n" +
                        "    vampirism:\n" +
                        "      icon: RED_DYE\n" +
                        "      name: test\n" +
                        "      price: 1\n" +
                        "      effect:\n" +
                        "        type: LIFESTEAL\n" +
                        "        amount: 1\n"
        );
    }

    private static Map<MenuSlotType, Integer> defaultMainSlots() {
        Map<MenuSlotType, Integer> slots = new EnumMap<MenuSlotType, Integer>(MenuSlotType.class);
        slots.put(MenuSlotType.MAIN_HAND, Integer.valueOf(20));
        slots.put(MenuSlotType.HELMET, Integer.valueOf(21));
        slots.put(MenuSlotType.CHESTPLATE, Integer.valueOf(22));
        slots.put(MenuSlotType.LEGGINGS, Integer.valueOf(23));
        slots.put(MenuSlotType.BOOTS, Integer.valueOf(24));
        return slots;
    }

    private static List<Integer> defaultUpgradeSlots() {
        return Arrays.asList(20, 21, 22, 23, 24, 30, 31, 32);
    }

    private static YamlConfiguration yaml(String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return configuration;
    }
}
