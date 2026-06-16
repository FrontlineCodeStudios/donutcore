package ro.andreilarazboi.donutcore.sell;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class SellMenuClickListener implements Listener {
    private final DonutSell plugin;
    private final boolean useNew;

    public SellMenuClickListener(DonutSell plugin) {
        this.plugin = plugin;
        this.useNew = plugin.getConfig().getBoolean("use-new-sell-menu", false);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        String oldTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.title", ""));
        String newTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("new-sell-menu.title", ""));
        boolean isOld = title.equals(oldTitle);
        boolean isNew = this.useNew && title.equals(newTitle);
        if (!isOld && !isNew) return;
        Inventory top = e.getInventory();
        int topSize = top.getSize();
        int slot = e.getRawSlot();
        if (isNew) {
            HashSet<Integer> buttonSlots = new HashSet<>();
            if (this.plugin.getMenusConfig().isConfigurationSection("new-sell-menu.item-settings")) {
                for (String cat : this.plugin.getMenusConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false)) {
                    int s = this.plugin.getMenusConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                    if (s < 0) continue;
                    buttonSlots.add(s);
                }
            }
            if (slot >= 0 && slot < topSize) {
                if (buttonSlots.contains(slot)) {
                    e.setCancelled(true);
                    p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK), 1.0f, 1.0f);
                    for (String cat : this.plugin.getMenusConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false)) {
                        int s = this.plugin.getMenusConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                        if (s != slot) continue;
                        this.plugin.getProgressGui().open(p, cat);
                        break;
                    }
                    return;
                }
                e.setCancelled(true);
                return;
            }
            if (e.isShiftClick()) { e.setCancelled(true); return; }
            if (e.getClick() == ClickType.DOUBLE_CLICK) { e.setCancelled(true); return; }
            if (e.getClick().isKeyboardClick()) { e.setCancelled(true); return; }
            InventoryAction a = e.getAction();
            if (a == InventoryAction.MOVE_TO_OTHER_INVENTORY || a == InventoryAction.HOTBAR_SWAP
                    || a == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
            return;
        }
        if (isOld && this.useNew) return;
        InventoryAction a = e.getAction();
        if (a == InventoryAction.COLLECT_TO_CURSOR || a == InventoryAction.HOTBAR_SWAP) {
            e.setCancelled(true);
            return;
        }
        int bottomStart = topSize - 9;
        if (slot < 0 || slot < bottomStart || slot >= bottomStart + 9) return;
        e.setCancelled(true);
        p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK), 1.0f, 1.0f);
        List<String> items = this.plugin.getMenusConfig().getStringList("sell-menu.items");
        int idx = slot - bottomStart;
        if (idx >= 0 && idx < items.size()) {
            this.plugin.getProgressGui().open(p, items.get(idx));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        String oldTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.title", ""));
        String newTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("new-sell-menu.title", ""));
        boolean isOld = title.equals(oldTitle);
        boolean isNew = this.useNew && title.equals(newTitle);
        if (!isOld && !isNew) return;
        Inventory top = e.getInventory();
        int topSize = top.getSize();
        if (isNew) {
            Iterator<Integer> it = e.getRawSlots().iterator();
            while (it.hasNext()) {
                int raw = it.next();
                if (raw >= 0 && raw < topSize) { e.setCancelled(true); return; }
            }
            return;
        }
        if (isOld && this.useNew) return;
        int bottomStart = topSize - 9;
        Iterator<Integer> it = e.getRawSlots().iterator();
        while (it.hasNext()) {
            int raw = it.next();
            if (raw >= bottomStart && raw < bottomStart + 9) { e.setCancelled(true); return; }
        }
    }
}
