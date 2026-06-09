
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

public class KeySelectGUI {
    private final DonutCrates plugin;
    private final NamespacedKey keyIdTag;
    private static final int[] KEY_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    public KeySelectGUI(DonutCrates plugin) {
        this.plugin = plugin;
        this.keyIdTag = new NamespacedKey((Plugin)plugin.getPlugin(), "select_key_id");
    }

    public NamespacedKey getKeyIdTag() {
        return this.keyIdTag;
    }

    public Inventory build(String crate) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(null, 54, Utils.toComponent("&#444444" + crate + " Select Key"));
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
        inv.setItem(45, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to crate settings."));
        inv.setItem(4, this.item(Material.BOOK, "&#0f99e3Select a Key", "&#bfbfbfCrate: &f" + crate, "", "&#bfbfbfPick which key should open this crate.", "&#bfbfbfYou can reuse the same key across", "&#bfbfbfmultiple crates/worlds."));
        String selected = this.plugin.getKeyIdForCrate(crate);
        if (selected == null || selected.isBlank()) {
            selected = crate;
        }
        selected = selected.trim();
        ConfigurationSection keysRoot = this.plugin.cfg.saves.getConfigurationSection("keys");
        if (keysRoot == null || keysRoot.getKeys(false).isEmpty()) {
            inv.setItem(22, this.item(Material.BARRIER, "&#d61111No Keys Found", "&#bfbfbfNo keys exist yet.", "&#bfbfbfCreate a crate (auto creates a key),", "&#bfbfbfor add keys in &fsaves.yml&7 under &fkeys:&7."));
            return inv;
        }
        int idx = 0;
        for (String keyId : keysRoot.getKeys(false)) {
            if (idx >= KEY_SLOTS.length) break;
            this.plugin.ensureKeyConfig(keyId);
            boolean isSelected = keyId.equalsIgnoreCase(selected);
            ItemStack icon = this.plugin.buildKeyItemById(keyId, 1);
            ItemMeta im = icon.getItemMeta();
            if (im == null) continue;
            im.displayName(Utils.toComponent("&#0fe30fKey: &f" + keyId));
            ArrayList<Component> lore = im.hasLore() && im.lore() != null ? new ArrayList<>(im.lore()) : new ArrayList<>();
            lore.removeIf(c -> {
                String s = LegacyComponentSerializer.legacySection().serialize(c);
                String t = Utils.stripColor(s);
                if (t == null) return false;
                return (t = t.toLowerCase(Locale.ROOT)).contains("currently selected") || t.contains("click to select");
            });
            lore.add(Utils.toComponent(""));
            if (isSelected) {
                lore.add(Utils.toComponent("&#0fe30f\u2714 Currently selected"));
                lore.add(Utils.toComponent("&#bfbfbfThis key will open &f" + crate));
                im.addEnchant(Enchantment.UNBREAKING, 1, true);
                im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            } else {
                lore.add(Utils.toComponent("&#bfbfbfClick to select this key."));
            }
            im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            im.getPersistentDataContainer().set(this.keyIdTag, PersistentDataType.STRING, keyId.toLowerCase(Locale.ROOT));
            im.lore(lore);
            icon.setItemMeta(im);
            inv.setItem(KEY_SLOTS[idx++], icon);
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

