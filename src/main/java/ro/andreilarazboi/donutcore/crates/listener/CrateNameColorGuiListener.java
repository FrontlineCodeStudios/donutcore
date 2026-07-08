
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CrateNameColorGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateNameColorGuiListener(DonutCrates plugin) {
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
        if (rawTitle == null || !rawTitle.endsWith(" Name Color")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Name Color".length()).trim();
        int slot = e.getRawSlot();
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta im = clicked.getItemMeta();
        String code = im.getPersistentDataContainer().get(this.plugin.guiCrateNameColor.colorTag(), PersistentDataType.STRING);
        if (code == null || code.isBlank()) {
            return;
        }
        UUID uid = p.getUniqueId();
        GuiUtil.playClick(this.plugin, p);
        this.plugin.pendingDisplayNameColor.put(uid, code);
        String plain = this.plugin.getCrateDisplayNamePlain(crate);
        this.plugin.setCrateDisplayName(crate, code + plain);
        this.plugin.msg(p, "&#FF073AApplied color &f" + code + " &#FF073Ato display name.");
        p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
    }
}

