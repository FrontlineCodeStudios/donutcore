
package ro.andreilarazboi.donutcore.crates.listener;

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

public class RowEditorGuiListener
implements Listener {
    private final DonutCrates plugin;

    public RowEditorGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        int rows;
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
        if (!rawTitle.endsWith(" Rows")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Rows".length());
        int raw = e.getRawSlot();
        if (raw == 18) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateSettings.build(crate));
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }
        String name = Utils.stripColor(clicked.getItemMeta().displayName());
        try {
            rows = Integer.parseInt(name.split(" ")[0]);
        }
        catch (Exception ignored) {
            return;
        }
        if (rows < 1 || rows > 6) {
            return;
        }
        int newSize = rows * 9;
        ConfigurationSection items = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate + ".Items");
        if (items != null) {
            for (String k : items.getKeys(false)) {
                ConfigurationSection it = items.getConfigurationSection(k);
                if (it == null || it.getInt("slot") < newSize) continue;
                it.set("slot", (newSize - 1));
            }
        }
        this.plugin.cfg.crates.set("Crates." + crate + ".rows", (Object)rows);
        this.plugin.cfg.saveAll();
        GuiUtil.playClick(this.plugin, p);
        p.openInventory(this.plugin.guiCrateSettings.build(crate));
    }
}

