package com.royalkingdoms.rokidivineprotection.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowSimple;
import com.royalkingdoms.rokidivineprotection.ChunkProtectionManager;
import com.royalkingdoms.rokidivineprotection.ProtectedChunkData;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class ProtectionGUI {

    private final ChunkProtectionManager chunkProtectionManager;

    public ProtectionGUI(ChunkProtectionManager chunkProtectionManager) {
        this.chunkProtectionManager = chunkProtectionManager;
    }

    public void showProtectedChunks(Player player) {
        UUID playerId = player.getUniqueId();
        List<ProtectedChunkData> playerChunks = chunkProtectionManager.getProtectedChunksByOwner(playerId);

        FormWindowSimple window = new FormWindowSimple("Protected Chunks", "");

        if (playerChunks.isEmpty()) {
            window.addButton(new ElementButton("No protected chunks."));
        } else {
            for (ProtectedChunkData chunk : playerChunks) {
                String coordinates = String.format("(%d, %d)", chunk.getChunkX() * 16 + 7, chunk.getChunkZ() * 16 + 8);
                String status = chunk.isProtectionActive() ? "Active" : "Inactive";
                String chunkInfo = String.format("Coords: %s\nWorld: %s\nExpires: %s\nStorage: %d\nStatus: %s",
                        coordinates, chunk.getWorldName(), chunk.getTimeRemaining(), chunk.getStorageCount(), status);
                window.addButton(new ElementButton(chunkInfo));
            }
        }

        window.addButton(new ElementButton("Add Trusted Player"));

        player.showFormWindow(window);
    }

    public void handleProtectedChunksResponse(Player player, FormResponseSimple response) {
        if (response == null) {
            return;
        }

        String buttonText = response.getClickedButton().getText();
        if (buttonText.equals("Add Trusted Player")) {
            showAddTrustedPlayerForm(player);
        }
    }

    private void showAddTrustedPlayerForm(Player player) {
        List<String> onlinePlayers = new ArrayList<>();
        for (Player p : player.getServer().getOnlinePlayers().values()) {
            onlinePlayers.add(p.getName());
        }

        FormWindowCustom form = new FormWindowCustom("Add Trusted Player");
        form.addElement(new ElementDropdown("Select Player", onlinePlayers));
        form.addElement(new ElementToggle("Can Break Blocks", false));
        form.addElement(new ElementToggle("Can Place Blocks", false));
        form.addElement(new ElementToggle("Can Open Chests", false));
        form.addElement(new ElementToggle("Can Open Doors", false));

        player.showFormWindow(form);
    }

    private void handleAddTrustedPlayerResponse(Player player, FormResponseCustom response) {
        String selectedPlayerName = response.getDropdownResponse(0).getElementContent();
        boolean canBreakBlocks = response.getToggleResponse(1);
        boolean canPlaceBlocks = response.getToggleResponse(2);
        boolean canOpenChests = response.getToggleResponse(3);
        boolean canOpenDoors = response.getToggleResponse(4);

        Player selectedPlayer = player.getServer().getPlayer(selectedPlayerName);
        if (selectedPlayer != null) {
            UUID ownerId = player.getUniqueId();
            UUID trustedPlayerId = selectedPlayer.getUniqueId();
            chunkProtectionManager.addTrustedPlayer(ownerId, trustedPlayerId, canBreakBlocks, canPlaceBlocks, canOpenChests, canOpenDoors);
            player.sendMessage("§aTrusted player added successfully!");
        } else {
            player.sendMessage("§cPlayer not found!");
        }
    }
}
