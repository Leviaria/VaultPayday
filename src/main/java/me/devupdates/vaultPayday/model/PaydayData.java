package me.devupdates.vaultPayday.model;

import java.util.UUID;

/**
 * Data model representing a player's payday information
 */
public class PaydayData {
    private final UUID playerUUID;
    private String playerName;
    private long minutesPlayed;
    private double pendingBalance;
    private long lastUpdated;
    private int totalPaydays;
    
    public PaydayData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.minutesPlayed = 0;
        this.pendingBalance = 0.0;
        this.lastUpdated = System.currentTimeMillis();
        this.totalPaydays = 0;
    }
    
    public PaydayData(UUID playerUUID, String playerName, long minutesPlayed, 
                     double pendingBalance, long lastUpdated, int totalPaydays) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.minutesPlayed = minutesPlayed;
        this.pendingBalance = pendingBalance;
        this.lastUpdated = lastUpdated;
        this.totalPaydays = totalPaydays;
    }
    
    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getMinutesPlayed() { return minutesPlayed; }
    public double getPendingBalance() { return pendingBalance; }
    public long getLastUpdated() { return lastUpdated; }
    public int getTotalPaydays() { return totalPaydays; }
    
    // Setters
    public void setPlayerName(String playerName) { 
        this.playerName = playerName; 
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setMinutesPlayed(long minutesPlayed) { 
        this.minutesPlayed = minutesPlayed; 
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setPendingBalance(double pendingBalance) { 
        this.pendingBalance = pendingBalance; 
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setLastUpdated(long lastUpdated) { 
        this.lastUpdated = lastUpdated; 
    }
    
    public void setTotalPaydays(int totalPaydays) { 
        this.totalPaydays = totalPaydays; 
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Utility methods
    public void addMinute() {
        this.minutesPlayed++;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addMinutes(long minutes) {
        this.minutesPlayed += minutes;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addPendingBalance(double amount) {
        this.pendingBalance += amount;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void resetPaydayCycle() {
        this.minutesPlayed = 0;
        this.pendingBalance = 0.0;
        this.totalPaydays++;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public boolean isReadyForPayday(long requiredMinutes) {
        return this.minutesPlayed >= requiredMinutes;
    }
    
    public long getRemainingMinutes(long requiredMinutes) {
        return Math.max(0, requiredMinutes - this.minutesPlayed);
    }
    
    public double getProgressPercentage(long requiredMinutes) {
        if (requiredMinutes <= 0) return 100.0;
        return Math.min(100.0, (double) this.minutesPlayed / requiredMinutes * 100.0);
    }
    
    @Override
    public String toString() {
        return "PaydayData{" +
                "playerUUID=" + playerUUID +
                ", playerName='" + playerName + '\'' +
                ", minutesPlayed=" + minutesPlayed +
                ", pendingBalance=" + pendingBalance +
                ", lastUpdated=" + lastUpdated +
                ", totalPaydays=" + totalPaydays +
                '}';
    }
}