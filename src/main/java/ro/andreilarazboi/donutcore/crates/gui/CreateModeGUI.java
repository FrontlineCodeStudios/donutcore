
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CreateModeGUI {

    public CreateModeGUI(DonutCrates plugin) {
    }

    public Inventory build() {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Utils.toComponent("&#444444Create Crate Mode"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Utils.toComponent(" "));
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        inv.setItem(11, this.item(Material.ENCHANTED_BOOK, "&#27B0F5&lRandom Reward", "&#bfbfbfClick to choose this option!"));
        inv.setItem(15, this.item(Material.CHEST, "&#FF073A&lChoose Reward", "&#bfbfbfClick to choose this option!"));
        inv.setItem(18, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to crate manager."));
        return inv;
    }

    private ItemStack item(Material material, String name, String ... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Utils.toComponent(name));
            ArrayList<Component> lore = new ArrayList<Component>();
            for (String line : loreLines) {
                lore.add(Utils.toComponent(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

