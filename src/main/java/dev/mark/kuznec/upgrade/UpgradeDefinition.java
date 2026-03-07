package dev.mark.kuznec.upgrade;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class UpgradeDefinition {

    private final String id;
    private final EquipmentType type;
    private final Material iconMaterial;
    private final String name;
    private final List<String> description;
    private final double price;
    private final UpgradeEffect effect;

    public UpgradeDefinition(String id,
                             EquipmentType type,
                             Material iconMaterial,
                             String name,
                             List<String> description,
                             double price,
                             UpgradeEffect effect) {
        this.id = id;
        this.type = type;
        this.iconMaterial = iconMaterial;
        this.name = name;
        this.description = description;
        this.price = price;
        this.effect = effect;
    }

    public String getId() {
        return id;
    }

    public EquipmentType getType() {
        return type;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public double getPrice() {
        return price;
    }

    public UpgradeEffect getEffect() {
        return effect;
    }

    public String getUniqueKey() {
        return type.getConfigKey() + "_" + id;
    }
}
