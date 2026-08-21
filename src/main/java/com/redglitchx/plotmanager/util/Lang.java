/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.util;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.PlayerSession;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-language message manager (English, Romanian, Spanish, German + custom).
 * <p>
 * Resolution order for every key:
 * <ol>
 *   <li>the viewer's language file ({@code plugins/PlotManager/lang/<code>.yml})</li>
 *   <li>the embedded English defaults</li>
 *   <li>legacy {@code config.yml} value (compatibility with pre-language configs)</li>
 *   <li>the key itself</li>
 * </ol>
 * Players may pick their own language with {@code /plot lang <code>} when
 * {@code language.per-player} is enabled in config.yml.
 */
public final class Lang {

    private final PlotManager plugin;
    private final File folder;
    private final Map<String, YamlConfiguration> loaded = new ConcurrentHashMap<>();
    private final YamlConfiguration embeddedEn = new YamlConfiguration();
    private final List<String> bundled = new ArrayList<>();
    private YamlConfiguration prefs;
    private String defaultCode = "en";
    private String fallbackCode = "en";
    private boolean perPlayer = true;
    private boolean autoDetect = true;

    public Lang(PlotManager plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "lang");
    }

    private static final List<String> KNOWN_BUNDLED = List.of("de", "en", "es", "ro");

    /** Detects bundled language files, extracts them and loads everything. */
    public void load() {
        bundled.clear();
        for (String code : KNOWN_BUNDLED) {
            if (plugin.getResource("lang/" + code + ".yml") != null) bundled.add(code);
        }
        if (bundled.isEmpty()) bundled.add("en");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create lang folder: " + folder);
        }
        for (String code : bundled) {
            File f = file(code);
            if (!f.exists()) {
                try {
                    plugin.saveResource("lang/" + code + ".yml", false);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Could not extract lang/" + code + ".yml: " + ex.getMessage());
                }
            }
        }
        // embedded English fallback straight from the jar (immune to user edits)
        embeddedEn.setDefaults(null);
        try (InputStream in = plugin.getResource("lang/en.yml")) {
            if (in != null) embeddedEn.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not read embedded lang/en.yml: " + ex.getMessage());
        }
        reload();
    }

    /** (Re)reads config settings, language files and player preferences from disk. */
    public void reload() {
        defaultCode = plugin.getConfig().getString("language.default",
                plugin.getConfig().getString("plugin.language", "en"));
        fallbackCode = plugin.getConfig().getString("language.fallback", "en");
        perPlayer = plugin.getConfig().getBoolean("language.per-player", true);
        autoDetect = plugin.getConfig().getBoolean("language.auto-detect", true);
        if (!bundled.contains(defaultCode)) defaultCode = "en";
        if (!exists(fallbackCode)) fallbackCode = "en";
        loaded.clear();
        for (String code : available()) {
            loaded.put(code, YamlConfiguration.loadConfiguration(file(code)));
        }
        File pf = new File(folder, "preferences.yml");
        prefs = YamlConfiguration.loadConfiguration(pf);
        prefs.setDefaults(embeddedEn); // never used, but keeps save() from dropping the path
        if (!pf.exists()) savePrefs();
    }

    /** Language codes available on this server (bundled + any custom files dropped in lang/). */
    public List<String> available() {
        TreeMap<String, Boolean> codes = new TreeMap<>();
        for (String c : bundled) codes.put(c, true);
        String[] files = folder.list((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (String f : files) {
                if (!f.equals("preferences.yml")) codes.put(f.substring(0, f.length() - 4), true);
            }
        }
        return new ArrayList<>(codes.keySet());
    }

    public boolean exists(String code) {
        return code != null && available().contains(code.toLowerCase(Locale.ROOT));
    }

    public String defaultCode() {
        return defaultCode;
    }

    public String fallbackCode() {
        return fallbackCode;
    }

    /** The language that should be used for this viewer (explicit choice > client locale > server default). */
    public String codeFor(CommandSender viewer) {
        if (perPlayer && viewer instanceof Player p) {
            PlayerSession s = plugin.sessions.get(p.getUniqueId());
            String code = s != null && s.lang != null ? s.lang : prefs.getString("players." + p.getUniqueId());
            if (code != null && !code.isEmpty() && !code.equals(defaultCode)) {
                code = code.toLowerCase(Locale.ROOT);
                if (exists(code)) return code;
            }
            if (autoDetect && (code == null || code.isEmpty())) {
                String client = p.locale().getLanguage().toLowerCase(Locale.ROOT);
                if (!client.equals(defaultCode) && !client.equals("en") && exists(client)) return client;
            }
        }
        return defaultCode;
    }

    /** Sets (or clears with null) a player's personal language and persists it. */
    public void setPlayerLanguage(Player player, String code) {
        PlayerSession s = plugin.sessions.get(player.getUniqueId());
        if (s != null) s.lang = code;
        if (code == null || code.isEmpty()) prefs.set("players." + player.getUniqueId(), null);
        else prefs.set("players." + player.getUniqueId(), code.toLowerCase(Locale.ROOT));
        savePrefs();
    }

    private void savePrefs() {
        try {
            prefs.save(new File(folder, "preferences.yml"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not save lang/preferences.yml: " + ex.getMessage());
        }
    }

    private File file(String code) {
        return new File(folder, code + ".yml");
    }

    private String resolve(String code, String key) {
        YamlConfiguration lang = code == null ? null : loaded.get(code);
        String value = lang == null ? null : lang.getString(key);
        if (value == null && fallbackCode != null && !fallbackCode.equals(code)) {
            YamlConfiguration fb = loaded.get(fallbackCode);
            value = fb == null ? null : fb.getString(key);
        }
        if (value == null) value = embeddedEn.getString(key);
        if (value == null) value = plugin.getConfig().getString(key); // legacy configs
        return value;
    }

    private List<String> resolveList(String code, String key) {
        YamlConfiguration lang = code == null ? null : loaded.get(code);
        List<String> value = lang == null ? null : lang.getStringList(key);
        if (value == null || value.isEmpty()) value = embeddedEn.getStringList(key);
        if (value == null || value.isEmpty()) value = plugin.getConfig().getStringList(key);
        return value == null ? List.of() : value;
    }

    /** Applies %placeholder% pairs given as varargs: ("%player%", name, ...). */
    private static String apply(String text, String... replacements) {
        if (text == null) return null;
        String out = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            out = out.replace(replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return out;
    }

    /** Raw translated line (colored, no prefix). Returns the key itself when unknown. */
    public String line(CommandSender viewer, String key, String... replacements) {
        String out = apply(resolve(codeFor(viewer), key), replacements);
        return out == null ? key : Text.color(out);
    }

    /** Translated string list (lore lines), colored. Empty list when unknown. */
    public List<String> list(CommandSender viewer, String key) {
        List<String> out = new ArrayList<>();
        for (String s : resolveList(codeFor(viewer), key)) out.add(Text.color(s));
        return out;
    }

    /** Sends a prefixed chat message. */
    public void msg(CommandSender viewer, String key, String... replacements) {
        String text = apply(resolve(codeFor(viewer), key), replacements);
        if (text == null || text.isEmpty()) return;
        Text.sendPrefixed(viewer, plugin.prefix(), Text.color(text));
    }

    /** Sends an unprefixed chat message. */
    public void raw(CommandSender viewer, String key, String... replacements) {
        String text = apply(resolve(codeFor(viewer), key), replacements);
        if (text == null || text.isEmpty()) return;
        Text.send(viewer, Text.color(text));
    }

    /** Broadcasts a translated line to every online player in their own language. */
    public void broadcast(String key, String... replacements) {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            msg(p, key, replacements);
        }
    }

    /** Sends a translated action bar message. */
    public void actionBar(Player player, String key, String... replacements) {
        String text = apply(resolve(codeFor(player), key), replacements);
        if (text == null || text.isEmpty()) return;
        Text.actionBar(player, Text.color(text));
    }

    /** Sends a translated title + subtitle (keys may contain %placeholders%). */
    public void title(Player player, String titleKey, String subtitleKey, String... replacements) {
        String t = apply(resolve(codeFor(player), titleKey), replacements);
        String s = apply(resolve(codeFor(player), subtitleKey), replacements);
        Text.title(player, t == null ? "" : Text.color(t), s == null ? "" : Text.color(s), 10, 40, 10);
    }

    /** True when the viewer's language defines the key. */
    public boolean has(CommandSender viewer, String key) {
        return resolve(codeFor(viewer), key) != null;
    }
}
