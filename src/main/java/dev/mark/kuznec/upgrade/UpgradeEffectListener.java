package dev.mark.kuznec.upgrade;

import dev.mark.kuznec.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpgradeEffectListener implements Listener {

    private static final EquipmentType[] ARMOR_TYPES = new EquipmentType[]{
            EquipmentType.HELMET,
            EquipmentType.CHESTPLATE,
            EquipmentType.LEGGINGS,
            EquipmentType.BOOTS
    };

    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;

    public UpgradeEffectListener(ConfigManager configManager, UpgradeManager upgradeManager) {
        this.configManager = configManager;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) {
            return;
        }

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (EquipmentType.fromItem(item) != EquipmentType.SWORD) {
            return;
        }

        List<UpgradeDefinition> upgrades = upgradeManager.getPurchasedUpgrades(item, EquipmentType.SWORD);
        for (UpgradeDefinition definition : upgrades) {
            UpgradeEffect effect = definition.getEffect();
            if (!roll(effect.getChance())) {
                continue;
            }

            switch (effect.getType()) {
                case LIFESTEAL:
                    double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                            ? attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                            : 20.0D;
                    attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + effect.getAmount()));
                    break;
                case DAMAGE_BONUS:
                    event.setDamage(event.getDamage() + effect.getAmount());
                    break;
                case FIRE:
                    event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), effect.getFireTicks()));
                    break;
                case TARGET_POTION:
                    if (event.getEntity() instanceof LivingEntity && effect.getPotionEffectType() != null) {
                        ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(
                                effect.getPotionEffectType(),
                                effect.getDurationTicks(),
                                effect.getAmplifier(),
                                true,
                                true
                        ));
                    }
                    break;
                default:
                    break;
            }

            if (configManager.isDebugMode()) {
                Bukkit.getLogger().info("[Debug] Эффект " + effect.getType()
                        + " (" + definition.getUniqueKey() + ")"
                        + " сработал у " + attacker.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        for (EquipmentType type : ARMOR_TYPES) {
            applyDefensiveEffects(player, type, event);
        }
    }

    private void applyDefensiveEffects(Player player, EquipmentType type, EntityDamageEvent event) {
        ItemStack item = type.getPlayerItem(player);
        if (!type.matches(item)) {
            return;
        }

        List<UpgradeDefinition> upgrades = upgradeManager.getPurchasedUpgrades(item, type);
        for (UpgradeDefinition definition : upgrades) {
            UpgradeEffect effect = definition.getEffect();
            if (!effect.matchesCause(event.getCause()) || !roll(effect.getChance())) {
                continue;
            }

            switch (effect.getType()) {
                case DAMAGE_REDUCTION:
                    event.setDamage(Math.max(0.0D, event.getDamage() * (1.0D - effect.getAmount())));
                    break;
                case CANCEL_CHANCE:
                    event.setCancelled(true);
                    break;
                case THORNS:
                    if (event instanceof EntityDamageByEntityEvent) {
                        Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                        if (damager instanceof Damageable) {
                            ((Damageable) damager).damage(effect.getAmount(), player);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        return null;
    }

    private boolean roll(double chance) {
        return chance >= 1.0D || ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public static final class PassiveEffectTask implements Runnable {

        private final ConfigManager configManager;
        private final UpgradeManager upgradeManager;
        private final Logger logger;

        public PassiveEffectTask(ConfigManager configManager, UpgradeManager upgradeManager, Logger logger) {
            this.configManager = configManager;
            this.upgradeManager = upgradeManager;
            this.logger = logger;
        }

        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    applyPassiveEffects(player);
                } catch (Exception exception) {
                    logger.log(Level.WARNING,
                            "Ошибка при применении пассивных эффектов к игроку " + player.getName(),
                            exception);
                }
            }
        }

        private void applyPassiveEffects(Player player) {
            Map<String, PotionEffect> strongestEffects = new HashMap<String, PotionEffect>();
            for (EquipmentType type : EquipmentType.values()) {
                ItemStack item = type.getPlayerItem(player);
                if (!type.matches(item)) {
                    continue;
                }

                for (UpgradeDefinition definition : upgradeManager.getPurchasedUpgrades(item, type)) {
                    UpgradeEffect effect = definition.getEffect();
                    if (effect.getType() != UpgradeEffectType.PASSIVE_POTION || effect.getPotionEffectType() == null) {
                        continue;
                    }

                    int duration = Math.max(effect.getDurationTicks(), (int) configManager.getPassiveRefreshTicks() + 20);
                    PotionEffect potionEffect = new PotionEffect(
                            effect.getPotionEffectType(),
                            duration,
                            effect.getAmplifier(),
                            true,
                            false
                    );
                    String key = effect.getPotionEffectType().getName();
                    PotionEffect current = strongestEffects.get(key);
                    if (current == null || current.getAmplifier() < potionEffect.getAmplifier()) {
                        strongestEffects.put(key, potionEffect);
                    }
                }
            }

            for (PotionEffect effect : strongestEffects.values()) {
                player.addPotionEffect(effect, true);
                if (configManager.isDebugMode()) {
                    logger.info("[Debug] Пассивный эффект " + effect.getType().getName()
                            + " (усилитель " + effect.getAmplifier() + ")"
                            + " применён к " + player.getName());
                }
            }
        }
    }
}
