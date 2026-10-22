package vn.astrax.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Hành động của nút: "[tag] nội dung".
 * [player] lệnh | [console] lệnh | [open] menu | [rank] [id] | [book] id | [close]
 * [message] chữ | [sound] key | [link] url | chữ hiển thị
 */
final class Actions {
    private final AstraxMenuPlugin plugin;

    Actions(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    /** Chạy ở tick kế tiếp (không đổi inventory ngay trong lúc xử lý click). */
    void run(Player p, List<String> actions) {
        if (actions.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            for (String a : actions) exec(p, a);
        });
    }

    private static String cmd(String s) {
        s = s.trim();
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private void exec(Player p, String raw) {
        String a = raw == null ? "" : raw.trim();
        if (a.isEmpty()) return;
        String tag = "", arg = a;
        if (a.startsWith("[")) {
            int i = a.indexOf(']');
            if (i > 0) {
                tag = a.substring(1, i).trim().toLowerCase(Locale.ROOT);
                arg = a.substring(i + 1).trim();
            }
        }
        arg = Placeholders.apply(plugin, p, arg);
        switch (tag) {
            case "player" -> p.performCommand(cmd(arg));
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd(arg));
            case "open", "menu" -> {
                if (!plugin.menus().open(p, arg)) plugin.messages().send(p, "menu-not-found", "menu", arg);
            }
            case "rank" -> plugin.ranks().openFromAction(p, arg);
            case "book" -> {
                if (!plugin.books().open(p, arg)) plugin.messages().send(p, "book-not-found", "book", arg);
            }
            case "close" -> p.closeInventory();
            case "message", "msg" -> p.sendMessage(Text.c(arg));
            case "sound" -> Items.sound(p, arg);
            case "link" -> {
                String[] parts = arg.split("\\|", 2);
                String url = parts[0].trim();
                String label = parts.length > 1 ? parts[1].trim() : "&b&n" + url;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    plugin.getLogger().warning("[link] URL phải bắt đầu bằng http:// hoặc https:// : " + url);
                    return;
                }
                Component c = Text.c(label).clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(HoverEvent.showText(Text.c("&7Bấm để mở liên kết")));
                p.sendMessage(c);
            }
            default -> plugin.getLogger().warning("Hành động không hợp lệ: " + raw);
        }
    }
}
