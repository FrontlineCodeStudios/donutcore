
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainEditorGUI {
    private final DonutCrates plugin;

    public MainEditorGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build() {
        int rows = 6;
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (String)Utils.formatColors("&#444444Crate Manager"));
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
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(Utils.toComponent("&#f5f5f5\u00ab Back"));
            bm.lore(List.of(Utils.toComponent("&7Return to the main editor.")));
            back.setItemMeta(bm);
        }
        inv.setItem(45, back);
        ItemStack create = new ItemStack(Material.NETHER_STAR);
        ItemMeta cm = create.getItemMeta();
        if (cm != null) {
            cm.displayName(Utils.toComponent("&#0fe30f&lCreate New Crate"));
            cm.lore(List.of(Utils.toComponent("&7Click, then left-click any block"), Utils.toComponent("&7to create a new crate there.")));
            create.setItemMeta(cm);
        }
        inv.setItem(49, create);
        int idx = 10;
        for (Map.Entry<String, Block> entry : this.plugin.crateMgr.crateBlocks.entrySet()) {
            String keyId;
            String crate = entry.getKey();
            Block block = entry.getValue();
            Material mat = Material.CHEST;
            if (block != null && block.getType() != Material.AIR) {
                mat = block.getType();
            }
            boolean keyMissing = !this.plugin.keyExists(keyId = this.plugin.getKeyIdForCrate(crate));
            ItemStack icon = new ItemStack(mat);
            ItemMeta im = icon.getItemMeta();
            if (im != null) {
                im.displayName(Utils.toComponent("&#f5f5f5" + crate));
                ArrayList<Component> lore = new ArrayList<Component>();
                lore.add(Utils.toComponent("&7Click to edit crate settings."));
                lore.add(Utils.toComponent(""));
                lore.add(Utils.toComponent("&#27B0F5Key: &f" + keyId));
                if (keyMissing) {
                    lore.add(Utils.toComponent(""));
                    lore.add(Utils.toComponent("&#d61111\u2716 Problem detected"));
                    lore.add(Utils.toComponent("&#bfbfbfSelected key is missing."));
                    lore.add(Utils.toComponent("&#bfbfbfOpen settings \u2192 Key to fix."));
                }
                im.lore(lore);
                icon.setItemMeta(im);
            }
            if (idx == 49) {
                ++idx;
            }
            if (idx >= inv.getSize()) break;
            inv.setItem(idx++, icon);
            if (idx % 9 != 8) continue;
            idx += 2;
        }
        return inv;
    }
}

