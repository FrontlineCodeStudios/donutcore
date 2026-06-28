
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CrateSettingsGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateSettingsGuiListener(DonutCrates plugin) {
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
        if (rawTitle == null || !rawTitle.endsWith(" Settings")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Settings".length()).trim();
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        UUID uid = p.getUniqueId();
        switch (slot) {
            case 27: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiMainEditor.build());
                break;
            }
            case 11: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiCrateRewards.build(crate));
                break;
            }
            case 13: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.previewReturnCrate.put(uid, crate);
                p.openInventory(this.plugin.guiCrate.build(crate, true));
                break;
            }
            case 15: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiHologram.build(crate));
                break;
            }
            case 20: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiCrateEdit.build(crate));
                break;
            }
            case 22: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiOpeningAnimations.build(crate));
                break;
            }
            case 24: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiKeySelect.build(crate));
            }
        }
    }
}

