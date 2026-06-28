package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ResetConfirmationGui implements Listener {
    private final DonutSell plugin;
    private final Map<UUID, UUID> pendingReset = new HashMap<>();
    private static final String GUI_TITLE_PREFIX = "Confirm Reset: ";

    public ResetConfirmationGui(DonutSell plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin.getPlugin());
    }

    public void open(Player admin, OfflinePlayer target) {
        this.pendingReset.put(admin.getUniqueId(), target.getUniqueId());
        String title = GUI_TITLE_PREFIX + target.getName();
        Inventory inv = Bukkit.createInventory(null, 36, Utils.toComponent(title));
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(target.getUniqueId());
            skullMeta.setPlayerProfile(profile);
            skullMeta.displayName(Utils.toComponent("§e" + target.getName()));
            ArrayList<Component> lore = new ArrayList<>();
            String totalStr = this.plugin.getFormattedTotalSold(target.getUniqueId());
            lore.add(Utils.toComponent("§7Total sold: §b" + totalStr));
            skullMeta.lore(lore);
            head.setItemMeta(skullMeta);
            inv.setItem(13, head);
        }
        ItemStack confirmPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cMeta = confirmPane.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Utils.toComponent("§aConfirm Reset"));
            confirmPane.setItemMeta(cMeta);
            inv.setItem(15, confirmPane);
        }
        ItemStack declinePane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta dMeta = declinePane.getItemMeta();
        if (dMeta != null) {
            dMeta.displayName(Utils.toComponent("§cCancel"));
            declinePane.setItemMeta(dMeta);
            inv.setItem(11, declinePane);
        }
        List<String> categoryOrder = this.plugin.getMenusConfig().getStringList("sell-menu.items");
        ConfigurationSection settingsSection = this.plugin.getMenusConfig().getConfigurationSection("sell-menu.item-settings");
        for (int i = 0; i < categoryOrder.size() && i < 9; ++i) {
            String catKey = categoryOrder.get(i);
            String catKeyLower = catKey.toLowerCase(Locale.ROOT);
            Material mat = Material.matchMaterial(catKey);
            String displayName = "§f" + catKey;
            if (settingsSection != null && settingsSection.isConfigurationSection(catKey)) {
                ConfigurationSection thisCatSec = settingsSection.getConfigurationSection(catKey);
                if (thisCatSec.contains("material")) {
                    String matName = thisCatSec.getString("material");
                    if (matName != null) {
                        Material override = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
                        if (override != null) mat = override;
                    }
                }
                if (thisCatSec.contains("displayname")) {
                    String dn = thisCatSec.getString("displayname");
                    if (dn != null) displayName = Utils.formatColors(dn);
                }
            }
            if (mat == null) continue;
            double rawCatSold = this.plugin.getRawTotalSold(target.getUniqueId(), catKeyLower);
            String formattedSold = Utils.abbreviateNumber(rawCatSold);
            ItemStack catItem = new ItemStack(mat);
            ItemMeta im = catItem.getItemMeta();
            if (im != null) {
                im.displayName(Utils.toComponent(displayName));
                ArrayList<Component> lore = new ArrayList<>();
                lore.add(Utils.toComponent("§7Sold: §b" + formattedSold));
                im.lore(lore);
                catItem.setItemMeta(im);
            }
            inv.setItem(27 + i, catItem);
        }
        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = Utils.stripColor(e.getView().title());
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player admin)) return;
        UUID adminUUID = admin.getUniqueId();
        if (!this.pendingReset.containsKey(adminUUID)) {
            admin.closeInventory();
            return;
        }
        int slot = e.getRawSlot();
        if (slot == 15) {
            UUID targetUUID = this.pendingReset.get(adminUUID);
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetUUID);
            this.plugin.resetPlayerData(targetUUID);
            admin.playSound(admin.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            admin.sendMessage(Utils.toComponent("§aAll sell stats for §e" + targetOffline.getName() + " §ahas been reset."));
            if (targetOffline.isOnline()) {
                ((Player) targetOffline).sendMessage(Utils.toComponent("§cYour sell stats has been reset by an admin."));
            }
            this.pendingReset.remove(adminUUID);
            admin.closeInventory();
            return;
        }
        if (slot == 11) {
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            this.pendingReset.remove(adminUUID);
            admin.closeInventory();
        }
    }
}
