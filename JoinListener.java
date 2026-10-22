package vn.astrax.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

final class JoinListener implements Listener {
    private final AstraxMenuPlugin plugin;
    private final NamespacedKey key;

    JoinListener(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    private boolean isMenuItem(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean first = !p.hasPlayedBefore();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            FileConfiguration c = plugin.getConfig();
            if (c.getBoolean("join.hint.enabled", true)) {
                p.sendMessage(Text.c(Placeholders.apply(plugin, p, c.getString("join.hint.message", ""))));
            }
            if (c.getBoolean("join.item.enabled", false)) giveItem(p, c);
            if (first && c.getBoolean("join.first-join-menu.enabled", true)) {
                plugin.menus().open(p, c.getString("join.first-join-menu.menu", "main"));
            }
        }, 40L);
    }

    private void giveItem(Player p, FileConfiguration c) {
        PlayerInventory inv = p.getInventory();
        for (ItemStack it : inv.getContents()) if (isMenuItem(it)) return;
        Material m = Items.material(c.getString("join.item.material", "NETHER_STAR"), Material.NETHER_STAR, plugin.getLogger(), "config.yml:join.item");
        ItemStack item = Items.make(m, 1, Text.c(c.getString("join.item.name", "&6Menu")),
                Text.lines(c.getStringList("join.item.lore")), true, null);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        int slot = Math.max(0, Math.min(8, c.getInt("join.item.slot", 8)));
        ItemStack cur = inv.getItem(slot);
        if (cur == null || cur.getType().isAir()) inv.setItem(slot, item);
        else inv.addItem(item);
    }

    // Không đặt ignoreCancelled: chuột phải vào không khí luôn bị coi là "cancelled" trong Bukkit.
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuItem(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!plugin.menus().open(p, plugin.getConfig().getString("join.item.menu", "main"))) {
            plugin.messages().send(p, "menu-not-found", "menu", plugin.getConfig().getString("join.item.menu", "main"));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isMenuItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMenuItem);
    }
}
