package com.clutch.mastery.manager;

import com.clutch.mastery.db.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlacedBlockManager {
    private final DatabaseManager databaseManager;
    private final Set<String> placedBlocks = ConcurrentHashMap.newKeySet();
    private final Set<Material> trackedMaterials = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS, Material.NETHER_GOLD_ORE
    );

    public PlacedBlockManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.placedBlocks.addAll(databaseManager.loadPlacedBlockKeys());
    }

    public boolean isTracked(Material material) {
        return trackedMaterials.contains(material);
    }

    public void markPlaced(Location location, Material material) {
        if (!isTracked(material)) {
            return;
        }
        placedBlocks.add(toKey(location));
        databaseManager.addPlacedBlock(location, material);
    }

    public boolean isPlaced(Location location) {
        return placedBlocks.contains(toKey(location));
    }

    public void remove(Location location) {
        if (placedBlocks.remove(toKey(location))) {
            databaseManager.removePlacedBlock(location);
        }
    }

    private String toKey(Location location) {
        return Objects.requireNonNull(location.getWorld()).getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
