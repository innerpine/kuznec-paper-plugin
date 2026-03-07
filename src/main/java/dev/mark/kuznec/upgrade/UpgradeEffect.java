package dev.mark.kuznec.upgrade;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UpgradeEffect {

    private final UpgradeEffectType type;
    private final double chance;
    private final double amount;
    private final int durationTicks;
    private final int amplifier;
    private final int fireTicks;
    private final PotionEffectType potionEffectType;
    private final Set<EntityDamageEvent.DamageCause> causes;

    private UpgradeEffect(UpgradeEffectType type,
                          double chance,
                          double amount,
                          int durationTicks,
                          int amplifier,
                          int fireTicks,
                          PotionEffectType potionEffectType,
                          Set<EntityDamageEvent.DamageCause> causes) {
        this.type = type;
        this.chance = chance;
        this.amount = amount;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.fireTicks = fireTicks;
        this.potionEffectType = potionEffectType;
        this.causes = causes;
    }

    public static UpgradeEffect fromConfig(ConfigurationSection section) {
        if (section == null) {
            return none();
        }

        UpgradeEffectType type = UpgradeEffectType.from(section.getString("type"));
        double chance = clamp(section.getDouble("chance", 1.0D));
        double amount = section.getDouble("amount", 0.0D);
        int durationTicks = section.getInt("duration-ticks", 200);
        int amplifier = section.getInt("amplifier", 0);
        int fireTicks = section.getInt("fire-ticks", 60);
        PotionEffectType potionEffectType = parsePotion(section.getString("potion"));
        Set<EntityDamageEvent.DamageCause> causes = parseCauses(section.getStringList("causes"));
        return new UpgradeEffect(type, chance, amount, durationTicks, amplifier, fireTicks, potionEffectType, causes);
    }

    public static UpgradeEffect none() {
        return new UpgradeEffect(UpgradeEffectType.NONE, 0.0D, 0.0D, 0, 0, 0, null, Collections.<EntityDamageEvent.DamageCause>emptySet());
    }

    public UpgradeEffectType getType() {
        return type;
    }

    public double getChance() {
        return chance;
    }

    public double getAmount() {
        return amount;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public PotionEffectType getPotionEffectType() {
        return potionEffectType;
    }

    public boolean matchesCause(EntityDamageEvent.DamageCause cause) {
        return causes.isEmpty() || causes.contains(cause);
    }

    private static PotionEffectType parsePotion(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return PotionEffectType.getByName(value.trim().toUpperCase(Locale.ROOT));
    }

    private static Set<EntityDamageEvent.DamageCause> parseCauses(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        EnumSet<EntityDamageEvent.DamageCause> causes = EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            try {
                causes.add(EntityDamageEvent.DamageCause.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Invalid causes are skipped to keep the config tolerant.
            }
        }
        return causes;
    }

    private static double clamp(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }
}
