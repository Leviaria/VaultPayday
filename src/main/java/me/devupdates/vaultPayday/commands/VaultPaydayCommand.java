package me.devupdates.vaultPayday.commands;

import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
import me.devupdates.vaultPayday.model.PaydayData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the /vaultpayday admin command with subcommands
 */
public class VaultPaydayCommand implements CommandExecutor, TabCompleter {
    private final VaultPayday plugin;
    private final PaydayManager paydayManager;
    private final ConfigManager configManager;
    
    public VaultPaydayCommand(VaultPayday plugin, PaydayManager paydayManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.paydayManager = paydayManager;
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission("vaultpayday.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no_permission"));
            return true;
        }
        
        // Show help if no arguments
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
                
            case "reset":
                handleReset(sender, args);
                break;
                
            case "settime":
                handleSetTime(sender, args);
                break;
                
            case "info":
                handleInfo(sender, args);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            case "help":
                showHelp(sender);
                break;
                
            default:
                sender.sendMessage(configManager.getFormattedMessage("unknown_subcommand")
                    .replace("%subcommand%", subCommand));
                showHelp(sender);
        }
        
        return true;
    }
    
    /**
     * Handle reload subcommand
     */
    private void handleReload(CommandSender sender) {
        try {
            configManager.reloadConfig();
            sender.sendMessage(configManager.getFormattedMessage("reload_success"));
            
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Configuration reloaded by " + sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage(configManager.getFormattedMessage("reload_error"));
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
        }
    }
    
    /**
     * Handle reset subcommand
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /vaultpayday reset <player>");
            return;
        }
        
        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(targetPlayerName);
        
        if (targetPlayer == null) {
            sender.sendMessage(configManager.getFormattedMessage("player_not_found"));
            return;
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();
        paydayManager.resetPlayerProgress(targetUUID);
        
        String message = configManager.getFormattedMessage("player_reset")
            .replace("%player%", targetPlayer.getName());
        sender.sendMessage(message);
        
        // Notify target player if online
        if (targetPlayer.isOnline()) {
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.sendMessage(configManager.getFormattedMessage("progress_reset_notification"));
            }
        }
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Reset payday progress for " + targetPlayer.getName() + " by " + sender.getName());
        }
    }
    
    /**
     * Handle settime subcommand
     */
    private void handleSetTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /vaultpayday settime <player> <minutes>");
            return;
        }
        
        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(targetPlayerName);
        
        if (targetPlayer == null) {
            sender.sendMessage(configManager.getFormattedMessage("player_not_found"));
            return;
        }
        
        long minutes;
        try {
            minutes = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getFormattedMessage("invalid_number"));
            return;
        }
        
        // Validate time range
        long maxMinutes = configManager.getPaydayIntervalMinutes();
        if (minutes < 0 || minutes > maxMinutes) {
            String message = configManager.getFormattedMessage("invalid_time")
                .replace("%max%", String.valueOf(maxMinutes));
            sender.sendMessage(message);
            return;
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();
        paydayManager.setPlayerTime(targetUUID, minutes);
        
        String message = configManager.getFormattedMessage("time_set")
            .replace("%player%", targetPlayer.getName())
            .replace("%time%", String.valueOf(minutes));
        sender.sendMessage(message);
        
        // Notify target player if online
        if (targetPlayer.isOnline()) {
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null) {
                String notification = configManager.getFormattedMessage("time_updated_notification")
                    .replace("%time%", String.valueOf(minutes));
                onlineTarget.sendMessage(notification);
            }
        }
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Set playtime for " + targetPlayer.getName() + " to " + minutes + " minutes by " + sender.getName());
        }
    }
    
    /**
     * Handle info subcommand
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /vaultpayday info <player>");
            return;
        }
        
        String targetPlayerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(targetPlayerName);
        
        if (targetPlayer == null) {
            sender.sendMessage(configManager.getFormattedMessage("player_not_found"));
            return;
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();
        PaydayData paydayData = paydayManager.getPlayerData(targetUUID);
        
        if (paydayData == null) {
            sender.sendMessage(configManager.getFormattedMessage("player_data_not_found"));
            return;
        }
        
        // Display detailed player information
        displayPlayerInfo(sender, targetPlayer, paydayData);
    }
    
    /**
     * Handle stats subcommand
     */
    private void handleStats(CommandSender sender) {
        // Get statistics asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                int trackedPlayers = plugin.getDataManager().getTotalPlayersCount().get();
                int pendingPayouts = plugin.getDataManager().getPendingPayoutsCount().get();
                long totalPaydays = plugin.getDataManager().getTotalPaydaysGiven().get();
                
                // Send statistics on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String message = configManager.getMessage("plugin_info")
                        .replace("%version%", plugin.getPluginMeta().getVersion())
                        .replace("%online_players%", String.valueOf(onlinePlayers))
                        .replace("%tracked_players%", String.valueOf(trackedPlayers))
                        .replace("%pending_payouts%", String.valueOf(pendingPayouts))
                        .replace("%total_paydays%", String.valueOf(totalPaydays))
                        .replace("&", "§");
                    
                    sender.sendMessage(message);
                });
                
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(configManager.getFormattedMessage("stats_error"));
                });
                plugin.getLogger().severe("Failed to retrieve statistics: " + e.getMessage());
            }
        });
    }
    
    /**
     * Display detailed player information
     */
    private void displayPlayerInfo(CommandSender sender, OfflinePlayer targetPlayer, PaydayData paydayData) {
        long requiredMinutes = configManager.getPaydayIntervalMinutes();
        long currentMinutes = paydayData.getMinutesPlayed();
        long remainingMinutes = paydayData.getRemainingMinutes(requiredMinutes);
        double progressPercentage = paydayData.getProgressPercentage(requiredMinutes);
        double pendingBalance = paydayData.getPendingBalance();
        int totalPaydays = paydayData.getTotalPaydays();
        
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬ §6Player Info: " + targetPlayer.getName() + " §8▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§7UUID: §e" + targetPlayer.getUniqueId());
        sender.sendMessage("§7Status: " + (targetPlayer.isOnline() ? "§aOnline" : "§cOffline"));
        sender.sendMessage("§7Time Played: §e" + currentMinutes + "§7/§e" + requiredMinutes + " §7minutes §8(§a" + String.format("%.1f", progressPercentage) + "%§8)");
        sender.sendMessage("§7Pending Balance: §a$" + String.format("%.2f", pendingBalance));
        sender.sendMessage("§7Remaining Time: §e" + remainingMinutes + " §7minutes");
        sender.sendMessage("§7Total Paydays: §e" + totalPaydays);
        sender.sendMessage("§7Last Updated: §e" + new java.util.Date(paydayData.getLastUpdated()));
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Show help message
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬ §6VaultPayday Commands §8▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§e/vaultpayday reload §7- Reload plugin configuration");
        sender.sendMessage("§e/vaultpayday reset <player> §7- Reset player's payday progress");
        sender.sendMessage("§e/vaultpayday settime <player> <minutes> §7- Set player's playtime");
        sender.sendMessage("§e/vaultpayday info <player> §7- Show detailed player information");
        sender.sendMessage("§e/vaultpayday stats §7- Show plugin statistics");
        sender.sendMessage("§e/vaultpayday help §7- Show this help message");
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vaultpayday.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("reload", "reset", "settime", "info", "stats", "help");
            return subCommands.stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || 
                                args[0].equalsIgnoreCase("settime") || 
                                args[0].equalsIgnoreCase("info"))) {
            // Second argument - player names for commands that require a player
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("settime")) {
            // Third argument for settime - suggest some common values
            return Arrays.asList("0", "15", "30", "45", "60");
        }
        
        return new ArrayList<>();
    }
}