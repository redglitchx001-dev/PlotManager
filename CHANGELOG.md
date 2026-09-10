# Changelog

PlotManager — Copyright (c) 2026 **RedGlitchX**. All Rights Reserved.

## Unreleased — one jar for all versions

### Changed
- **Single deliverable: `PlotManagerv1-All.jar`.** The per-version jars are gone;
  the one universal jar is compiled against the oldest supported Paper API
  (1.21) and runs on **1.21.x through 26.x**.
- CI now **verify-compiles** the code against every supported Paper version and
  builds only the universal jar; releases ship that jar + source + checksums.
- Minimum server version lowered to **Paper 1.21**.

### Fixed
- `PlotStore.saveSync` can no longer lose **all** plots because of one corrupt
  entry, and no longer risks `ConcurrentModificationException` from the
  blackmarket list during async saves.
- `PlotStore.readPlot` no longer throws on a malformed plot id — the entry is
  skipped with a warning instead.
- Merged plots are now persisted immediately (`mergePlots` saves).
- NPE guards: `/plot freeze` with an unloaded plot world, owner-head/skull and
  visitor tracking for ownerless plots, deleted-plot tip replies, null-seller
  blackmarket listings, null shop materials, and null PlaceholderAPI params.
- Hologram purchase errors now use the null-safe translated message instead of
  a raw config lookup that could NPE on edited configs.
- `Items.glow` no longer uses the deprecated `Enchantment.*` constant (resolved
  by key, safe on future Paper versions).
- Removed dead code and unused imports across listeners, tasks and GUIs.

## 1.0.0

### Fixed — the plugin no longer depends on the optional plugins
- **Startup is now failure-proof.** Every subsystem (languages, storage, Discord,
  BlueMap, listeners, holograms, tasks, banner) boots inside its own guard. A
  failure is logged and the feature is marked *degraded* instead of aborting
  `onEnable` and leaving the server with no PlotManager at all.
- **Vault is no longer able to break the plugin.** `EconomyService` now talks to
  Vault purely through reflection and holds no compile-time reference to
  `net.milkbowl.vault.*`, so a missing, renamed or forked Vault can never throw
  `NoClassDefFoundError` while the plugin is enabling.
- **Vault moved from `depend` to `softdepend`** in `plugin.yml`: the plugin loads
  and runs with money features switched off rather than refusing to start.
- **The bundled Discord bot is isolated.** It was split into a JDA-free façade
  (`DiscordBot`) and a gateway (`DiscordGateway`) that is loaded reflectively and
  only when the bot is enabled with a real token — the shaded Discord library is
  never touched otherwise.
- **PlaceholderAPI stays reflection-only** and now reports whether the expansion
  actually registered.
- **Repeating tasks are individually guarded** and mute themselves after five
  consecutive failures instead of spamming the console or dragging other
  features down.
- **`onDisable` is null-safe**, so a partially started plugin still saves plots.
- **Fixed `VoiceHook` writing its channel tag into the radar-map key**; it now
  uses a dedicated `voicechannel` key and no longer calls the no-op
  `Player#setPersistent`.
- Shading now merges `META-INF/services`, keeping the bundled libraries working
  after relocation.

### Added
- **`/plot hooks`** (aliases `diagnose`, `status`) — a dependency report listing
  every integration, whether it is required or optional, and any degraded
  subsystem. Works from the console too.
- `economy.no_economy_message`, translated into all four bundled languages.
- Release pipeline: one `.jar` per Minecraft version (from the oldest supported
  release to the newest Paper build) plus the full source code, published to
  GitHub Releases with SHA256 checksums.
- Jar manifest now carries `Implementation-Vendor`, `Author`, `Copyright` and
  `License` entries, and the licence is embedded in the jar.

### Changed
- Build no longer depends on JitPack, EngineHub, CodeMC or the BlueMap
  repository, and drops the unused VaultAPI and LuckPerms artifacts — fewer
  moving parts, far fewer ways for a build to fail.
- Version scheme is now semantic (`1.0.0`); jars are named
  `PlotManager-<version>-mc<minecraft>.jar`.

### Legal
- Added the proprietary **LICENSE** (Copyright (c) 2026 RedGlitchX, All Rights
  Reserved) and stamped a copyright header on every source, config and language
  file.
