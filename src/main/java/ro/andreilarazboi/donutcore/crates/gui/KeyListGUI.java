
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class KeyListGUI {
    private final DonutCrates plugin;
    private static final int[] KEY_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final NamespacedKey keyIdTag;

    public KeyListGUI(DonutCrates plugin) {
        this.plugin = plugin;
        this.keyIdTag = new NamespacedKey((Plugin)plugin.getPlugin(), "key_id");
    }

    public NamespacedKey getKeyIdTag() {
        return this.keyIdTag;
    }

    public Inventory build() {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444Key Manager"));
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
            ArrayList<Component> bl = new ArrayList<Component>();
            bl.add(Utils.toComponent("&#bfbfbfReturn to the &#FF073ACrates Editor&#bfbfbf."));
            bm.lore(bl);
            back.setItemMeta(bm);
        }
        inv.setItem(45, back);
        inv.setItem(4, this.item(Material.BOOK, "&#0f99e3Keys", "&#bfbfbfThese keys can be reused across crates.", "&#bfbfbfEdit a key to change its item/virtual mode."));
        FileConfiguration saves = this.plugin.cfg.saves;
        ConfigurationSection keysRoot = saves.getConfigurationSection("keys");
        if (keysRoot == null || keysRoot.getKeys(false).isEmpty()) {
            inv.setItem(22, this.item(Material.BARRIER, "&#d61111No Keys Found", "&#bfbfbfCreate a crate to auto-create a key,", "&#bfbfbfor add keys in &fsaves.yml&7 under &fkeys:&7."));
            return inv;
        }
        int index = 0;
        for (String keyId : keysRoot.getKeys(false)) {
            if (index >= KEY_SLOTS.length) break;
            this.plugin.ensureKeyConfig(keyId);
            ItemStack icon = this.plugin.buildKeyItemById(keyId, 1);
            ItemMeta im = icon.getItemMeta();
            if (im == null) continue;
            im.displayName(Utils.toComponent("&#FF073AKey: &f" + keyId));
            ArrayList<Component> lore = new ArrayList<Component>();
            lore.add(Utils.toComponent("&#bfbfbfClick to edit this key."));
            lore.add(Utils.toComponent("&#bfbfbfUsed by crates: &f" + this.plugin.countCratesUsingKey(keyId)));
            im.lore(lore);
            im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            im.getPersistentDataContainer().set(this.keyIdTag, PersistentDataType.STRING, keyId.toLowerCase(Locale.ROOT));
            icon.setItemMeta(im);
            inv.setItem(KEY_SLOTS[index++], icon);
        }
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta im = i.getItemMeta();
        if (im != null) {
            im.displayName(Utils.toComponent(name));
            if (loreLines.length > 0) {
                ArrayList<Component> lore = new ArrayList<Component>();
                for (String s : loreLines) {
                    lore.add(Utils.toComponent(s));
                }
                im.lore(lore);
            }
            i.setItemMeta(im);
        }
        return i;
    }
}

