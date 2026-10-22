package vn.astrax.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** {player} {online} {max} {rank} + PlaceholderAPI (nếu có). */
final class Placeholders {
    private Placeholders() {}

    static String apply(AstraxMenuPlugin pl, Player p, String s) {
        if (s == null) return "";
        s = s.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
        if (p == null) return s;
        s = s.replace("{player}", p.getName());
        if (s.contains("{rank}")) s = s.replace("{rank}", pl.ranks().currentRankName(p));
        if (s.indexOf('%') >= 0 && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                s = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, s);
            } catch (Throwable ignored) {
                // PAPI lỗi thì giữ nguyên chuỗi
            }
        }
        return s;
    }
}
