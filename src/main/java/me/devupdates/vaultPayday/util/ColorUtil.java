package me.devupdates.vaultPayday.util;

import org.bukkit.ChatColor;

/**
 * Utility class for handling color codes
 */
public class ColorUtil {
    
    /**
     * Convert & color codes to § color codes
     * @param text The text with & color codes
     * @return Text with § color codes
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Remove all color codes from text
     * @param text The text with color codes
     * @return Plain text without color codes
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }
    
    /**
     * Format a message with color codes and placeholder replacement
     * @param message The message template
     * @param replacements Pairs of placeholder and replacement (placeholder1, replacement1, placeholder2, replacement2, ...)
     * @return Formatted and colorized message
     */
    public static String formatMessage(String message, String... replacements) {
        if (message == null) return "";
        
        String result = message;
        
        // Apply replacements
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        
        // Apply color codes
        return colorize(result);
    }
}