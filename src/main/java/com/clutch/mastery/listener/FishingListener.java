package com.clutch.mastery.listener;

import com.clutch.mastery.manager.CooldownManager;
import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class FishingListener implements Listener {
    private final JavaPlugin plugin;
    private final MasteryManager masteryManager;
    private final CooldownManager cooldownManager;
    private final Set<Material> treasureItems = EnumSet.of(
            Material.BOW, Material.ENCHANTED_BOOK, Material.FISHING_ROD, Material.NAME_TAG,
            Material.NAUTILUS_SHELL, Material.SADDLE
    );

    public FishingListener(JavaPlugin plugin, MasteryManager masteryManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.masteryManager = masteryManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caughtItem)) {
            return;
        }
        Player player = event.getPlayer();
        if (!cooldownManager.tryUse(player.getUniqueId(), MasteryType.FISHING, plugin.getConfig().getLong("cooldowns.fishing", 1000L))) {
            return;
        }
        ItemStack caughtStack = caughtItem.getItemStack();
        boolean treasure = treasureItems.contains(caughtStack.getType());
        int exp = treasure ? plugin.getConfig().getInt("fishing-treasure-exp", 25) : plugin.getConfig().getInt("fishing-normal-exp", 12);
        if (masteryManager.addExp(player, MasteryType.FISHING, exp) && masteryManager.rollBonus(MasteryType.FISHING, player.getUniqueId())) {
            ItemStack bonus = caughtStack.clone();
            bonus.setAmount(1);
            masteryManager.giveItem(player, bonus);
        }
    }
}
