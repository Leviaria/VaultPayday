package me.devupdates.vaultPayday;

import me.devupdates.vaultPayday.commands.PaydayCommand;
import me.devupdates.vaultPayday.commands.VaultPaydayCommand;
import me.devupdates.vaultPayday.data.DataManager;
import me.devupdates.vaultPayday.data.SQLiteDataManager;
import me.devupdates.vaultPayday.integration.JobsIntegrationManager;
import me.devupdates.vaultPayday.integration.PlaceholderAPIIntegration;
import me.devupdates.vaultPayday.manager.ConfigManager;
import me.devupdates.vaultPayday.manager.PaydayManager;
// import me.devupdates.vaultPayday.metrics.MetricsManager; // Temporarily disabled
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultPayday extends JavaPlugin implements Listener {
    
    // Core managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private PaydayManager paydayManager;
    private JobsIntegrationManager jobsIntegrationManager;
    private PlaceholderAPIIntegration placeholderAPIIntegration;
    // private MetricsManager metricsManager; // Temporarily disabled
    
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Starting VaultPayday v" + getPluginMeta().getVersion());
        
        // Check dependencies
        if (!checkDependencies()) {
            getLogger().severe("Required dependencies not found! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        try {
            initializeManagers();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register commands
        registerCommands();
        
        // Initialize metrics (bStats) - Temporarily disabled due to dependency issues
        /*
        try {
            metricsManager = new MetricsManager(this, configManager, paydayManager);
            metricsManager.initialize();
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
            if (configManager.isDebugMode()) {
                e.printStackTrace();
            }
        }
        */
        getLogger().info("bStats metrics temporarily disabled - will be re-enabled in next update");
        
        getLogger().info("VaultPayday enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Shutting down VaultPayday...");
        
        // Shutdown managers in reverse order
        /*
        if (metricsManager != null) {
            metricsManager.shutdown();
        }
        */
        
        if (placeholderAPIIntegration != null) {
            placeholderAPIIntegration.unregister();
            getLogger().info("PlaceholderAPI integration unregistered.");
        }
        
        if (jobsIntegrationManager != null) {
            jobsIntegrationManager.shutdown();
        }
        
        if (paydayManager != null) {
            paydayManager.shutdown();
        }
        
        if (dataManager != null) {
            dataManager.close().join(); // Wait for completion
        }
        
        getLogger().info("VaultPayday disabled successfully!");
    }
    
    /**
     * Check if required dependencies are available
     */
    private boolean checkDependencies() {
        // Check Vault
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin not found!");
            return false;
        }
        
        // Check Jobs
        if (getServer().getPluginManager().getPlugin("Jobs") == null) {
            getLogger().severe("Jobs plugin not found!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Initialize all plugin managers
     */
    private void initializeManagers() {
        // Initialize ConfigManager
        configManager = new ConfigManager(this);
        
        // Validate configuration
        if (!configManager.validateConfig()) {
            throw new RuntimeException("Invalid configuration detected!");
        }
        
        // Initialize DataManager based on config
        String storageType = configManager.getStorageType();
        if ("sqlite".equalsIgnoreCase(storageType)) {
            dataManager = new SQLiteDataManager(this, configManager.getSqliteFilename());
        } else {
            throw new RuntimeException("Unsupported storage type: " + storageType);
        }
        
        // Initialize database
        dataManager.initialize().join(); // Wait for completion
        
        // Initialize PaydayManager
        paydayManager = new PaydayManager(this, dataManager, configManager);
        paydayManager.initialize();
        
        // Initialize JobsIntegrationManager (only if Jobs plugin is available)
        if (getServer().getPluginManager().getPlugin("Jobs") != null) {
            jobsIntegrationManager = new JobsIntegrationManager(this, paydayManager, configManager);
            jobsIntegrationManager.initialize();
        } else {
            getLogger().warning("Jobs plugin not found! JobsReborn integration disabled.");
        }
        
        // Initialize PlaceholderAPI integration (only if PlaceholderAPI is available and enabled)
        if (configManager.isPlaceholderAPIEnabled() && 
            getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderAPIIntegration = new PlaceholderAPIIntegration(this, paydayManager, configManager);
                if (placeholderAPIIntegration.register()) {
                    getLogger().info("PlaceholderAPI integration registered successfully!");
                } else {
                    getLogger().warning("Failed to register PlaceholderAPI integration!");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to initialize PlaceholderAPI integration: " + e.getMessage());
                placeholderAPIIntegration = null;
            }
        } else {
            getLogger().info("PlaceholderAPI integration disabled or plugin not found.");
        }
        
        getLogger().info("All managers initialized successfully!");
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        // Register /payday command
        PaydayCommand paydayCommand = new PaydayCommand(this, paydayManager, configManager);
        getCommand("payday").setExecutor(paydayCommand);
        
        // Register /vaultpayday command
        VaultPaydayCommand vaultPaydayCommand = new VaultPaydayCommand(this, paydayManager, configManager);
        getCommand("vaultpayday").setExecutor(vaultPaydayCommand);
        getCommand("vaultpayday").setTabCompleter(vaultPaydayCommand);
        
        getLogger().info("Commands registered successfully!");
    }
    
    // Event handlers
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        paydayManager.onPlayerJoin(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        paydayManager.onPlayerLeave(event.getPlayer());
    }
    
    // Getters for managers
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public PaydayManager getPaydayManager() { return paydayManager; }
    public JobsIntegrationManager getJobsIntegrationManager() { return jobsIntegrationManager; }
    public PlaceholderAPIIntegration getPlaceholderAPIIntegration() { return placeholderAPIIntegration; }
    // public MetricsManager getMetricsManager() { return metricsManager; } // Temporarily disabled
}
