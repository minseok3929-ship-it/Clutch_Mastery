package com.clutch.mastery.listener;

import com.clutch.mastery.manager.CooldownManager;
import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.manager.PlacedBlockManager;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class MiningListener implements Listener {
    private final JavaPlugin plugin;
    private final MasteryManager masteryManager;
    private final CooldownManager cooldownManager;
    private final PlacedBlockManager placedBlockManager;
    private final Set<Material> normalOres = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE
    );
    private final Set<Material> rareOres = EnumSet.of(
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.ANCIENT_DEBRIS, Material.NETHER_GOLD_ORE
    );

    public MiningListener(JavaPlugin plugin, MasteryManager masteryManager, CooldownManager cooldownManager, PlacedBlockManager placedBlockManager) {
        this.plugin = plugin;
        this.masteryManager = masteryManager;
        this.cooldownManager = cooldownManager;
        this.placedBlockManager = placedBlockManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        boolean normal = normalOres.contains(material);
        boolean rare = rareOres.contains(material);
        if (!normal && !rare) {
            return;
        }
        boolean wasPlaced = placedBlockManager.isPlaced(block.getLocation());
        placedBlockManager.remove(block.getLocation());
        if (wasPlaced) {
            return;
        }
        Player player = event.getPlayer();
        if (!cooldownManager.tryUse(player.getUniqueId(), MasteryType.MINING, plugin.getConfig().getLong("cooldowns.mining", 200L))) {
            return;
        }
        int exp = normal ? plugin.getConfig().getInt("mining-normal-exp", 8) : plugin.getConfig().getInt("mining-rare-exp", 15);
        if (masteryManager.addExp(player, MasteryType.MINING, exp)) {
            int level = masteryManager.getData(player.getUniqueId(), MasteryType.MINING).getLevel();
            if (level >= 25) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 0));
            }
            if (level >= 50) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1800, 0));
            }
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!tool.containsEnchantment(Enchantment.SILK_TOUCH) && masteryManager.rollBonus(MasteryType.MINING, player.getUniqueId())) {
                ItemStack bonus = getFirstDrop(block, tool, player);
                if (bonus != null) {
                    masteryManager.giveItem(player, bonus);
                }
            }
        }
    }

    private ItemStack getFirstDrop(Block block, ItemStack tool, Player player) {
        Collection<ItemStack> drops = block.getDrops(tool, player);
        return drops.stream()
                .filter(item -> item != null && !item.getType().isAir() && item.getAmount() > 0)
                .findFirst()
                .map(item -> {
                    ItemStack clone = item.clone();
                    clone.setAmount(1);
                    return clone;
                })
                .orElse(null);
    }
}
