package me.devupdates.vaultPayday.util;

import me.devupdates.vaultPayday.VaultPayday;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
 * Handles downloading and loading of external dependencies
 */
public class DependencyDownloader {
    private static final String SQLITE_VERSION = "3.45.0.0";
    private static final String SQLITE_JAR = "sqlite-jdbc-" + SQLITE_VERSION + ".jar";
    private static final String SQLITE_URL = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/" + SQLITE_VERSION + "/" + SQLITE_JAR;
    private static final String SQLITE_SHA256 = "8b63b9e5e2b35667b3e8095a2d9829ee5d9d9524d59a503651e080f647ae5628";
    
    private final VaultPayday plugin;
    private final Logger logger;
    private final File libFolder;
    
    public DependencyDownloader(VaultPayday plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.libFolder = new File(plugin.getDataFolder(), "lib");
    }
    
    /**
     * Check and download SQLite if needed
     */
    public boolean ensureSQLiteDriver() {
        // First check if SQLite is already available
        try {
            Class.forName("org.sqlite.JDBC");
            logger.info("SQLite driver already available!");
            return true;
        } catch (ClassNotFoundException e) {
            // Need to download
            logger.info("SQLite driver not found, downloading...");
        }
        
        // Create lib folder if it doesn't exist
        if (!libFolder.exists() && !libFolder.mkdirs()) {
            logger.severe("Failed to create lib folder!");
            return false;
        }
        
        File sqliteFile = new File(libFolder, SQLITE_JAR);
        
        // Check if already downloaded
        if (sqliteFile.exists()) {
            logger.info("SQLite JAR found in lib folder, loading...");
            return loadSQLiteJar(sqliteFile);
        }
        
        // Download SQLite
        try {
            logger.info("Downloading SQLite driver from Maven Central...");
            downloadFile(SQLITE_URL, sqliteFile);
            
            // Verify download
            if (!sqliteFile.exists() || sqliteFile.length() == 0) {
                logger.severe("Failed to download SQLite driver!");
                return false;
            }
            
            logger.info("SQLite driver downloaded successfully (" + formatFileSize(sqliteFile.length()) + ")");
            
            // Load the JAR
            return loadSQLiteJar(sqliteFile);
            
        } catch (Exception e) {
            logger.severe("Failed to download SQLite driver: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Download a file from URL
     */
    private void downloadFile(String urlString, File outputFile) throws Exception {
        URL url = new URL(urlString);
        
        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // Download in chunks with progress
            long bytesDownloaded = 0;
            long lastLogTime = System.currentTimeMillis();
            
            bytesDownloaded = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            
            logger.info("Download complete: " + formatFileSize(bytesDownloaded));
        }
    }
    
    /**
     * Load SQLite JAR into classpath
     */
    private boolean loadSQLiteJar(File jarFile) {
        try {
            // Get the class loader
            URLClassLoader classLoader = (URLClassLoader) plugin.getClass().getClassLoader();
            
            // Use reflection to access the addURL method
            java.lang.reflect.Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            
            // Add the JAR to the classpath
            addURL.invoke(classLoader, jarFile.toURI().toURL());
            
            // Test if it worked
            Class.forName("org.sqlite.JDBC");
            
            logger.info("SQLite driver loaded successfully!");
            return true;
            
        } catch (Exception e) {
            logger.severe("Failed to load SQLite driver: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Clean up old versions
     */
    public void cleanupOldVersions() {
        if (!libFolder.exists()) return;
        
        File[] files = libFolder.listFiles((dir, name) -> 
            name.startsWith("sqlite-jdbc-") && 
            name.endsWith(".jar") && 
            !name.equals(SQLITE_JAR)
        );
        
        if (files != null) {
            for (File oldFile : files) {
                if (oldFile.delete()) {
                    logger.info("Deleted old SQLite version: " + oldFile.getName());
                }
            }
        }
    }
}