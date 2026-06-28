
package ro.andreilarazboi.donutcore.crates.listener;

import java.time.Duration;
import net.kyori.adventure.title.Title;
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

public class CreateModeGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CreateModeGuiListener(DonutCrates plugin) {
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
        String title = Utils.stripColor(e.getView().title());
        if (!title.equals("Create Crate Mode")) {
            return;
        }
        if (!p.hasPermission("donutcrate.admin")) {
            return;
        }
        if (e.getRawSlot() >= top.getSize()) {
            return;
        }
        e.setCancelled(true);
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (slot == 18) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiMainEditor.build());
            return;
        }
        if (slot != 11 && slot != 15) {
            return;
        }
        boolean random = slot == 11;
        this.plugin.pendingCreate.add(p.getUniqueId());
        this.plugin.pendingCreateRandom.put(p.getUniqueId(), random);
        GuiUtil.playClick(this.plugin, p);
        p.closeInventory();
        p.showTitle(Title.title(Utils.toComponent("\u00a7d[Creating crate]"), Utils.toComponent("\u00a77Left-click a block to create a crate!"), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(3000L), Duration.ofMillis(500L))));
    }
}

