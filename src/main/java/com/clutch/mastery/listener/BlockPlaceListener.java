package com.clutch.mastery.listener;

import com.clutch.mastery.manager.PlacedBlockManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    private final PlacedBlockManager placedBlockManager;

    public BlockPlaceListener(PlacedBlockManager placedBlockManager) {
        this.placedBlockManager = placedBlockManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlockManager.markPlaced(event.getBlockPlaced().getLocation(), event.getBlockPlaced().getType());
    }
}
