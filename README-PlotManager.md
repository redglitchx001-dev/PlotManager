# 🟢 PlotManager: The Ultimate Plot System
*Designed by RedGlitchX*

> **Copyright (c) 2026 RedGlitchX. All Rights Reserved.**
> Proprietary software — see [LICENSE](LICENSE). Redistribution prohibited.

**PlotManager** is a revolutionary, 53-feature survival plot management plugin designed to completely redefine how players claim, build, and interact on a Minecraft Survival server. It leaves absolutely zero room for lag, griefing, or exploits, and introduces next-generation features that no other plugin currently possesses.

## 🌟 Core Philosophy
- **Zero Dependencies for Core Mechanics:** Holograms are built-in natively using `ArmorStands`. No HD, no FancyHolograms.
- **100% Configurable:** If it exists on the screen, in chat, or in a GUI, it is editable in the `config.yml`.
- **Absolute Protection:** Prevents every exploit known to Minecraft (Piston pushing, Hopper minecart theft, Dispenser arrows, liquid flow).
- **Asynchronous & Lag-Free:** All massive operations (Saving, FAWE Rollbacks, Border Particles) are strictly run off the main thread.
- **Multi-Language:** Full translations for **English (en), Română (ro), Español (es), Deutsch (de)** — plus any custom language you drop in `plugins/PlotManager/lang/`.

## 🌍 Languages / Limbă / Idioma / Sprache
- **Server default:** set `language.default` in `config.yml` (`en`, `ro`, `es`, `de`, or any custom code).
- **Per player:** with `language.per-player: true`, everyone picks their own language via `/plot lang <code>` (or the Language button in `/plot menu`) — preference is saved in `lang/preferences.yml`.
- **Fully editable:** every message, GUI label, sign line, title and action bar lives in `plugins/PlotManager/lang/<code>.yml` (auto-extracted on first run). Colors with `&` codes and `&#RRGGBB` hex, placeholders like `%player%`.
- **Add a language:** copy `lang/en.yml` to `lang/fr.yml` (for example), translate it — it is detected automatically. Missing keys fall back to English, then to legacy `config.yml` values.

## 📦 Required & Optional Dependencies

**Required:**
- **Paper 1.21.4+** and **Java 21+** — that is all.

**Optional, for the money features only:**
- **Vault** (economy bridge) + an economy provider such as **EssentialsX** or **CMI**.
  Without them PlotManager still loads; claim costs, plot banks, shops and paid
  upgrades simply stay switched off. Run `/plot hooks` to see the live status.

**Soft-Depends (Optional, unlocks features):**
- **FastAsyncWorldEdit (FAWE)** — Unlocks the 7-day auto-wipe & natural rollback.
- **BlueMap** — Unlocks the live 3D web map synchronization.
- **PlaceholderAPI** — Unlocks custom placeholders in holograms and scoreboards.
- **ProtocolLib** — Unlocks advanced packet features (Holograms will fallback to ArmorStands if not found).
- **SimpleVoiceChat** — Unlocks Feature 46 (Private Plot Voice Channels).

*Note: The Discord bot (JDA) is built straight into the `.jar` file and does NOT require any external Discord plugins.*

---

## 🚀 The 53 God-Tier Features (Highlights)

### 🏰 Plot Claiming & Generation
1. **Wand-Based Selection:** Claim natural survival terrain, no ugly pre-generated grid worlds.
2. **FAWE 7-Day Rollback:** If a plot expires, FAWE pastes the original pristine terrain back with zero lag.
3. **Smart Clear:** Use `/plot flag autowipe_snow true` to auto-melt snow and destroy weeds instantly.

### 🛡️ Protection & Border Control
4. **Bouncer Shield:** Banned players physically bounce off an invisible green particle wall.
5. **Advanced Exploit Prevention:** Endermen blocked, crop trampling blocked, pistons blocked.
6. **Live 3D BlueMap Web Sync:** Plots instantly draw color-coded boxes on your server's live web map.

### 💰 Economy & Shops
7. **Plot Vaults:** Virtual double-chests saved in Base64 so they never corrupt.
8. **Plot Factories & Auto-Restock:** Vaults automatically restock empty Chest Shops while you sleep.
9. **Offline Generators:** Buy generators that deposit Emeralds into your vault every 30 minutes, even when offline.
10. **The Blackmarket:** Secret illegal GUI shop that avoids taxes, with a 5% chance the Discord Bot "snitches" to the admins!

### ⚙️ Mechanics & Interaction
11. **Advanced Drone Mode:** `/plot drone` puts you in invisible flight mode to view your plot from above. Teleports you back if you fly 10 blocks outside. Strictly Survival mode to prevent underground X-Ray.
12. **GPS Navigation:** Clicking a plot in `/plot browse` draws a glowing particle path to their front door.
13. **Holographic Mailbox:** Drop an item in a specific 3D zone, and it visually gets sucked into the owner's Mailbox GUI.
14. **Plot Private Voice Chat:** Full SimpleVoiceChat hook. Walk inside a plot to instantly enter a soundproof voice channel.

### 🎨 Cosmetics & Upgrades
15. **Massive Cosmetic Shop:** 25 Particle Trails and 25 Custom Block Borders (Slime, Amethyst, Obsidian).
16. **Built-in Auto-Holograms:** Free plots show price holograms. Claimed plots show stats. Premium VIP plots get custom Golden Star Holograms.

---

## 🔗 LuckPerms Integration
You **do not** need to mess with complex permission nodes! Just type your LuckPerms group name into the `config.yml`:
```yaml
vip:
  luckperms_groups:
    - "vip"
    - "sponsor"
```

## 🛠️ Build Instructions for Developers
A massive, 1,300+ line blueprint file (`PlotManager-Master-Plan.txt`) has been generated on your device. This file contains the **exact Java architecture**, NBT packet instructions, and Edge-Case logic required to code this plugin without a single flaw. 

**Give that file to the building AI, and watch the magic happen.**

---

© 2026 **RedGlitchX** — PlotManager. All Rights Reserved.
