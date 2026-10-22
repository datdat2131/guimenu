package vn.astrax.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/** /rank: danh sách rank + quyền lợi, xem trước nội dung /kit của từng rank. */
final class RankManager {
    record Rank(String id, String name, String icon, String kit, String permission, String price,
                List<String> description, List<String> perks) {}

    private final AstraxMenuPlugin plugin;
    private final KitReader kits;
    private YamlConfiguration cfg = new YamlConfiguration();
    private List<Rank> ranks = List.of();
    private List<Integer> slots = List.of();
    private int rows = 4;
    private boolean requireOwned = true, showKit = true;
    private String backMenu = "main", kitCommand = "kit {kit}";

    RankManager(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
        this.kits = new KitReader(plugin);
    }

    private String t(String key, String def) {
        return cfg.getString("texts." + key, def);
    }

    int reload() {
        cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "ranks.yml"));
        rows = Math.max(3, Math.min(6, cfg.getInt("settings.rows", 4)));
        requireOwned = cfg.getBoolean("settings.require-owned-to-claim", true);
        showKit = cfg.getBoolean("settings.show-kit-contents", true);
        backMenu = cfg.getString("settings.back-menu", "main");
        kitCommand = cfg.getString("settings.kit-command", "kit {kit}");
        kits.setPath(cfg.getString("settings.kits-file", "auto"));
        final int max = rows * 9;
        List<Integer> sl = new ArrayList<>(cfg.getIntegerList("settings.slots"));
        sl.removeIf(s -> s < 0 || s >= max);
        slots = sl;
        List<Rank> out = new ArrayList<>();
        ConfigurationSection rs = cfg.getConfigurationSection("ranks");
        if (rs != null) {
            for (String id : rs.getKeys(false)) {
                ConfigurationSection r = rs.getConfigurationSection(id);
                if (r == null) continue;
                String lid = id.toLowerCase(Locale.ROOT);
                out.add(new Rank(lid, r.getString("name", id), r.getString("icon", "PAPER"), r.getString("kit", ""),
                        r.getString("permission", "group." + lid), r.getString("price", ""),
                        r.getStringList("description"), r.getStringList("perks")));
            }
        }
        ranks = out;
        if (out.size() > slots.size()) {
            plugin.getLogger().warning("[ranks.yml] có " + out.size() + " rank nhưng chỉ " + slots.size()
                    + " ô trong settings.slots -> rank thừa sẽ không hiện trong /rank (vẫn xem được bằng /rank <id>)");
        }
        return out.size();
    }

    List<String> ids() {
        List<String> l = new ArrayList<>();
        for (Rank r : ranks) l.add(r.id());
        return l;
    }

    Rank find(String id) {
        if (id == null) return null;
        for (Rank r : ranks) if (r.id().equalsIgnoreCase(id.trim())) return r;
        return null;
    }

    /** isPermissionSet: tránh OP bị coi là có mọi group.* */
    boolean owns(Player p, Rank r) {
        return p.isPermissionSet(r.permission()) && p.hasPermission(r.permission());
    }

    Rank currentRank(Player p) {
        for (int i = ranks.size() - 1; i >= 0; i--) if (owns(p, ranks.get(i))) return ranks.get(i);
        return null;
    }

    String currentRankName(Player p) {
        Rank r = currentRank(p);
        return r == null ? cfg.getString("settings.default-rank-name", "&7Thành viên") : r.name();
    }

    void openFromAction(Player p, String arg) {
        if (arg == null || arg.isBlank()) {
            openList(p);
            return;
        }
        Rank r = find(arg);
        if (r == null) plugin.messages().send(p, "rank-not-found", "rank", arg);
        else openDetail(p, r);
    }

    // ------------------------------------------------------------------ chữ

    private String fill(String s, Rank r, KitReader.Kit kit) {
        String cd = "";
        if (kit != null) cd = kit.delay() < 0 ? t("kit-once", "Chỉ dùng 1 lần") : kit.delay() == 0 ? t("kit-nocd", "Không hồi") : Text.duration(kit.delay());
        return s.replace("{kit}", r.kit()).replace("{price}", r.price()).replace("{rank}", r.name()).replace("{cooldown}", cd);
    }

    private List<String> infoLines(Player p, Rank r, KitReader.Kit kit, boolean isCurrent) {
        List<String> l = new ArrayList<>();
        boolean owned = owns(p, r);
        l.add(isCurrent ? t("status-current", "&6★ Rank hiện tại của bạn")
                : owned ? t("status-owned", "&a✔ Bạn đã sở hữu rank này") : t("status-locked", "&c✖ Bạn chưa sở hữu rank này"));
        if (!r.description().isEmpty()) {
            l.add(" ");
            l.addAll(r.description());
        }
        if (!r.perks().isEmpty()) {
            l.add(" ");
            l.add(t("perks-title", "&6Quyền lợi:"));
            l.addAll(r.perks());
        }
        if (!r.kit().isBlank()) {
            l.add(" ");
            l.add(t("kit-line", "&eKit: &f/kit {kit}"));
            if (kit != null) l.add(t("kit-cooldown", "&7Hồi kit: &f{cooldown}"));
        }
        if (!r.price().isBlank()) l.add(t("price-line", "&7Cách nhận: &f{price}"));
        List<String> out = new ArrayList<>();
        for (String s : l) out.add(fill(s, r, kit));
        return out;
    }

    private Material icon(Rank r) {
        return Items.material(r.icon(), Material.PAPER, plugin.getLogger(), "ranks.yml:" + r.id());
    }

    // ------------------------------------------------------------------ GUI danh sách

    private final class ListHolder implements AstraxHolder {
        Inventory inv;
        final Map<Integer, String> bySlot = new HashMap<>();
        int backSlot = -1, closeSlot = -1;

        @Override
        public Inventory getInventory() {
            return inv;
        }

        @Override
        public boolean click(Player p, int slot, ClickType type) {
            if (slot == backSlot && backSlot >= 0) {
                plugin.actions().run(p, List.of("[open] " + backMenu));
                return true;
            }
            if (slot == closeSlot) {
                plugin.actions().run(p, List.of("[close]"));
                return true;
            }
            Rank r = find(bySlot.get(slot));
            if (r == null) return false;
            if (type.isRightClick()) claim(p, r);
            else openDetail(p, r);
            return true;
        }
    }

    void openList(Player p) {
        ListHolder h = new ListHolder();
        Inventory inv = Bukkit.createInventory(h, rows * 9, Text.c(Placeholders.apply(plugin, p, cfg.getString("settings.title", "&6&lRANK"))));
        h.inv = inv;
        ItemStack filler = plugin.fillerItem();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        Rank cur = currentRank(p);

        List<String> head = new ArrayList<>();
        head.add(" ");
        head.add(t("header-current", "&7Rank của bạn: {rank}").replace("{rank}", currentRankName(p)));
        head.add(" ");
        head.add(t("header-hint-1", "&eChuột trái &7vào rank: xem vật phẩm trong kit"));
        head.add(t("header-hint-2", "&eChuột phải &7vào rank: nhận kit"));
        inv.setItem(4, Items.make(Material.PLAYER_HEAD, 1, Text.c(t("header-name", "&e{player}").replace("{player}", p.getName())),
                Text.lines(head), false, p.getName()));

        for (int i = 0; i < ranks.size() && i < slots.size(); i++) {
            Rank r = ranks.get(i);
            KitReader.Kit kit = r.kit().isBlank() ? null : kits.get(r.kit());
            List<String> lore = infoLines(p, r, kit, cur != null && cur.id().equals(r.id()));
            if (!r.kit().isBlank()) {
                lore.add(" ");
                if (showKit) lore.add(fill(t("hint-left", "&eChuột trái: &7xem vật phẩm trong kit"), r, kit));
                lore.add(fill(t("hint-right", "&eChuột phải: &7nhận kit &f/kit {kit}"), r, kit));
            }
            boolean glow = owns(p, r);
            inv.setItem(slots.get(i), Items.make(icon(r), 1, Text.c(r.name()), Text.lines(lore), glow, null));
            h.bySlot.put(slots.get(i), r.id());
        }
        int bottom = (rows - 1) * 9;
        if (backMenu != null && !backMenu.isBlank()) {
            h.backSlot = bottom;
            inv.setItem(bottom, Items.make(Material.ARROW, 1, Text.c(t("back", "&c« Quay lại menu")), null, false, null));
        }
        h.closeSlot = bottom + 4;
        inv.setItem(h.closeSlot, Items.make(Material.BARRIER, 1, Text.c(t("close", "&cĐóng")), null, false, null));
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ GUI chi tiết rank

    private final class DetailHolder implements AstraxHolder {
        Inventory inv;
        final String rankId;

        DetailHolder(String rankId) {
            this.rankId = rankId;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }

        @Override
        public boolean click(Player p, int slot, ClickType type) {
            Rank r = find(rankId);
            if (r == null) return false;
            if (slot == 45) {
                openList(p);
                return true;
            }
            if (slot == 53 && !r.kit().isBlank()) {
                claim(p, r);
                return true;
            }
            return false;
        }
    }

    void openDetail(Player p, Rank r) {
        KitReader.Kit kit = (r.kit().isBlank() || !showKit) ? null : kits.get(r.kit());
        DetailHolder h = new DetailHolder(r.id());
        Inventory inv = Bukkit.createInventory(h, 54, Text.c(Placeholders.apply(plugin, p, fill(cfg.getString("settings.detail-title", "&8Rank: {rank}"), r, kit))));
        h.inv = inv;
        ItemStack filler = plugin.fillerItem();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        if (kit != null && !kit.items().isEmpty()) {
            int n = 0;
            List<ItemStack> items = kit.items();
            for (ItemStack it : items) {
                if (n >= 45) break;
                inv.setItem(n++, it.clone());
            }
            if (items.size() > 45) {
                inv.setItem(44, Items.make(Material.PAPER, 1,
                        Text.c(t("more-items", "&7...và &f{n} &7vật phẩm khác").replace("{n}", String.valueOf(items.size() - 44))), null, false, null));
            }
        } else {
            String why = r.kit().isBlank() ? t("no-kit", "&7Rank này không có kit.")
                    : !showKit ? t("preview-off", "&7Xem trước kit đang tắt.")
                    : t("kit-unreadable", "&7Không đọc được kit &f{kit}&7 từ kits.yml.").replace("{kit}", r.kit());
            inv.setItem(22, Items.make(Material.BARRIER, 1, Text.c(t("empty-title", "&cKhông có nội dung để xem trước")),
                    List.of(Text.c(why)), false, null));
        }

        List<String> info = infoLines(p, r, kit, false);
        if (kit != null && kit.others() > 0) info.add(t("kit-others", "&7+ {n} phần thưởng đặc biệt khác").replace("{n}", String.valueOf(kit.others())));
        inv.setItem(49, Items.make(icon(r), 1, Text.c(r.name()), Text.lines(info), owns(p, r), null));
        inv.setItem(45, Items.make(Material.ARROW, 1, Text.c(t("back-list", "&c« Danh sách rank")), null, false, null));
        if (!r.kit().isBlank()) {
            inv.setItem(53, Items.make(Material.CHEST, 1, Text.c(fill(t("claim-name", "&a&lNhận kit /kit {kit}"), r, kit)),
                    List.of(Text.c(t("claim-lore", "&7Bấm để chạy lệnh nhận kit."))), true, null));
        }
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ nhận kit

    void claim(Player p, Rank r) {
        if (r.kit().isBlank()) {
            plugin.messages().send(p, "kit-missing");
            return;
        }
        if (requireOwned && !owns(p, r)) {
            plugin.messages().send(p, "rank-not-owned", "rank", Text.plain(r.name()));
            return;
        }
        String cmd = kitCommand.replace("{kit}", r.kit()).replace("{player}", p.getName()).trim();
        final String run = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            p.closeInventory();
            p.performCommand(run);
        });
    }

    // ------------------------------------------------------------------ chat / chẩn đoán

    void sendText(CommandSender to) {
        to.sendMessage(Text.c("&8&m------&r &6Danh sách rank &8&m------"));
        Player pl = to instanceof Player pp ? pp : null;
        for (Rank r : ranks) {
            String st = pl == null ? "" : owns(pl, r) ? " &a✔" : " &c✖";
            String kit = r.kit().isBlank() ? "" : " &7- &e/kit " + r.kit();
            to.sendMessage(Text.c(" " + r.name() + st + kit));
        }
        if (pl != null) to.sendMessage(Text.c("&7Gõ &e/rank <tên> &7để xem chi tiết."));
    }

    void diagnose(CommandSender to) {
        to.sendMessage(Text.c("&6[Rank] &7Tệp kit: &f" + kits.where() + " &7(" + kits.count() + " kit)"));
        for (Rank r : ranks) {
            String s = r.kit().isBlank() ? "&7không có kit"
                    : kits.get(r.kit()) != null ? "&akit OK" : "&ckhông thấy kit '" + r.kit() + "'";
            to.sendMessage(Text.c(" &f" + r.id() + " &8| &7quyền &f" + r.permission() + " &8| " + s));
        }
    }
}
