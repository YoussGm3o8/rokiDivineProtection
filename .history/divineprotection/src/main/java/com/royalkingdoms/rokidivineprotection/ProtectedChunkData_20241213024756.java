package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.level.Position;
import java.util.UUID;

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
        this.chestCount = 0;
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

    public void incrementChestCount() {
        chestCount++;
    }

    public void decrementChestCount() {
        if (chestCount > 0) {
            chestCount--;
        }
    }

    public boolean isOverChestLimit() {
        return chestCount > 2;
    }
}