# DarkEnde - EnderChest Plugin

Ein einfaches Minecraft Plugin für erweiterte EnderChests mit 3 Stufen und Anti-Dupe Schutz.

## Features

- **3 EnderChest Stufen:**
  - Stufe 1: Normale Größe (27 Slots - 3x9)
  - Stufe 2: 1.5x so groß (41 Slots)
  - Stufe 3: 2x so groß (54 Slots - 6x9)

- **Rang-basierte Zuweisung:** Konfigurierbare Ränge in der config.yml
- **Anti-Dupe Schutz:** Verhindert Item-Duplikation
- **Einfache Verwaltung:** Mit `/endrang` Befehl

## Installation

1. Projekt mit Maven kompilieren:
   ```bash
   mvn clean package
   ```

2. Die `.jar` Datei aus `target/` in den `plugins/` Ordner deines Servers kopieren

3. Server neustarten

## Konfiguration

### Ränge einstellen (config.yml):
```yaml
ranks:
  default: 1
  vip: 2
  premium: 3
  admin: 3
```

### Permissions:
- `darkende.admin` - Kann EnderChest Stufen setzen
- `darkende.use` - Kann erweiterte EnderChests nutzen
- `darkende.rank.<rangname>` - Weist Rang zu (z.B. `darkende.rank.vip`)

## Befehle

- `/endrang <spieler> <stufe (1-3)>` - Setzt EnderChest Stufe für einen Spieler

## Anti-Dupe Schutz

Das Plugin enthält einen grundlegenden Anti-Dupe Schutz, der:
- Zeitabstände zwischen EnderChest-Öffnungen überwacht
- Inventar-Snapshots erstellt
- Verdächtige Aktivitäten protokolliert

## Entwickelt von

Denny - Version 1.0.0
