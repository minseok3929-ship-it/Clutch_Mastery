package com.clutch.mastery.listener;

import com.clutch.mastery.manager.CooldownManager;
import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.manager.PlacedBlockManager;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;

public class FarmingListener implements Listener {
    private final MasteryManager masteryManager;
    private final CooldownManager cooldownManager;
    private final PlacedBlockManager placedBlockManager;
    private final JavaPlugin plugin;
    private final Map<Material, Material> cropItems = new EnumMap<>(Material.class);

    public FarmingListener(JavaPlugin plugin, MasteryManager masteryManager, CooldownManager cooldownManager, PlacedBlockManager placedBlockManager) {
        this.plugin = plugin;
        this.masteryManager = masteryManager;
        this.cooldownManager = cooldownManager;
        this.placedBlockManager = placedBlockManager;
        cropItems.put(Material.WHEAT, Material.WHEAT);
        cropItems.put(Material.CARROTS, Material.CARROT);
        cropItems.put(Material.POTATOES, Material.POTATO);
        cropItems.put(Material.BEETROOTS, Material.BEETROOT);
        cropItems.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material crop = block.getType();
        if (!cropItems.containsKey(crop)) {
            return;
        }
        boolean wasPlaced = placedBlockManager.isPlaced(block.getLocation());
        placedBlockManager.remove(block.getLocation());
        if (wasPlaced || !(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        Player player = event.getPlayer();
        long cooldown = plugin.getConfig().getLong("cooldowns.farming", 200L);
        if (!cooldownManager.tryUse(player.getUniqueId(), MasteryType.FARMING, cooldown)) {
            return;
        }
        if (masteryManager.addExp(player, MasteryType.FARMING, plugin.getConfig().getInt("farming-exp", 5))) {
            int level = masteryManager.getData(player.getUniqueId(), MasteryType.FARMING).getLevel();
            if (level >= 25) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 0));
            }
            if (level >= 50) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0));
            }
            if (masteryManager.rollBonus(MasteryType.FARMING, player.getUniqueId())) {
                masteryManager.giveItem(player, new ItemStack(cropItems.get(crop), 1));
            }
        }
    }
}
