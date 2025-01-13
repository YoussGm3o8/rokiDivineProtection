package com.royalkingdoms.rokidivineprotection;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockBarrel;
import cn.nukkit.block.BlockChest;
import cn.nukkit.block.BlockHopper;
import cn.nukkit.block.BlockShulkerBox;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.block.BlockGrowEvent;
import cn.nukkit.event.block.BlockIgniteEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Level;
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
    private final Map<UUID, Map<UUID, TrustedPlayerPermissions>> trustedPlayers = new ConcurrentHashMap<>();
    private final File trustedPlayersFile;

    public ChunkProtectionManager(RokiDivineProtection plugin) {
        this.plugin = plugin;
        this.chunksFile = new File(plugin.getDataFolder(), "protected_chunks.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.trustedPlayersFile = new File(plugin.getDataFolder(), "trusted_players.json");
        
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load protected chunks
        loadProtectedChunks();
        loadTrustedPlayers();
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

        int storageBlockCount = countStorageBlocksInChunk(playerPos);

        // Check economy
        EconomyAPI economy = EconomyAPI.getInstance();
        double playerMoney = economy.myMoney(player);

        if (playerMoney < CLAIM_COST) {
            player.sendMessage("§cYou need $" + CLAIM_COST + " to claim this land!");
            return false;
        }

        ProtectedChunkData chunkData = new ProtectedChunkData(player.getUniqueId(), playerPos);
        chunkData.setStorageCount(storageBlockCount);

        if (storageBlockCount > MAX_CHESTS) {
            player.sendMessage("§cThis chunk contains " + storageBlockCount + " storage blocks. " +
                               "It will be unprotected until reduced to 2 or fewer!");
        }    

        // Reduce money
        economy.reduceMoney(player, CLAIM_COST);

        // Create chunk protection
        protectedChunks.put(chunkKey, chunkData);

        // Save chunks after modification
        saveProtectedChunks();

        player.sendTitle("§6Divine Protection", "§eLand Claimed!", 10, 70, 20);
        return true;
    }

    public void unprotectChunk(String chunkKey) {
        if (protectedChunks.containsKey(chunkKey)) {
            // Remove the chunk from the protection map
            protectedChunks.remove(chunkKey);
            plugin.getLogger().info("Chunk protection removed for key: " + chunkKey);
        }
    }

    public List<ProtectedChunkData> getProtectedChunksByOwner(UUID ownerId) {
        List<ProtectedChunkData> ownedChunks = new ArrayList<>();
        for (ProtectedChunkData chunk : protectedChunks.values()) {
            if (chunk.getOwner().equals(ownerId)) {
                ownedChunks.add(chunk);
            }
        }
        return ownedChunks;
    }

    private int countStorageBlocksInChunk(Position pos) {
        Level level = pos.level;
        int chunkX = pos.getChunkX();
        int chunkZ = pos.getChunkZ();
        
        int storageBlockCount = 0;
    
        // Iterate through all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 320; y++) {
                    // Get the block at this position within the chunk
                    Block block = level.getBlock(
                        (chunkX * 16) + x, 
                        y, 
                        (chunkZ * 16) + z
                    );
    
                    // Check if it's a storage block
                    if (isStorageBlock(block)) {
                        storageBlockCount++;
                    }
                }
            }
        }
    
        return storageBlockCount;
    }

    public String getChunkKey(Position pos) {
        if (pos == null || pos.level == null) {
            return null;
        }
        int chunkX = pos.getChunkX();
        int chunkZ = pos.getChunkZ();
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
            if (protectedChunk != null && protectedChunk.getOwner().equals(player.getUniqueId())) {
                // Update the last access time when the player enters their own chunk
                protectedChunk.updateLastAccessTime();
                saveProtectedChunks();  // Save the updated chunk data
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
            if (protectedChunk != null && protectedChunk.getOwner().equals(player.getUniqueId())) {
                // Update the last access time when the player teleports to their own chunk
                protectedChunk.updateLastAccessTime();
                saveProtectedChunks();  // Save the updated chunk data
            }
        }
    }

    private boolean isStorageBlock(Block block) {
        return block instanceof BlockChest || 
               block instanceof BlockBarrel || 
               block instanceof BlockHopper || 
               block instanceof BlockShulkerBox;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        if (chunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
        if (protectedChunk != null) {

            int currentStorageCount = protectedChunk.getStorageCount();

            if (!protectedChunk.getOwner().equals(player.getUniqueId()) && currentStorageCount <= 2 ) {
                event.setCancelled(true);
                player.sendTitle("§cDivine Protection", "§4Cannot Build Here!", 10, 70, 20);
            } else if (isStorageBlock(event.getBlock())) {
                int previousStorageCount = protectedChunk.getStorageCount();
                protectedChunk.incrementStorageCount();
                currentStorageCount = protectedChunk.getStorageCount();

                if (previousStorageCount <= 2 && currentStorageCount > 2) {
                    player.sendTitle("§cDivine Protection", "§4Chunk is no longer protected!", 10, 70, 20);
                }
                saveProtectedChunks();                
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        if (chunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
        if (protectedChunk != null) {
            int currentStorageCount = protectedChunk.getStorageCount();
            if (!protectedChunk.getOwner().equals(player.getUniqueId()) && currentStorageCount <= 2) {
                event.setCancelled(true);
                player.sendTitle("§cDivine Protection", "§4Cannot Break Here!", 10, 70, 20);
            } else if (isStorageBlock(event.getBlock())) {
                int previousStorageCount = protectedChunk.getStorageCount();
                protectedChunk.decrementStorageCount();
                currentStorageCount = protectedChunk.getStorageCount();
                if (previousStorageCount > 2 && currentStorageCount <= 2) {
                    player.sendTitle("§6Divine Protection", "§eProtection reactivated!", 10, 70, 20);
                }
                saveProtectedChunks();
            }
        }
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        String chunkKey = getChunkKey(event.getBlock().getLocation());
        if (chunkKey != null && protectedChunks.containsKey(chunkKey)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Create a list to track chunks that need full protection
        List<String> protectedChunkKeys = new ArrayList<>();
        
        // First, identify chunks that should be fully protected
        for (Block block : event.getBlockList()) {
            String chunkKey = getChunkKey(block.getLocation());
            
            // Skip if chunk key is null
            if (chunkKey == null) {
                continue;
            }
            
            // Get protected chunk data
            ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
            
            // If chunk is protected with 2 or fewer storage blocks
            if (protectedChunk != null && protectedChunk.getStorageCount() <= 2) {
                protectedChunkKeys.add(chunkKey);
            }
        }
        
        // Remove blocks from chunks that need full protection
        event.getBlockList().removeIf(block -> {
            String chunkKey = getChunkKey(block.getLocation());
            return chunkKey != null && protectedChunkKeys.contains(chunkKey);
        });
        
        // Track storage block removals
        Map<String, Integer> storageBlockRemovals = new HashMap<>();
        
        // Iterate through blocks that were actually removed in the explosion
        for (Block block : new ArrayList<>(event.getBlockList())) {
            String chunkKey = getChunkKey(block.getLocation());
            
            // Skip if chunk key is null
            if (chunkKey == null) {
                continue;
            }
            
            ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);
            
            // If it's a storage block in a protected chunk
            if (protectedChunk != null && isStorageBlock(block)) {
                storageBlockRemovals.merge(chunkKey, 1, Integer::sum);
            }
        }
        
        // Apply storage block removals
        for (Map.Entry<String, Integer> entry : storageBlockRemovals.entrySet()) {
            ProtectedChunkData protectedChunk = protectedChunks.get(entry.getKey());
            if (protectedChunk != null) {
                for (int i = 0; i < entry.getValue(); i++) {
                    protectedChunk.decrementStorageCount();
                }
            }
        }
        
        // Save updated chunk data
        saveProtectedChunks();
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Position blockPos = event.getBlock().getLocation();
        String chunkKey = getChunkKey(blockPos);

        if (chunkKey == null) {
            return;
        }

        ProtectedChunkData protectedChunk = protectedChunks.get(chunkKey);

        // Check if the chunk is protected
        if (protectedChunk != null && protectedChunk.getStorageCount() <= 2) {
            // Cancel the burn event
            event.setCancelled(true);

            // Replace fire with air while preserving the original block
            Block burnedBlock = event.getBlock();
            Level level = burnedBlock.getLevel();

            if (burnedBlock.getId() == Block.FIRE) {
                level.setBlock(burnedBlock.getLocation(), Block.get(Block.AIR), true, true);
            }
        }
    }

        
    public ProtectedChunkData getProtectedChunk(String chunkKey) {
        return protectedChunks.get(chunkKey);
    }

    public void removeProtectedChunk(String chunkKey) {
        if (protectedChunks.containsKey(chunkKey)) {
            protectedChunks.remove(chunkKey);
            saveProtectedChunks(); // Ensure changes are persisted
        }
    }

    public Collection<ProtectedChunkData> getAllProtectedChunks() {
        return new ArrayList<>(protectedChunks.values());
    }

    public void addTrustedPlayer(UUID ownerId, UUID trustedPlayerId, boolean canBreakBlocks, boolean canPlaceBlocks, boolean canOpenChests, boolean canOpenDoors) {
        trustedPlayers.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>())
                      .put(trustedPlayerId, new TrustedPlayerPermissions(canBreakBlocks, canPlaceBlocks, canOpenChests, canOpenDoors));
        saveTrustedPlayers();
    }

    public TrustedPlayerPermissions getTrustedPlayerPermissions(UUID ownerId, UUID trustedPlayerId) {
        return trustedPlayers.getOrDefault(ownerId, Collections.emptyMap()).get(trustedPlayerId);
    }

    private void saveTrustedPlayers() {
        try (Writer writer = new FileWriter(trustedPlayersFile)) {
            gson.toJson(trustedPlayers, writer);
            plugin.getLogger().info("Saved trusted players and their permissions.");
        } catch (IOException e) {
            plugin.getLogger().error("Error saving trusted players: " + e.getMessage());
        }
    }

    private void loadTrustedPlayers() {
        if (!trustedPlayersFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(trustedPlayersFile)) {
            Type type = new TypeToken<Map<UUID, Map<UUID, TrustedPlayerPermissions>>>(){}.getType();
            Map<UUID, Map<UUID, TrustedPlayerPermissions>> loadedTrustedPlayers = gson.fromJson(reader, type);
            
            if (loadedTrustedPlayers != null) {
                trustedPlayers.putAll(loadedTrustedPlayers);
                plugin.getLogger().info("Loaded trusted players and their permissions.");
            }
        } catch (IOException e) {
            plugin.getLogger().error("Error loading trusted players: " + e.getMessage());
        }
    }

}