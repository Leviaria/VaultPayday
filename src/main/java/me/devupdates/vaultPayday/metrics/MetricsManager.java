package me.devupdates.vaultPayday.metrics;

import me.devupdates.vaultPayday.VaultPayday;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Manages bStats metrics for the plugin
 */
public class MetricsManager {
    private final VaultPayday plugin;
    private final ConfigManager configManager;
    private final PaydayManager paydayManager;
    private Metrics metrics;
    
    // bStats plugin ID for VaultPayday
    private static final int PLUGIN_ID = 26751;
    
    public MetricsManager(VaultPayday plugin, ConfigManager configManager, PaydayManager paydayManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.paydayManager = paydayManager;
    }
    
    /**
     * Initialize bStats metrics
     */
    public void initialize() {
        try {
            metrics = new Metrics(plugin, PLUGIN_ID);
            
            // Add custom charts
            addCustomCharts();
            
            plugin.getLogger().info("bStats metrics initialized successfully! Plugin ID: " + PLUGIN_ID);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
            if (configManager.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Add custom charts to bStats
     */
    private void addCustomCharts() {
        // Chart: Storage Type
        metrics.addCustomChart(new SimplePie("storage_type", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.getStorageType().toLowerCase();
            }
        }));
        
        // Chart: Payday Interval
        metrics.addCustomChart(new SimplePie("payday_interval", new Callable<String>() {
            @Override
            public String call() throws Exception {
                long minutes = configManager.getPaydayIntervalMinutes();
                if (minutes <= 30) return "≤30 minutes";
                else if (minutes <= 60) return "31-60 minutes";
                else if (minutes <= 120) return "61-120 minutes";
                else return ">120 minutes";
            }
        }));
        
        // Chart: Message Type
        metrics.addCustomChart(new SimplePie("message_type", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.getMessageType().toLowerCase();
            }
        }));
        
        // Chart: Active Players
        metrics.addCustomChart(new SingleLineChart("active_players", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return paydayManager.getActivePlayersCount();
            }
        }));
        
        // Chart: Server Type
        metrics.addCustomChart(new SimplePie("server_type", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Bukkit.getName();
            }
        }));
        
        // Chart: Java Version
        metrics.addCustomChart(new SimplePie("java_version", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String version = System.getProperty("java.version");
                if (version.startsWith("1.8")) return "Java 8";
                else if (version.startsWith("11")) return "Java 11";
                else if (version.startsWith("17")) return "Java 17";
                else if (version.startsWith("21")) return "Java 21";
                else return "Java " + version.split("\\.")[0];
            }
        }));
        
        // Chart: Multipliers Enabled
        metrics.addCustomChart(new SimplePie("multipliers_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.isMultipliersEnabled() ? "Enabled" : "Disabled";
            }
        }));
        
        // Chart: PlaceholderAPI Integration
        metrics.addCustomChart(new SimplePie("placeholderapi_integration", new Callable<String>() {
            @Override
            public String call() throws Exception {
                boolean hasPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
                boolean enabled = configManager.isPlaceholderAPIEnabled();
                
                if (hasPlaceholderAPI && enabled) return "Enabled";
                else if (hasPlaceholderAPI && !enabled) return "Available but Disabled";
                else return "Not Available";
            }
        }));
        
        // Chart: JobsReborn Integration Status
        metrics.addCustomChart(new SimplePie("jobs_integration", new Callable<String>() {
            @Override
            public String call() throws Exception {
                boolean hasJobs = Bukkit.getPluginManager().getPlugin("Jobs") != null;
                return hasJobs ? "Integrated" : "Not Available";
            }
        }));
        
        // Chart: Online Player Count Range
        metrics.addCustomChart(new SimplePie("online_players_range", new Callable<String>() {
            @Override
            public String call() throws Exception {
                int count = Bukkit.getOnlinePlayers().size();
                if (count == 0) return "0 players";
                else if (count <= 10) return "1-10 players";
                else if (count <= 25) return "11-25 players";
                else if (count <= 50) return "26-50 players";
                else if (count <= 100) return "51-100 players";
                else if (count <= 200) return "101-200 players";
                else return "200+ players";
            }
        }));
        
        // Chart: Performance Settings
        metrics.addCustomChart(new SimplePie("performance_profile", new Callable<String>() {
            @Override
            public String call() throws Exception {
                int updateInterval = configManager.getTimeUpdateInterval();
                int maxPlayers = configManager.getMaxPlayersPerCycle();
                
                if (updateInterval <= 30 && maxPlayers >= 100) return "High Performance";
                else if (updateInterval <= 60 && maxPlayers >= 50) return "Balanced";
                else if (updateInterval <= 120 && maxPlayers >= 25) return "Conservative";
                else return "Custom";
            }
        }));
        
        // Chart: Feature Usage
        metrics.addCustomChart(new AdvancedPie("feature_usage", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> featureUsage = new HashMap<>();
                
                // Count servers using each feature
                if (configManager.isShowProgressCommand()) {
                    featureUsage.put("Payday Command", 1);
                }
                
                if (configManager.isMultipliersEnabled()) {
                    featureUsage.put("Multipliers", 1);
                }
                
                if (configManager.isPlaceholderAPIEnabled() && 
                    Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    featureUsage.put("PlaceholderAPI", 1);
                }
                
                if (Bukkit.getPluginManager().getPlugin("Jobs") != null) {
                    featureUsage.put("JobsReborn", 1);
                }
                
                if (configManager.isShowProgressNotifications()) {
                    featureUsage.put("Progress Notifications", 1);
                }
                
                // If no specific features, show basic usage
                if (featureUsage.isEmpty()) {
                    featureUsage.put("Basic Setup", 1);
                }
                
                return featureUsage;
            }
        }));
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Added " + 12 + " custom bStats charts");
        }
    }
    
    /**
     * Shutdown metrics
     */
    public void shutdown() {
        if (metrics != null) {
            // bStats doesn't need explicit shutdown
            plugin.getLogger().info("bStats metrics shutdown completed");
        }
    }
    
    /**
     * Check if metrics are enabled
     */
    public boolean isEnabled() {
        return metrics != null;
    }
}