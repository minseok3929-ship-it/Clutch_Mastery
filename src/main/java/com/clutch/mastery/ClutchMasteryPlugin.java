package com.clutch.mastery;

import com.clutch.mastery.command.MasteryCommand;
import com.clutch.mastery.db.DatabaseManager;
import com.clutch.mastery.gui.MasteryGUI;
import com.clutch.mastery.listener.*;
import com.clutch.mastery.manager.CooldownManager;
import com.clutch.mastery.manager.MasteryManager;
import com.clutch.mastery.manager.PlacedBlockManager;
import com.clutch.mastery.task.AutoSaveTask;
import com.clutch.mastery.task.PassiveEffectTask;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClutchMasteryPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private MasteryManager masteryManager;
    private CooldownManager cooldownManager;
    private PlacedBlockManager placedBlockManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        masteryManager = new MasteryManager(this, databaseManager);
        cooldownManager = new CooldownManager();
        placedBlockManager = new PlacedBlockManager(databaseManager);
        MasteryGUI masteryGUI = new MasteryGUI(masteryManager);

        registerCommands(masteryGUI);
        registerListeners();
        registerTasks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            masteryManager.loadPlayer(player);
        }
        getLogger().info("ClutchMastery 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (masteryManager != null) {
            masteryManager.saveAllOnlineNow();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("ClutchMastery 플러그인이 비활성화되었습니다.");
    }

    private void registerCommands(MasteryGUI masteryGUI) {
        PluginCommand command = getCommand("숙련도");
        if (command == null) {
            getLogger().severe("plugin.yml에 /숙련도 명령어가 등록되어 있지 않습니다.");
            return;
        }
        MasteryCommand masteryCommand = new MasteryCommand(this, masteryManager, databaseManager, masteryGUI);
        command.setExecutor(masteryCommand);
        command.setTabCompleter(masteryCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(masteryManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(placedBlockManager), this);
        Bukkit.getPluginManager().registerEvents(new FarmingListener(this, masteryManager, cooldownManager, placedBlockManager), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this, masteryManager, cooldownManager, placedBlockManager), this);
        Bukkit.getPluginManager().registerEvents(new FishingListener(this, masteryManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
    }

    private void registerTasks() {
        long autosaveTicks = Math.max(1L, getConfig().getLong("autosave-interval-minutes", 5L)) * 60L * 20L;
        long passiveTicks = Math.max(1L, getConfig().getLong("passive-effect-interval-seconds", 5L)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, new AutoSaveTask(masteryManager), autosaveTicks, autosaveTicks);
        Bukkit.getScheduler().runTaskTimer(this, new PassiveEffectTask(masteryManager), passiveTicks, passiveTicks);
    }
}
