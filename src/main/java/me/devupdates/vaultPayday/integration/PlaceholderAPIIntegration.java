package me.devupdates.vaultPayday.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
import me.devupdates.vaultPayday.model.PaydayData;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PlaceholderAPI integration for VaultPayday
 * Provides custom placeholders for other plugins to use
 */
public class PlaceholderAPIIntegration extends PlaceholderExpansion {
    private final VaultPayday plugin;
    private final PaydayManager paydayManager;
    private final ConfigManager configManager;
    
    public PlaceholderAPIIntegration(VaultPayday plugin, PaydayManager paydayManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.paydayManager = paydayManager;
        this.configManager = configManager;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "vaultpayday";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Keep expansion loaded even when PAPI reloads
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        UUID playerUUID = player.getUniqueId();
        PaydayData paydayData = paydayManager.getPlayerData(playerUUID);
        
        // If player data is not available, return default values
        if (paydayData == null) {
            return getDefaultValue(params);
        }
        
        long requiredMinutes = configManager.getPaydayIntervalMinutes();
        
        switch (params.toLowerCase()) {
            // Time-related placeholders
            case "time_played":
                return String.valueOf(paydayData.getMinutesPlayed());
                
            case "time_required":
                return String.valueOf(requiredMinutes);
                
            case "time_remaining":
                return String.valueOf(paydayData.getRemainingMinutes(requiredMinutes));
                
            case "time_progress_percentage":
                return String.format("%.1f", paydayData.getProgressPercentage(requiredMinutes));
                
            case "time_progress_percentage_rounded":
                return String.valueOf(Math.round(paydayData.getProgressPercentage(requiredMinutes)));
                
            // Balance-related placeholders
            case "pending_balance":
                return String.format("%.2f", paydayData.getPendingBalance());
                
            case "pending_balance_rounded":
                return String.valueOf(Math.round(paydayData.getPendingBalance()));
                
            case "pending_balance_formatted":
                return formatBalance(paydayData.getPendingBalance());
                
            // Status placeholders
            case "is_ready":
                return paydayData.isReadyForPayday(requiredMinutes) ? "true" : "false";
                
            case "status":
                return paydayData.isReadyForPayday(requiredMinutes) ? "Ready" : "In Progress";
                
            case "status_color":
                return paydayData.isReadyForPayday(requiredMinutes) ? "&a" : "&e";
                
            // Statistics placeholders
            case "total_paydays":
                return String.valueOf(paydayData.getTotalPaydays());
                
            case "last_updated":
                return formatTimestamp(paydayData.getLastUpdated());
                
            // Progress bar placeholders
            case "progress_bar":
                return createProgressBar(paydayData.getProgressPercentage(requiredMinutes), 20);
                
            case "progress_bar_short":
                return createProgressBar(paydayData.getProgressPercentage(requiredMinutes), 10);
                
            case "progress_bar_mini":
                return createProgressBar(paydayData.getProgressPercentage(requiredMinutes), 5);
                
            // Formatted time placeholders
            case "next_payday":
                return formatMinutesToTime(paydayData.getRemainingMinutes(requiredMinutes));
                
            case "time_played_formatted":
                return formatMinutesToTime(paydayData.getMinutesPlayed());
                
            case "time_required_formatted":
                return formatMinutesToTime(requiredMinutes);
                
            // Advanced placeholders
            case "earnings_per_minute":
                if (paydayData.getMinutesPlayed() > 0) {
                    double earningsPerMinute = paydayData.getPendingBalance() / paydayData.getMinutesPlayed();
                    return String.format("%.2f", earningsPerMinute);
                }
                return "0.00";
                
            case "estimated_total":
                if (paydayData.getMinutesPlayed() > 0) {
                    double earningsPerMinute = paydayData.getPendingBalance() / paydayData.getMinutesPlayed();
                    double estimatedTotal = earningsPerMinute * requiredMinutes;
                    return String.format("%.2f", estimatedTotal);
                }
                return String.format("%.2f", paydayData.getPendingBalance());
                
            default:
                return null; // Placeholder not found
        }
    }
    
    /**
     * Get default value for placeholders when player data is not available
     */
    private String getDefaultValue(String params) {
        switch (params.toLowerCase()) {
            case "time_played":
            case "time_remaining":
            case "total_paydays":
                return "0";
                
            case "time_required":
                return String.valueOf(configManager.getPaydayIntervalMinutes());
                
            case "time_progress_percentage":
            case "time_progress_percentage_rounded":
            case "pending_balance":
            case "pending_balance_rounded":
            case "earnings_per_minute":
            case "estimated_total":
                return "0.00";
                
            case "pending_balance_formatted":
                return "$0.00";
                
            case "is_ready":
                return "false";
                
            case "status":
                return "Not Started";
                
            case "status_color":
                return "&7";
                
            case "last_updated":
                return "Never";
                
            case "progress_bar":
                return "&7████████████████████";
                
            case "progress_bar_short":
                return "&7██████████";
                
            case "progress_bar_mini":
                return "&7█████";
                
            case "next_payday":
            case "time_played_formatted":
                return "0m";
                
            case "time_required_formatted":
                return formatMinutesToTime(configManager.getPaydayIntervalMinutes());
                
            default:
                return "";
        }
    }
    
    /**
     * Create a visual progress bar
     */
    private String createProgressBar(double percentage, int length) {
        int filledBars = (int) (percentage / 100.0 * length);
        int emptyBars = length - filledBars;
        
        StringBuilder progressBar = new StringBuilder("&a");
        
        // Add filled bars
        for (int i = 0; i < filledBars; i++) {
            progressBar.append("█");
        }
        
        // Add empty bars
        progressBar.append("&7");
        for (int i = 0; i < emptyBars; i++) {
            progressBar.append("█");
        }
        
        return progressBar.toString();
    }
    
    /**
     * Format balance with currency symbol
     */
    private String formatBalance(double balance) {
        return String.format("$%.2f", balance);
    }
    
    /**
     * Format timestamp to readable format
     */
    private String formatTimestamp(long timestamp) {
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), 
            java.time.ZoneId.systemDefault()
        );
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm");
        return dateTime.format(formatter);
    }
    
    /**
     * Format minutes to human-readable time format
     */
    private String formatMinutesToTime(long minutes) {
        if (minutes < 60) {
            return minutes + "m";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }
}