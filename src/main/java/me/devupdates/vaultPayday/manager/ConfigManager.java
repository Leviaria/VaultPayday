package me.devupdates.vaultPayday.manager;

import me.devupdates.vaultPayday.VaultPayday;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages plugin configuration and provides easy access to settings
 */
public class ConfigManager {
    private final VaultPayday plugin;
    private FileConfiguration config;
    
    // Cached configuration values
    private long paydayIntervalMinutes;
    private String messageType;
    private boolean showProgressCommand;
    private int paydayCommandCooldown;
    private String storageType;
    private String sqliteFilename;
    private int yamlSaveInterval;
    private boolean autoSaveOnEvents;
    private int timeUpdateInterval;
    private int maxPlayersPerCycle;
    private boolean multipliersEnabled;
    private Map<String, Double> permissionMultipliers;
    private Map<String, Double> jobMultipliers;
    private boolean placeholderAPIEnabled;
    private boolean interceptAllPayments;
    private List<String> specificJobs;
    private double minimumPayment;
    private String messagePrefix;
    private boolean debugMode;
    private boolean showProgressNotifications;
    private boolean worldWhitelistMode;
    private List<String> whitelistedWorlds;
    private List<String> blacklistedWorlds;
    
    public ConfigManager(VaultPayday plugin) {
        this.plugin = plugin;
        this.permissionMultipliers = new HashMap<>();
        this.jobMultipliers = new HashMap<>();
        loadConfig();
    }
    
    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load and cache configuration values
        cacheConfigValues();
        
        plugin.getLogger().info("Configuration loaded successfully!");
        if (debugMode) {
            plugin.getLogger().info("Debug mode enabled - verbose logging active");
        }
    }
    
    private void cacheConfigValues() {
        // Payday system settings
        paydayIntervalMinutes = config.getLong("payday_interval_minutes", 60);
        messageType = config.getString("notifications.message_type", "chat");
        showProgressCommand = config.getBoolean("commands.show_progress_command", true);
        paydayCommandCooldown = config.getInt("commands.payday_command_cooldown", 30);
        
        // Storage settings
        storageType = config.getString("storage.type", "sqlite");
        sqliteFilename = config.getString("storage.sqlite_filename", "payday_data.db");
        yamlSaveInterval = config.getInt("storage.yaml_save_interval", 300);
        autoSaveOnEvents = config.getBoolean("storage.auto_save_on_events", true);
        
        // Performance settings
        timeUpdateInterval = config.getInt("performance.time_update_interval", 60);
        maxPlayersPerCycle = config.getInt("performance.max_players_per_cycle", 50);
        
        // Multiplier settings
        multipliersEnabled = config.getBoolean("multipliers.enabled", true);
        loadMultipliers();
        
        // Integration settings
        placeholderAPIEnabled = config.getBoolean("integrations.placeholderapi.enabled", true);
        interceptAllPayments = config.getBoolean("integrations.jobs.intercept_all_payments", true);
        specificJobs = config.getStringList("integrations.jobs.specific_jobs");
        minimumPayment = config.getDouble("integrations.jobs.minimum_payment", 0.01);
        
        // Message settings
        messagePrefix = config.getString("messages.prefix", "&8[&6VaultPayday&8] &r");
        
        // Advanced settings
        debugMode = config.getBoolean("advanced.debug", false);
        
        // Notification settings
        showProgressNotifications = config.getBoolean("notifications.show_progress", true);
        
        // World settings
        worldWhitelistMode = config.getBoolean("advanced.worlds.whitelist_mode", false);
        whitelistedWorlds = config.getStringList("advanced.worlds.whitelisted_worlds");
        blacklistedWorlds = config.getStringList("advanced.worlds.blacklisted_worlds");
    }
    
    private void loadMultipliers() {
        permissionMultipliers.clear();
        jobMultipliers.clear();
        
        if (multipliersEnabled) {
            // Load permission multipliers
            if (config.isConfigurationSection("multipliers.permissions")) {
                Set<String> permissions = config.getConfigurationSection("multipliers.permissions").getKeys(false);
                for (String permission : permissions) {
                    double multiplier = config.getDouble("multipliers.permissions." + permission, 1.0);
                    permissionMultipliers.put(permission, multiplier);
                }
            }
            
            // Load job multipliers
            if (config.isConfigurationSection("multipliers.jobs")) {
                Set<String> jobs = config.getConfigurationSection("multipliers.jobs").getKeys(false);
                for (String job : jobs) {
                    double multiplier = config.getDouble("multipliers.jobs." + job, 1.0);
                    jobMultipliers.put(job, multiplier);
                }
            }
        }
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    // Getter methods for cached values
    public long getPaydayIntervalMinutes() { return paydayIntervalMinutes; }
    public String getMessageType() { return messageType; }
    public boolean isShowProgressCommand() { return showProgressCommand; }
    public int getPaydayCommandCooldown() { return paydayCommandCooldown; }
    public String getStorageType() { return storageType; }
    public String getSqliteFilename() { return sqliteFilename; }
    public int getYamlSaveInterval() { return yamlSaveInterval; }
    public boolean isAutoSaveOnEvents() { return autoSaveOnEvents; }
    public int getTimeUpdateInterval() { return timeUpdateInterval; }
    public int getMaxPlayersPerCycle() { return maxPlayersPerCycle; }
    public boolean isMultipliersEnabled() { return multipliersEnabled; }
    public Map<String, Double> getPermissionMultipliers() { return new HashMap<>(permissionMultipliers); }
    public Map<String, Double> getJobMultipliers() { return new HashMap<>(jobMultipliers); }
    public boolean isPlaceholderAPIEnabled() { return placeholderAPIEnabled; }
    public boolean isInterceptAllPayments() { return interceptAllPayments; }
    public List<String> getSpecificJobs() { return specificJobs; }
    public double getMinimumPayment() { return minimumPayment; }
    public String getMessagePrefix() { return messagePrefix; }
    public boolean isDebugMode() { return debugMode; }
    public boolean isShowProgressNotifications() { return showProgressNotifications; }
    public boolean isWorldWhitelistMode() { return worldWhitelistMode; }
    public List<String> getWhitelistedWorlds() { return whitelistedWorlds; }
    public List<String> getBlacklistedWorlds() { return blacklistedWorlds; }
    
    // Message retrieval methods
    public String getMessage(String key) {
        return config.getString("messages." + key, "Message not found: " + key);
    }
    
    public String getFormattedMessage(String key) {
        String message = getMessage(key);
        return (messagePrefix + message).replace("&", "§");
    }
    
    /**
     * Get message with color codes converted but without prefix
     * @param key Message key
     * @return Colored message without prefix
     */
    public String getColoredMessage(String key) {
        return getMessage(key).replace("&", "§");
    }
    
    // Configuration validation
    public boolean validateConfig() {
        boolean valid = true;
        
        if (paydayIntervalMinutes <= 0) {
            plugin.getLogger().warning("payday_interval_minutes must be greater than 0!");
            valid = false;
        }
        
        if (!storageType.equals("sqlite") && !storageType.equals("yaml")) {
            plugin.getLogger().warning("storage.type must be either 'sqlite' or 'yaml'!");
            valid = false;
        }
        
        if (timeUpdateInterval <= 0) {
            plugin.getLogger().warning("performance.time_update_interval must be greater than 0!");
            valid = false;
        }
        
        if (maxPlayersPerCycle <= 0) {
            plugin.getLogger().warning("performance.max_players_per_cycle must be greater than 0!");
            valid = false;
        }
        
        return valid;
    }
}