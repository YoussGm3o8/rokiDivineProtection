package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockChest;
import cn.nukkit.block.BlockFire;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.block.BlockBurnEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Position;
import me.onebone.economyapi.EconomyAPI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkProtectionManager implements Listener {
    private final RokiDivineProtection plugin;
    private final Map<String, ProtectedChunkData> protectedChunks = new ConcurrentHashMap<>();
    private static final int CLAIM_COST = 16000;
    private static final int MAX_CHESTS = 2;
    private final File chunksFile;
    private final Gson gson;

    public ChunkProtectionManager(RokiDivineProtection plugin) {
        this.plugin = plugin;
        this.chunksFile = new File(plugin.getDataFolder(), "protected_chunks.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load protected chunks
        loadProtectedChunks();
    }

    private void loadProtectedChunks() {
        if (!chunksFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(chunksFile)) {
            Type type = new TypeToken<List<ProtectedChunkData>>(){}.getType();
            List<ProtectedChunkData> loadedChunks = gson.fromJson(reader, type);
            
            if (loadedChunks != null) {
                for (ProtectedChunkData chunk : loadedChunks) {
                    // Reconstruct the chunk key
                    String chunkKey = chunk.getWorldName() + ":" + chunk.getChunkX() + ":" + chunk.getChunkZ();
                    protectedChunks.put(chunkKey, chunk);
                }
                plugin.getLogger().info("Loaded " + loadedChunks.size() + " protected chunks.");
            }
        } catch (IOException e) {
            plugin.getLogger().error("Error loading protected chunks: " + e.getMessage());
        }
    }

    public void saveProtectedChunks() {
        try (Writer writer = new FileWriter(chunksFile)) {
            List<ProtectedChunkData> chunksToSave = new ArrayList<>(protectedChunks.values());
            gson.toJson(chunksToSave, writer);
            plugin.getLogger().info("Saved " + chunksToSave.size() + " protected chunks.");
        } catch (IOException e) {
            plugin.getLogger().error("Error saving protected chunks: " + e.getMessage());
        }
    }

    public boolean claimLand(Player player) {
        Position playerPos = player.getPosition();
        
        // Check if the player is in the correct world
        if (!playerPos.level.getName().equalsIgnoreCase("world")) {
            player.sendMessage("§cYou can only claim land in the 'world' dimension!");
            return false;
        }

        String chunkKey = getChunkKey(playerPos);

        // Check if chunk is already claimed
        if (protectedChunks.containsKey(chunkKey)) {
            player.sendMessage("§cThis chunk is already protected!");
            return false;
        }

        // Check economy
        EconomyAPI economy = EconomyAPI.getInstance();
        double playerMoney = economy.myMoney(player);

        if (playerMoney < CLAIM_COST) {
            player.sendMessage("§cYou need $" + CLAIM_COST + " to claim this land!");
            return false;
        }

        // Reduce money
        economy.reduceMoney(player, CLAIM_COST);

        // Create chunk protection
        ProtectedChunkData chunkData = new ProtectedChunkData(player.getUniqueId(), playerPos);
        protectedChunks.put(chunkKey, chunkData);

        // Save chunks after modification
        saveProtectedChunks();

        player.sendTitle("§6Divine Protection", "§eLand Claimed!", 10, 70, 20);
        return true;
    }

    private String getChunkKey(Position pos) {
        // Only allow chunk protection in the "world" dimension
        if (!pos.level.getName().equalsIgnoreCase("world")) {
            return null;
        }
        int chunkX = (int) Math.floor(pos.x / 16);
        int chunkZ = (int) Math.floor(pos.z / 16);
        return pos.level.getName() + ":" + chunkX + ":" + chunkZ;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Position from = event.getFrom();
        Position to = event.getTo();

        String fromChunkKey = getChunkKey(from);
        String toChunkKey = getChunkKey(to);

        // Skip if not in world or chunk key is null
        if (fromChunkKey == null || toChunkKey == null) {
            return;
        }

        // Only check if moving to a different chunk
        if (!fromChunkKey.equals(toChunkKey)) {
            ProtectedChunkData protectedChunk = protectedChunks.get(toChunkKey);
            if (protectedChunk != null && !protectedChunk.getOwner().equals(player.getUniqueId())) {
                if (!protectedChunk.isOverChestLimit()) {
                    event.setCancelled(true);
                    player.sendTitle("§cDivine Protection", "§4Forbidden Territory!", 10, 70, 20);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Position to = event.getTo();
        String toChunkKey = getChunkKey(to);
        Player player = event.getPlayer();

        // Skip if not in world or chunk key is null
        if (toChunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(toChunkKey);
        if (protectedChunk != null && !protectedChunk.getOwner().equals(player.getUniqueId())) {
            if (!protectedChunk.isOverChestLimit()) {
                event.setCancelled(true);
                player.sendTitle("§cDivine Protection", "§4Teleport Blocked!", 10, 70, 20);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        // Skip if not in world or chunk key is null
        if (chunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
        if (protectedChunk != null) {
            if (!protectedChunk.getOwner().equals(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendTitle("§cDivine Protection", "§4Cannot Build Here!", 10, 70, 20);
            } else if (event.getBlock() instanceof BlockChest) {
                protectedChunk.incrementChestCount();
                saveProtectedChunks(); // Save after chest addition
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        // Skip if not in world or chunk key is null
        if (chunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
        if (protectedChunk != null) {
            if (!protectedChunk.getOwner().equals(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendTitle("§cDivine Protection", "§4Cannot Break Here!", 10, 70, 20);
            } else if (event.getBlock() instanceof BlockChest) {
                protectedChunk.decrementChestCount();
                saveProtectedChunks(); // Save after chest removal
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blockList = event.getBlockList();
        blockList.removeIf(block -> {
            String chunkKey = getChunkKey(block.getLocation());
            return chunkKey != null && protectedChunks.containsKey(chunkKey);
        });
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        // Cancel fire burning in protected chunks
        if (chunkKey != null && protectedChunks.containsKey(chunkKey)) {
            event.setCancelled(true);
        }
    }

}