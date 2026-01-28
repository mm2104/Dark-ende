package com.denny.darkende;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DarkEnde extends JavaPlugin implements Listener {
    
    private Map<UUID, Integer> playerChestLevels = new HashMap<>();
    private Map<UUID, Long> lastChestOpen = new HashMap<>();
    private Map<UUID, ItemStack[]> lastInventorySnapshot = new HashMap<>();
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new EnderChestListener(this), this);
        getLogger().info("DarkEnde Plugin wurde aktiviert!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DarkEnde Plugin wurde deaktiviert!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("endrang")) {
            if (!sender.hasPermission("darkende.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.no_permission")));
                return true;
            }
            
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Verwendung: /endrang <spieler> <stufe (1-3)>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.player_not_found")));
                return true;
            }
            
            int level;
            try {
                level = Integer.parseInt(args[1]);
                if (level < 1 || level > 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        getConfig().getString("messages.invalid_level")));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.invalid_level")));
                return true;
            }
            
            setPlayerChestLevel(target, level);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                getConfig().getString("messages.level_set")
                    .replace("%player%", target.getName())
                    .replace("%level%", String.valueOf(level))));
            return true;
        }
        return false;
    }
    
    private void setPlayerChestLevel(Player player, int level) {
        playerChestLevels.put(player.getUniqueId(), level);
    }
    
    private int getPlayerChestLevel(Player player) {
        // Zuerst prüfen ob explizit gesetzt
        if (playerChestLevels.containsKey(player.getUniqueId())) {
            return playerChestLevels.get(player.getUniqueId());
        }
        
        // Dann nach Rang in Konfiguration prüfen
        FileConfiguration config = getConfig();
        if (config.contains("ranks")) {
            for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
                if (player.hasPermission("darkende.rank." + rank)) {
                    return config.getInt("ranks." + rank);
                }
            }
        }
        
        // Standardwert
        return config.getInt("ranks.default", 1);
    }
    
    private int getChestSize(int level) {
        switch (level) {
            case 1:
                return 27; // Normale EnderChest (3x9)
            case 2:
                return 41; // 1.5x so groß (ca. 4x9 + 5)
            case 3:
                return 54; // 2x so groß (6x9)
            default:
                return 27;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) {
            return;
        }
        
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Anti-Dupe Prüfung
        if (getConfig().getBoolean("anti_dupe.enabled", true)) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            if (lastChestOpen.containsKey(playerId)) {
                long timeDiff = (currentTime - lastChestOpen.get(playerId)) / 1000;
                if (timeDiff < getConfig().getInt("anti_dupe.check_interval", 5)) {
                    getLogger().warning("Mögliche Duplikationsaktion von " + player.getName() + " erkannt!");
                    // Hier könnten weitere Anti-Dupe Maßnahmen ergriffen werden
                }
            }
            
            lastChestOpen.put(playerId, currentTime);
            lastInventorySnapshot.put(playerId, event.getInventory().getContents().clone());
        }
    }
    
    public Inventory createCustomEnderChest(Player player) {
        int level = getPlayerChestLevel(player);
        int size = getChestSize(level);
        
        Inventory chest = Bukkit.createInventory(null, size, 
            ChatColor.translateAlternateColorCodes('&', 
                getConfig().getString("messages.chest_opened")
                    .replace("%level%", String.valueOf(level))));
        
        // Bestehende EnderChest Items übernehmen
        ItemStack[] enderItems = player.getEnderChest().getContents();
        for (int i = 0; i < Math.min(enderItems.length, chest.getSize()); i++) {
            if (enderItems[i] != null) {
                chest.setItem(i, enderItems[i].clone());
            }
        }
        
        return chest;
    }
    
    public void saveCustomEnderChest(Player player, Inventory customChest) {
        // Items zurück zur echten EnderChest speichern
        player.getEnderChest().clear();
        for (int i = 0; i < Math.min(customChest.getSize(), 27); i++) {
            ItemStack item = customChest.getItem(i);
            if (item != null) {
                player.getEnderChest().setItem(i, item.clone());
            }
        }
    }
}
