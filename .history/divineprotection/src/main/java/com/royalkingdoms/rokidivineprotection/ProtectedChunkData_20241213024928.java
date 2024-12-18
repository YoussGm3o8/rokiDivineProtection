package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.level.Position;
import java.util.UUID;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

public class ProtectedChunkData {
    private UUID owner;
    private String worldName;
    private int chunkX;
    private int chunkZ;
    private boolean protectionActive = true; // Default to active
    private int chestCount = 0;
    private static final int MAX_CHESTS = 2;

    public ProtectedChunkData(UUID owner, Position chunkPosition) {
        this.owner = owner;
        this.worldName = chunkPosition.level.getName();
        this.chunkX = (int) Math.floor(chunkPosition.x / 16);
        this.chunkZ = (int) Math.floor(chunkPosition.z / 16);
    }

    public ProtectedChunkData(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chestCount = 0;
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

    public int getChestCount() {
        return chestCount;
    }

    public boolean isProtectionActive() {
        return protectionActive;
    }

    public void setProtectionActive(boolean active) {
        this.protectionActive = active;
    }

    public void incrementChestCount() {
        chestCount++;
        if (chestCount > MAX_CHESTS) {
            setProtectionActive(false);
        }
    }

    public void decrementChestCount() {
        chestCount = Math.max(0, chestCount - 1);
        if (chestCount <= MAX_CHESTS && !protectionActive) {
            setProtectionActive(true);
        }
    }

    public boolean isOverChestLimit() {
        return chestCount > MAX_CHESTS;
    }
}