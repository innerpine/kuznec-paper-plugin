package dev.mark.kuznec.upgrade;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class UpgradeEffectTest {

    @Test
    void fromConfigNullSectionReturnsNoneEffect() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(null);
        assertEquals(UpgradeEffectType.NONE, effect.getType());
        assertEquals(0.0D, effect.getChance(), 1e-9);
        assertEquals(0.0D, effect.getAmount(), 1e-9);
        assertNull(effect.getPotionEffectType());
    }

    @Test
    void fromConfigParsesLifestealCorrectly() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml(
                "type: LIFESTEAL\n" +
                "chance: 0.25\n" +
                "amount: 2.0\n"
        ));
        assertEquals(UpgradeEffectType.LIFESTEAL, effect.getType());
        assertEquals(0.25D, effect.getChance(), 1e-9);
        assertEquals(2.0D, effect.getAmount(), 1e-9);
    }

    @Test
    void fromConfigClampsCanceAbove1() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml(
                "type: DAMAGE_BONUS\n" +
                "chance: 5.0\n" +
                "amount: 1.0\n"
        ));
        assertEquals(1.0D, effect.getChance(), 1e-9);
    }

    @Test
    void fromConfigClampsChanceBelow0() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml(
                "type: DAMAGE_BONUS\n" +
                "chance: -0.5\n" +
                "amount: 1.0\n"
        ));
        assertEquals(0.0D, effect.getChance(), 1e-9);
    }

    @Test
    void matchesCauseReturnsTrueWhenCausesListIsEmpty() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml(
                "type: DAMAGE_REDUCTION\n" +
                "amount: 0.1\n"
        ));
        // No causes configured → matches any damage source
        assertTrue(effect.matchesCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
        assertTrue(effect.matchesCause(EntityDamageEvent.DamageCause.FALL));
        assertTrue(effect.matchesCause(EntityDamageEvent.DamageCause.FIRE));
    }

    @Test
    void matchesCauseFiltersWhenSpecificCausesConfigured() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml(
                "type: DAMAGE_REDUCTION\n" +
                "amount: 0.1\n" +
                "causes:\n" +
                "  - ENTITY_ATTACK\n" +
                "  - PROJECTILE\n"
        ));
        assertTrue(effect.matchesCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
        assertTrue(effect.matchesCause(EntityDamageEvent.DamageCause.PROJECTILE));
        assertFalse(effect.matchesCause(EntityDamageEvent.DamageCause.FALL));
        assertFalse(effect.matchesCause(EntityDamageEvent.DamageCause.FIRE));
    }

    @Test
    void fromConfigUsesDefaultsWhenFieldsMissing() {
        UpgradeEffect effect = UpgradeEffect.fromConfig(yaml("type: FIRE\n"));
        // defaults: chance=1.0, amount=0.0, fire-ticks=60, duration-ticks=200, amplifier=0
        assertEquals(1.0D, effect.getChance(), 1e-9);
        assertEquals(0.0D, effect.getAmount(), 1e-9);
        assertEquals(60, effect.getFireTicks());
        assertEquals(200, effect.getDurationTicks());
        assertEquals(0, effect.getAmplifier());
    }

    private static org.bukkit.configuration.ConfigurationSection yaml(String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return configuration;
    }
}
