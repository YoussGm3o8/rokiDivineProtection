package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.level.Position;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.checkerframework.checker.units.qual.C;

public class ProtectedChunkData {
    private UUID owner;
    private String worldName;
    private int chunkX;
    private int chunkZ;
    private boolean protectionActive = true; // Default to active
    private long lastAccessTime;
    private int storageCount = 0;
    private static final int MAX_STORAGE = 2;
    private static final long CLAIM_EXPIRATION_PERIOD = 14 * 24 * 60 * 60 * 1000L;
    private Set<WarningType> sentWarnings = EnumSet.noneOf(WarningType.class);

    public enum WarningType {
        SEVEN_DAYS,
        THREE_DAYS,
        ONE_DAY
    }

    public boolean hasWarningBeenSent(WarningType warningType) {
        return sentWarnings.contains(warningType);
    }

    public void markWarningSent(WarningType warningType) {
        sentWarnings.add(warningType);
    }

    public void resetWarnings() {
        sentWarnings.clear();
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
        resetWarnings(); // Reset warnings when chunk is accessed
    }

    public long getLastAccessTime() {
        return this.lastAccessTime;
    }

    public ProtectedChunkData(UUID owner, Position chunkPosition) {
        this.owner = owner;
        this.worldName = chunkPosition.level.getName();
        this.chunkX = (int) Math.floor(chunkPosition.x / 16);
        this.chunkZ = (int) Math.floor(chunkPosition.z / 16);
        this.lastAccessTime = System.currentTimeMillis();
    }


    public boolean hasExpired() {
        return System.currentTimeMillis() - lastAccessTime > CLAIM_EXPIRATION_PERIOD;
    }

    public ProtectedChunkData(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.storageCount = 0;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getStorageCount() {
        return storageCount;
    }

    public boolean isProtectionActive() {
        return protectionActive;
    }

    public void setProtectionActive(boolean active) {
        this.protectionActive = active;
    }

    public void incrementStorageCount() {
        storageCount++;
        if (storageCount > MAX_STORAGE) {
            setProtectionActive(false);
        }
    }

    public void decrementStorageCount() {
        storageCount = Math.max(0, storageCount - 1);
        if (storageCount <= MAX_STORAGE && !protectionActive) {
            setProtectionActive(true);
        }
    }

    public boolean isOverChestLimit() {
        return storageCount > MAX_STORAGE;
    }

    public long getLastNotifiedTime() {
        return lastAccessTime;
    }
    
    public void setLastNotifiedTime(long time) {
        this.lastAccessTime = time;
    }

    public String getTimeRemaining() {
        long currentTime = System.currentTimeMillis();
        long timeLeft = (lastAccessTime + CLAIM_EXPIRATION_PERIOD) - currentTime;
    
        if (timeLeft <= 0) {
            return "Expired";
        }
    
        long days = timeLeft / (1000 * 60 * 60 * 24);
        long hours = (timeLeft / (1000 * 60 * 60)) % 24;
        long minutes = (timeLeft / (1000 * 60)) % 60;
    
        if (days > 0) {
            return String.format("%dd, %dh, %dm", days, hours, minutes);
        } else {
            return String.format("%dh, %dm", hours, minutes);
        }
    }
    

    public void setStorageCount(int storageBlockCount) {
        this.storageCount = storageBlockCount;
    }
}