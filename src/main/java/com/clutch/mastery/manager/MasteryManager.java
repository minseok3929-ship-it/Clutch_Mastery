package com.clutch.mastery.manager;

import com.clutch.mastery.db.DatabaseManager;
import com.clutch.mastery.model.MasteryData;
import com.clutch.mastery.model.MasteryType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MasteryManager {
    public static final String PREFIX = "§8[CLUTCH] §f";

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, EnumMap<MasteryType, MasteryData>> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public MasteryManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        databaseManager.loadPlayer(uuid).thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> cache.put(uuid, data)));
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }

    public void savePlayer(UUID uuid) {
        EnumMap<MasteryType, MasteryData> data = cache.get(uuid);
        if (data != null) {
            databaseManager.savePlayer(uuid, data);
        }
    }

    public void saveAllOnlineAsync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayer(player.getUniqueId());
        }
    }

    public void saveAllOnlineNow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            EnumMap<MasteryType, MasteryData> data = cache.get(player.getUniqueId());
            if (data != null) {
                databaseManager.savePlayerNow(player.getUniqueId(), data);
            }
        }
    }

    public MasteryData getData(UUID uuid, MasteryType type) {
        return cache.computeIfAbsent(uuid, ignored -> createDefaultData()).computeIfAbsent(type, ignored -> new MasteryData());
    }

    public Map<MasteryType, MasteryData> getPlayerData(UUID uuid) {
        return Collections.unmodifiableMap(cache.computeIfAbsent(uuid, ignored -> createDefaultData()));
    }

    private EnumMap<MasteryType, MasteryData> createDefaultData() {
        EnumMap<MasteryType, MasteryData> data = new EnumMap<>(MasteryType.class);
        for (MasteryType type : MasteryType.values()) {
            data.put(type, new MasteryData());
        }
        return data;
    }

    public boolean addExp(Player player, MasteryType type, int amount) {
        if (player == null || !player.isOnline() || amount <= 0) {
            return false;
        }
        MasteryData data = getData(player.getUniqueId(), type);
        int maxLevel = getMaxLevel();
        if (data.getLevel() >= maxLevel) {
            data.setLevel(maxLevel);
            data.setExp(0);
            return false;
        }

        int beforeLevel = data.getLevel();
        data.setExp(data.getExp() + amount);
        while (data.getLevel() < maxLevel && data.getExp() >= getRequiredExp(data.getLevel())) {
            data.setExp(data.getExp() - getRequiredExp(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
        }
        if (data.getLevel() >= maxLevel) {
            data.setLevel(maxLevel);
            data.setExp(0);
        }

        int required = data.getLevel() >= maxLevel ? getRequiredExp(maxLevel) : getRequiredExp(data.getLevel());
        player.sendActionBar(Component.text("§a" + type.getKoreanName() + " 숙련도 §f+" + amount + " EXP §7(" + data.getExp() + "/" + required + ")"));

        if (data.getLevel() > beforeLevel) {
            playLevelUp(player, type, data.getLevel(), beforeLevel);
        }
        return true;
    }

    private void playLevelUp(Player player, MasteryType type, int newLevel, int oldLevel) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.showTitle(Title.title(
                Component.text("§a숙련도 상승!"),
                Component.text("§f" + type.getKoreanName() + " Lv." + newLevel),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500))
        ));
        if (oldLevel < 25 && newLevel >= 25) {
            player.sendMessage(PREFIX + "§a숙련의 길에 들어섰습니다.");
        }
        if (oldLevel < 50 && newLevel >= 50) {
            player.sendMessage(PREFIX + "§6해당 분야의 달인이 되었습니다.");
        }
    }

    public void setLevel(UUID uuid, MasteryType type, int level) {
        MasteryData data = getData(uuid, type);
        data.setLevel(Math.min(getMaxLevel(), Math.max(1, level)));
        data.setExp(0);
    }

    public void reset(UUID uuid) {
        cache.put(uuid, createDefaultData());
        databaseManager.resetPlayer(uuid);
    }

    public int getRequiredExp(int level) {
        int base = plugin.getConfig().getInt("exp-formula.base", 100);
        int linear = plugin.getConfig().getInt("exp-formula.level-multiplier", 75);
        int square = plugin.getConfig().getInt("exp-formula.level-square-multiplier", 5);
        return base + (level * linear) + (level * level * square);
    }

    public int getMaxLevel() {
        return plugin.getConfig().getInt("max-level", 50);
    }

    public double getBonusChance(int level) {
        double chance = level * plugin.getConfig().getDouble("bonus-chance-per-level", 0.4);
        return Math.min(chance, plugin.getConfig().getDouble("bonus-chance-max", 20.0));
    }

    public boolean rollBonus(MasteryType type, UUID uuid) {
        return random.nextDouble() * 100.0 < getBonusChance(getData(uuid, type).getLevel());
    }

    public void giveItem(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0) {
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }
}
