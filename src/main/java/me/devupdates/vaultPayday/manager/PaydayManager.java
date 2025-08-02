package me.devupdates.vaultPayday.manager;

import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.data.DataManager;
import me.devupdates.vaultPayday.model.PaydayData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages the core payday system logic
 */
public class PaydayManager {
    private final VaultPayday plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private Economy economy;
    
    // Cache for active player data
    private final Map<UUID, PaydayData> activePlayerData;
    private final Map<UUID, Long> joinTimes; // Track when players joined
    
    // Task management
    private BukkitTask timeTrackingTask;
    private BukkitTask cacheCleanupTask;
    
    public PaydayManager(VaultPayday plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.activePlayerData = new ConcurrentHashMap<>();
        this.joinTimes = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the payday manager
     */
    public void initialize() {
        // Setup Vault economy
        if (!setupEconomy()) {
            plugin.getLogger().severe("Failed to setup Vault economy! Plugin will be disabled.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        
        // Start time tracking task
        startTimeTrackingTask();
        
        // Start cache cleanup task
        startCacheCleanupTask();
        
        plugin.getLogger().info("PaydayManager initialized successfully");
    }
    
    /**
     * Setup Vault economy integration
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        this.economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Start the central time tracking task
     */
    private void startTimeTrackingTask() {
        int intervalTicks = configManager.getTimeUpdateInterval() * 20; // Convert seconds to ticks
        
        timeTrackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerTimes();
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Time tracking task started (interval: " + configManager.getTimeUpdateInterval() + "s)");
    }
    
    /**
     * Start the cache cleanup task
     */
    private void startCacheCleanupTask() {
        int cleanupInterval = 1800; // 30 minutes in seconds
        int intervalTicks = cleanupInterval * 20;
        
        cacheCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupInactiveCache();
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }
    
    /**
     * Update playtime for all online players
     */
    private void updatePlayerTimes() {
        var onlinePlayers = Bukkit.getOnlinePlayers();
        int maxPlayersPerCycle = configManager.getMaxPlayersPerCycle();
        int processed = 0;
        
        for (Player player : onlinePlayers) {
            if (processed >= maxPlayersPerCycle) {
                break; // Prevent processing too many players at once
            }
            
            updatePlayerTime(player);
            processed++;
        }
        
        if (configManager.isDebugMode() && processed > 0) {
            plugin.getLogger().info("Updated playtime for " + processed + " players");
        }
    }
    
    /**
     * Update playtime for a specific player
     */
    private void updatePlayerTime(Player player) {
        UUID playerUUID = player.getUniqueId();
        PaydayData data = activePlayerData.get(playerUUID);
        
        if (data == null) {
            return; // Player data not loaded yet
        }
        
        // Calculate minutes to add based on time tracking interval
        long minutesToAdd = configManager.getTimeUpdateInterval() / 60; // Convert seconds to minutes
        if (minutesToAdd < 1) {
            minutesToAdd = 1; // Always add at least 1 minute
        }
        
        data.addMinutes(minutesToAdd);
        
        // Check if player is ready for payday
        if (data.isReadyForPayday(configManager.getPaydayIntervalMinutes())) {
            processPayday(player, data);
        }
        
        // Save data asynchronously
        if (configManager.isAutoSaveOnEvents()) {
            dataManager.savePlayerData(data);
        }
    }
    
    /**
     * Process payday for a player
     */
    private void processPayday(Player player, PaydayData data) {
        double baseAmount = data.getPendingBalance();
        
        if (baseAmount <= 0) {
            // No pending balance, just reset the cycle
            data.resetPaydayCycle();
            return;
        }
        
        // Apply multipliers
        final double finalAmount = applyMultipliers(player, baseAmount);
        
        // Give payment through Vault
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (economy.depositPlayer(player, finalAmount).transactionSuccess()) {
                // Payment successful
                String message = configManager.getMessage("payday_received")
                    .replace("%amount%", String.format("%.2f", finalAmount))
                    .replace("%time%", String.valueOf(configManager.getPaydayIntervalMinutes()));
                
                // Add prefix and send notification (color codes will be converted in sendNotification)
                String fullMessage = configManager.getMessagePrefix() + message;
                sendNotification(player, fullMessage);
                
                // Reset payday cycle
                data.resetPaydayCycle();
                
                // Save data
                dataManager.savePlayerData(data);
                
                plugin.getLogger().info("Payday processed for " + player.getName() + ": $" + String.format("%.2f", finalAmount));
                
            } else {
                plugin.getLogger().warning("Failed to deposit payday amount for " + player.getName());
            }
        });
    }
    
    /**
     * Apply multipliers to the payday amount
     */
    private double applyMultipliers(Player player, double baseAmount) {
        if (!configManager.isMultipliersEnabled()) {
            return baseAmount;
        }
        
        double multiplier = 1.0;
        
        // Check permission-based multipliers
        for (Map.Entry<String, Double> entry : configManager.getPermissionMultipliers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                multiplier = Math.max(multiplier, entry.getValue());
            }
        }
        
        return baseAmount * multiplier;
    }
    
