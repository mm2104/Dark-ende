package com.denny.darkende;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CrossOverListener implements Listener {
    
    private final DarkEnde plugin;
    
    public CrossOverListener(DarkEnde plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getConfig().getBoolean("crossover.sync_on_join", true)) {
            // Verzögerte Synchronisation nach dem Join
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                plugin.forceSyncPlayer(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.sync_success")));
            }, 20L); // 1 Sekunde Verzögerung
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getConfig().getBoolean("crossover.sync_on_quit", true)) {
            // Sofortige Synchronisation beim Verlassen
            plugin.forceSyncPlayer(player);
        }
    }
}
