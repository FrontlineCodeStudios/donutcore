
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

public class CrateDisplayNameGUI {
    private final DonutCrates plugin;

    public CrateDisplayNameGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crate) {
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Utils.toComponent("&#444444" + crate + " Display Name"));
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
        String currentRaw = this.plugin.cfg.crates.getString("Crates." + crate + ".displayname", null);
        String preview = currentRaw == null || currentRaw.isBlank() ? Utils.formatColors("&7" + crate) : Utils.formatColors(currentRaw);
        inv.setItem(4, this.item(Material.NAME_TAG, "&#27B0F5&lCrate Display Name", "&#bfbfbfThis is what players see in chat/messages.", "", "&#27B0F5Current: &r" + preview, "&#7f7f7f(raw: " + (currentRaw == null ? "default" : currentRaw) + ")", "", "&#444444&#ffffff&lTip", "&#bfbfbf1) Set text first", "&#bfbfbf2) Then pick a color"));
        inv.setItem(10, this.item(Material.OAK_SIGN, "&#0fe30f&lSet Text", "&#bfbfbfClick to type the name (no color codes)."));
        inv.setItem(12, this.item(Material.LIME_DYE, "&#0f99e3&lPick Color", "&#bfbfbfClick to choose a Minecraft color."));
        inv.setItem(14, this.item(Material.AMETHYST_SHARD, "&#27B0F5&lHex Color", "&#bfbfbfClick to type a hex color"));
        inv.setItem(16, this.item(Material.BARRIER, "&#d61111&lReset to Default", "&#bfbfbfResets display name back to the crate ID.", "", "&#7f7f7f(Default = " + crate + ")"));
        inv.setItem(18, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to crate edit menu."));
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(Utils.toComponent(name));
            ArrayList<Component> lore = new ArrayList<>();
            for (String s : loreLines) {
                lore.add(Utils.toComponent(s));
            }
            im.lore(lore);
            it.setItemMeta(im);
        }
        return it;
    }
}

