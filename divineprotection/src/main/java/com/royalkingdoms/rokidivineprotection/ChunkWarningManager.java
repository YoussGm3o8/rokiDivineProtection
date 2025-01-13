package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class ChunkWarningManager implements Listener {
    private final RokiDivineProtection plugin;
    private static final long DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000L;
    private static final long CLAIM_EXPIRATION_PERIOD = 14 * DAY_IN_MILLISECONDS;

    public ChunkWarningManager(RokiDivineProtection plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        List<String> expiringChunks = new ArrayList<>();
        for (ProtectedChunkData chunk : plugin.getChunkProtectionManager().getAllProtectedChunks()) {
            // Check if the chunk is owned by the player
            if (chunk.getOwner().equals(playerUUID)) {
                long expirationTime = chunk.getLastNotifiedTime() + CLAIM_EXPIRATION_PERIOD;
                long remainingTime = expirationTime - currentTime;

                // Notify if the chunk is close to expiration (7 days remaining or less)
                if (remainingTime <= 7 * DAY_IN_MILLISECONDS) {
                    expiringChunks.add(String.format("(%d, %d) in world '%s'",
                            chunk.getChunkX() * 16 + 8, chunk.getChunkZ() * 16 + 7, chunk.getWorldName()));
                }
            }
        }

        // Notify player if there are any expiring chunks
        if (!expiringChunks.isEmpty()) {
            player.sendMessage("§c[Chunk Protection] Some of your protected chunks are expiring soon!");
            player.sendMessage("§eUse /findprotectedchunks to view expiring chunks.");
            expiringChunks.forEach(chunk -> player.sendMessage("§e" + chunk));
        }
    }
}
