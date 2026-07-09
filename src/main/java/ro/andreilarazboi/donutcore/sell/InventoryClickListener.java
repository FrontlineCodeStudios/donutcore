package ro.andreilarazboi.donutcore.sell;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

@SuppressWarnings({"deprecation", "removal"})
public class InventoryClickListener
implements Listener {
    private final DonutSell plugin;
    private final Set<UUID> suppressClear = new HashSet<UUID>();

    public InventoryClickListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!this.plugin.isModuleActive()) return;
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player)e.getWhoClicked();
        UUID uuid = p.getUniqueId();
        ViewTracker vt = this.plugin.getViewTracker();
        if (!vt.isTracked(uuid)) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        if (e.getClickedInventory() != top) {
            return;
        }
        e.setCancelled(true);
        Sound clickSound = Sound.valueOf((String)this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"));
        p.playSound(p.getLocation(), clickSound, 1.0f, 1.0f);
        int slot = e.getRawSlot();
        int page = vt.getPage(uuid);
        int prev = this.plugin.getMenusConfig().getInt("item-prices-menu.previous.previous-page-slot");
        int next = this.plugin.getMenusConfig().getInt("item-prices-menu.next.next-page-slot");
        int filter = this.plugin.getMenusConfig().getInt("item-prices-menu.filter.slot");
        int refresh = this.plugin.getMenusConfig().getInt("item-prices-menu.refresh.slot");
        int sort = this.plugin.getMenusConfig().getInt("item-prices-menu.sort.slot");
        if (slot == prev && page > 1) {
            this.suppressClear.add(uuid);
            this.plugin.getItemPricesMenu().open(p, page - 1);
        } else if (slot == next) {
            this.suppressClear.add(uuid);
            this.plugin.getItemPricesMenu().open(p, page + 1);
        } else if (slot == sort) {
            vt.cycleOrder(uuid);
            this.suppressClear.add(uuid);
            this.plugin.getItemPricesMenu().open(p, page);
        } else if (slot == filter) {
            List<String> options = this.plugin.getMenusConfig().getStringList("item-prices-menu.filter.lore");
            String cur = vt.getFilter(uuid);
            int idx = options.indexOf(cur == null ? "all" : cur);
            idx = (idx + 1) % options.size();
            String nextCat = (String)options.get(idx);
            vt.setFilter(uuid, nextCat.equalsIgnoreCase("all") ? null : nextCat);
            this.suppressClear.add(uuid);
            this.plugin.getItemPricesMenu().open(p, page);
        } else if (slot == refresh) {
            vt.setFilter(uuid, null);
            this.suppressClear.add(uuid);
            this.plugin.getItemPricesMenu().open(p, 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!this.plugin.isModuleActive()) return;
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player)e.getWhoClicked();
        UUID uuid = p.getUniqueId();
        ViewTracker vt = this.plugin.getViewTracker();
        if (!vt.isTracked(uuid)) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        int size = top.getSize();
        Iterator<Integer> iterator = e.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int raw = (Integer)iterator.next();
            if (raw < 0 || raw >= size) continue;
            e.setCancelled(true);
            Sound clickSound = Sound.valueOf((String)this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"));
            p.playSound(p.getLocation(), clickSound, 1.0f, 1.0f);
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!this.plugin.isModuleActive()) return;
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        Player p = (Player)e.getPlayer();
        UUID uuid = p.getUniqueId();
        ViewTracker vt = this.plugin.getViewTracker();
        if (!vt.isTracked(uuid)) {
            return;
        }
        if (this.suppressClear.remove(uuid)) {
            return;
        }
        String filter = vt.getFilter(uuid);
        if (filter != null && filter.isEmpty()) {
            return;
        }
        vt.clear(uuid);
    }
}

