
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.Locale;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class HologramBackgroundGuiListener
implements Listener {
    private final DonutCrates plugin;
    private final NamespacedKey bgColorTag;

    public HologramBackgroundGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
        this.bgColorTag = new NamespacedKey((Plugin)plugin.getPlugin(), "holo_bg_color");
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder)) {
            return;
        }
        String rawTitle = Utils.stripColor(e.getView().title());
        if (!rawTitle.endsWith(" Hologram Background")) {
            return;
        }
        if (!p.hasPermission("donutcrate.admin")) {
            return;
        }
        int topSize = top.getSize();
        if (e.getRawSlot() >= topSize) {
            return;
        }
        e.setCancelled(true);
        String crate = rawTitle.substring(0, rawTitle.length() - " Hologram Background".length());
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiHologram.build(crate));
            return;
        }
        if (!clicked.hasItemMeta() || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE || clicked.getType() == Material.ARROW) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String id = meta.getPersistentDataContainer().get(this.bgColorTag, PersistentDataType.STRING);
        if (id == null || id.isEmpty()) {
            return;
        }
        String base = "Crates." + crate + ".Hologram";
        ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
        if (h == null) {
            h = this.plugin.cfg.crates.createSection(base);
        }
        h.set("bgColor", id);
        h.set("background-transparent", id.equalsIgnoreCase("TRANSPARENT"));
        this.plugin.cfg.saveAll();
        this.plugin.holoMgr.refreshCrate(crate);
        GuiUtil.playClick(this.plugin, p);
        this.plugin.msg(p, "&#0fe30fSelected background &f" + id.toLowerCase(Locale.ROOT).replace('_', ' ') + " &#0fe30ffor crate &f" + crate);
        p.openInventory(this.plugin.guiHologram.build(crate));
    }
}

