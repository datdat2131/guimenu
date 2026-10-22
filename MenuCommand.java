package vn.astrax.menu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /menu [id] và /astraxmenu <reload|list|open|check>. */
final class MenuCommand implements CommandExecutor, TabCompleter {
    private final AstraxMenuPlugin plugin;

    MenuCommand(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("astraxmenu")) return admin(s, a);
        if (!(s instanceof Player p)) {
            plugin.messages().send(s, "players-only");
            return true;
        }
        if (!p.hasPermission("astraxmenu.use")) {
            plugin.messages().send(p, "no-permission");
            return true;
        }
        String id = a.length > 0 ? a[0] : plugin.getConfig().getString("main-menu", "main");
        if (!plugin.menus().open(p, id)) plugin.messages().send(p, "menu-not-found", "menu", id);
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("astraxmenu.admin")) {
            plugin.messages().send(s, "no-permission");
            return true;
        }
        String sub = a.length == 0 ? "help" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> plugin.messages().send(s, "reloaded", "info", plugin.reloadAll(s));
            case "list" -> {
                s.sendMessage(Text.c("&6Menu: &f" + String.join(", ", plugin.menus().ids())));
                s.sendMessage(Text.c("&6Sách: &f" + String.join(", ", plugin.books().ids())));
                s.sendMessage(Text.c("&6Rank: &f" + String.join(", ", plugin.ranks().ids())));
            }
            case "open" -> {
                if (a.length < 3) {
                    s.sendMessage(Text.c("&e/astraxmenu open <người chơi> <menu>"));
                    break;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    plugin.messages().send(s, "player-not-found", "player", a[1]);
                    break;
                }
                if (!plugin.menus().open(t, a[2])) plugin.messages().send(s, "menu-not-found", "menu", a[2]);
            }
            case "check" -> {
                s.sendMessage(Text.c("&6[AstraxMenu] &7PlaceholderAPI: &f" + Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                        + " &7| Essentials: &f" + (Bukkit.getPluginManager().isPluginEnabled("Essentials"))));
                plugin.ranks().diagnose(s);
                List<String> w = plugin.menus().validate();
                if (w.isEmpty()) s.sendMessage(Text.c("&aMọi liên kết [open]/[book] đều hợp lệ."));
                else for (String x : w) s.sendMessage(Text.c("&c! " + x));
            }
            default -> {
                s.sendMessage(Text.c("&e/astraxmenu reload &7- tải lại cấu hình"));
                s.sendMessage(Text.c("&e/astraxmenu list &7- liệt kê menu/sách/rank"));
                s.sendMessage(Text.c("&e/astraxmenu open <người> <menu> &7- mở menu cho người chơi"));
                s.sendMessage(Text.c("&e/astraxmenu check &7- kiểm tra cấu hình, kit, liên kết"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("astraxmenu")) {
            if (!s.hasPermission("astraxmenu.admin")) return out;
            if (a.length == 1) out.addAll(List.of("reload", "list", "open", "check"));
            else if (a.length == 2 && a[0].equalsIgnoreCase("open")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            else if (a.length == 3 && a[0].equalsIgnoreCase("open")) out.addAll(plugin.menus().ids());
        } else if (a.length == 1) {
            out.addAll(plugin.menus().ids());
        }
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
