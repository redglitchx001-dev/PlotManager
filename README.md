<div align="center">

# 🟢 PlotManager V1

### The Ultimate Survival Plot System — designed by **RedGlitchX**

**Wand-claim natural terrain. Protect it. Upgrade it. Grow it into an empire.**

53 features · built-in holograms · bundled Discord bot · 4 languages · zero core dependencies

![Paper](https://img.shields.io/badge/Paper-1.21.0%20%E2%80%93%201.21.11%20%26%2026.x-0E8A16?labelColor=2B2B2B)
![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&labelColor=2B2B2B)
![Languages](https://img.shields.io/badge/Languages-EN%20%7C%20RO%20%7C%20ES%20%7C%20DE-3178C6?labelColor=2B2B2B)
![Economy](https://img.shields.io/badge/Needs-Vault%20%2B%20economy-FFB900?labelColor=2B2B2B)

</div>

---

## 📑 Table of contents

1. [Why PlotManager?](#-why-plotmanager)
2. [Compatibility](#-compatibility)
3. [Requirements](#-requirements)
4. [Installation](#-installation)
5. [Building the jar](#-building-the-jar)
6. [Languages 🌍](#-languages--)
7. [Quick start](#-quick-start)
8. [Commands](#-commands)
9. [Permissions](#-permissions)
10. [Feature map](#-feature-map)
11. [Configuration](#%EF%B8%8F-configuration)
12. [Premium tiers (LuckPerms)](#-premium-tiers-luckperms)
13. [Discord bot](#-discord-bot)
14. [FAQ](#-faq)

---

## 💎 Why PlotManager?

- **🪄 Natural-terrain claiming** — claim any cuboid with the wand, not grid squares. Merge adjacent plots into one empire.
- **🛡️ Absolute protection** — blocks every exploit known to Minecraft: piston pushing, hopper-minecart theft, dispenser arrows, liquid flow, portal griefing.
- **✨ Built-in holograms** — native ArmorStand holograms. **No** DecentHolograms, **No** FancyHolograms, **No** extra plugins.
- **🌍 4 languages out of the box** — English, Română, Español, Deutsch — and per-player language selection. Add your own in one file.
- **⚡ Asynchronous & lag-free** — saving, FAWE rollbacks, border particles and leaderboard scans run strictly off the main thread.
- **🏦 A living economy** — plot banks, offline generators, chest shops, global market, tip jars, a tax-free **blackmarket**, a player **Mayor** who sets server tax.
- **🎙️ Bundled Discord bot** — chat sync, claim/reset logs, snitch alerts and Plot Lord announcements via shaded JDA. No external Discord plugins.
- **🔧 100% configurable** — every message, GUI label, sign, title, sound, particle and price is editable. Colors with `&` codes and `&#RRGGBB` hex.

---

## ✅ Compatibility

One jar, the whole 1.21 line and beyond — `plugin.yml` declares `api-version: '1.21'`, the widest range, so it loads on every server below.

| Minecraft / Paper | Verification |
|---|---|
| **1.21 · 1.21.1 · 1.21.2 · 1.21.3** | ✅ — every API the plugin calls predates 1.21; bracketed by verified 1.21.1 (binary artifact) and 1.21.4 below |
| **1.21.1** | ✅ compiled against the real `paper-api 1.21.1-R0.1-SNAPSHOT` artifact |
| **1.21.4** | ✅ compiled against `paper-api 1.21.4-R0.1-SNAPSHOT` sources |
| **1.21.5** | ✅ compiled against `paper-api 1.21.5-R0.1-SNAPSHOT` sources |
| **1.21.6** | ✅ compiled against `paper-api 1.21.6-R0.1-SNAPSHOT` sources |
| **1.21.7** | ✅ compiled against `paper-api 1.21.7-R0.1-SNAPSHOT` sources |
| **1.21.8** | ✅ compiled against `paper-api 1.21.8-R0.1-SNAPSHOT` sources |
| **1.21.9** | ✅ compiled against `paper-api 1.21.9-R0.1-SNAPSHOT` sources |
| **1.21.10** | ✅ compiled against `paper-api 1.21.10-R0.1-SNAPSHOT` sources |
| **1.21.11** | ✅ compiled against `paper-api 1.21.11-R0.1-SNAPSHOT` sources (default build target) |
| **26.x (26.1+)** | ✅ compiled against `paper-api 26.1.2-R0.1-SNAPSHOT` sources |

- Java 21 runtime (the JVM shipped with modern Paper) — required.
- Also expected to work on Paper forks (Purpur, Pufferfish); Folia not specifically tested.
- To retarget the build: see [Building the jar](#-building-the-jar).

## 📦 Requirements

| Plugin | Role |
|---|---|
| **Paper 1.21.x – 26.x** | Server |
| **Vault** | Required — economy bridge |
| **EssentialsX / CMI / any Vault economy** | Required — money provider |
| FastAsyncWorldEdit (FAWE) | Optional — natural 7-day rollback |
| BlueMap | Optional — live 3D web-map boxes |
| PlaceholderAPI | Optional — `%plotmanager_*%` placeholders |
| LuckPerms | Optional — premium tiers by group |
| Simple Voice Chat | Optional — private plot voice isolation |

## 🚀 Installation

1. Drop `PlotManager-1.0.jar` into `plugins/`.
2. Make sure **Vault + an economy plugin** are installed.
3. Start once — `plugins/PlotManager/config.yml` and `lang/*.yml` generate automatically.
4. Set `world_settings.protected_world` to your survival world name.
5. (Optional) Add your Discord token under `discord:` and set `discord.enabled: true`.
6. Give staff `plotmanager.admin`; players already have `plotmanager.use` and `plotmanager.claim`.

## 🔨 Building the jar

```bash
mvn -DskipTests package
```

Output: `target/PlotManager-1.0.jar`

**Retarget the Paper API** (all three are verified to compile):

```bash
mvn -DskipTests package -Dpaper.version=1.21.4-R0.1-SNAPSHOT   # oldest target
mvn -DskipTests package -Dpaper.version=26.1.2-R0.1-SNAPSHOT   # newest target
```

Java **21** is required to build and run.

## 🌍 Languages

| | Language | Code | File |
|---|---|---|---|
| 🇬🇧 | English | `en` | `lang/en.yml` |
| 🇷🇴 | Română | `ro` | `lang/ro.yml` |
| 🇪🇸 | Español | `es` | `lang/es.yml` |
| 🇩🇪 | Deutsch | `de` | `lang/de.yml` |

- **Server default** — `config.yml → language.default: en`
- **Fallback language** — `language.fallback: en` — used automatically when a key is missing from the selected language
- **Client auto-detect** — `language.auto-detect: true` — players see their client's language before the server default (a Romanian client instantly gets `ro`)
- **Per player** — with `language.per-player: true`, everyone picks their own:
  - `/plot lang ro` (tab-completed, `/plot lang reset` to undo)
  - or the **🌐 Language button** in `/plot menu`
  - saved per player in `lang/preferences.yml`
- **Everything** is translated: chat, all GUIs, signs, action bars, enter/leave titles, broadcasts, upgrade names, the help menu.
- **Add your own language:** copy `lang/en.yml` → `lang/fr.yml`, translate, done — it's auto-detected. Missing keys fall back to English, then to legacy `config.yml` values.
- Files support `&` color codes, `&#RRGGBB` hex colors and `%placeholders%`.

```yaml
language:
  default: "en"        # server default: en | ro | es | de | <custom>
  fallback: "en"       # used when a key is missing from the selected language
  per-player: true     # /plot lang <code> per player + menu button
  auto-detect: true    # follow each player's client locale (ro_RO -> ro)
```

## ⚡ Quick start

```
/plot wand            # Golden Axe (staff)
                      # left-click pos 1 · right-click pos 2
/plot claim           # buy the cuboid
/plot menu            # master GUI (bank, vault, members, upgrades…)
/plot help            # command list
```

## 🧭 Commands

Aliases: `/plot` · `/p` · `/plots`

### Player

| Command | Description |
|---|---|
| `/plot` or `/plot menu` | Master GUI |
| `/plot help` | Help menu |
| `/plot lang [code\|reset]` | Your language |
| `/plot wand` | Selection wand (staff) |
| `/plot claim` · `unclaim [confirm]` | Claim / release a plot |
| `/plot info` | Plot stats |
| `/plot home [n\|player]` · `sethome` | Teleport / set spawn |
| `/plot add <player> [role] [--no-chests]` | Trust (builder / co-owner / visitor) |
| `/plot remove <player>` · `promote` · `demote` | Member management |
| `/plot ban <player>` · `unban` | Plot bans (bouncer shield) |
| `/plot flag <flag> <true\|false>` | Toggle plot flags |
| `/plot deposit / withdraw <amount>` | Plot bank |
| `/plot description <text>` · `rename <name>` · `private` | Identity |
| `/plot list` · `visit <player>` · `browse` · `merge` | Navigation |
| `/plot vault` · `market` · `blackmarket` · `map` · `maplink` | Economy & tools |
| `/plot music <disc>` · `cosmetics` | Music discs, borders & particles |
| `/plot holo create / addline / removeline / delete` · `holomove` | Holograms |
| `/plot setmailbox` · `fly` · `chat` · `drone` · `tax <percent>` | Utilities |

### Admin (`plotmanager.admin`)

`reload` · `delete` · `freeze` / `unfreeze` · `purge` · `inspect` · `seize` · `godwand` · `rollbackwand` · `adminspy` · `audit` · `settop` · `editprices`

## 🔑 Permissions

| Permission | Default | Grants |
|---|---|---|
| `plotmanager.use` | everyone | Basic usage |
| `plotmanager.claim` | everyone | Claiming |
| `plotmanager.drone` | everyone | Drone camera |
| `plotmanager.blackmarket` | everyone | Blackmarket access |
| `plotmanager.wand` | op | Selection wand |
| `plotmanager.premium.claim` | op | Premium plots |
| `plotmanager.mayor` | op | Tax powers (also granted dynamically to the #1 plot owner) |
| `plotmanager.bypass` | op | Bypass protections |
| `plotmanager.admin` | op | Full admin toolkit |

## 🗺️ Feature map

**Claiming & terrain**
Wand cuboid claiming · merging · wilderness protection · explosion / piston / hopper-minecart / liquid / portal exploit blocking · freeze & quarantine · FAWE natural rollback (7-day wipe) · BlueMap markers

**Roles & security**
Owner / Co-Owner / Builder / Visitor with `--no-chests` trust · bouncer shields & plot bans · combat tag · admin spy · audit log · inspect (wealth scan) · seize · purge

**Economy**
Plot bank & upkeep · virtual vaults (Base64 pages) · offline generators (iron/gold/diamond) · crop boost · factories & auto-restock · smart hoppers & smart-clear · tip jars · chest shops (sign-based) · global market with GPS navigation · anonymous blackmarket with snitch tips · Mayor system (richest plot sets server tax) · level-up rewards

**Cosmetics & social**
Built-in ArmorStand holograms + player holograms with profanity filter · titles & action bars · music discs · plot chat · mailboxes (barrel + holographic drop zone) · GPS trails · radar map · elevators (iron blocks) · drone camera mode · border & particle cosmetics · leaderboard hologram

**Integrations**
Discord JDA bot (chat sync, logs, snitch tips, Plot Lord alerts) · PlaceholderAPI · LuckPerms premium tiers · Simple Voice Chat isolation · BlueMap

## ⚙️ Configuration

Everything lives in `plugins/PlotManager/`:

```
plugins/PlotManager/
├── config.yml          # mechanics, prices, protections, sounds, GUI settings
├── lang/
│   ├── en.yml ro.yml es.yml de.yml   # messages (edit freely)
│   └── preferences.yml # per-player language choices
└── plots/              # plot data (auto-saved async)
```

Message-related keys that used to sit in `config.yml` still work — the `lang/` files simply take priority, so old customized configs keep running.

## 👑 Premium tiers (LuckPerms)

No custom permission nodes needed — map your LuckPerms groups directly:

```yaml
premium:
  tiers:
    vip:
      luckperms_groups:
        - "vip"
        - "sponsor"
```

Members of those groups automatically get that tier's claim cost, plot cap, hopper/spawner limits and hologram style.

## 🤖 Discord bot

Set `discord.enabled: true` and paste a bot token — the JDA bot is **bundled inside the jar** (shaded & relocated by Maven), so no external Discord plugins are needed.

- Discord ⇄ Minecraft chat sync (both directions)
- Claim / reset / purge / seize / snitch logs to configurable channels
- "Plot Lord" role announcements when a plot reaches the configured level

## ❓ FAQ

**Does it run on 26.x servers?** Yes — one jar covers Paper 1.21.0 → 1.21.11 and 26.x. The `api-version` stays at `1.21` for the widest compatibility.

**Can I add my own language?** Copy `lang/en.yml` to `lang/<code>.yml`, translate, restart. It's picked up automatically.

**Do I need a hologram plugin?** No. Holograms are native ArmorStands.

**I customized messages in `config.yml` before.** They still apply — `lang/` values only take priority once present.

**Where do players' language choices live?** `lang/preferences.yml` — delete a line to reset that player.

---

<div align="center">

**PlotManager V1** · Copyright (C) **RedGlitchX** · All Rights Reserved

</div>
