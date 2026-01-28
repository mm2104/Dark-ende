package com.denny.darkende;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class EnderChestListener implements Listener {
    
    private final DarkEnde plugin;
    
    public EnderChestListener(DarkEnde plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getClickedBlock() == null || 
            !event.getClickedBlock().getType().toString().contains("ENDER_CHEST")) {
            return;
        }
        
        Player player = event.getPlayer();
        if (!player.hasPermission("darkende.use")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.no_permission")));
            return;
        }
        
        // Standard-EnderChest Öffnung verhindern
        event.setCancelled(true);
        
        // Benutzerdefinierte EnderChest öffnen
        Inventory customChest = plugin.createCustomEnderChest(player);
        player.openInventory(customChest);
    }
}
