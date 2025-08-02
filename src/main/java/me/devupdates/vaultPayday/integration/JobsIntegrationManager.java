package me.devupdates.vaultPayday.integration;

import com.gamingmesh.jobs.api.JobsPaymentEvent;
import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Manages integration with JobsReborn plugin
 */
public class JobsIntegrationManager implements Listener {
    private final VaultPayday plugin;
    private final PaydayManager paydayManager;
    private final ConfigManager configManager;
    
    public JobsIntegrationManager(VaultPayday plugin, PaydayManager paydayManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.paydayManager = paydayManager;
        this.configManager = configManager;
    }
    
    /**
     * Initialize the Jobs integration
     */
    public void initialize() {
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("JobsReborn integration initialized successfully!");
    }
    
    /**
     * Handle JobsReborn payment events
     * This intercepts payments and adds them to pending balance instead
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJobsPayment(JobsPaymentEvent event) {
        OfflinePlayer offlinePlayer = event.getPlayer();
        
        // Check if player is online
        if (!offlinePlayer.isOnline()) {
            return; // Skip offline players
        }
        
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return; // Safety check
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("vaultpayday.bypass")) {
            // Allow normal Jobs payment
            return;
        }
        
        // Check minimum payment threshold
        double paymentAmount = 0.0;
        try {
            paymentAmount = event.get(com.gamingmesh.jobs.container.CurrencyType.MONEY);
        } catch (Exception e) {
            // Fallback: try to get payment amount from different method
            if (configManager.isDebugMode()) {
                plugin.getLogger().warning("Could not get payment amount from JobsPaymentEvent: " + e.getMessage());
            }
            return;
        }
        
        if (paymentAmount < configManager.getMinimumPayment()) {
            // Allow small payments through normally
            return;
        }
        
        // For now, intercept all jobs since we can't reliably get job name
        // This can be improved later when we have access to job information
        String jobName = "Unknown";
        if (!shouldInterceptJob(jobName)) {
            // Allow normal payment for this job
            return;
        }
        
        // Check world restrictions
        String worldName = player.getWorld().getName();
        if (!isPaydayEnabledInWorld(worldName)) {
            // Allow normal payment in this world
            return;
        }
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Intercepting Jobs payment: " + player.getName() + 
                " - Amount: $" + String.format("%.2f", paymentAmount));
        }
        
        // Cancel the original payment
        event.setCancelled(true);
        
        // Add to pending payday balance
        UUID playerUUID = player.getUniqueId();
        paydayManager.addPendingBalance(playerUUID, paymentAmount);
        
        // Optional: Send notification to player
        if (configManager.isShowProgressNotifications()) {
            sendInterceptionNotification(player, paymentAmount);
        }
    }
    
    /**
     * Check if the job should be intercepted based on configuration
     */
    private boolean shouldInterceptJob(String jobName) {
        if (configManager.isInterceptAllPayments()) {
            return true;
        }
        
        // Check specific jobs list - for now, since we can't get job name reliably,
        // we'll intercept all payments when intercept_all_payments is true
        return configManager.getSpecificJobs().isEmpty() || configManager.getSpecificJobs().contains(jobName);
    }
    
    /**
     * Check if payday system is enabled in the given world
     */
    private boolean isPaydayEnabledInWorld(String worldName) {
        // Check if using whitelist mode
        if (configManager.isWorldWhitelistMode()) {
            return configManager.getWhitelistedWorlds().contains(worldName);
        }
        
        // Check blacklist
        return !configManager.getBlacklistedWorlds().contains(worldName);
    }
    
    /**
     * Send notification to player about payment interception
     */
    private void sendInterceptionNotification(Player player, double amount) {
        // Get current pending balance for progress info
        UUID playerUUID = player.getUniqueId();
        var paydayData = paydayManager.getPlayerData(playerUUID);
        
        if (paydayData != null) {
            String message = configManager.getMessage("payment_intercepted")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%pending%", String.format("%.2f", paydayData.getPendingBalance()))
                .replace("%time%", String.valueOf(paydayData.getMinutesPlayed()))
                .replace("%required%", String.valueOf(configManager.getPaydayIntervalMinutes()))
                .replace("%remaining%", String.valueOf(paydayData.getRemainingMinutes(configManager.getPaydayIntervalMinutes())));
            
            // Send as action bar with proper color codes
            String fullMessage = configManager.getMessagePrefix() + message;
            String coloredMessage = fullMessage.replace("&", "§");
            player.sendActionBar(net.kyori.adventure.text.Component.text(coloredMessage));
        }
    }
    
    /**
     * Shutdown the Jobs integration
     */
    public void shutdown() {
        // Unregister events
        JobsPaymentEvent.getHandlerList().unregister(this);
        
        plugin.getLogger().info("JobsReborn integration shutdown completed");
    }
}