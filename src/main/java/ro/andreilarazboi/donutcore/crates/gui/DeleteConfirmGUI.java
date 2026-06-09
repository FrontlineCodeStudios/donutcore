
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DeleteConfirmGUI {
    private final DonutCrates plugin;

    public DeleteConfirmGUI(DonutCrates pl) {
        this.plugin = pl;
    }

    public Inventory build(Player p, ItemStack display) {
        ConfigurationSection c = this.plugin.cfg.config.getConfigurationSection("delete-confirm-menu");
        String title = Utils.formatColors(DeleteConfirmGUI.getString(c, "title", "&#8b0000\u1d05\u1d07\u029f\u1d07\u1d1b\u1d07 \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d"));
        int size = DeleteConfirmGUI.clamp9(DeleteConfirmGUI.getInt(c, "rows", 3));
        boolean fill = DeleteConfirmGUI.getBool(c, "fillerEnabled", true);
        Material filler = DeleteConfirmGUI.safeMat(DeleteConfirmGUI.getString(c, "fillerMaterial", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        Inventory inv = Bukkit.createInventory(null, (int)size, (String)title);
        if (fill) {
            ItemStack f = new ItemStack(filler);
            ItemMeta fm = f.getItemMeta();
            if (fm != null) {
                fm.displayName(Utils.toComponent(" "));
                f.setItemMeta(fm);
            }
            for (int i = 0; i < size; ++i) {
                inv.setItem(i, f);
            }
        }
        int clickedSlot = DeleteConfirmGUI.getInt(c, "ClickedItem.slot", 13);
        String clickedNameTpl = DeleteConfirmGUI.getString(c, "ClickedItem.displayname", "%ClickedItemName%");
        List<String> clickedLore = DeleteConfirmGUI.getList(c, "ClickedItem.lore");
        ItemStack mid = display == null ? new ItemStack(Material.BARRIER) : display.clone();
        ItemMeta cm = mid.getItemMeta();
        if (cm != null) {
            String repl;
            if (display != null && display.hasItemMeta() && display.getItemMeta().hasDisplayName() && display.getItemMeta().displayName() != null) {
                repl = LegacyComponentSerializer.legacySection().serialize(display.getItemMeta().displayName());
            } else {
                repl = display != null ? display.getType().name() : "Unknown";
            }
            cm.displayName(Utils.toComponent(clickedNameTpl.replace("%ClickedItemName%", repl)));
            if (!clickedLore.isEmpty()) {
                cm.lore(Utils.toComponents(clickedLore));
            }
            mid.setItemMeta(cm);
        }
        DeleteConfirmGUI.safeSet(inv, clickedSlot, mid);
        Material confirmMat = DeleteConfirmGUI.safeMat(DeleteConfirmGUI.getString(c, "Confirm.material", "LIME_STAINED_GLASS_PANE"), Material.LIME_STAINED_GLASS_PANE);
        int confirmSlot = DeleteConfirmGUI.getInt(c, "Confirm.slot", 15);
        String confirmName = DeleteConfirmGUI.getString(c, "Confirm.displayname", "&aConfirm Delete");
        List<String> confirmLore = DeleteConfirmGUI.getList(c, "Confirm.lore");
        ItemStack con = new ItemStack(confirmMat);
        ItemMeta cmeta = con.getItemMeta();
        if (cmeta != null) {
            cmeta.displayName(Utils.toComponent(confirmName));
            if (!confirmLore.isEmpty()) {
                cmeta.lore(Utils.toComponents(confirmLore));
            }
            con.setItemMeta(cmeta);
        }
        DeleteConfirmGUI.safeSet(inv, confirmSlot, con);
        Material declineMat = DeleteConfirmGUI.safeMat(DeleteConfirmGUI.getString(c, "Decline.material", "RED_STAINED_GLASS_PANE"), Material.RED_STAINED_GLASS_PANE);
        int declineSlot = DeleteConfirmGUI.getInt(c, "Decline.slot", 11);
        String declineName = DeleteConfirmGUI.getString(c, "Decline.displayname", "&cCancel");
        List<String> declineLore = DeleteConfirmGUI.getList(c, "Decline.lore");
        ItemStack dec = new ItemStack(declineMat);
        ItemMeta dmeta = dec.getItemMeta();
        if (dmeta != null) {
            dmeta.displayName(Utils.toComponent(declineName));
            if (!declineLore.isEmpty()) {
                dmeta.lore(Utils.toComponents(declineLore));
            }
            dec.setItemMeta(dmeta);
        }
        DeleteConfirmGUI.safeSet(inv, declineSlot, dec);
        return inv;
    }

    private static int clamp9(int rows) {
        rows = Math.max(1, Math.min(6, rows));
        return rows * 9;
    }

    private static void safeSet(Inventory inv, int slot, ItemStack is) {
        if (slot < 0 || slot >= inv.getSize()) {
            return;
        }
        inv.setItem(slot, is);
    }

    private static Material safeMat(String name, Material def) {
        try {
            return Material.valueOf((String)name);
        }
        catch (Exception ignored) {
            return def;
        }
    }

    private static String getString(ConfigurationSection c, String path, String def) {
        return c != null && c.isString(path) ? c.getString(path) : def;
    }

    private static int getInt(ConfigurationSection c, String path, int def) {
        return c != null && c.isInt(path) ? c.getInt(path) : def;
    }

    private static boolean getBool(ConfigurationSection c, String path, boolean def) {
        return c != null && c.isBoolean(path) ? c.getBoolean(path) : def;
    }

    private static List<String> getList(ConfigurationSection c, String path) {
        return c != null && c.isList(path) ? c.getStringList(path) : Collections.emptyList();
    }
}

