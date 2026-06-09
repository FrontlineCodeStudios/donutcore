
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KeyEditorGUI {
    private final DonutCrates plugin;

    public KeyEditorGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String keyId) {
        ItemStack keyItem;
        Material material;
        this.plugin.ensureKeyConfig(keyId);
        FileConfiguration saves = this.plugin.cfg.saves;
        String base = "keys." + keyId;
        String name = saves.getString(base + ".displayname", "&#0fe30f" + keyId + " Key");
        String mat = saves.getString(base + ".material", "TRIPWIRE_HOOK");
        boolean virt = saves.getBoolean(base + ".virtual", true);
        try {
            material = Material.valueOf((String)mat);
        }
        catch (IllegalArgumentException ex) {
            material = Material.TRIPWIRE_HOOK;
        }
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(null, 27, Utils.toComponent("&#444444" + keyId + " Key Editor"));
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
        if (saves.isItemStack(base + ".item")) {
            ItemStack tmp = saves.getItemStack(base + ".item");
            keyItem = tmp == null ? new ItemStack(material) : tmp.clone();
        } else {
            keyItem = new ItemStack(material);
            ItemMeta km0 = keyItem.getItemMeta();
            if (km0 != null) {
                km0.displayName(Utils.toComponent(name));
                keyItem.setItemMeta(km0);
            }
        }
        ItemMeta km = keyItem.getItemMeta();
        if (km != null) {
            ArrayList<Component> lore = km.hasLore() && km.lore() != null ? new ArrayList<>(km.lore()) : new ArrayList<>();
            lore.add(Utils.toComponent(""));
            lore.add(Utils.toComponent("&#bfbfbfPut an item on your cursor"));
            lore.add(Utils.toComponent("&#bfbfbfand click this slot to set"));
            lore.add(Utils.toComponent("&#bfbfbfthe physical key item."));
            km.lore(lore);
            km.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            keyItem.setItemMeta(km);
        }
        inv.setItem(10, keyItem);
        inv.setItem(12, this.item(Material.NAME_TAG, "&#f5f5f5Edit Key Displayname", "&#bfbfbfClick to edit the name using a sign.", "&#bfbfbfSupports & and &#RRGGBB colors."));
        inv.setItem(14, this.item(virt ? Material.LIME_DYE : Material.RED_DYE, virt ? "&#f5f5f5Virtual Keys: &#0fe30fENABLED" : "&#f5f5f5Virtual Keys: &#d61111DISABLED", "&#bfbfbfIf enabled, keys are stored virtually", "&#bfbfbfin saves.yml (per player) instead of items."));
        inv.setItem(16, this.item(Material.CHEST, "&#0fe30fReceive Key Item", "&#bfbfbfClick to receive one key item", "&#bfbfbfso you can clone it or test it."));
        inv.setItem(18, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to the &#0fe30fKey Manager&#bfbfbf."));
        int usedBy = this.plugin.countCratesUsingKey(keyId);
        inv.setItem(26, this.item(Material.BARRIER, "&#d61111&lDelete Key", new String[]{"&#bfbfbfDeletes this key permanently.", usedBy > 0 ? "&#d61111Warning: &#bfbfbfUsed by &f" + usedBy + " &#bfbfbfcrates." : "&#bfbfbfNot used by any crate.", "", "&#d61111This action cannot be undone."}));
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
            im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            i.setItemMeta(im);
        }
        return i;
    }
}

