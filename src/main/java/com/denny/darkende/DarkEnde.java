package com.denny.darkende;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DarkEnde extends JavaPlugin implements Listener {
    
    private Map<UUID, Integer> playerChestLevels = new HashMap<>();
    private Map<UUID, Long> lastChestOpen = new HashMap<>();
    private Map<UUID, ItemStack[]> lastInventorySnapshot = new HashMap<>();
    
    // Cross-Over Sync
    private File syncDataFile;
    private YamlConfiguration syncData;
    private boolean crossOverEnabled;
    private long syncInterval;
    private Map<UUID, Long> lastSyncTime = new HashMap<>();
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Cross-Over Sync initialisieren
        setupCrossOverSync();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new EnderChestListener(this), this);
        
        // Cross-Over Listener registrieren wenn aktiviert
        if (crossOverEnabled) {
            Bukkit.getPluginManager().registerEvents(new CrossOverListener(this), this);
        }
        
        // Sync Task starten
        if (crossOverEnabled) {
            startSyncTask();
        }
        
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
        
        if (command.getName().equalsIgnoreCase("endsync")) {
            if (!sender.hasPermission("darkende.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.no_permission")));
                return true;
            }
            
            if (!crossOverEnabled) {
                sender.sendMessage(ChatColor.RED + "Cross-Over Sync ist nicht aktiviert!");
                return true;
            }
            
            Player target;
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Nur Spieler können sich selbst synchronisieren!");
                    return true;
                }
                target = (Player) sender;
            } else {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        getConfig().getString("messages.player_not_found")));
                    return true;
                }
            }
            
            forceSyncPlayer(target);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                getConfig().getString("messages.sync_success")));
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
        
        // Cross-Over Sync speichern
        if (crossOverEnabled) {
            saveEnderChestToSync(player, customChest);
        }
    }
    
    private void setupCrossOverSync() {
        crossOverEnabled = getConfig().getBoolean("crossover.enabled", false);
        syncInterval = getConfig().getLong("crossover.sync_interval", 30) * 20L; // Ticks
        
        if (crossOverEnabled) {
            syncDataFile = new File(getDataFolder(), "crossover_sync.yml");
            syncData = YamlConfiguration.loadConfiguration(syncDataFile);
            getLogger().info("Cross-Over Sync aktiviert!");
        }
    }
    
    private void startSyncTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (shouldSync(player)) {
                    syncPlayerEnderChest(player);
                }
            }
        }, syncInterval, syncInterval);
    }
    
    private boolean shouldSync(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastSync = lastSyncTime.getOrDefault(playerId, 0L);
        
        return (currentTime - lastSync) > (syncInterval / 20 * 1000);
    }
    
    private void syncPlayerEnderChest(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Von Datei laden
        ItemStack[] syncedItems = loadEnderChestFromSync(player);
        if (syncedItems != null) {
            // Mit aktueller EnderChest zusammenführen
            mergeEnderChests(player, syncedItems);
        }
        
        // Zu Datei speichern
        saveEnderChestToSync(player, player.getEnderChest());
        lastSyncTime.put(playerId, System.currentTimeMillis());
    }
    
    private void saveEnderChestToSync(Player player, Inventory inventory) {
        if (!crossOverEnabled) return;
        
        UUID playerId = player.getUniqueId();
        String path = "players." + playerId.toString() + ".enderchest";
        
        // Items serialisieren
        Map<String, Object> items = new HashMap<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                items.put(String.valueOf(i), item.serialize());
            }
        }
        
        syncData.set(path, items);
        syncData.set(path + ".timestamp", System.currentTimeMillis());
        syncData.set(path + ".level", getPlayerChestLevel(player));
        
        try {
            syncData.save(syncDataFile);
        } catch (IOException e) {
            getLogger().warning("Konnte Cross-Over Sync nicht speichern: " + e.getMessage());
        }
    }
    
    private ItemStack[] loadEnderChestFromSync(Player player) {
        if (!crossOverEnabled) return null;
        
        UUID playerId = player.getUniqueId();
        String path = "players." + playerId.toString() + ".enderchest";
        
        if (!syncData.contains(path)) {
            return null;
        }
        
        Map<String, Object> itemsData = (Map<String, Object>) syncData.get(path);
        ItemStack[] items = new ItemStack[54]; // Maximale Größe
        
        for (Map.Entry<String, Object> entry : itemsData.entrySet()) {
            if (entry.getKey().equals("timestamp") || entry.getKey().equals("level")) {
                continue;
            }
            
            try {
                int slot = Integer.parseInt(entry.getKey());
                if (slot < items.length) {
                    items[slot] = ItemStack.deserialize((Map<String, Object>) entry.getValue());
                }
            } catch (Exception e) {
                getLogger().warning("Fehler beim Deserialisieren von Item in Slot " + entry.getKey());
            }
        }
        
        return items;
    }
    
    private void mergeEnderChests(Player player, ItemStack[] syncedItems) {
        Inventory currentChest = player.getEnderChest();
        
        // Sync-Items haben Priorität
        for (int i = 0; i < Math.min(syncedItems.length, currentChest.getSize()); i++) {
            if (syncedItems[i] != null) {
                currentChest.setItem(i, syncedItems[i].clone());
            }
        }
    }
    
    public void forceSyncPlayer(Player player) {
        if (crossOverEnabled) {
            CompletableFuture.runAsync(() -> syncPlayerEnderChest(player));
        }
    }
}
