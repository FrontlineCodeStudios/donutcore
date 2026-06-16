
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.CrateHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PlayerCrateGuiListener
implements Listener {
    private final CrateOpenService openService;

    public PlayerCrateGuiListener(CrateOpenService openService) {
        this.openService = openService;
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        boolean inTop;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        int topSize = top.getSize();
        inTop = e.getRawSlot() < topSize;
        if (!(holder instanceof CrateHolder)) {
            return;
        }
        CrateHolder ch = (CrateHolder)holder;
        if (inTop) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            this.openService.handleCrateGuiClick(p, ch, clicked, e.getRawSlot());
            return;
        }
        ClickType ct = e.getClick();
        InventoryAction act = e.getAction();
        if (ct.isShiftClick() || act == InventoryAction.MOVE_TO_OTHER_INVENTORY || act == InventoryAction.HOTBAR_SWAP || act == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
        }
    }
}

