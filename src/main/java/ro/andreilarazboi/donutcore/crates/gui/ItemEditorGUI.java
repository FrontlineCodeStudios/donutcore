
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemEditorGUI {
    private final DonutCrates plugin;

    public ItemEditorGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crate, String key) {
        String base = "Crates." + crate + ".Items." + key;
        ConfigurationSection sec = this.plugin.cfg.crates.getConfigurationSection(base);
        if (sec == null) {
            sec = this.plugin.cfg.crates.createSection(base);
        }
        boolean randomEnabled = this.plugin.cfg.crates.getBoolean("Crates." + crate + ".random.enabled", false);
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Utils.toComponent("&#444444" + crate + " Item Editor"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        ItemStack preview = this.plugin.guiItemUtil.buildItemFromSection(sec);
        inv.setItem(11, preview);
        if (randomEnabled) {
            double chance = sec.getDouble("chance", 0.0);
            Object chanceText = chance == 0.0 ? "AUTO" : chance + "%";
            ItemStack chanceItem = new ItemStack(Material.CLOCK);
            ItemMeta cim = chanceItem.getItemMeta();
            cim.displayName(Utils.toComponent("&#f5f5f5Edit Chance"));
            ArrayList<Component> clore = new ArrayList<Component>();
            clore.add(Utils.toComponent("&#bfbfbfCurrent: &f" + (String)chanceText));
            clore.add(Utils.toComponent(""));
            clore.add(Utils.toComponent("&#bfbfbfLeft-click: &fSet custom chance"));
            clore.add(Utils.toComponent("&#bfbfbfRight-click: &fSet AUTO mode"));
            cim.lore(clore);
            chanceItem.setItemMeta(cim);
            inv.setItem(4, chanceItem);
        }
        inv.setItem(13, this.item(Material.NAME_TAG, "&#f5f5f5Edit Display Name", "&#bfbfbfClick, then type a new name in chat."));
        inv.setItem(14, this.item(Material.WRITABLE_BOOK, "&#f5f5f5Edit Lore", "&#bfbfbfClick to add a lore line in chat.", "&#bfbfbfShift-right-click to clear all lore."));
        boolean giveItem = sec.getBoolean("giveitem", true);
        inv.setItem(15, this.item(giveItem ? Material.LIME_DYE : Material.RED_DYE, giveItem ? "&#f5f5f5Give Item: &#0fe30fENABLED" : "&#f5f5f5Give Item: &#d61111DISABLED", "&#bfbfbfIf disabled, only the command will run."));
        if (!giveItem) {
            String cmd = sec.getString("command", "");
            inv.setItem(16, this.item(Material.COMMAND_BLOCK, "&#f5f5f5Edit Command", new String[]{cmd.isEmpty() ? "&#bfbfbfClick, then type a command in chat." : "&#bfbfbfCurrent: &f" + cmd, "&#bfbfbfUse &f%player% &#bfbfbfto insert player name."}));
        }
        if (randomEnabled) {
            boolean broadcast = sec.getBoolean("broadcast", false);
            inv.setItem(22, this.item(broadcast ? Material.LIME_DYE : Material.RED_DYE, broadcast ? "&#f5f5f5Broadcast: &#0fe30fENABLED" : "&#f5f5f5Broadcast: &#d61111DISABLED", "&#bfbfbfIf enabled, use &fconfig.yml", "&#bfbfbfsection &f'broadcast-message' &#bfbfbffor the format."));
        }
        inv.setItem(18, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to the &#0fe30fRewards&#bfbfbf list."));
        inv.setItem(26, this.item(Material.BARRIER, "&#d61111Delete Reward", "&#bfbfbfClick to delete this reward."));
        return inv;
    }

    private ItemStack item(Material mat, String name, String ... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta im = i.getItemMeta();
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
        return i;
    }
}

