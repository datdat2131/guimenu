package vn.astrax.menu;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

final class Messages {
    private static final Map<String, String> DEF = Map.ofEntries(
            Map.entry("prefix", "&8[&6BoxMine&8] "),
            Map.entry("no-permission", "&cBạn không có quyền dùng lệnh này."),
            Map.entry("players-only", "&cLệnh này chỉ dùng trong game."),
            Map.entry("menu-not-found", "&cKhông có menu &e{menu}&c."),
            Map.entry("book-not-found", "&cKhông có sách hướng dẫn &e{book}&c."),
            Map.entry("rank-not-found", "&cKhông có rank &e{rank}&c. Gõ &e/rank&c để xem danh sách."),
            Map.entry("rank-not-owned", "&cBạn chưa có rank &f{rank}&c nên chưa dùng được kit này."),
            Map.entry("kit-missing", "&cRank này không có kit."),
            Map.entry("reloaded", "&aĐã tải lại AstraxMenu &7({info})"),
            Map.entry("player-not-found", "&cKhông thấy người chơi &e{player}&c.")
    );
    private final AstraxMenuPlugin plugin;
    private YamlConfiguration yaml = new YamlConfiguration();

    Messages(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    void reload() {
        yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    String raw(String key) {
        return yaml.getString(key, DEF.getOrDefault(key, key));
    }

    void send(CommandSender to, String key, String... kv) {
        String s = raw(key);
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        to.sendMessage(Text.c(raw("prefix") + s));
    }
}
