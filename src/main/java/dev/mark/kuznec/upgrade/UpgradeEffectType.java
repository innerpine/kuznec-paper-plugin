package dev.mark.kuznec.upgrade;

import java.util.Locale;

public enum UpgradeEffectType {
    NONE,
    LIFESTEAL,
    DAMAGE_BONUS,
    FIRE,
    TARGET_POTION,
    DAMAGE_REDUCTION,
    THORNS,
    PASSIVE_POTION,
    CANCEL_CHANCE;

    public static UpgradeEffectType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }

        try {
            return UpgradeEffectType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
