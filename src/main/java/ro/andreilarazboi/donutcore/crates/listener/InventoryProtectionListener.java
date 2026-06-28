
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.Iterator;
import ro.andreilarazboi.donutcore.crates.CrateHolder;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryProtectionListener
implements Listener {
    private final CrateOpenService openService;

    public InventoryProtectionListener(CrateOpenService openService) {
        this.openService = openService;
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof EditorHolder || top.getHolder() instanceof CrateHolder || top.getHolder() instanceof OpeningAnimationHolder) {
            int topSize = top.getSize();
            Iterator<Integer> iterator = e.getRawSlots().iterator();
            while (iterator.hasNext()) {
                int raw = iterator.next();
                if (raw >= topSize) continue;
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        HumanEntity humanEntity = e.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        if (!(e.getView().getTopInventory().getHolder() instanceof OpeningAnimationHolder)) {
            return;
        }
        this.openService.onAnimationInventoryClosed(p);
    }
}

