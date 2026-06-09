
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateSettingsGUI {
    private final DonutCrates plugin;

    public CrateSettingsGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crateName) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, Utils.toComponent("&#444444" + crateName + " Settings"));
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
        String keyId = this.plugin.getKeyIdForCrate(crateName);
        boolean keyMissing = !this.plugin.keyExists(keyId);
        boolean animEnabled = this.plugin.cfg.crates.getBoolean("Crates." + crateName + ".opening-animation.enabled", false);
        String typeId = this.plugin.cfg.crates.getString("Crates." + crateName + ".opening-animation.type", OpeningAnimationType.ROW_SPIN.id());
        OpeningAnimationType type = OpeningAnimationType.byId(typeId);
        inv.setItem(4, this.item(Material.BOOK, "&#0f99e3&lCrate Settings", "&#bfbfbfManage core crate features.", "&#7f7f7f(advanced actions are in Edit Crate)"));
        inv.setItem(11, this.item(Material.CHEST, "&#0fe30f&lRewards", "&#bfbfbfEdit crate rewards in a separate menu."));
        inv.setItem(13, this.item(Material.ENDER_CHEST, "&#0f99e3&lPreview", "&#bfbfbfOpen a preview of this crate."));
        inv.setItem(15, this.item(Material.ENDER_EYE, "&#f5f5f5&lHologram", "&#bfbfbfConfigure hologram text and style."));
        inv.setItem(20, this.item(Material.NETHER_STAR, "&#f5f5f5&lEdit Crate", "&#bfbfbfRename ID, move, rows,", "&#bfbfbfdisplay name, random mode, copy/delete."));
        ArrayList<Component> animLore = new ArrayList<>();
        animLore.add(Utils.toComponent("&#bfbfbfChoose how the crate opens."));
        animLore.add(Utils.toComponent(""));
        animLore.add(Utils.toComponent("&#27B0F5Enabled: " + (animEnabled ? "&#0fe30fYES" : "&#d61111NO")));
        animLore.add(Utils.toComponent("&#27B0F5Selected: &f" + (type != null ? type.displayName() : "ROW_SPIN")));
        animLore.add(Utils.toComponent(""));
        animLore.add(Utils.toComponent("&#bfbfbfClick to configure."));
        ItemStack animItem = new ItemStack(animEnabled ? Material.AMETHYST_SHARD : Material.QUARTZ);
        ItemMeta aim = animItem.getItemMeta();
        if (aim != null) {
            aim.displayName(Utils.toComponent("&#0f99e3&lOpening Animations"));
            aim.lore(animLore);
            animItem.setItemMeta(aim);
        }
        inv.setItem(22, animItem);
        ArrayList<Component> keyLore = new ArrayList<>();
        keyLore.add(Utils.toComponent("&#27B0F5Selected: &f" + keyId));
        keyLore.add(Utils.toComponent(""));
        keyLore.add(Utils.toComponent(keyMissing ? "&#d61111Selected key is missing. Click to fix." : "&#bfbfbfClick to change the selected key."));
        ItemStack keyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta kim = keyItem.getItemMeta();
        if (kim != null) {
            kim.displayName(Utils.toComponent("&#0fe30f&lKey"));
            kim.lore(keyLore);
            keyItem.setItemMeta(kim);
        }
        inv.setItem(24, keyItem);
        inv.setItem(27, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to the &#0fe30fCrate Manager&#bfbfbf."));
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta im = i.getItemMeta();
        if (im != null) {
            im.displayName(Utils.toComponent(name));
            if (loreLines.length > 0) {
                ArrayList<Component> lore = new ArrayList<>();
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

