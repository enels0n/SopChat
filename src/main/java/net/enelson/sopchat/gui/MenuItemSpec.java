package net.enelson.sopchat.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuItemSpec {

    private final String materialSpec;
    private final Material fallbackMaterial;
    private final String name;
    private final Integer customModelData;
    private final List<String> lore;

    public MenuItemSpec(String materialSpec, Material fallbackMaterial, String name, Integer customModelData, List<String> lore) {
        this.materialSpec = materialSpec;
        this.fallbackMaterial = fallbackMaterial;
        this.name = name;
        this.customModelData = customModelData;
        this.lore = Collections.unmodifiableList(new ArrayList<String>(lore));
    }

    public static MenuItemSpec fromSection(ConfigurationSection section, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        if (section == null) {
            return new MenuItemSpec(fallbackMaterial.name(), fallbackMaterial, fallbackName, null, fallbackLore);
        }
        String materialSpec = section.getString("material", fallbackMaterial.name());
        Integer customModelData = section.contains("custom-model-data") ? Integer.valueOf(section.getInt("custom-model-data")) : null;
        List<String> lore = section.contains("lore")
                ? new ArrayList<String>(section.getStringList("lore"))
                : new ArrayList<String>(fallbackLore);
        return new MenuItemSpec(
                materialSpec,
                fallbackMaterial,
                section.getString("name", fallbackName),
                customModelData,
                lore
        );
    }

    public String getMaterialSpec() {
        return materialSpec;
    }

    public Material getFallbackMaterial() {
        return fallbackMaterial;
    }

    public String getName() {
        return name;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public List<String> getLore() {
        return lore;
    }
}
