
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class CrateNameColorGUI {
    private final DonutCrates plugin;
    private final NamespacedKey colorTag;

    public CrateNameColorGUI(DonutCrates plugin) {
        this.plugin = plugin;
        this.colorTag = new NamespacedKey((Plugin)plugin.getPlugin(), "crate_name_color");
    }

    public Inventory build(String crate) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, 54, Utils.toComponent("&#444444" + crate + " Name Color"));
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
        inv.setItem(45, this.simple(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to display name editor."));
        inv.setItem(4, this.simple(Material.BOOK, "&#0f99e3Pick a Minecraft Color", "&#bfbfbfClick a color to apply it."));
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29};
        ColorOpt[] colors = new ColorOpt[]{new ColorOpt("&0", Material.BLACK_CONCRETE, "Black"), new ColorOpt("&1", Material.BLUE_CONCRETE, "Dark Blue"), new ColorOpt("&2", Material.GREEN_CONCRETE, "Dark Green"), new ColorOpt("&3", Material.CYAN_CONCRETE, "Dark Aqua"), new ColorOpt("&4", Material.RED_CONCRETE, "Dark Red"), new ColorOpt("&5", Material.PURPLE_CONCRETE, "Dark Purple"), new ColorOpt("&6", Material.ORANGE_CONCRETE, "Gold"), new ColorOpt("&7", Material.LIGHT_GRAY_CONCRETE, "Gray"), new ColorOpt("&8", Material.GRAY_CONCRETE, "Dark Gray"), new ColorOpt("&9", Material.LIGHT_BLUE_CONCRETE, "Blue"), new ColorOpt("&a", Material.LIME_CONCRETE, "Green"), new ColorOpt("&b", Material.LIGHT_BLUE_WOOL, "Aqua"), new ColorOpt("&c", Material.PINK_CONCRETE, "Red"), new ColorOpt("&d", Material.MAGENTA_CONCRETE, "Light Purple"), new ColorOpt("&e", Material.YELLOW_CONCRETE, "Yellow"), new ColorOpt("&f", Material.WHITE_CONCRETE, "White")};
        String currentRaw = this.plugin.getCrateDisplayNameRaw(crate);
        String currentColored = currentRaw == null ? "" : Utils.formatColors(currentRaw);
        for (int i = 0; i < colors.length; ++i) {
            ColorOpt c = colors[i];
            ItemStack it = new ItemStack(c.mat);
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                boolean selected = currentColored.startsWith(Utils.formatColors(c.code));
                im.displayName(Utils.toComponent(c.code + "&l" + c.name + " &8(" + c.code + ")"));
                ArrayList<Component> lore = new ArrayList<>();
                lore.add(Utils.toComponent("&#bfbfbfClick to apply this color."));
                lore.add(Utils.toComponent("&#7f7f7fApplies to the display name."));
                if (selected) {
                    lore.add(Utils.toComponent(""));
                    lore.add(Utils.toComponent("&#0fe30f\u2714 Currently applied"));
                    im.addEnchant(Enchantment.UNBREAKING, 1, true);
                    im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                }
                im.lore(lore);
                im.getPersistentDataContainer().set(this.colorTag, PersistentDataType.STRING, c.code);
                it.setItemMeta(im);
            }
            inv.setItem(slots[i], it);
        }
        return inv;
    }

    public NamespacedKey colorTag() {
        return this.colorTag;
    }

    private ItemStack simple(Material mat, String name, String loreLine) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Utils.toComponent(name));
            im.lore(List.of(Utils.toComponent(loreLine)));
            it.setItemMeta(im);
        }
        return it;
    }

    private record ColorOpt(String code, Material mat, String name) {
    }
}

