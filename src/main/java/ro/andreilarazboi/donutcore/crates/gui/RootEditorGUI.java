
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.List;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RootEditorGUI {

    public RootEditorGUI(DonutCrates plugin) {
    }

    public Inventory build() {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Utils.toComponent("&#444444Crates Editor"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        inv.setItem(11, this.item(Material.CHEST, "&#FF073A&lCrates", "&7Edit crates, rewards and settings."));
        inv.setItem(15, this.item(Material.TRIPWIRE_HOOK, "&#FF073A&lKeys", "&7Manage crate keys and key behaviour."));
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta im = i.getItemMeta();
        im.displayName(Utils.toComponent(name));
        if (loreLines.length > 0) {
            im.lore(Utils.toComponents(List.of(loreLines)));
        }
        i.setItemMeta(im);
        return i;
    }
}

