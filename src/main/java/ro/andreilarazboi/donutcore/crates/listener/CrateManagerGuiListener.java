
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CrateManagerGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateManagerGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        String crate;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder)) {
            return;
        }
        String title = e.getView().getTitle();
        if (!title.equals(Utils.formatColors("&#444444Crate Manager"))) {
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
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiRootEditor.build());
            return;
        }
        if (slot == 49) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCreateMode.build());
            return;
        }
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() && this.plugin.crateMgr.crateExists(crate = Utils.stripColor(clicked.getItemMeta().displayName()))) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateSettings.build(crate));
        }
    }
}

