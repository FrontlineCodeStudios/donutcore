
package ro.andreilarazboi.donutcore.crates.gui;

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

public class ConfirmGUI {
    private final DonutCrates plugin;

    public ConfirmGUI(DonutCrates pl) {
        this.plugin = pl;
    }

    public Inventory build(Player p, ItemStack clicked, String crateName) {
        ConfigurationSection c = this.plugin.cfg.config.getConfigurationSection("confirm-menu");
        int rows = c.getInt("rows", 3);
        boolean fill = c.getBoolean("fillerEnabled", false);
        Material filler = Material.valueOf((String)c.getString("fillerMaterial", "GRAY_STAINED_GLASS_PANE"));
        Inventory inv = Bukkit.createInventory(null, rows * 9, Utils.toComponent(c.getString("title")));
        if (fill) {
            ItemStack f = new ItemStack(filler);
            ItemMeta fm = f.getItemMeta();
            fm.displayName(Utils.toComponent(" "));
            f.setItemMeta(fm);
            for (int i = 0; i < rows * 9; ++i) {
                inv.setItem(i, f);
            }
        }
        ItemStack copy = clicked.clone();
        ItemMeta cm = copy.getItemMeta();
        ItemMeta clickedMeta = clicked.getItemMeta();
        String clickedName = (clickedMeta != null && clickedMeta.hasDisplayName() && clickedMeta.displayName() != null)
                ? LegacyComponentSerializer.legacySection().serialize(clickedMeta.displayName())
                : clicked.getType().name();
        cm.displayName(Utils.toComponent(c.getString("ClickedItem.displayname", "%ClickedItemName%").replace("%ClickedItemName%", clickedName)));
        copy.setItemMeta(cm);
        inv.setItem(c.getInt("ClickedItem.slot", 13), copy);
        ItemStack con = new ItemStack(Material.valueOf((String)c.getString("Confirm.material", "LIME_STAINED_GLASS_PANE")));
        ItemMeta cmeta = con.getItemMeta();
        cmeta.displayName(Utils.toComponent(c.getString("Confirm.displayname", "&aConfirm")));
        cmeta.lore(Utils.toComponents(c.getStringList("Confirm.lore")));
        con.setItemMeta(cmeta);
        inv.setItem(c.getInt("Confirm.slot", 15), con);
        ItemStack dec = new ItemStack(Material.valueOf((String)c.getString("Decline.material", "RED_STAINED_GLASS_PANE")));
        ItemMeta dmeta = dec.getItemMeta();
        dmeta.displayName(Utils.toComponent(c.getString("Decline.displayname", "&cDecline")));
        dmeta.lore(Utils.toComponents(c.getStringList("Decline.lore")));
        dec.setItemMeta(dmeta);
        inv.setItem(c.getInt("Decline.slot", 11), dec);
        return inv;
    }
}

