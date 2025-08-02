package me.devupdates.vaultPayday.data;

import me.devupdates.vaultPayday.model.PaydayData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for data storage operations
 */
public interface DataManager {
    
    /**
     * Initialize the data storage system
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Load player data from storage
     * @param playerUUID Player's UUID
     * @param playerName Player's name
     * @return CompletableFuture containing the PaydayData or null if not found
     */
    CompletableFuture<PaydayData> loadPlayerData(UUID playerUUID, String playerName);
    
    /**
     * Save player data to storage
     * @param data PaydayData to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerData(PaydayData data);
    
    /**
     * Delete player data from storage
     * @param playerUUID Player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deletePlayerData(UUID playerUUID);
    
    /**
     * Get total number of players in the system
     * @return CompletableFuture containing the count
     */
    CompletableFuture<Integer> getTotalPlayersCount();
    
    /**
     * Get total number of pending payouts
     * @return CompletableFuture containing the count
     */
    CompletableFuture<Integer> getPendingPayoutsCount();
    
    /**
     * Get total number of paydays given
     * @return CompletableFuture containing the count
     */
    CompletableFuture<Long> getTotalPaydaysGiven();
    
    /**
     * Close/cleanup the data storage system
     * @return CompletableFuture that completes when cleanup is done
     */
    CompletableFuture<Void> close();
    
    /**
     * Create a backup of the data
     * @return CompletableFuture that completes when backup is done
     */
    CompletableFuture<Boolean> createBackup();
}