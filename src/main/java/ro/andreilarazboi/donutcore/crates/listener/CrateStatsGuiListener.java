
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.CrateStatsHolder;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CrateStatsGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateStatsGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        InventoryHolder inventoryHolder = event.getView().getTopInventory().getHolder();
        if (!(inventoryHolder instanceof CrateStatsHolder)) {
            return;
        }
        CrateStatsHolder holder = (CrateStatsHolder)inventoryHolder;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= topSize) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        if (!holder.isHistoryView()) {
            String crate = this.plugin.crateMgr.crateBlocks.keySet().stream().sorted((s1, s2) -> s1.compareToIgnoreCase(s2)).skip(event.getRawSlot()).findFirst().orElse(null);
            if (crate == null) {
                return;
            }
            player.openInventory(this.plugin.guiCrateStats.buildHistory(player, crate));
            return;
        }
        if (event.getRawSlot() == 53) {
            player.openInventory(this.plugin.guiCrateStats.buildMain(player));
        }
    }
}

