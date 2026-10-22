package vn.astrax.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

final class Items {
    private Items() {}

    static ItemStack make(Material m, int amount, Component name, List<Component> lore, boolean glow, String skullOwner) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        if (name != null) meta.displayName(name);
        if (lore != null && !lore.isEmpty()) meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (glow) meta.setEnchantmentGlintOverride(true);
        if (meta instanceof SkullMeta sm && skullOwner != null && !skullOwner.isBlank()) {
            Player op = Bukkit.getPlayerExact(skullOwner); // chỉ người đang online: tránh tra cứu mạng trên main thread
            if (op != null) sm.setOwningPlayer(op);
        }
        it.setItemMeta(meta);
        return it;
    }

    static Material material(String name, Material fallback, Logger log, String where) {
        Material m = name == null ? null : Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (m == null || !m.isItem() || m.isAir()) {
            if (log != null) log.warning("[" + where + "] Material không hợp lệ: '" + name + "' -> dùng " + fallback);
            return fallback;
        }
        return m;
    }

    /** Phát âm thanh theo key (vd ui.button.click). Chấp nhận UI_BUTTON_CLICK; "none" = tắt. */
    static void sound(Player p, String key) {
        if (key == null || key.isBlank() || key.equalsIgnoreCase("none")) return;
        String k = key.trim();
        if (!k.contains(".") && !k.contains(":")) k = k.toLowerCase(Locale.ROOT).replace('_', '.');
        p.playSound(p.getLocation(), k, 0.7f, 1.0f);
    }
}
