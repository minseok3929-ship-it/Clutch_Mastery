package com.clutch.mastery.task;

import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PassiveEffectTask implements Runnable {
    private final MasteryManager masteryManager;

    public PassiveEffectTask(MasteryManager masteryManager) {
        this.masteryManager = masteryManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = masteryManager.getData(player.getUniqueId(), MasteryType.FISHING).getLevel();
            if (level >= 25) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 300, 0, true, false, true));
            }
            if (level >= 50) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300, 0, true, false, true));
            }
        }
    }
}
