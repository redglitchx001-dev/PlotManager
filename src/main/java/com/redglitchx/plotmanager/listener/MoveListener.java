package com.redglitchx.plotmanager.listener;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.PlayerSession;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import com.redglitchx.plotmanager.util.FX;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

public class MoveListener implements Listener {
    private final PlotManager plugin;

    public MoveListener(PlotManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        PlayerSession session = plugin.session(player);
        Plot from = plugin.store.index.at(event.getFrom());
        Plot to = plugin.store.index.at(event.getTo());

        if (to != null && to.frozen && !plugin.bypass(player) && !session.adminSpy) {
            event.setCancelled(true);
            plugin.lang.msg(player, "admin.quarantine_message");
            return;
        }

        if (to != null && to.isBanned(player.getUniqueId()) && !plugin.bypass(player) && !session.adminSpy
                && plugin.cfg().getBoolean("bouncer_shield.enabled", true)) {
            long now = System.currentTimeMillis();
            event.setTo(event.getFrom());
            Vector knock = to.cuboid().outward(event.getTo()).multiply(plugin.cfg().getDouble("bouncer_shield.knockback_strength", 1.5));
            player.setVelocity(knock);
            FX.spawn(event.getTo(), plugin.cfg().getString("bouncer_shield.particle_type", "VILLAGER_HAPPY"),
                    plugin.cfg().getInt("bouncer_shield.particle_count", 30));
            if (now - session.lastBouncer > plugin.cfg().getInt("bouncer_shield.cooldown_seconds", 3) * 1000L) {
                plugin.lang.msg(player, "bouncer_shield.cooldown_message");
                FX.play(player, plugin.cfg().getString("bouncer_shield.knockback_sound", "ENTITY_IRON_GOLEM_HURT"));
                session.lastBouncer = now;
            }
            return;
        }

        if (session.drone) {
            plugin.tickDrone(player, to);
        }

        if (from != to) {
            if (from != null) leave(player, from);
            if (to != null) enter(player, to);
            session.lastPlot = to == null ? null : to.id;
            plugin.updateFly(player, to);
        }

        if (plugin.cfg().getBoolean("titles.actionbar.enabled", true) && player.getTicksLived() % 20 == 0) {
            if (to != null) {
                String bar = plugin.lang.line(player, "titles.actionbar.inside_plot");
                Text.actionBar(player, plugin.placeholders(player, bar, to));
            } else {
                Text.actionBar(player, plugin.lang.line(player, "titles.actionbar.wilderness"));
            }
        }

        if (session.gpsTarget != null) {
            plugin.tickGps(player);
        }

        if (to != null && to.particleCosmetic != null && player.getTicksLived() % 8 == 0 && to.isMember(player.getUniqueId())) {
            String effect = plugin.cfg().getString("cosmetics.particles." + to.particleCosmetic + ".effect", "HEART");
            FX.spawn(player.getLocation().add(0, 0.2, 0), effect, 3);
        }
    }

    private void enter(Player player, Plot plot) {
        if (plugin.cfg().getBoolean("titles.enter_plot.enabled", true)) {
            Text.title(player,
                    plugin.placeholders(player, plugin.lang.line(player, "titles.enter_plot.title"), plot),
                    plugin.placeholders(player, plugin.lang.line(player, "titles.enter_plot.subtitle"), plot),
                    plugin.cfg().getInt("titles.enter_plot.fade_in", 10),
                    plugin.cfg().getInt("titles.enter_plot.stay", 40),
                    plugin.cfg().getInt("titles.enter_plot.fade_out", 10));
            FX.play(player, plugin.cfg().getString("titles.enter_plot.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                    (float) plugin.cfg().getDouble("titles.enter_plot.sound_volume", 0.5),
                    (float) plugin.cfg().getDouble("titles.enter_plot.sound_pitch", 1.0));
        }
        if (plugin.cfg().getBoolean("music.play_on_enter", true) && plot.musicUnlocked && plot.musicDisc != null) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(plot.musicDisc),
                        (float) plugin.cfg().getDouble("music.volume", 1.0), 1f);
            } catch (Exception ignored) {}
        }
        if (!plot.isOwner(player.getUniqueId())) {
            if (plugin.getServer().getOfflinePlayer(plot.owner).isOnline()) {
                plugin.addPlotExp(plot, plugin.cfg().getInt("leveling.exp_per_visitor_minute", 5) / 12);
            } else {
                plot.visitorsOffline++;
            }
        }
        plugin.voice.enter(player, plot);
    }

    private void leave(Player player, Plot plot) {
        if (plugin.cfg().getBoolean("titles.leave_plot.enabled", true)) {
            Text.title(player,
                    plugin.lang.line(player, "titles.leave_plot.title"),
                    plugin.placeholders(player, plugin.lang.line(player, "titles.leave_plot.subtitle"), plot),
                    plugin.cfg().getInt("titles.leave_plot.fade_in", 5),
                    plugin.cfg().getInt("titles.leave_plot.stay", 20),
                    plugin.cfg().getInt("titles.leave_plot.fade_out", 5));
            FX.play(player, plugin.cfg().getString("titles.leave_plot.sound", "BLOCK_NOTE_BLOCK_BASS"),
                    (float) plugin.cfg().getDouble("titles.leave_plot.sound_volume", 0.3),
                    (float) plugin.cfg().getDouble("titles.leave_plot.sound_pitch", 0.5));
        }
        if (plugin.cfg().getBoolean("music.stop_on_leave", true) && plot.musicDisc != null) {
            try { player.stopSound(Sound.valueOf(plot.musicDisc)); } catch (Exception ignored) {}
        }
        plugin.voice.leave(player, plot);
    }

    @EventHandler
    public void onToggleFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (plugin.session(player).drone) return;
        Plot plot = plugin.store.index.at(player.getLocation());
        if (event.isFlying() && !plugin.canFly(player, plot) && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        plugin.tryElevator(event.getPlayer(), true);
    }
}
