
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.List;
import java.util.Map;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KeysEditorGUI {
    private final DonutCrates plugin;

    public KeysEditorGUI(DonutCrates pl) {
        this.plugin = pl;
    }

    public Inventory build() {
        ItemStack back;
        ItemMeta bm;
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444Keys Editor"));
        holder.setInventory(inv);
        int idx = 0;
        for (Map.Entry<String, Block> entry : this.plugin.crateMgr.crateBlocks.entrySet()) {
            String crate = entry.getKey();
            ItemStack keyIcon = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta im = keyIcon.getItemMeta();
            if (im != null) {
                im.displayName(Utils.toComponent("&#0f99e3" + crate + " Key"));
                im.lore(List.of(Utils.toComponent("&#aaaaaaClick to open the key editor.")));
                keyIcon.setItemMeta(im);
            }
            if (idx >= inv.getSize()) break;
            inv.setItem(idx++, keyIcon);
        }
        if ((bm = (back = new ItemStack(Material.ARROW)).getItemMeta()) != null) {
            bm.displayName(Utils.toComponent("&#f5c542Back"));
            bm.lore(List.of(Utils.toComponent("&#aaaaaaClick to go back to the Editor Menu.")));
            back.setItemMeta(bm);
        }
        inv.setItem(53, back);
        return holder.getInventory();
    }
}

