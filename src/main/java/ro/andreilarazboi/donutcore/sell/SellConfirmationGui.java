package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public final class SellConfirmationGui
implements Listener {
    private static final Map<UUID, PendingSale> PENDING = new HashMap<UUID, PendingSale>();
    private final DonutSell plugin;

    public SellConfirmationGui(DonutSell plugin) {
        this.plugin = plugin;
    }

    public static void open(DonutSell plugin, Player player, Inventory sellInventory, DonutSell.SellQuote quote) {
        SellConfirmationGui.open(plugin, player, sellInventory, quote, true);
    }

    public static void open(DonutSell plugin, Player player, Inventory sellInventory, DonutSell.SellQuote quote, boolean markSourceTransfer) {
        ConfigurationSection cfg = plugin.getMenusConfig().getConfigurationSection("action-sell-menu.confirmation");
        String title = Utils.formatColors(cfg != null ? cfg.getString("title", "Confirm sale") : "Confirm sale");
        int rows = cfg != null ? cfg.getInt("rows", 3) : 3;
        rows = Math.max(1, Math.min(6, rows));
        Inventory inv = Bukkit.createInventory((InventoryHolder)null, (int)(rows * 9), (String)title);
        int cancelSlot = cfg != null ? cfg.getInt("cancel.slot", 11) : 11;
        int confirmSlot = cfg != null ? cfg.getInt("confirm.slot", 15) : 15;
        inv.setItem(cancelSlot, SellConfirmationGui.buildButton(cfg != null ? cfg.getConfigurationSection("cancel") : null, Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&7Go back without selling"), quote));
        inv.setItem(confirmSlot, SellConfirmationGui.buildButton(cfg != null ? cfg.getConfigurationSection("confirm") : null, Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&7Sell these items"), quote));
        PENDING.put(player.getUniqueId(), new PendingSale(sellInventory, quote, title, cancelSlot, confirmSlot));
        if (markSourceTransfer) {
            plugin.markActionMenuTransfer(player.getUniqueId());
        }
        player.openInventory(inv);
    }

    private static ItemStack buildButton(ConfigurationSection cfg, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, DonutSell.SellQuote quote) {
        ItemStack item;
        ItemMeta meta;
        Material material = fallbackMaterial;
        if (cfg != null) {
            try {
                material = Material.valueOf((String)cfg.getString("material", fallbackMaterial.name()).toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {
                material = fallbackMaterial;
            }
        }
        if ((meta = (item = new ItemStack(material, 1)).getItemMeta()) != null) {
            String amount = Utils.abbreviateNumber(quote.payout());
            String name = cfg != null ? cfg.getString("displayname", fallbackName) : fallbackName;
            meta.setDisplayName(Utils.formatColors(name.replace("{amount}", amount)));
            List<String> loreSource = cfg != null ? cfg.getStringList("lore") : fallbackLore;
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : loreSource) {
                lore.add(Utils.formatColors(line.replace("{amount}", amount)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        PendingSale pending = PENDING.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (!event.getView().getTitle().equals(pending.title())) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == pending.cancelSlot()) {
            PENDING.remove(player.getUniqueId());
            player.openInventory(pending.sellInventory());
            this.plugin.getSellGui().refreshActionSellButton(player, pending.sellInventory());
            return;
        }
        if (slot == pending.confirmSlot()) {
            PENDING.remove(player.getUniqueId());
            this.plugin.sellActionInventory(player, pending.sellInventory(), this.plugin.getActionSellButtonSlot());
            this.plugin.getSellGui().refreshActionSellButton(player, pending.sellInventory());
            if (this.plugin.getMenusConfig().getBoolean("action-sell-menu.close-on-sell", false)) {
                this.plugin.returnActionSellItems(player, pending.sellInventory());
                player.closeInventory();
            } else {
                player.openInventory(pending.sellInventory());
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        PendingSale pending = PENDING.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (!event.getView().getTitle().equals(pending.title())) {
            return;
        }
        this.plugin.runAtPlayerLater(player, () -> {
            PendingSale stillPending = PENDING.remove(player.getUniqueId());
            if (stillPending != null && player.isOnline()) {
                player.openInventory(stillPending.sellInventory());
                this.plugin.getSellGui().refreshActionSellButton(player, stillPending.sellInventory());
            }
        }, 1L);
    }

    private record PendingSale(Inventory sellInventory, DonutSell.SellQuote quote, String title, int cancelSlot, int confirmSlot) {
    }
}

