
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.ArrayList;
import java.util.List;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HologramTemplatesGuiListener
implements Listener {
    private final DonutCrates plugin;

    public HologramTemplatesGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
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
        String rawTitle = Utils.stripColor(e.getView().getTitle());
        if (!rawTitle.endsWith(" Hologram Templates")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Hologram Templates".length());
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
        if (!clicked.hasItemMeta() || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }
        String name = Utils.stripColor(clicked.getItemMeta().displayName());
        if (!name.startsWith("Template: ")) {
            return;
        }
        String templateId = name.substring("Template: ".length()).trim();
        if (templateId.isEmpty()) {
            return;
        }
        ConfigurationSection root = this.plugin.cfg.config.getConfigurationSection("hologram-templates");
        if (root == null) {
            this.plugin.msg(p, "&#ff5555No hologram templates defined in config.yml.");
            p.openInventory(this.plugin.guiHologram.build(crate));
            return;
        }
        ConfigurationSection tmpl = root.getConfigurationSection(templateId);
        if (tmpl == null) {
            this.plugin.msg(p, "&#ff5555Template '&f" + templateId + "&#ff5555' not found.");
            p.openInventory(this.plugin.guiHologram.build(crate));
            return;
        }
        List lines = tmpl.getStringList("lines");
        double offsetY = tmpl.getDouble("offset-y", 1.5);
        String base = "Crates." + crate + ".Hologram";
        ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
        if (h == null) {
            h = this.plugin.cfg.crates.createSection(base);
        }
        h.set("template", templateId);
        if (lines != null) {
            h.set("lines", new ArrayList(lines));
        }
        h.set("offsetY", offsetY);
        this.plugin.cfg.saveAll();
        this.plugin.holoMgr.refreshCrate(crate);
        GuiUtil.playClick(this.plugin, p);
        this.plugin.msg(p, "&#0fe30fSelected hologram template &f" + templateId + " &#0fe30ffor crate &f" + crate);
        p.openInventory(this.plugin.guiHologram.build(crate));
    }
}

