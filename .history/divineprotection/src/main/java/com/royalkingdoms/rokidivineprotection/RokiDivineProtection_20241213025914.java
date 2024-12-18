package com.royalkingdoms.rokidivineprotection;

import java.util.UUID;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;

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
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            long currentTime = System.currentTimeMillis();

            for (ProtectedChunkData protectedChunk : chunkProtectionManager.getProtectedChunks()) {
                if (!protectedChunk.isProtectionActive()) {
                    UUID ownerId = protectedChunk.getOwner();
                    Player player = getServer().getPlayer(ownerId);

                    if (player != null && currentTime - protectedChunk.getLastNotifiedTime() > 300000) { // 5 minutes
                        player.sendMessage(String.format(
                            "§cReminder: Your chunk at (%d, %d) in world '%s' is unprotected due to excess chests.",
                            protectedChunk.getChunkX(), protectedChunk.getChunkZ(), protectedChunk.getWorldName()
                        ));
                        protectedChunk.setLastNotifiedTime(currentTime);
                    }
                }
            }
        }, 20 * 60); // Run every minute
    }
}