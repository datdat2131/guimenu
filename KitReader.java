package vn.astrax.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/** Đọc kits.yml của EssentialsX (chỉ đọc) để xem trước nội dung /kit. */
final class KitReader {
    record Kit(String name, long delay, List<ItemStack> items, int others) {}

    private final AstraxMenuPlugin plugin;
    private String path = "auto";
    private File file;
    private long mtime = Long.MIN_VALUE;
    private final Map<String, Kit> cache = new HashMap<>();

    KitReader(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    synchronized void setPath(String p) {
        path = (p == null || p.isBlank()) ? "auto" : p.trim();
        file = null;
        mtime = Long.MIN_VALUE;
        cache.clear();
    }

    synchronized File resolve() {
        File plugins = plugin.getDataFolder().getParentFile();
        if (!"auto".equalsIgnoreCase(path)) return new File(plugins, path);
        for (String d : new String[]{"Essentials", "EssentialsX"}) {
            File f = new File(new File(plugins, d), "kits.yml");
            if (f.isFile()) return f;
        }
        return null;
    }

    synchronized Kit get(String name) {
        refresh();
        return name == null ? null : cache.get(name.toLowerCase(Locale.ROOT));
    }

    synchronized int count() {
        refresh();
        return cache.size();
    }

    synchronized String where() {
        File f = resolve();
        return f == null ? "(không tìm thấy kits.yml)" : f.getPath();
    }

    private void refresh() {
        File f = resolve();
        if (f == null || !f.isFile()) {
            cache.clear();
            file = null;
            mtime = Long.MIN_VALUE;
            return;
        }
        if (f.equals(file) && f.lastModified() == mtime) return;
        file = f;
        mtime = f.lastModified();
        cache.clear();
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection ks = y.getConfigurationSection("kits");
            if (ks == null) return;
            for (String name : ks.getKeys(false)) {
                ConfigurationSection k = ks.getConfigurationSection(name);
                if (k == null) continue;
                long delay = (long) k.getDouble("delay", 0);
                List<ItemStack> items = new ArrayList<>();
                int others = 0;
                List<?> list = k.getList("items");
                if (list != null) {
                    for (Object o : list) {
                        String line = String.valueOf(o).trim();
                        if (line.isEmpty()) continue;
                        ItemStack it = line.startsWith("/") ? null : parse(line);
                        if (it != null) items.add(it);
                        else others++;
                    }
                }
                cache.put(name.toLowerCase(Locale.ROOT), new Kit(name, delay, items, others));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Không đọc được " + f + ": " + ex);
            cache.clear();
        }
    }

    /** Dòng item Essentials: "<material> <số lượng> [enchant:cấp] [name:Tên_Có_Gạch] [lore:a|b]". */
    private ItemStack parse(String line) {
        String[] t = line.split("\\s+");
        String mt = t[0];
        int c = mt.indexOf(':');
        if (c > 0) mt = mt.substring(0, c);
        Material m = Material.matchMaterial(mt.toUpperCase(Locale.ROOT));
        if (m == null || !m.isItem() || m.isAir()) return null;
        int amount = 1, start = 1;
        if (t.length > 1) {
            try {
                amount = Integer.parseInt(t[1]);
                start = 2;
            } catch (NumberFormatException ignored) {
                // không có số lượng
            }
        }
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        List<Component> lore = new ArrayList<>();
        for (int i = start; i < t.length; i++) {
            String tok = t[i];
            int ci = tok.indexOf(':');
            if (ci <= 0) continue;
            String key = tok.substring(0, ci).toLowerCase(Locale.ROOT);
            String val = tok.substring(ci + 1);
            switch (key) {
                case "name", "displayname" -> meta.displayName(Text.c(val.replace('_', ' ')));
                case "lore" -> {
                    for (String s : val.split("\\|")) lore.add(Text.c(s.replace('_', ' ')));
                }
                case "player", "owner", "color", "effect", "power", "itemflags", "splash", "firework" -> { }
                default -> {
                    try {
                        Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
                        if (e != null) {
                            int lv = 1;
                            try {
                                lv = Integer.parseInt(val);
                            } catch (NumberFormatException ignored) {
                                // mặc định cấp 1
                            }
                            meta.addEnchant(e, Math.max(1, lv), true);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // key không hợp lệ
                    }
                }
            }
        }
        if (amount > 64) lore.add(Text.c("&7Số lượng: &fx" + amount));
        if (!lore.isEmpty()) meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }
}
