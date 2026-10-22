package vn.astrax.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

/** Mọi GUI của plugin đều là holder này; GuiListener huỷ click và chuyển vào đây. */
interface AstraxHolder extends InventoryHolder {
    /** @return true nếu ô vừa bấm có tác dụng (để phát âm thanh). */
    boolean click(Player p, int slot, ClickType type);
}
