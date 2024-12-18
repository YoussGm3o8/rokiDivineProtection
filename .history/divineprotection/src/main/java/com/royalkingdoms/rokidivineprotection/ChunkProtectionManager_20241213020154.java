package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.level.Position;
import java.util.UUID;

public class ProtectedChunkData {
    private UUID owner;
    private Position chunkPosition;
    private int chestCount;

    public ProtectedChunkData(UUID owner, Position chunkPosition) {
        this.owner = owner;
        this.chunkPosition = chunkPosition;
        this.chestCount = 0;
    }

    public UUID getOwner() {
        return owner;
    }

    public Position getChunkPosition() {
        return chunkPosition;
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