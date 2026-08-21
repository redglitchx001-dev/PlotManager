# PlotManager V1

The Ultimate Survival Plot System for **Paper 1.21.x** — designed by **RedGlitchX**.

Wand-claim natural terrain, protect it, upgrade it, and grow it into an empire. 53 features, built-in holograms (no FancyHolograms), bundled Discord bot, and fully configurable green-theme messages.

## Requirements

| Plugin | Role |
|---|---|
| **Paper 1.21+** | Server |
| **Vault** | Required |
| **EssentialsX / CMI / any Vault economy** | Required |
| FastAsyncWorldEdit | Optional — 7-day nature rollback |
| BlueMap | Optional — 3D web map boxes |
| PlaceholderAPI | Optional — `%plotmanager_*%` placeholders |
| LuckPerms | Optional — premium tiers by group name |
| Simple Voice Chat | Optional — plot voice isolation tag |

Java **21** is required to compile and run.

## Build the jar

```bash
mvn -DskipTests package
```

Output: `target/PlotManager-1.0.jar`

Drop that file into your server's `plugins/` folder and restart.

## First-time setup

1. Install Vault + an economy plugin.
2. Start the server once so `plugins/PlotManager/config.yml` is generated.
3. Set `world_settings.protected_world` to your survival world name.
4. (Optional) Paste your Discord bot token under `discord:` and enable it.
5. Give staff `plotmanager.admin` and `plotmanager.wand`.
6. Players need `plotmanager.use` and `plotmanager.claim` (default: true).

## Quick start (in game)

```
/plot wand          # Golden Axe (staff)
# Left-click pos 1, right-click pos 2
/plot claim         # Buy the cuboid
/plot menu          # Master GUI
/plot help          # Command list
```

## Premium tiers (LuckPerms)

You do **not** need custom permission nodes. Put your LuckPerms group names in `config.yml`:

```yaml
premium:
  tiers:
    vip:
      luckperms_groups:
        - "vip"
        - "sponsor"
```

Anyone in those groups gets that tier's claim cost, plot cap, hopper/spawner limits, and hologram style.

## Feature map

- Wand claiming, merging, wilderness protection, explosion/piston/hopper-minecart/liquid exploit blocking
- Roles (Owner / Co-Owner / Builder / Visitor) with `--no-chests` trust
- Bouncer shields, freeze/quarantine, combat tag
- Plot bank, vaults (Base64), generators (work offline), tip jars, chest shops, global market, blackmarket
- Leveling, fly, crop boost, factories/auto-restock, smart hoppers, smart-clear flags
- Built-in ArmorStand holograms + player-bought holos with profanity filter
- Titles, actionbar, music discs, mailboxes (barrel + holographic drop zone)
- GPS browse trails, radar map, elevators (iron blocks), drone camera mode
- Discord JDA bot (chat sync, logs, snitch tips, Plot Lord alerts)
- BlueMap markers, FAWE schematic rollback, PlaceholderAPI
- Admin toolkit: godwand, rollback wand, inspect, seize, purge, spy, audit, price editor, leaderboard

All messages, GUI titles, particles, sounds, and prices live in `config.yml`.

## Commands

Player: `/plot` (aliases `/p`, `/plots`) — `menu`, `claim`, `unclaim`, `info`, `home`, `add`, `flag`, `vault`, `market`, `browse`, `drone`, `holo`, `blackmarket`, …

Admin: `reload`, `delete`, `freeze`, `purge`, `inspect`, `seize`, `godwand`, `rollbackwand`, `adminspy`, `audit`, `settop`, `editprices`

Full list: `/plot help` or the master plan file.

## License

Copyright (C) RedGlitchX. All Rights Reserved.
