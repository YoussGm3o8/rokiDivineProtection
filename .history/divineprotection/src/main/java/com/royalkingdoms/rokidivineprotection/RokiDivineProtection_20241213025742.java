package com.royalkingdoms.rokidivineprotection;

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
}