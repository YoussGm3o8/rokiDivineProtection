package com.royalkingdoms.rokidivineprotection;

public class TrustedPlayerPermissions {
    private boolean canBreakBlocks;
    private boolean canPlaceBlocks;
    private boolean canOpenChests;
    private boolean canOpenDoors;

    public TrustedPlayerPermissions(boolean canBreakBlocks, boolean canPlaceBlocks, boolean canOpenChests, boolean canOpenDoors) {
        this.canBreakBlocks = canBreakBlocks;
        this.canPlaceBlocks = canPlaceBlocks;
        this.canOpenChests = canOpenChests;
        this.canOpenDoors = canOpenDoors;
    }

    public boolean canBreakBlocks() {
        return canBreakBlocks;
    }

    public boolean canPlaceBlocks() {
        return canPlaceBlocks;
    }

    public boolean canOpenChests() {
        return canOpenChests;
    }

    public boolean canOpenDoors() {
        return canOpenDoors;
    }

    public void setCanBreakBlocks(boolean canBreakBlocks) {
        this.canBreakBlocks = canBreakBlocks;
    }

    public void setCanPlaceBlocks(boolean canPlaceBlocks) {
        this.canPlaceBlocks = canPlaceBlocks;
    }

    public void setCanOpenChests(boolean canOpenChests) {
        this.canOpenChests = canOpenChests;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }
}
