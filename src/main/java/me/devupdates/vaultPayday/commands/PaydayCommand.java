package me.devupdates.vaultPayday.commands;

import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
import me.devupdates.vaultPayday.model.PaydayData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the /payday command for players to check their progress
 */
public class PaydayCommand implements CommandExecutor {
    private final VaultPayday plugin;
    private final PaydayManager paydayManager;
    private final ConfigManager configManager;
    
    // Command cooldown management
    private final Map<UUID, Long> commandCooldowns;
    
    public PaydayCommand(VaultPayday plugin, PaydayManager paydayManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.paydayManager = paydayManager;
        this.configManager = configManager;
        this.commandCooldowns = new HashMap<>();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getFormattedMessage("commands_player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("vaultpayday.check")) {
            player.sendMessage(configManager.getFormattedMessage("no_permission"));
            return true;
        }
        
        // Check if command is enabled
        if (!configManager.isShowProgressCommand()) {
            player.sendMessage(configManager.getFormattedMessage("command_disabled"));
            return true;
        }
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remainingTime = getRemainingCooldown(player);
            String message = configManager.getFormattedMessage("command_cooldown")
                .replace("%seconds%", String.valueOf(remainingTime));
            player.sendMessage(message);
            return true;
        }
        
        // Set cooldown
        setCooldown(player);
        
        // Get player data
        UUID playerUUID = player.getUniqueId();
        PaydayData paydayData = paydayManager.getPlayerData(playerUUID);
        
        if (paydayData == null) {
            player.sendMessage(configManager.getFormattedMessage("data_not_loaded"));
            return true;
        }
        
        // Display payday information
        displayPaydayInfo(player, paydayData);
        
        return true;
    }
    
    /**
     * Display detailed payday information to the player
     */
    private void displayPaydayInfo(Player player, PaydayData paydayData) {
        long requiredMinutes = configManager.getPaydayIntervalMinutes();
        long currentMinutes = paydayData.getMinutesPlayed();
        long remainingMinutes = paydayData.getRemainingMinutes(requiredMinutes);
        double progressPercentage = paydayData.getProgressPercentage(requiredMinutes);
        double pendingBalance = paydayData.getPendingBalance();
        int totalPaydays = paydayData.getTotalPaydays();
        
        // Create progress bar
        String progressBar = createProgressBar(progressPercentage);
        
        // Get the formatted message and replace placeholders
        String message = configManager.getMessage("payday_info")
            .replace("%time%", String.valueOf(currentMinutes))
            .replace("%required%", String.valueOf(requiredMinutes))
            .replace("%remaining%", String.valueOf(remainingMinutes))
            .replace("%percentage%", String.format("%.1f", progressPercentage))
            .replace("%pending%", String.format("%.2f", pendingBalance))
            .replace("%total_paydays%", String.valueOf(totalPaydays))
            .replace("%progress_bar%", progressBar)
            .replace("&", "§");
        
        player.sendMessage(message);
        
        // Additional information based on status
        if (paydayData.isReadyForPayday(requiredMinutes)) {
            player.sendMessage(configManager.getFormattedMessage("payday_ready"));
        } else if (pendingBalance <= 0) {
            player.sendMessage(configManager.getFormattedMessage("no_pending_earnings"));
        }
    }
    
    /**
     * Create a visual progress bar
     */
    private String createProgressBar(double percentage) {
        int totalBars = 20;
        int filledBars = (int) (percentage / 100.0 * totalBars);
        int emptyBars = totalBars - filledBars;
        
        StringBuilder progressBar = new StringBuilder("§a");
        
        // Add filled bars
        for (int i = 0; i < filledBars; i++) {
            progressBar.append("█");
        }
        
        // Add empty bars
        progressBar.append("§7");
        for (int i = 0; i < emptyBars; i++) {
            progressBar.append("█");
        }
        
        return progressBar.toString();
    }
    
    /**
     * Check if player is on cooldown
     */
    private boolean isOnCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!commandCooldowns.containsKey(playerUUID)) {
            return false;
        }
        
        long lastUsed = commandCooldowns.get(playerUUID);
        long cooldownTime = configManager.getPaydayCommandCooldown() * 1000L; // Convert to milliseconds
        
        return (System.currentTimeMillis() - lastUsed) < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    private long getRemainingCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        long lastUsed = commandCooldowns.get(playerUUID);
        long cooldownTime = configManager.getPaydayCommandCooldown() * 1000L;
        long elapsed = System.currentTimeMillis() - lastUsed;
        
        return Math.max(0, (cooldownTime - elapsed) / 1000);
    }
    
    /**
     * Set cooldown for player
     */
    private void setCooldown(Player player) {
        commandCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Clean up cooldowns for offline players
     */
    public void cleanupCooldowns() {
        long currentTime = System.currentTimeMillis();
        long maxCooldownTime = configManager.getPaydayCommandCooldown() * 1000L * 2; // Double the cooldown time
        
        commandCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxCooldownTime);
    }
}