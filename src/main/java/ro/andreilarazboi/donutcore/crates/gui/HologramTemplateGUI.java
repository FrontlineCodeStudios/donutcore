
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HologramTemplateGUI {
    private final DonutCrates plugin;
    private static final int[] TEMPLATE_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    public HologramTemplateGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crate) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444" + crate + " Hologram Templates"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Utils.toComponent("&#f5f5f5\u00ab Back"));
        ArrayList<Component> bl = new ArrayList<Component>();
        bl.add(Utils.toComponent("&#bfbfbfReturn to the hologram editor."));
        bm.lore(bl);
        back.setItemMeta(bm);
        inv.setItem(45, back);
        ItemStack header = new ItemStack(Material.BOOK);
        ItemMeta hm = header.getItemMeta();
        hm.displayName(Utils.toComponent("&#0f99e3Select a Hologram Template"));
        ArrayList<Component> hl = new ArrayList<Component>();
        hl.add(Utils.toComponent("&#bfbfbfClick a template below to"));
        hl.add(Utils.toComponent("&#bfbfbfchange how this crate's"));
        hl.add(Utils.toComponent("&#bfbfbfhologram text looks."));
        hl.add(Utils.toComponent(""));
        hl.add(Utils.toComponent("&#bfbfbfYou can edit and add templates"));
        hl.add(Utils.toComponent("&#bfbfbfin &fconfig.yml &#bfbfbfunder"));
        hl.add(Utils.toComponent("&f'hologram-templates'&#bfbfbf."));
        hm.lore(hl);
        header.setItemMeta(hm);
        inv.setItem(4, header);
        ConfigurationSection root = this.plugin.cfg.config.getConfigurationSection("hologram-templates");
        String currentTemplate = this.plugin.cfg.crates.getString("Crates." + crate + ".Hologram.template", null);
        if (currentTemplate == null && root != null && root.isConfigurationSection("default")) {
            currentTemplate = "default";
        }
        if (root == null || root.getKeys(false).isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta nm = none.getItemMeta();
            nm.displayName(Utils.toComponent("&#d61111No Templates Defined"));
            ArrayList<Component> nl = new ArrayList<Component>();
            nl.add(Utils.toComponent("&#bfbfbfAdd templates under &f'hologram-templates'"));
            nl.add(Utils.toComponent("&#bfbfbfin &fconfig.yml &7to use this menu."));
            nm.lore(nl);
            none.setItemMeta(nm);
            inv.setItem(22, none);
            return inv;
        }
        int idx = 0;
        for (String id : root.getKeys(false)) {
            boolean isSelected;
            if (idx >= TEMPLATE_SLOTS.length) break;
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            Material mat = Material.ENDER_EYE;
            ArrayList<Component> lore = new ArrayList<Component>();
            isSelected = currentTemplate != null && currentTemplate.equalsIgnoreCase(id);
            if (isSelected) {
                lore.add(Utils.toComponent("&#FF073A\u2714 Currently selected"));
            } else {
                lore.add(Utils.toComponent("&#bfbfbfClick to use this template."));
            }
            List<String> templateLines = sec.getStringList("lines");
            if (!templateLines.isEmpty()) {
                lore.add(Utils.toComponent(""));
                lore.add(Utils.toComponent("&#bfbfbfPreview:"));
                int shown = Math.min(3, templateLines.size());
                for (int i2 = 0; i2 < shown; ++i2) {
                    lore.add(Utils.toComponent("&7\u2022 &f" + templateLines.get(i2)));
                }
                if (templateLines.size() > 3) {
                    lore.add(Utils.toComponent("&7..."));
                }
            }
            ItemStack icon = new ItemStack(mat);
            ItemMeta im = icon.getItemMeta();
            im.displayName(Utils.toComponent("&#FF073ATemplate: &f" + id));
            im.lore(lore);
            if (isSelected) {
                im.addEnchant(Enchantment.UNBREAKING, 1, true);
                im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }
            icon.setItemMeta(im);
            inv.setItem(TEMPLATE_SLOTS[idx++], icon);
        }
        return inv;
    }
}

