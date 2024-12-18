package com.royalkingdoms.rokidivineprotection;

import java.util.UUID;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;

public class RokiDivineProtection extends PluginBase {
    private ChunkProtectionManager chunkProtectionManager;

    

    @Override
    public void onEnable() {
        chunkProtectionManager = new ChunkProtectionManager(this);
        getServer().getPluginManager().registerEvents(chunkProtectionManager, this);
        getLogger().info("RokiDivineProtection has been enabled!");

        startReminderTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("RokiDivineProtection has been disabled!");
        if (chunkProtectionManager != null) {
            chunkProtectionManager.saveProtectedChunks();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("protectland")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            return chunkProtectionManager.claimLand(player);
        }
        return false;
    }

    private void startReminderTask() {
        // Schedule a repeating task to send reminders about unprotected chunks
        getServer().getScheduler().scheduleRepeatingTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                long currentTime = System.currentTimeMillis();

                // Iterate through protected chunks
                for (ProtectedChunkData protectedChunk : chunkProtectionManager.getProtectedChunks()) {
                    // Check if protection is not active
                    if (!protectedChunk.isProtectionActive()) {
                        // Get the chunk owner
                        Player player = getServer().getPlayerExact(protectedChunk.getOwner().toString());

                        // Send reminder if player is online and not recently notified
                        if (player != null && 
                            currentTime - protectedChunk.getLastNotifiedTime() >= 300000) { // 5 minutes
                            
                            player.sendMessage(String.format(
                                "§cReminder: Your chunk at (%d, %d) in world '%s' is unprotected due to excess chests.",
                                protectedChunk.getChunkX(), 
                                protectedChunk.getChunkZ(), 
                                protectedChunk.getWorldName()
                            ));

                            // Update the last notification time
                            protectedChunk.setLastNotifiedTime(currentTime);
                        }
                    }
                }
            }
        }, 20 * 60); // Run every minute (20 ticks = 1 second)
    }
}