package com.redglitchx.plotmanager.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public final class Text {
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SEC = LegacyComponentSerializer.legacySection();
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat MONEY_INT = new DecimalFormat("#,##0");
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Text() {}

    public static String color(String input) {
        if (input == null) return "";
        Matcher m = HEX.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder repl = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                repl.append('§').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(repl.toString()));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static Component component(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        String colored = color(input);
        return LEGACY_SEC.deserialize(colored);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String apply(String template, Map<String, String> placeholders) {
        if (template == null) return "";
        String out = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(component(message));
    }

    public static void sendPrefixed(CommandSender sender, String prefix, String message) {
        send(sender, prefix + (message == null ? "" : message));
    }

    public static void broadcast(String message) {
        Bukkit.getServer().sendMessage(component(message));
    }

    public static String money(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.001) {
            return MONEY_INT.format(amount);
        }
        return MONEY.format(amount);
    }

    public static String formatDate(long epoch, String pattern, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern == null ? "dd/MM/yyyy HH:mm" : pattern);
        if (timezone != null && !timezone.isEmpty()) {
            sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        return sdf.format(new Date(epoch));
    }

    public static Component clickableUrl(String message, String url, String hover) {
        return component(message)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(component(hover)));
    }

    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                component(title == null ? "" : title),
                component(subtitle == null ? "" : subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(Math.max(0, fadeIn) * 50L),
                        java.time.Duration.ofMillis(Math.max(0, stay) * 50L),
                        java.time.Duration.ofMillis(Math.max(0, fadeOut) * 50L)
                )
        ));
    }

    public static void actionBar(Player player, String message) {
        player.sendActionBar(component(message));
    }

    public static String serializeLegacy(Component component) {
        return LEGACY_AMP.serialize(component);
    }

    public static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
