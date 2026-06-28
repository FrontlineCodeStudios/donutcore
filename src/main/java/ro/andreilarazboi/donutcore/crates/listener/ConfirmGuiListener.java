
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ConfirmGuiListener
implements Listener {
    private final DonutCrates plugin;
    private final CrateOpenService openService;

    public ConfirmGuiListener(DonutCrates plugin, CrateOpenService openService) {
        this.plugin = plugin;
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
        String title = Utils.stripColor(LegacyComponentSerializer.legacySection().serialize(e.getView().title()));
        ConfigurationSection confirm = this.plugin.cfg.config.getConfigurationSection("confirm-menu");
        if (confirm == null) {
            return;
        }
        if (!title.equals(Utils.stripColor(Utils.formatColors(confirm.getString("title", ""))))) {
            return;
        }
        int topSize = e.getView().getTopInventory().getSize();
        inTop = e.getRawSlot() < topSize;
        if (!inTop) {
            return;
        }
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        String confMat = confirm.getString("Confirm.material", "LIME_STAINED_GLASS_PANE");
        String decMat = confirm.getString("Decline.material", "RED_STAINED_GLASS_PANE");
        if (clicked.getType().name().equalsIgnoreCase(decMat)) {
            GuiUtil.playClick(this.plugin, p);
            String crate = this.plugin.pendingCrate.get(p.getUniqueId());
            if (crate != null) {
                p.openInventory(this.plugin.guiCrate.build(crate, false));
            }
            this.plugin.pendingCrate.remove(p.getUniqueId());
            this.plugin.pendingSlot.remove(p.getUniqueId());
            return;
        }
        if (e.getClick().isLeftClick() && clicked.getType().name().equalsIgnoreCase(confMat)) {
            this.openService.handleConfirmAccept(p);
        }
    }
}

