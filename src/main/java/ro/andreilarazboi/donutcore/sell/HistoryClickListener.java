package ro.andreilarazboi.donutcore.sell;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class HistoryClickListener implements Listener {
    private final DonutSell plugin;
    private final Set<UUID> suppressClear = new HashSet<>();

    public HistoryClickListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        HistoryTracker tracker = this.plugin.getHistoryTracker();
        SellHistoryGui gui = this.plugin.getSellHistoryGui();
        String title = e.getView().getTitle();
        if (!tracker.isTracked(p.getUniqueId())) return;
        if (!gui.matchesTitle(title)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        int page = tracker.getPage(p.getUniqueId());
        if (e.getCurrentItem() != null && !e.getCurrentItem().getType().isAir()) {
            Sound clickSound = Utils.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
            p.playSound(p.getLocation(), clickSound, 1.0f, 1.0f);
        }
        if (slot == gui.getSortSlot()) {
            tracker.cycleOrder(p.getUniqueId());
            this.suppressClear.add(p.getUniqueId());
            gui.open(p, page);
            return;
        }
        if (slot == gui.getRefreshSlot()) {
            tracker.setFilter(p.getUniqueId(), null);
            this.suppressClear.add(p.getUniqueId());
            gui.open(p, 1);
            return;
        }
        int perPage = (gui.getRows() - 1) * 9;
        int total = this.plugin.getHistory(p.getUniqueId()).size();
        int maxPages = (total + perPage - 1) / perPage;
        if (slot == gui.getBackSlot() && page > 1) {
            this.suppressClear.add(p.getUniqueId());
            gui.open(p, page - 1);
        } else if (slot == gui.getNextSlot() && page < maxPages) {
            this.suppressClear.add(p.getUniqueId());
            gui.open(p, page + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        HistoryTracker tracker = this.plugin.getHistoryTracker();
        SellHistoryGui gui = this.plugin.getSellHistoryGui();
        if (!tracker.isTracked(p.getUniqueId())) return;
        if (!gui.matchesTitle(e.getView().getTitle())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (this.suppressClear.remove(id)) return;
        this.plugin.getHistoryTracker().clear(id);
    }
}
