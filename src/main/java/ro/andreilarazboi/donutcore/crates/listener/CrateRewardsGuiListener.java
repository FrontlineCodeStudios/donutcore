
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CrateRewardsGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateRewardsGuiListener(DonutCrates plugin) {
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
        String rawTitle = Utils.stripColor(e.getView().title());
        if (!rawTitle.endsWith(" Rewards")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Rewards".length());
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        ClickType ct = e.getClick();
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateSettings.build(crate));
            return;
        }
        if (slot == 49) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.crateMgr.addItemFromStack(crate, cursor, p);
                p.openInventory(this.plugin.guiCrateRewards.build(crate));
            }
            return;
        }
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (ct == ClickType.SHIFT_LEFT || ct == ClickType.SHIFT_RIGHT) {
            int targetSlot;
            if (slot >= 45 || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }
            GuiUtil.playClick(this.plugin, p);
            boolean moveLeft = ct == ClickType.SHIFT_LEFT;
            targetSlot = moveLeft ? slot - 1 : slot + 1;
            if (targetSlot < 0 || targetSlot >= 45) {
                return;
            }
            CrateConfigUtil.swapSlots(this.plugin, crate, slot, targetSlot);
            p.openInventory(this.plugin.guiCrateRewards.build(crate));
            return;
        }
        String key = CrateConfigUtil.findItemKeyBySlot(this.plugin, crate, slot);
        if (key != null && ct.isLeftClick()) {
            GuiUtil.playClick(this.plugin, p);
            UUID uid = p.getUniqueId();
            this.plugin.pendingEditorCrate.put(uid, crate);
            this.plugin.pendingEditorItemKey.put(uid, key);
            p.openInventory(this.plugin.guiItemEditor.build(crate, key));
        }
    }
}

