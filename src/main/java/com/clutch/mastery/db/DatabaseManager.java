package com.clutch.mastery.db;

import com.clutch.mastery.model.MasteryData;
import com.clutch.mastery.model.MasteryRankEntry;
import com.clutch.mastery.model.MasteryType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final ExecutorService executor;
    private final String jdbcUrl;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ClutchMastery-SQLite");
            thread.setDaemon(true);
            return thread;
        });
        File dbFile = new File(plugin.getDataFolder(), "mastery.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public void initialize() {
        plugin.getDataFolder().mkdirs();
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_mastery (uuid TEXT NOT NULL, mastery_type TEXT NOT NULL, level INTEGER NOT NULL DEFAULT 1, exp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, mastery_type))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS placed_blocks (world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, material TEXT NOT NULL, placed_at INTEGER NOT NULL, PRIMARY KEY(world, x, y, z))");
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "SQLite 초기화 중 오류가 발생했습니다.", exception);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public CompletableFuture<EnumMap<MasteryType, MasteryData>> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            EnumMap<MasteryType, MasteryData> data = createDefaultData();
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT mastery_type, level, exp FROM player_mastery WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        try {
                            MasteryType type = MasteryType.valueOf(resultSet.getString("mastery_type"));
                            data.put(type, new MasteryData(resultSet.getInt("level"), resultSet.getInt("exp")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "플레이어 숙련도 로드 중 오류가 발생했습니다: " + uuid, exception);
            }
            return data;
        }, executor);
    }

    private EnumMap<MasteryType, MasteryData> createDefaultData() {
        EnumMap<MasteryType, MasteryData> data = new EnumMap<>(MasteryType.class);
        for (MasteryType type : MasteryType.values()) {
            data.put(type, new MasteryData());
        }
        return data;
    }

    public CompletableFuture<Void> savePlayer(UUID uuid, Map<MasteryType, MasteryData> data) {
        EnumMap<MasteryType, MasteryData> snapshot = new EnumMap<>(MasteryType.class);
        for (MasteryType type : MasteryType.values()) {
            snapshot.put(type, data.getOrDefault(type, new MasteryData()).copy());
        }
        return CompletableFuture.runAsync(() -> savePlayerNow(uuid, snapshot), executor);
    }

    public void savePlayerNow(UUID uuid, Map<MasteryType, MasteryData> data) {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO player_mastery(uuid, mastery_type, level, exp) VALUES(?, ?, ?, ?) ON CONFLICT(uuid, mastery_type) DO UPDATE SET level = excluded.level, exp = excluded.exp")) {
            for (Map.Entry<MasteryType, MasteryData> entry : data.entrySet()) {
                statement.setString(1, uuid.toString());
                statement.setString(2, entry.getKey().name());
                statement.setInt(3, entry.getValue().getLevel());
                statement.setInt(4, entry.getValue().getExp());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "플레이어 숙련도 저장 중 오류가 발생했습니다: " + uuid, exception);
        }
    }

    public CompletableFuture<Void> resetPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM player_mastery WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "플레이어 숙련도 초기화 중 오류가 발생했습니다: " + uuid, exception);
            }
        }, executor);
    }

    public CompletableFuture<List<MasteryRankEntry>> loadRanking(MasteryType type) {
        return CompletableFuture.supplyAsync(() -> {
            List<MasteryRankEntry> entries = new ArrayList<>();
            String sql = type == null
                    ? "SELECT uuid, SUM(level) AS level_score, SUM(exp) AS exp_score FROM player_mastery GROUP BY uuid ORDER BY level_score DESC, exp_score DESC LIMIT 10"
                    : "SELECT uuid, level AS level_score, exp AS exp_score FROM player_mastery WHERE mastery_type = ? ORDER BY level DESC, exp DESC LIMIT 10";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (type != null) {
                    statement.setString(1, type.name());
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        entries.add(new MasteryRankEntry(UUID.fromString(resultSet.getString("uuid")), resultSet.getInt("level_score"), resultSet.getInt("exp_score")));
                    }
                }
            } catch (SQLException | IllegalArgumentException exception) {
                plugin.getLogger().log(Level.SEVERE, "숙련도 랭킹 로드 중 오류가 발생했습니다.", exception);
            }
            return entries;
        }, executor);
    }

    public Set<String> loadPlacedBlockKeys() {
        Set<String> keys = new HashSet<>();
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT world, x, y, z FROM placed_blocks"); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                keys.add(resultSet.getString("world") + ':' + resultSet.getInt("x") + ':' + resultSet.getInt("y") + ':' + resultSet.getInt("z"));
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "설치 블록 로드 중 오류가 발생했습니다.", exception);
        }
        return keys;
    }

    public void addPlacedBlock(Location location, Material material) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO placed_blocks(world, x, y, z, material, placed_at) VALUES(?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, Objects.requireNonNull(location.getWorld()).getUID().toString());
                statement.setInt(2, location.getBlockX());
                statement.setInt(3, location.getBlockY());
                statement.setInt(4, location.getBlockZ());
                statement.setString(5, material.name());
                statement.setLong(6, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException | NullPointerException exception) {
                plugin.getLogger().log(Level.SEVERE, "설치 블록 저장 중 오류가 발생했습니다.", exception);
            }
        }, executor);
    }

    public void removePlacedBlock(Location location) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM placed_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setString(1, Objects.requireNonNull(location.getWorld()).getUID().toString());
                statement.setInt(2, location.getBlockX());
                statement.setInt(3, location.getBlockY());
                statement.setInt(4, location.getBlockZ());
                statement.executeUpdate();
            } catch (SQLException | NullPointerException exception) {
                plugin.getLogger().log(Level.SEVERE, "설치 블록 삭제 중 오류가 발생했습니다.", exception);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
