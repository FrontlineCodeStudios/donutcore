
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class OpeningAnimationsGUI {
    private final DonutCrates plugin;
    private final NamespacedKey animKey;

    public OpeningAnimationsGUI(DonutCrates plugin) {
        this.plugin = plugin;
        this.animKey = new NamespacedKey((Plugin)plugin.getPlugin(), "opening_anim_id");
    }

    public Inventory build(String crateName) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444" + crateName + " Opening Animations"));
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
        inv.setItem(45, this.simple(Material.ARROW, "&#0f99e3Back", List.of("&7Return to crate settings."), false, null));
        inv.setItem(49, this.simple(Material.PAPER, "&#FFD700Animation Beta", List.of("&7This animation feature is in beta.", "&7You may experience bugs.", "&7If you find any bugs, please let me know."), false, null));
        String current = this.getCurrent(crateName);
        inv.setItem(10, this.animItem(1, OpeningAnimationType.ROW_SPIN, current));
        inv.setItem(11, this.animItem(2, OpeningAnimationType.CIRCLE_SPIN, current));
        inv.setItem(12, this.animItem(3, OpeningAnimationType.CAROUSEL, current));
        inv.setItem(13, this.animItem(4, OpeningAnimationType.SPIRAL_REVEAL, current));
        inv.setItem(14, this.animItem(5, OpeningAnimationType.CASCADE_DROP, current));
        inv.setItem(15, this.animItem(6, OpeningAnimationType.MATRIX_RAIN, current));
        inv.setItem(16, this.animItem(7, OpeningAnimationType.DOUBLE_SPIN, current));
        inv.setItem(31, this.disabledItem(current));
        return inv;
    }

    private String getCurrent(String crate) {
        boolean enabled = this.plugin.cfg.crates.getBoolean("Crates." + crate + ".opening-animation.enabled", false);
        if (!enabled) {
            return "NONE";
        }
        String type = this.plugin.cfg.crates.getString("Crates." + crate + ".opening-animation.type", "ROW_SPIN");
        if (type == null || type.isBlank()) {
            type = "ROW_SPIN";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private ItemStack animItem(int number, OpeningAnimationType type, String current) {
        String id = type.id();
        boolean selected = id.equalsIgnoreCase(current);
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("&8(BETA)");
        lore.add(" ");
        if (selected) {
            lore.add("&a&l\u2714 SELECTED");
        } else {
            lore.add("&eLeft-click to select");
            lore.add("&bRight-click to preview");
        }
        String name = (selected ? "&#00FF00" : "&#FFD700") + "Animation #" + number;
        return this.simple(type.icon(), name, lore, selected, id);
    }

    private ItemStack disabledItem(String current) {
        boolean selected = "NONE".equalsIgnoreCase(current);
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("&7No opening animation");
        lore.add("&7Rewards appear instantly");
        lore.add(" ");
        if (selected) {
            lore.add("&c&l\u2714 SELECTED");
        } else {
            lore.add("&eLeft-click to disable animations");
        }
        String name = (selected ? "&#FF0000" : "&#888888") + "Disabled";
        return this.simple(Material.BARRIER, name, lore, selected, "NONE");
    }

    private ItemStack simple(Material mat, String name, List<String> lore, boolean enchanted, String hiddenId) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Utils.toComponent(name));
            if (lore != null) {
                ArrayList<Component> out = new ArrayList<Component>();
                for (String s : lore) {
                    out.add(Utils.toComponent(s));
                }
                im.lore(out);
            }
            if (hiddenId != null) {
                im.getPersistentDataContainer().set(this.animKey, PersistentDataType.STRING, hiddenId.toUpperCase(Locale.ROOT));
            }
            if (enchanted) {
                im.addEnchant(Enchantment.UNBREAKING, 1, true);
                im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }
            im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            it.setItemMeta(im);
        }
        return it;
    }
}

