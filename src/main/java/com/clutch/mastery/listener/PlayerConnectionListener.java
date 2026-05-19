package com.clutch.mastery.listener;

import com.clutch.mastery.manager.CooldownManager;
import com.clutch.mastery.manager.MasteryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final MasteryManager masteryManager;
    private final CooldownManager cooldownManager;

    public PlayerConnectionListener(MasteryManager masteryManager, CooldownManager cooldownManager) {
        this.masteryManager = masteryManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        masteryManager.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        masteryManager.unloadPlayer(event.getPlayer().getUniqueId());
        cooldownManager.clear(event.getPlayer().getUniqueId());
    }
}
