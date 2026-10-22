package vn.astrax.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/** Đọc menus.yml và mở menu cấu hình được. */
final class MenuManager {
    record ItemDef(String id, List<Integer> slots, String material, int amount, String name, List<String> lore,
                   boolean glow, String skull, List<String> left, List<String> right) {}

    record MenuDef(String id, String title, int rows, boolean filler, String openSound, List<ItemDef> items) {}

    private final class Holder implements AstraxHolder {
        final MenuDef def;
        final Map<Integer, ItemDef> bySlot = new HashMap<>();
        Inventory inv;

        Holder(MenuDef def) {
            this.def = def;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }

        @Override
        public boolean click(Player p, int slot, ClickType type) {
            ItemDef it = bySlot.get(slot);
            if (it == null) return false;
            List<String> acts = type.isRightClick() ? it.right() : it.left();
            if (acts.isEmpty()) return false;
            plugin.actions().run(p, acts);
            return true;
        }
    }

    private final AstraxMenuPlugin plugin;
    private Map<String, MenuDef> menus = new LinkedHashMap<>();

    MenuManager(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    Set<String> ids() {
        return menus.keySet();
    }

    int reload() {
        Map<String, MenuDef> out = new LinkedHashMap<>();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menus.yml"));
        ConfigurationSection root = y.getConfigurationSection("menus");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection m = root.getConfigurationSection(id);
                if (m == null) continue;
                String mid = id.toLowerCase(Locale.ROOT);
                int rows = Math.max(1, Math.min(6, m.getInt("rows", 6)));
                List<ItemDef> items = new ArrayList<>();
                ConfigurationSection is = m.getConfigurationSection("items");
                if (is != null) {
                    for (String iid : is.getKeys(false)) {
                        ConfigurationSection c = is.getConfigurationSection(iid);
                        if (c == null) continue;
                        List<Integer> slots = new ArrayList<>();
                        if (c.isList("slots")) slots.addAll(c.getIntegerList("slots"));
                        else if (c.contains("slot")) slots.add(c.getInt("slot"));
                        final int max = rows * 9;
                        slots.removeIf(s -> s < 0 || s >= max);
                        if (slots.isEmpty()) {
                            plugin.getLogger().warning("[menus.yml:" + mid + "." + iid + "] thiếu/sai slot (0-" + (max - 1) + ") -> bỏ qua");
                            continue;
                        }
                        List<String> actions = c.getStringList("actions");
                        List<String> left = c.isList("left-actions") ? c.getStringList("left-actions") : actions;
                        List<String> right = c.isList("right-actions") ? c.getStringList("right-actions") : actions;
                        items.add(new ItemDef(iid, slots, c.getString("material", "PAPER"), c.getInt("amount", 1),
                                c.getString("name", " "), c.getStringList("lore"), c.getBoolean("glow", false),
                                c.getString("skull-owner", ""), left, right));
                    }
                }
                out.put(mid, new MenuDef(mid, m.getString("title", mid), rows, m.getBoolean("filler", true),
                        m.getString("open-sound", "none"), items));
            }
        }
        menus = out;
        return out.size();
    }

    /** Kiểm tra các [open]/[book] trỏ tới menu/sách có thật; trả về danh sách cảnh báo. */
    List<String> validate() {
        List<String> warn = new ArrayList<>();
        for (MenuDef d : menus.values()) {
            for (ItemDef it : d.items()) {
                List<String> all = new ArrayList<>(it.left());
                all.addAll(it.right());
                for (String a : all) {
                    String t = a.trim();
                    String low = t.toLowerCase(Locale.ROOT);
                    if (low.startsWith("[open]") || low.startsWith("[menu]")) {
                        String target = t.substring(t.indexOf(']') + 1).trim().toLowerCase(Locale.ROOT);
                        if (!menus.containsKey(target)) warn.add(d.id() + "." + it.id() + ": [open] tới menu không tồn tại '" + target + "'");
                    } else if (low.startsWith("[book]")) {
                        String target = t.substring(t.indexOf(']') + 1).trim();
                        if (!plugin.books().has(target)) warn.add(d.id() + "." + it.id() + ": [book] tới sách không tồn tại '" + target + "'");
                    }
                }
            }
        }
        return warn;
    }

    boolean open(Player p, String id) {
        MenuDef d = menus.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT));
        if (d == null) return false;
        Holder h = new Holder(d);
        Inventory inv = Bukkit.createInventory(h, d.rows() * 9, Text.c(Placeholders.apply(plugin, p, d.title())));
        h.inv = inv;
        if (d.filler()) {
            ItemStack f = plugin.fillerItem();
            for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);
        }
        for (ItemDef it : d.items()) {
            ItemStack st = build(it, p);
            for (int s : it.slots()) {
                inv.setItem(s, st);
                h.bySlot.put(s, it);
            }
        }
        p.openInventory(inv);
        Items.sound(p, d.openSound());
        return true;
    }

    private ItemStack build(ItemDef d, Player p) {
        Material m = Items.material(d.material(), Material.PAPER, plugin.getLogger(), "menus.yml:" + d.id());
        List<String> lore = new ArrayList<>();
        for (String l : d.lore()) lore.add(Placeholders.apply(plugin, p, l));
        return Items.make(m, d.amount(), Text.c(Placeholders.apply(plugin, p, d.name())), Text.lines(lore),
                d.glow(), Placeholders.apply(plugin, p, d.skull()));
    }
}
