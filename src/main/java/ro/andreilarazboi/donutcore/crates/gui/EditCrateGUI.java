
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

public class EditCrateGUI {
    private final DonutCrates plugin;

    public EditCrateGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crateName) {
        boolean randomEnabled = this.plugin.cfg.crates.getBoolean("Crates." + crateName + ".random.enabled", false);
        int rows = this.plugin.cfg.crates.getInt("Crates." + crateName + ".rows", 3);
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(null, 27, Utils.toComponent("&#444444" + crateName + " Edit"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        inv.setItem(10, this.item(Material.NETHER_STAR, "&#0fe30f&lCopy Crate", "&#bfbfbfClone this crate into a new one,", "&#bfbfbfthen left-click a block to choose", "&#bfbfbfthe new crate location."));
        inv.setItem(11, this.item(Material.COMPASS, "&#f5f5f5&lMove Crate", "&#bfbfbfClick, then left-click a block", "&#bfbfbfto move this crate there."));
        inv.setItem(12, this.item(Material.BARRIER, "&#d61111&lDelete Crate", "&#bfbfbfOpen a confirmation dialog.", "&#d61111This action cannot be undone."));
        inv.setItem(13, this.item(Material.NAME_TAG, "&#f5f5f5&lRename Crate ID", "&#bfbfbfClick, then type a new &fID &#bfbfbfin chat.", "&#27B0F5Current: &f" + crateName));
        String displayRaw = this.plugin.getCrateDisplayNameRaw(crateName);
        String displayPretty = this.plugin.getCrateDisplayNameFormatted(crateName);
        inv.setItem(15, this.item(Material.GLOW_ITEM_FRAME, "&#27B0F5&lDisplay Name", "&#27B0F5Current: &r" + displayPretty, "&#7f7f7f(raw: " + (displayRaw == null ? "default" : displayRaw) + ")", "", "&#bfbfbfClick to edit."));
        inv.setItem(14, this.item(Material.COMPARATOR, "&#f5f5f5&lRows", "&#bfbfbfChange how many rows the", "&#bfbfbfcrate preview uses.", "", "&#27B0F5Current: &f" + rows));
        inv.setItem(16, this.item(randomEnabled ? Material.ENCHANTED_BOOK : Material.BOOK, randomEnabled ? "&#27B0F5&lRandom Rewards: &#0fe30fENABLED" : "&#27B0F5&lRandom Rewards: &#d61111DISABLED", "&#bfbfbfWhen enabled, opening this crate", "&#bfbfbfrolls a random reward based on", "&#bfbfbfper-item &#27B0F5chance&#bfbfbf values.", "", "&#bfbfbfClick to toggle."));
        inv.setItem(18, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to the &#0fe30fCrate Settings&#bfbfbf."));
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta im = i.getItemMeta();
        im.displayName(Utils.toComponent(name));
        if (loreLines.length > 0) {
            ArrayList<Component> lore = new ArrayList<Component>();
            for (String s : loreLines) {
                lore.add(Utils.toComponent(s));
            }
            im.lore(lore);
        }
        i.setItemMeta(im);
        return i;
    }
}

