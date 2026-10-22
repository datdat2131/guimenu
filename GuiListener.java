package vn.astrax.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Huỷ mọi thao tác lấy/đặt đồ trong GUI của plugin và chuyển click cho holder. */
final class GuiListener implements Listener {
    private final AstraxMenuPlugin plugin;
    private final Map<UUID, Long> last = new HashMap<>();

    GuiListener(AstraxMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof AstraxHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) return;
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("gui.click-cooldown-ms", 250);
        Long prev = last.get(p.getUniqueId());
        if (prev != null && now - prev < cd) return;
        last.put(p.getUniqueId(), now);
        if (h.click(p, raw, e.getClick())) {
            Items.sound(p, plugin.getConfig().getString("gui.click-sound", "ui.button.click"));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof AstraxHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        last.remove(e.getPlayer().getUniqueId());
    }
}
