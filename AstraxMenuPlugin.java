package vn.astrax.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class AstraxMenuPlugin extends JavaPlugin {
    private static final String[] RESOURCES = {"menus.yml", "ranks.yml", "guides.yml", "messages.yml"};

    private Messages messages;
    private Actions actions;
    private MenuManager menus;
    private RankManager ranks;
    private GuideBooks books;
    private ItemStack filler;

    Messages messages() { return messages; }
    Actions actions() { return actions; }
    MenuManager menus() { return menus; }
    RankManager ranks() { return ranks; }
    GuideBooks books() { return books; }
    ItemStack fillerItem() { return filler.clone(); }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String r : RESOURCES) if (!new File(getDataFolder(), r).exists()) saveResource(r, false);
        messages = new Messages(this);
        actions = new Actions(this);
        menus = new MenuManager(this);
        ranks = new RankManager(this);
        books = new GuideBooks(this);
        JoinListener join = new JoinListener(this);

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(join, this);

        MenuCommand mc = new MenuCommand(this);
        bind("menu", mc, mc);
        bind("astraxmenu", mc, mc);
        RankCommand rc = new RankCommand(this);
        bind("rank", rc, rc);

        getLogger().info("AstraxMenu " + getPluginMeta().getVersion() + " đã bật: " + reloadAll(null));
    }

    private void bind(String name, org.bukkit.command.CommandExecutor ex, org.bukkit.command.TabCompleter tc) {
        PluginCommand c = getCommand(name);
        if (c == null) {
            getLogger().severe("Thiếu lệnh '" + name + "' trong plugin.yml");
            return;
        }
        c.setExecutor(ex);
        c.setTabCompleter(tc);
    }

    /** Tải lại mọi cấu hình; trả về tóm tắt. Cảnh báo ghi ra console (và gửi cho người gọi nếu có). */
    String reloadAll(CommandSender who) {
        reloadConfig();
        messages.reload();
        Material fm = Items.material(getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE, getLogger(), "config.yml:gui.filler-material");
        filler = Items.make(fm, 1, Text.c(getConfig().getString("gui.filler-name", " ")), null, false, null);
        int b = books.reload();
        int m = menus.reload();
        int r = ranks.reload();
        List<String> warn = menus.validate();
        for (String w : warn) {
            getLogger().warning("[menus.yml] " + w);
            if (who != null) who.sendMessage(Text.c("&c! " + w));
        }
        return m + " menu, " + r + " rank, " + b + " sách hướng dẫn";
    }
}
