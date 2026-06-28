
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class HologramBackgroundGUI {
    private final DonutCrates plugin;
    private final NamespacedKey bgColorTag;
    private static final int[] COLOR_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public HologramBackgroundGUI(DonutCrates plugin) {
        this.plugin = plugin;
        this.bgColorTag = new NamespacedKey((Plugin)plugin.getPlugin(), "holo_bg_color");
    }

    private List<BgOption> getOptions() {
        ArrayList<BgOption> list = new ArrayList<BgOption>();
        list.add(new BgOption("TRANSPARENT", Material.WHITE_DYE, "Transparent (No Box)", "No background, only text."));
        list.add(new BgOption("DARK", Material.BLACK_WOOL, "Default Dark Background", "Classic dark box behind the text."));
        list.add(new BgOption("WHITE", Material.WHITE_WOOL, "White", "White background."));
        list.add(new BgOption("ORANGE", Material.ORANGE_WOOL, "Orange", "Orange background."));
        list.add(new BgOption("MAGENTA", Material.MAGENTA_WOOL, "Magenta", "Magenta background."));
        list.add(new BgOption("LIGHT_BLUE", Material.LIGHT_BLUE_WOOL, "Light Blue", "Light blue background."));
        list.add(new BgOption("YELLOW", Material.YELLOW_WOOL, "Yellow", "Yellow background."));
        list.add(new BgOption("LIME", Material.LIME_WOOL, "Lime", "Lime green background."));
        list.add(new BgOption("PINK", Material.PINK_WOOL, "Pink", "Pink background."));
        list.add(new BgOption("GRAY", Material.GRAY_WOOL, "Gray", "Gray background."));
        list.add(new BgOption("LIGHT_GRAY", Material.LIGHT_GRAY_WOOL, "Light Gray", "Soft light gray background."));
        list.add(new BgOption("CYAN", Material.CYAN_WOOL, "Cyan", "Cyan background."));
        list.add(new BgOption("PURPLE", Material.PURPLE_WOOL, "Purple", "Purple background."));
        list.add(new BgOption("BLUE", Material.BLUE_WOOL, "Blue", "Blue background."));
        list.add(new BgOption("BROWN", Material.BROWN_WOOL, "Brown", "Brown background."));
        list.add(new BgOption("GREEN", Material.GREEN_WOOL, "Green", "Dark green background."));
        list.add(new BgOption("RED", Material.RED_WOOL, "Red", "Red background."));
        list.add(new BgOption("BLACK", Material.BLACK_WOOL, "Black", "Pure black background."));
        return list;
    }

    public Inventory build(String crate) {
        EditorHolder holder = new EditorHolder();
        Inventory inv= Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444" + crate + " Hologram Background"));
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
        hm.displayName(Utils.toComponent("&#0f99e3Select Background Color"));
        ArrayList<Component> hl = new ArrayList<Component>();
        hl.add(Utils.toComponent("&#bfbfbfThis controls the box behind"));
        hl.add(Utils.toComponent("&#bfbfbfyour hologram text."));
        hl.add(Utils.toComponent(""));
        hl.add(Utils.toComponent("&#bfbfbfUse &fTransparent &7for no box at all."));
        hm.lore(hl);
        header.setItemMeta(hm);
        inv.setItem(4, header);
        String base = "Crates." + crate + ".Hologram";
        ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
        String selectedId = "TRANSPARENT";
        if (h != null) {
            String cfgId = h.getString("bgColor", null);
            boolean legacyTransparent = h.getBoolean("background-transparent", true);
            selectedId = cfgId != null ? cfgId : (legacyTransparent ? "TRANSPARENT" : "DARK");
        }
        List<BgOption> options = this.getOptions();
        int idx = 0;
        for (BgOption opt : options) {
            if (idx >= COLOR_SLOTS.length) break;
            boolean isSelected = opt.id.equalsIgnoreCase(selectedId);
            ItemStack icon = new ItemStack(opt.material);
            ItemMeta im = icon.getItemMeta();
            im.displayName(Utils.toComponent(isSelected ? "&#0fe30f" + opt.name + " &7(Selected)" : "&#f5f5f5" + opt.name));
            ArrayList<Component> lore = new ArrayList<Component>();
            lore.add(Utils.toComponent("&#bfbfbf" + opt.description));
            lore.add(Utils.toComponent(""));
            if (isSelected) {
                lore.add(Utils.toComponent("&#0fe30f\u2714 Currently selected"));
                im.addEnchant(Enchantment.UNBREAKING, 1, true);
                im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            } else {
                lore.add(Utils.toComponent("&#bfbfbfClick to select this background."));
            }
            im.lore(lore);
            im.getPersistentDataContainer().set(this.bgColorTag, PersistentDataType.STRING, opt.id.toUpperCase(Locale.ROOT));
            icon.setItemMeta(im);
            inv.setItem(COLOR_SLOTS[idx++], icon);
        }
        return inv;
    }

    private record BgOption(String id, Material material, String name, String description) {
    }
}

