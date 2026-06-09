
package ro.andreilarazboi.donutcore.crates.gui;

import ro.andreilarazboi.donutcore.crates.CrateHolder;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateGUI {
    private final DonutCrates plugin;

    public CrateGUI(DonutCrates pl) {
        this.plugin = pl;
    }

    public Inventory build(String crateName, boolean preview) {
        ConfigurationSection items;
        ConfigurationSection sec = this.plugin.cfg.crates.getConfigurationSection("Crates." + crateName);
        if (sec == null) {
            return Bukkit.createInventory(null, 9, Utils.toComponent(Utils.formatColors("&cInvalid crate")));
        }
        String title = Utils.formatColors(sec.getString("title", crateName).replace("%crate%", crateName));
        int rows = sec.getInt("rows", 3);
        boolean fill = sec.getBoolean("fillerEnabled", false);
        Material filler = Material.valueOf((String)sec.getString("fillerMaterial", "GRAY_STAINED_GLASS_PANE"));
        CrateHolder holder = new CrateHolder(crateName, preview);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, Utils.toComponent(title));
        holder.setInventory(inv);
        if (fill) {
            ItemStack f = new ItemStack(filler);
            ItemMeta fm = f.getItemMeta();
            fm.displayName(Utils.toComponent(" "));
            f.setItemMeta(fm);
            for (int i = 0; i < rows * 9; ++i) {
                inv.setItem(i, f);
            }
        }
        if ((items = sec.getConfigurationSection("Items")) != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection it = items.getConfigurationSection(key);
                ItemStack is = this.plugin.guiItemUtil.buildItemFromSection(it);
                inv.setItem(it.getInt("slot", 0), is);
            }
        }
        return inv;
    }
}

