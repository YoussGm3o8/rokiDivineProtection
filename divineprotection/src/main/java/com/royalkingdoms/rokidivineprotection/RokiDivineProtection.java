package com.royalkingdoms.rokidivineprotection;

import java.util.List;
import java.util.UUID;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import com.royalkingdoms.rokidivineprotection.gui.ProtectionGUI;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.window.FormWindowSimple;


public class RokiDivineProtection extends PluginBase implements Listener {
    private ChunkProtectionManager chunkProtectionManager;
    private ProtectionGUI protectionGUI;

    @Override
    public void onEnable() {
        chunkProtectionManager = new ChunkProtectionManager(this);
        protectionGUI = new ProtectionGUI(chunkProtectionManager);

        getServer().getPluginManager().registerEvents(chunkProtectionManager, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("RokiDivineProtection has been enabled!");

        startReminderTask();
        startChunkExpirationTask();
    }

    public ChunkProtectionManager getChunkProtectionManager() {
        return chunkProtectionManager;
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

        if (command.getName().equalsIgnoreCase("findprotectedchunks")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
    
            Player player = (Player) sender;
            listProtectedChunks(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("protectland")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            return chunkProtectionManager.claimLand(player);
        }

        if (command.getName().equalsIgnoreCase("unprotectland")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            return unprotectLand(player);
        }

        if (command.getName().equalsIgnoreCase("divineprotection")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            protectionGUI.showProtectedChunks(player);
            return true;
        }

        return false;
    }

    private boolean unprotectLand(Player player) {
        String chunkKey = chunkProtectionManager.getChunkKey(player.getPosition());
        if (chunkKey == null) {
            player.sendMessage("§cYou are not in a protected chunk!");
            return true;
        }

        ProtectedChunkData protectedChunk = chunkProtectionManager.getProtectedChunk(chunkKey);
        if (protectedChunk == null || !protectedChunk.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cYou do not own this chunk!");
            return true;
        }

        // Remove the chunk protection
        chunkProtectionManager.removeProtectedChunk(chunkKey);
        player.sendMessage("§aChunk protection removed!");
        return true;
    }

    private void listProtectedChunks(Player player) {
        UUID playerId = player.getUniqueId();
        List<ProtectedChunkData> playerChunks = chunkProtectionManager.getProtectedChunksByOwner(playerId);
    
        if (playerChunks.isEmpty()) {
            player.sendMessage("§eNo protected chunks.");
            return;
        }
    
        player.sendMessage("§aYour protected chunks:");
        player.sendMessage("§6----------------------------------------");
        player.sendMessage(String.format("§b%-10s %-6s %-10s %-7s %-10s", "Coords  ", "World  ", "Expires ", "Count ", " Status"));
        player.sendMessage("§6----------------------------------------");
    
        for (ProtectedChunkData chunk : playerChunks) {
            String coordinates = String.format("(%d, %d)", chunk.getChunkX() * 16 + 7, chunk.getChunkZ() * 16 + 8);
            player.sendMessage(String.format("§e%-10s %-6s %-10s %-8d %-10s", coordinates, chunk.getWorldName(), chunk.getTimeRemaining(), chunk.getStorageCount(), chunk.isProtectionActive() ? "Active" : "Inactive"));
        }
        player.sendMessage("§6----------------------------------------");
    }
    
    
    private void startChunkExpirationTask() {
        getServer().getScheduler().scheduleRepeatingTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                // Iterate through all protected chunks
                for (ProtectedChunkData chunk : chunkProtectionManager.getAllProtectedChunks()) {
                    if (chunk.hasExpired()) {
                        UUID owner = chunk.getOwner();
                        String chunkLocation = String.format("(%d, %d) in world '%s'", 
                            chunk.getChunkX()*16 + 7, chunk.getChunkZ()*16 + 8, chunk.getWorldName());
    
                        // Remove the chunk's protection permanently
                        chunkProtectionManager.unprotectChunk(
                            chunk.getWorldName() + ":" + chunk.getChunkX() + ":" + chunk.getChunkZ()
                        );
    
                        // Notify the owner if online
                        Player player = getServer().getPlayerExact(owner.toString());
                        if (player != null) {
                            player.sendMessage("§cYour chunk protection at " + chunkLocation + " has expired and is no longer protected.");
                        }
                    }
                }
            }
        }, 20*60);//20 * 60 * 60); // Run every hour
    }

    private void startReminderTask() {
        // Schedule a repeating task to send reminders about unprotected chunks
        getServer().getScheduler().scheduleRepeatingTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                long currentTime = System.currentTimeMillis();

                // Iterate through protected chunks
                for (ProtectedChunkData protectedChunk : chunkProtectionManager.getAllProtectedChunks()) {
                    // Check if protection is not active
                    if (!protectedChunk.isProtectionActive()) {
                        // Get the chunk owner
                        Player player = getServer().getPlayerExact(protectedChunk.getOwner().toString());

                        // Send reminder if player is online and not recently notified
                        if (player != null && 
                            currentTime - protectedChunk.getLastNotifiedTime() >= 300000) { // 5 minutes
                            
                            player.sendMessage(String.format(
                                "§cReminder: Your chunk at (%d, %d) in world '%s' is unprotected due to excess chests.",
                                protectedChunk.getChunkX()*16 + 7, 
                                protectedChunk.getChunkZ()*16 + 8, 
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

    @EventHandler
    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (event.getWindow() instanceof FormWindowSimple) {
            FormResponseSimple response = (FormResponseSimple) event.getResponse();
            protectionGUI.handleProtectedChunksResponse(player, response);
        }
    }
}