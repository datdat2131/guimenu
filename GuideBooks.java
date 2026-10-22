package vn.astrax.menu;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/** Sách hướng dẫn (guides.yml) mở bằng [book] id hoặc /menu. */
final class GuideBooks {
    private record BookDef(String title, String author, List<String> pages) {}

    private final AstraxMenuPlugin plugin;
    private Map<String, BookDef> books = new LinkedHashMap<>();

    GuideBooks(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    int reload() {
        Map<String, BookDef> out = new LinkedHashMap<>();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "guides.yml"));
        ConfigurationSection root = y.getConfigurationSection("books");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection b = root.getConfigurationSection(id);
                if (b == null) continue;
                List<String> pages = b.getStringList("pages");
                if (pages.isEmpty()) {
                    plugin.getLogger().warning("[guides.yml:" + id + "] không có trang nào -> bỏ qua");
                    continue;
                }
                out.put(id.toLowerCase(Locale.ROOT), new BookDef(b.getString("title", id), b.getString("author", "Server"), pages));
            }
        }
        books = out;
        return out.size();
    }

    boolean has(String id) {
        return id != null && books.containsKey(id.trim().toLowerCase(Locale.ROOT));
    }

    Set<String> ids() {
        return books.keySet();
    }

    boolean open(Player p, String id) {
        BookDef b = books.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT));
        if (b == null) return false;
        List<Component> pages = new ArrayList<>();
        for (String s : b.pages()) pages.add(Text.c(Placeholders.apply(plugin, p, s)));
        p.openBook(Book.book(Text.c(b.title()), Text.c(b.author()), pages));
        return true;
    }
}
