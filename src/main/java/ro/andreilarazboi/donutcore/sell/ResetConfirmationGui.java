package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
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
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public class ResetConfirmationGui
implements Listener {
    private final DonutSell plugin;
    private final Map<UUID, UUID> pendingReset = new HashMap<UUID, UUID>();
    private static final String GUI_TITLE_PREFIX = "Confirm Reset: ";

    public ResetConfirmationGui(DonutSell plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin.getPlugin());
    }

    public void open(Player admin, OfflinePlayer target) {
        ItemStack declinePane;
        ItemMeta dMeta;
        ItemStack confirmPane;
        ItemMeta cMeta;
        this.pendingReset.put(admin.getUniqueId(), target.getUniqueId());
        String title = GUI_TITLE_PREFIX + target.getName();
        Inventory inv = Bukkit.createInventory(null, (int)36, (String)title);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta)head.getItemMeta();
        if (skullMeta != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile((UUID)target.getUniqueId());
            skullMeta.setOwnerProfile(profile);
            skullMeta.setDisplayName("\u00a7e" + target.getName());
            List<String> lore = new ArrayList<>();
            String totalStr = this.plugin.getFormattedTotalSold(target.getUniqueId());
            lore.add("\u00a77Total sold: \u00a7b" + totalStr);
            skullMeta.setLore(lore);
            head.setItemMeta((ItemMeta)skullMeta);
            inv.setItem(13, head);
        }
        if ((cMeta = (confirmPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE)).getItemMeta()) != null) {
            cMeta.setDisplayName("\u00a7aConfirm Reset");
            confirmPane.setItemMeta(cMeta);
            inv.setItem(15, confirmPane);
        }
        if ((dMeta = (declinePane = new ItemStack(Material.RED_STAINED_GLASS_PANE)).getItemMeta()) != null) {
            dMeta.setDisplayName("\u00a7cCancel");
            declinePane.setItemMeta(dMeta);
            inv.setItem(11, declinePane);
        }
        List categoryOrder = this.plugin.getMenusConfig().getStringList("sell-menu.items");
        ConfigurationSection settingsSection = this.plugin.getMenusConfig().getConfigurationSection("sell-menu.item-settings");
        for (int i = 0; i < categoryOrder.size() && i < 9; ++i) {
            String catKey = (String)categoryOrder.get(i);
            String catKeyLower = catKey.toLowerCase(Locale.ROOT);
            Material mat = Material.matchMaterial((String)catKey);
            Object displayName = "\u00a7f" + catKey;
            if (settingsSection != null && settingsSection.isConfigurationSection(catKey)) {
                String dn;
                Material override;
                String matName;
                ConfigurationSection thisCatSec = settingsSection.getConfigurationSection(catKey);
                if (thisCatSec.contains("material") && (matName = thisCatSec.getString("material")) != null && (override = Material.matchMaterial((String)matName.toUpperCase(Locale.ROOT))) != null) {
                    mat = override;
                }
                if (thisCatSec.contains("displayname") && (dn = thisCatSec.getString("displayname")) != null) {
                    displayName = Utils.formatColors(dn);
                }
            }
            if (mat == null) continue;
            double rawCatSold = this.plugin.getRawTotalSold(target.getUniqueId(), catKeyLower);
            String formattedSold = Utils.abbreviateNumber(rawCatSold);
            ItemStack catItem = new ItemStack(mat);
            ItemMeta im = catItem.getItemMeta();
            if (im != null) {
                im.setDisplayName((String)displayName);
                List<String> lore = new ArrayList<>();
                lore.add("\u00a77Sold: \u00a7b" + formattedSold);
                im.setLore(lore);
                catItem.setItemMeta(im);
            }
            inv.setItem(27 + i, catItem);
        }
        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        String title = view.getTitle();
        if (title == null || !title.startsWith(GUI_TITLE_PREFIX)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player admin = (Player)e.getWhoClicked();
        UUID adminUUID = admin.getUniqueId();
        if (!this.pendingReset.containsKey(adminUUID)) {
            admin.closeInventory();
            return;
        }
        int slot = e.getRawSlot();
        if (slot == 15) {
            UUID targetUUID = this.pendingReset.get(adminUUID);
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer((UUID)targetUUID);
            this.plugin.resetPlayerData(targetUUID);
            admin.playSound(admin.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            admin.sendMessage(Utils.formatColors("\u00a7aAll sell stats for \u00a7e" + targetOffline.getName() + " \u00a7ahas been reset."));
            if (targetOffline.isOnline()) {
                ((Player)targetOffline).sendMessage(Utils.formatColors("\u00a7cYour sell stats has been reset by an admin."));
            }
            this.pendingReset.remove(adminUUID);
            admin.closeInventory();
            return;
        }
        if (slot == 11) {
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            this.pendingReset.remove(adminUUID);
            admin.closeInventory();
            return;
        }
    }
}

