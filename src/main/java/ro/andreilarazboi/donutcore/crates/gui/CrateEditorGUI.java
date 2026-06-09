
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateEditorGUI {
    private final DonutCrates plugin;

    public CrateEditorGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crateName) {
        int rows = this.plugin.cfg.crates.getInt("Crates." + crateName + ".rows", 3);
        rows = Math.max(1, Math.min(6, rows));
        int editorRows = Math.min(rows + 1, 6);
        int size = editorRows * 9;
        int rewardAreaSize = rows * 9;
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, size, Utils.toComponent("&#444444" + crateName + " Rewards"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < size; ++i) {
            inv.setItem(i, filler);
        }
        ConfigurationSection items = this.plugin.cfg.crates.getConfigurationSection("Crates." + crateName + ".Items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection it = items.getConfigurationSection(key);
                int slot = it.getInt("slot", 0);
                if (slot < 0 || slot >= rewardAreaSize) continue;
                ItemStack is = this.plugin.guiItemUtil.buildItemFromSection(it);
                ItemMeta im = is.getItemMeta();
                ArrayList<Component> lore = im != null && im.hasLore() && im.lore() != null ? new ArrayList<>(im.lore()) : new ArrayList<>();
                double chance = it.getDouble("chance", 0.0);
                lore.add(Utils.toComponent(""));
                if (chance > 0.0) {
                    lore.add(Utils.toComponent("&#27B0F5Chance: &f" + chance + "%"));
                } else {
                    lore.add(Utils.toComponent("&#27B0F5Chance: &fAuto (equal weight)"));
                }
                lore.add(Utils.toComponent("&#bfbfbfClick to edit reward."));
                lore.add(Utils.toComponent("&#bfbfbfShift-Left/Right to move."));
                if (im == null) {
                    im = is.getItemMeta();
                }
                im.lore(lore);
                is.setItemMeta(im);
                inv.setItem(slot, is);
            }
        }
        int bottomRowStart = (editorRows - 1) * 9;
        inv.setItem(bottomRowStart, this.named(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to crate settings."));
        int center = bottomRowStart + 4;
        inv.setItem(center, this.named(Material.PURPLE_STAINED_GLASS_PANE, "&#991be1&lAdd Reward", "&#bfbfbfDrag an item from your inventory", "&#bfbfbfonto this slot to add it as a reward."));
        return holder.getInventory();
    }

    private ItemStack named(Material m, String name, String ... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.displayName(Utils.toComponent(name));
        if (lore.length > 0) {
            ArrayList<Component> l = new ArrayList<>();
            for (String s : lore) {
                l.add(Utils.toComponent(s));
            }
            im.lore(l);
        }
        i.setItemMeta(im);
        return i;
    }
}

