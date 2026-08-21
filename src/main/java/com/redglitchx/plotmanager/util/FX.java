/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public final class FX {
    private static final Map<String, String> PARTICLE_ALIASES = Map.ofEntries(
            Map.entry("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
            Map.entry("HAPPY_VILLAGER", "HAPPY_VILLAGER"),
            Map.entry("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
            Map.entry("SPELL_WITCH", "WITCH"),
            Map.entry("CRIT_MAGIC", "ENCHANTED_HIT"),
            Map.entry("MAGIC_CRIT", "ENCHANTED_HIT"),
            Map.entry("SMOKE_LARGE", "LARGE_SMOKE"),
            Map.entry("SMOKE_NORMAL", "SMOKE"),
            Map.entry("SNOW_SHOVEL", "ITEM_SNOWBALL"),
            Map.entry("DRIP_LAVA", "DRIPPING_LAVA"),
            Map.entry("DRIP_WATER", "DRIPPING_WATER"),
            Map.entry("TOTEM", "TOTEM_OF_UNDYING"),
            Map.entry("SLIME", "ITEM_SLIME"),
            Map.entry("REDSTONE", "DUST"),
            Map.entry("SPELL", "EFFECT"),
            Map.entry("SPELL_MOB", "ENTITY_EFFECT"),
            Map.entry("SPELL_INSTANT", "INSTANT_EFFECT"),
            Map.entry("COMPOSTER", "COMPOSTER"),
            Map.entry("CAMPFIRE_COSY_SMOKE", "CAMPFIRE_COSY_SMOKE"),
            Map.entry("SOUL_FIRE_FLAME", "SOUL_FIRE_FLAME"),
            Map.entry("CHERRY_LEAVES", "CHERRY_LEAVES")
    );

    private FX() {}

    public static Particle particle(String name, Particle fallback) {
        if (name == null || name.isEmpty()) return fallback;
        String key = name.trim().toUpperCase(Locale.ROOT);
        String mapped = PARTICLE_ALIASES.getOrDefault(key, key);
        try {
            return Particle.valueOf(mapped);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return Particle.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static Sound sound(String name, Sound fallback) {
        if (name == null || name.isEmpty()) return fallback;
        String key = name.trim().toUpperCase(Locale.ROOT);
        try {
            return Sound.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static void play(Player player, String soundName) {
        Sound s = sound(soundName, Sound.UI_BUTTON_CLICK);
        player.playSound(player.getLocation(), s, 1f, 1f);
    }

    public static void play(Player player, String soundName, float volume, float pitch) {
        Sound s = sound(soundName, Sound.UI_BUTTON_CLICK);
        player.playSound(player.getLocation(), s, volume, pitch);
    }

    public static void spawn(Location loc, String particleName, int count) {
        if (loc == null || loc.getWorld() == null) return;
        Particle p = particle(particleName, Particle.HAPPY_VILLAGER);
        spawn(loc, p, count, Color.LIME);
    }

    public static void spawn(Location loc, Particle particle, int count, Color color) {
        if (loc == null || loc.getWorld() == null || particle == null) return;
        World world = loc.getWorld();
        try {
            if (particle == Particle.DUST || particle.name().contains("DUST")) {
                Particle.DustOptions dust = new Particle.DustOptions(color == null ? Color.LIME : color, 1.2f);
                world.spawnParticle(particle, loc, count, 0.35, 0.6, 0.35, 0.01, dust);
            } else {
                world.spawnParticle(particle, loc, count, 0.35, 0.6, 0.35, 0.01);
            }
        } catch (Exception ignored) {
            try {
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, count, 0.35, 0.6, 0.35, 0.01);
            } catch (Exception ignored2) {
            }
        }
    }

    public static Color color(String name) {
        if (name == null) return Color.LIME;
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "RED" -> Color.RED;
            case "BLUE" -> Color.BLUE;
            case "YELLOW" -> Color.YELLOW;
            case "GOLD" -> Color.ORANGE;
            case "WHITE" -> Color.WHITE;
            case "BLACK" -> Color.BLACK;
            case "AQUA", "CYAN" -> Color.AQUA;
            case "PURPLE" -> Color.PURPLE;
            case "ORANGE" -> Color.ORANGE;
            default -> Color.LIME;
        };
    }
}
