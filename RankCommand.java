package vn.astrax.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /rank, /rank <id>, /rank list. */
final class RankCommand implements CommandExecutor, TabCompleter {
    private final AstraxMenuPlugin plugin;

    RankCommand(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("astraxmenu.rank")) {
            plugin.messages().send(s, "no-permission");
            return true;
        }
        if (a.length > 0 && a[0].equalsIgnoreCase("list")) {
            plugin.ranks().sendText(s);
            return true;
        }
        if (!(s instanceof Player p)) {
            plugin.ranks().sendText(s);
            return true;
        }
        if (a.length == 0) {
            plugin.ranks().openList(p);
            return true;
        }
        RankManager.Rank r = plugin.ranks().find(a[0]);
        if (r == null) plugin.messages().send(p, "rank-not-found", "rank", a[0]);
        else plugin.ranks().openDetail(p, r);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.add("list");
            out.addAll(plugin.ranks().ids());
            String last = a[0].toLowerCase(Locale.ROOT);
            out.removeIf(x -> !x.startsWith(last));
        }
        return out;
    }
}
