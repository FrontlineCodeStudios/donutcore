package ro.andreilarazboi.donutcore.sell;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

@SuppressWarnings({"deprecation", "removal"})
public class SellMenuClickListener
implements Listener {
    private final DonutSell plugin;

    public SellMenuClickListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        String title = e.getView().getTitle();
        String oldTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.title", ""));
        String newTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("new-sell-menu.title", ""));
        String actionTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("action-sell-menu.title", ""));
        boolean isOld = title.equals(oldTitle);
        boolean isNew = title.equals(newTitle);
        boolean isAction = title.equals(actionTitle);
        if (!(isOld || isNew || isAction)) {
            return;
        }
        Inventory top = e.getInventory();
        int topSize = top.getSize();
        int slot = e.getRawSlot();
        if (isAction) {
            this.handleActionSellClick(e, p, top, topSize, slot);
            return;
        }
        if (isNew) {
            this.handleSellMultiClick(e, p, topSize, slot);
            return;
        }
        if (isOld && this.plugin.isSellMultiMenuEnabled()) {
            return;
        }
        int bottomStart = topSize - 9;
        if (slot < 0) {
            return;
        }
        if (slot < bottomStart || slot >= bottomStart + 9) {
            return;
        }
        e.setCancelled(true);
        this.playClickSound(p);
        List items = this.plugin.getMenusConfig().getStringList("sell-menu.items");
        int idx = slot - bottomStart;
        if (idx >= 0 && idx < items.size()) {
            this.plugin.getProgressGui().open(p, (String)items.get(idx));
        }
    }

    private void handleActionSellClick(InventoryClickEvent e, Player p, Inventory top, int topSize, int slot) {
        int sellSlot = this.plugin.getActionSellButtonSlot();
        if (slot >= 0 && slot < topSize && slot == sellSlot) {
            e.setCancelled(true);
            this.playClickSound(p);
            DonutSell.SellQuote quote = this.plugin.quoteSellInventory(p, top, sellSlot);
            if (quote.payout() <= 0.0 || quote.items() <= 0L) {
                this.playDeclinedSound(p);
                this.plugin.getSellGui().refreshActionSellButton(p, top);
                return;
            }
            boolean confirmEnabled = this.plugin.getConfig().getBoolean("sale-confirmation.enabled", true);
            double threshold = this.plugin.getConfig().getDouble("sale-confirmation.threshold", Double.MAX_VALUE);
            if (confirmEnabled && quote.payout() >= threshold) {
                SellConfirmationGui.open(this.plugin, p, top, quote);
                return;
            }
            this.plugin.sellActionInventory(p, top, sellSlot);
            this.plugin.getSellGui().refreshActionSellButton(p, top);
            if (this.plugin.getMenusConfig().getBoolean("action-sell-menu.close-on-sell", false)) {
                this.plugin.returnActionSellItems(p, top);
                p.closeInventory();
            }
            return;
        }
        if (slot >= 0 && slot < topSize) {
            if (e.getClick().isKeyboardClick() || e.getAction() == InventoryAction.HOTBAR_SWAP) {
                e.setCancelled(true);
                return;
            }
            this.plugin.runAtPlayerLater(p, () -> this.plugin.getSellGui().refreshActionSellButton(p, top), 1L);
            return;
        }
        if (e.isShiftClick() || e.getClick() == ClickType.DOUBLE_CLICK || e.getClick().isKeyboardClick() || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || e.getAction() == InventoryAction.HOTBAR_SWAP || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            this.plugin.runAtPlayerLater(p, () -> this.plugin.getSellGui().refreshActionSellButton(p, top), 1L);
        }
    }

    private void handleSellMultiClick(InventoryClickEvent e, Player p, int topSize, int slot) {
        int s;
        HashSet<Integer> buttonSlots = new HashSet<Integer>();
        if (this.plugin.getMenusConfig().isConfigurationSection("new-sell-menu.item-settings")) {
            for (String cat : this.plugin.getMenusConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false)) {
                s = this.plugin.getMenusConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                if (s < 0) continue;
                buttonSlots.add(s);
            }
        }
        if (slot >= 0 && slot < topSize) {
            if (buttonSlots.contains(slot)) {
                e.setCancelled(true);
                this.playClickSound(p);
                for (String cat : this.plugin.getMenusConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false)) {
                    s = this.plugin.getMenusConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                    if (s != slot) continue;
                    this.plugin.getProgressGui().open(p, cat);
                    break;
                }
                return;
            }
            e.setCancelled(true);
            return;
        }
        if (e.isShiftClick()) {
            e.setCancelled(true);
            return;
        }
        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            e.setCancelled(true);
            return;
        }
        if (e.getClick().isKeyboardClick()) {
            e.setCancelled(true);
            return;
        }
        InventoryAction a = e.getAction();
        if (a == InventoryAction.MOVE_TO_OTHER_INVENTORY || a == InventoryAction.HOTBAR_SWAP || a == InventoryAction.HOTBAR_MOVE_AND_READD || a == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        String title = e.getView().getTitle();
        String oldTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.title", ""));
        String newTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("new-sell-menu.title", ""));
        String actionTitle = Utils.formatColors(this.plugin.getMenusConfig().getString("action-sell-menu.title", ""));
        boolean isOld = title.equals(oldTitle);
        boolean isNew = title.equals(newTitle);
        boolean isAction = title.equals(actionTitle);
        if (!(isOld || isNew || isAction)) {
            return;
        }
        Inventory top = e.getInventory();
        int topSize = top.getSize();
        if (isAction) {
            int sellSlot = this.plugin.getActionSellButtonSlot();
            Iterator iterator = e.getRawSlots().iterator();
            while (iterator.hasNext()) {
                int raw = (Integer)iterator.next();
                if (raw != sellSlot) continue;
                e.setCancelled(true);
                return;
            }
            this.plugin.runAtPlayerLater(p, () -> this.plugin.getSellGui().refreshActionSellButton(p, top), 1L);
            return;
        }
        if (isNew) {
            Iterator sellSlot = e.getRawSlots().iterator();
            while (sellSlot.hasNext()) {
                int raw = (Integer)sellSlot.next();
                if (raw < 0 || raw >= topSize) continue;
                e.setCancelled(true);
                return;
            }
            return;
        }
        if (isOld && this.plugin.isSellMultiMenuEnabled()) {
            return;
        }
        int bottomStart = topSize - 9;
        Iterator iterator = e.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int raw = (Integer)iterator.next();
            if (raw < bottomStart || raw >= bottomStart + 9) continue;
            e.setCancelled(true);
            return;
        }
    }

    private void playClickSound(Player p) {
        p.playSound(p.getLocation(), Sound.valueOf((String)this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK").toUpperCase()), 1.0f, 1.0f);
    }

    private void playDeclinedSound(Player p) {
        p.playSound(p.getLocation(), Sound.valueOf((String)this.plugin.getConfig().getString("sounds.declined", "ENTITY_VILLAGER_NO").toUpperCase()), 1.0f, 1.0f);
    }
}

