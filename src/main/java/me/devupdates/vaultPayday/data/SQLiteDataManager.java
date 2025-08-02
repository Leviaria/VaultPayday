package me.devupdates.vaultPayday.data;

import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.model.PaydayData;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite implementation of DataManager
 */
public class SQLiteDataManager implements DataManager {
    private final VaultPayday plugin;
    private final String databasePath;
    private Connection connection;
    
    // SQL statements
    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS payday_data (
            uuid TEXT PRIMARY KEY,
            player_name TEXT NOT NULL,
            minutes_played INTEGER NOT NULL DEFAULT 0,
            pending_balance REAL NOT NULL DEFAULT 0.0,
            last_updated INTEGER NOT NULL,
            total_paydays INTEGER NOT NULL DEFAULT 0
        )
        """;
    
    private static final String SELECT_PLAYER = 
        "SELECT * FROM payday_data WHERE uuid = ?";
    
    private static final String INSERT_OR_UPDATE_PLAYER = """
        INSERT OR REPLACE INTO payday_data 
        (uuid, player_name, minutes_played, pending_balance, last_updated, total_paydays) 
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    
    private static final String DELETE_PLAYER = 
        "DELETE FROM payday_data WHERE uuid = ?";
    
    private static final String COUNT_PLAYERS = 
        "SELECT COUNT(*) FROM payday_data";
    
    private static final String COUNT_PENDING_PAYOUTS = 
        "SELECT COUNT(*) FROM payday_data WHERE pending_balance > 0";
    
    private static final String SUM_TOTAL_PAYDAYS = 
        "SELECT SUM(total_paydays) FROM payday_data";
    
    public SQLiteDataManager(VaultPayday plugin, String filename) {
        this.plugin = plugin;
        this.databasePath = new File(plugin.getDataFolder(), filename).getAbsolutePath();
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create plugin data folder if it doesn't exist
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                
                // Load SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");
                
                // Create connection
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                connection.setAutoCommit(true);
                
                // Create table if it doesn't exist
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(CREATE_TABLE);
                }
                
                plugin.getLogger().info("SQLite database initialized: " + databasePath);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<PaydayData> loadPlayerData(UUID playerUUID, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(SELECT_PLAYER)) {
                stmt.setString(1, playerUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PaydayData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getLong("minutes_played"),
                            rs.getDouble("pending_balance"),
                            rs.getLong("last_updated"),
                            rs.getInt("total_paydays")
                        );
                    } else {
                        // Player not found, create new data
                        PaydayData newData = new PaydayData(playerUUID, playerName);
                        savePlayerData(newData); // Save immediately
                        return newData;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data for " + playerUUID + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> savePlayerData(PaydayData data) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(INSERT_OR_UPDATE_PLAYER)) {
                stmt.setString(1, data.getPlayerUUID().toString());
                stmt.setString(2, data.getPlayerName());
                stmt.setLong(3, data.getMinutesPlayed());
                stmt.setDouble(4, data.getPendingBalance());
                stmt.setLong(5, data.getLastUpdated());
                stmt.setInt(6, data.getTotalPaydays());
                
                stmt.executeUpdate();
                
                // Debug logging would go here if needed
                // plugin.getLogger().info("Saved data for player " + data.getPlayerName());
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data for " + 
                    data.getPlayerUUID() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerUUID) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(DELETE_PLAYER)) {
                stmt.setString(1, playerUUID.toString());
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    plugin.getLogger().info("Deleted data for player " + playerUUID);
                } else {
                    plugin.getLogger().warning("No data found to delete for player " + playerUUID);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete player data for " + 
                    playerUUID + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> getTotalPlayersCount() {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(COUNT_PLAYERS);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get total players count: " + e.getMessage());
                return 0;
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> getPendingPayoutsCount() {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(COUNT_PENDING_PAYOUTS);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get pending payouts count: " + e.getMessage());
                return 0;
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getTotalPaydaysGiven() {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(SUM_TOTAL_PAYDAYS);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get total paydays given: " + e.getMessage());
                return 0L;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("SQLite database connection closed");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> createBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File backupDir = new File(plugin.getDataFolder(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                
                String timestamp = String.valueOf(System.currentTimeMillis());
                File backupFile = new File(backupDir, "payday_backup_" + timestamp + ".db");
                
                // Simple file copy for SQLite database
                java.nio.file.Files.copy(
                    new File(databasePath).toPath(),
                    backupFile.toPath()
                );
                
                plugin.getLogger().info("Database backup created: " + backupFile.getName());
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create database backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Check if the database connection is valid
     */
    public boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Reconnect to the database if connection is lost
     */
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
                
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                connection.setAutoCommit(true);
                
                plugin.getLogger().info("Database reconnected successfully");
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reconnect to database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}