    /**
     * Send notification to player based on configuration
     */
    private void sendNotification(Player player, String message) {
        String messageType = configManager.getMessageType().toLowerCase();
        
        // Convert color codes (&) to section symbols (§) for display
        String coloredMessage = message.replace("&", "§");
        
        switch (messageType) {
            case "chat" -> player.sendMessage(coloredMessage);
            case "actionbar" -> {
                // Use Adventure API with proper color support
                player.sendActionBar(net.kyori.adventure.text.Component.text(coloredMessage));
            }
            case "title" -> {
                String[] parts = coloredMessage.split("\\n", 2);
                String title = parts[0];
                String subtitle = parts.length > 1 ? parts[1] : "";
                player.showTitle(net.kyori.adventure.title.Title.title(
                    net.kyori.adventure.text.Component.text(title),
                    net.kyori.adventure.text.Component.text(subtitle),
                    net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3500),
                        java.time.Duration.ofMillis(1000)
                    )
                ));
            }
            case "none" -> {
                // Do nothing
            }
            default -> player.sendMessage(coloredMessage); // Fallback to chat
        }
    }
    
    /**
     * Handle player joining
     */
    public void onPlayerJoin(Player player) {
        UUID playerUUID = player.getUniqueId();
        joinTimes.put(playerUUID, System.currentTimeMillis());
        
        // Check if data is already loaded (reconnect case)
        PaydayData existingData = activePlayerData.get(playerUUID);
        if (existingData != null) {
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Player data already loaded for " + player.getName());
            }
            return;
        }
        
        // Load player data asynchronously
        dataManager.loadPlayerData(playerUUID, player.getName())
            .thenAccept(data -> {
                activePlayerData.put(playerUUID, data);
                
                if (configManager.isDebugMode()) {
                    plugin.getLogger().info("Loaded payday data for " + player.getName() + 
                        " (Minutes: " + data.getMinutesPlayed() + ", Balance: $" + String.format("%.2f", data.getPendingBalance()) + ")");
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Failed to load payday data for " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Handle player leaving
     */
    public void onPlayerLeave(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Calculate and add final playtime
        Long joinTime = joinTimes.remove(playerUUID);
        if (joinTime != null) {
            PaydayData data = activePlayerData.get(playerUUID);
            if (data != null) {
                long sessionTime = System.currentTimeMillis() - joinTime;
                long minutesPlayed = TimeUnit.MILLISECONDS.toMinutes(sessionTime);
                
                if (minutesPlayed > 0) {
                    data.addMinutes(minutesPlayed);
                }
                
                // Save data before removing from cache
                dataManager.savePlayerData(data);
            }
        }
        
        // Remove from active cache
        activePlayerData.remove(playerUUID);
    }
    
    /**
     * Add pending balance to a player (called by JobsReborn integration)
     */
    public void addPendingBalance(UUID playerUUID, double amount) {
        PaydayData data = activePlayerData.get(playerUUID);
        
        if (data != null) {
            // Data is loaded, add immediately
            data.addPendingBalance(amount);
            
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Added $" + String.format("%.2f", amount) + 
                    " to pending balance for " + data.getPlayerName());
            }
        } else {
            // Data not loaded yet - this can happen if player just joined
            // Get player name for loading
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null) {
                if (configManager.isDebugMode()) {
                    plugin.getLogger().info("Player data not loaded yet for " + player.getName() + 
                        ", loading synchronously to add $" + String.format("%.2f", amount));
                }
                
                // Load data synchronously and add balance
                dataManager.loadPlayerData(playerUUID, player.getName())
                    .thenAccept(loadedData -> {
                        activePlayerData.put(playerUUID, loadedData);
                        loadedData.addPendingBalance(amount);
                        
                        if (configManager.isDebugMode()) {
                            plugin.getLogger().info("Loaded data and added $" + String.format("%.2f", amount) + 
                                " to pending balance for " + player.getName());
                        }
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to load player data for balance addition: " + throwable.getMessage());
                        return null;
                    });
            }
        }
    }
    
    /**
     * Get player data from cache or load it synchronously if needed
     */
    public PaydayData getPlayerData(UUID playerUUID) {
        PaydayData data = activePlayerData.get(playerUUID);
        
        if (data == null) {
            // Try to get player and load data if they're online
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null) {
                if (configManager.isDebugMode()) {
                    plugin.getLogger().info("Player data not in cache for " + player.getName() + ", attempting to load...");
                }
                
                // Load synchronously by joining the future
                try {
                    data = dataManager.loadPlayerData(playerUUID, player.getName()).get();
                    activePlayerData.put(playerUUID, data);
                    
                    if (configManager.isDebugMode()) {
                        plugin.getLogger().info("Successfully loaded player data for " + player.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load player data synchronously for " + player.getName() + ": " + e.getMessage());
                }
            }
        }
        
        return data;
    }
    
    /**
     * Reset player's payday progress
     */
    public void resetPlayerProgress(UUID playerUUID) {
        PaydayData data = activePlayerData.get(playerUUID);
        if (data != null) {
            data.setMinutesPlayed(0);
            data.setPendingBalance(0.0);
            dataManager.savePlayerData(data);
        }
    }
    
    /**
     * Set player's playtime
     */
    public void setPlayerTime(UUID playerUUID, long minutes) {
        PaydayData data = activePlayerData.get(playerUUID);
        if (data != null) {
            data.setMinutesPlayed(Math.max(0, Math.min(minutes, configManager.getPaydayIntervalMinutes())));
            dataManager.savePlayerData(data);
        }
    }
    
    /**
     * Clean up inactive cache entries
     */
    private void cleanupInactiveCache() {
        var onlineUUIDs = Bukkit.getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .collect(java.util.stream.Collectors.toSet());
        
        activePlayerData.entrySet().removeIf(entry -> !onlineUUIDs.contains(entry.getKey()));
        joinTimes.entrySet().removeIf(entry -> !onlineUUIDs.contains(entry.getKey()));
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Cache cleanup completed. Active players: " + activePlayerData.size());
        }
    }
    
    /**
     * Shutdown the payday manager
     */
    public void shutdown() {
        // Cancel tasks
        if (timeTrackingTask != null) {
            timeTrackingTask.cancel();
        }
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }
        
        // Save all active player data
        for (PaydayData data : activePlayerData.values()) {
            dataManager.savePlayerData(data);
        }
        
        // Clear caches
        activePlayerData.clear();
        joinTimes.clear();
        
        plugin.getLogger().info("PaydayManager shutdown completed");
    }
    
    // Getters
    public Economy getEconomy() { return economy; }
    public int getActivePlayersCount() { return activePlayerData.size(); }
}