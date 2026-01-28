# DarkEnde - EnderChest Plugin

Ein einfaches Minecraft Plugin für erweiterte EnderChests mit 3 Stufen, Anti-Dupe Schutz und Cross-Over Synchronisation.

## Features

- **3 EnderChest Stufen:**
  - Stufe 1: Normale Größe (27 Slots - 3x9)
  - Stufe 2: 1.5x so groß (41 Slots)
  - Stufe 3: 2x so groß (54 Slots - 6x9)

- **Rang-basierte Zuweisung:** Konfigurierbare Ränge in der config.yml
- **Anti-Dupe Schutz:** Verhindert Item-Duplikation
- **Cross-Over Sync:** Synchronisiert EnderChests zwischen mehreren Servern
- **Einfache Verwaltung:** Mit `/endrang` und `/endsync` Befehlen

## Cross-Over Sync

Die Cross-Over Synchronisation ermöglicht es, EnderChest Inhalte zwischen mehreren Servern zu synchronisieren:

### Einrichtung:
1. **Cross-Over Sync aktivieren:**
   ```yaml
   crossover:
     enabled: true
     sync_interval: 30 # Sekunden
   ```

2. **Gemeinsames Verzeichnis:** Alle Server müssen auf dasselbe `plugins/DarkEnde/crossover_sync.yml` Verzeichnis zugreifen können (z.B. via Netzwerkfreigabe)

3. **Automatische Synchronisation:**
   - Beim Spieler join/quit
   - Alle 30 Sekunden (konfigurierbar)
   - Manuell mit `/endsync`

### Befehle:
- `/endsync` - Synchronisiert dich selbst
- `/endsync <spieler>` - Synchronisiert einen anderen Spieler

## Installation

1. Projekt mit Maven kompilieren:
   ```bash
   mvn clean package
   ```

2. Die `.jar` Datei aus `target/` in den `plugins/` Ordner deines Servers kopieren

3. Server neustarten

4. Cross-Over Sync bei Bedarf in der `config.yml` aktivieren

## Konfiguration

### Ränge einstellen (config.yml):
```yaml
ranks:
  default: 1
  vip: 2
  premium: 3
  admin: 3
```

### Cross-Over Sync (config.yml):
```yaml
crossover:
  enabled: false
  sync_interval: 30
  sync_on_join: true
  sync_on_quit: true
  priority: "sync"
```

## Permissions:
- `darkende.admin` - Kann EnderChest Stufen setzen und synchronisieren
- `darkende.use` - Kann erweiterte EnderChests nutzen
- `darkende.rank.<rangname>` - Weist Rang zu (z.B. `darkende.rank.vip`)

## Befehle

- `/endrang <spieler> <stufe (1-3)>` - Setzt EnderChest Stufe für einen Spieler
- `/endsync [spieler]` - Manuelle Synchronisation (nur mit Cross-Over Sync)

## Anti-Dupe Schutz

Das Plugin enthält einen grundlegenden Anti-Dupe Schutz, der:
- Zeitabstände zwischen EnderChest-Öffnungen überwacht
- Inventar-Snapshots erstellt
- Verdächtige Aktivitäten protokolliert

## Multi-Server Setup

Für Cross-Over Sync zwischen mehreren Servern:

1. **Alle Server installieren:** Dieselbe Plugin-Version auf allen Servern
2. **Gemeinsamer Speicher:** Netzwerkfreigabe für `plugins/DarkEnde/` Ordner
3. **Konfiguration:** Cross-Over Sync auf allen Servern aktivieren
4. **Testen:** Mit `/endsync` die Synchronisation prüfen

## Entwickelt von

Denny - Version 1.0.0
