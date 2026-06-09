
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HologramGUI {
    private final DonutCrates plugin;

    public HologramGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory build(String crate) {
        String base = "Crates." + crate + ".Hologram";
        ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
        if (h == null) {
            h = this.plugin.cfg.crates.createSection(base);
        }
        boolean enabled = h.getBoolean("enabled", false);
        double offsetY = h.getDouble("offsetY", 1.5);
        List<String> lines = h.getStringList("lines");
        String templateId = h.getString("template", null);
        boolean shadow = h.getBoolean("shadow", true);
        String bgId = h.getString("bgColor", null);
        boolean legacyTransparent = h.getBoolean("background-transparent", true);
        if (bgId == null) {
            bgId = legacyTransparent ? "TRANSPARENT" : "DARK";
        }
        Object templateLabel = templateId != null && this.plugin.cfg.config.isConfigurationSection("hologram-templates." + templateId) ? templateId : (templateId != null ? templateId + " (missing)" : "Custom");
        EditorHolder holder = new EditorHolder();
        Inventory inv = Bukkit.createInventory(null, 36, Utils.toComponent("&#444444" + crate + " Hologram"));
        holder.setInventory(inv);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Utils.toComponent(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        int lineCount = lines != null ? lines.size() : 0;
        inv.setItem(4, this.item(Material.BOOK, "&#0f99e3Hologram Editor", "&#bfbfbfCrate: &f" + crate, "&#bfbfbfLines: &f" + lineCount, "&#bfbfbfOffsetY: &f" + String.format(Locale.US, "%.2f", offsetY)));
        inv.setItem(10, this.item(enabled ? Material.LIME_DYE : Material.RED_DYE, enabled ? "&#0fe30fHologram: ENABLED" : "&#d61111Hologram: DISABLED", "&#bfbfbfToggle the hologram visibility", "&#bfbfbfabove this crate.", "", "&#bfbfbfCurrent lines: &f" + lineCount));
        inv.setItem(12, this.item(Material.ENDER_EYE, "&#0f99e3Hologram Template", "&#bfbfbfCurrent: &f" + (String)templateLabel, "", "&#bfbfbfClick to choose a hologram layout.", "&#bfbfbfTemplates are configured in &fconfig.yml"));
        inv.setItem(13, this.item(Material.WHITE_DYE, "&#0f99e3Background Color", "&#bfbfbfSelected: &f" + this.prettyBgName(bgId), "", "&#bfbfbfClick to choose background color."));
        inv.setItem(14, this.item(Material.INK_SAC, shadow ? "&#0fe30fText Shadow: ENABLED" : "&#d61111Text Shadow: DISABLED", "&#bfbfbfToggles Minecraft's new", "&#bfbfbftext shadow on the hologram.", "", "&#bfbfbfClick to toggle."));
        inv.setItem(16, this.item(Material.STONE_SLAB, "&#f5f5f5Offset Y -", "&#bfbfbfMove the hologram &f-0.25", "&#bfbfbfblocks &bdown&#bfbfbf.", "", "&#bfbfbfCurrent: &f" + String.format(Locale.US, "%.2f", offsetY)));
        inv.setItem(25, this.item(Material.STONE_SLAB, "&#f5f5f5Offset Y +", "&#bfbfbfMove the hologram &f+0.25", "&#bfbfbfblocks &bup&#bfbfbf.", "", "&#bfbfbfCurrent: &f" + String.format(Locale.US, "%.2f", offsetY)));
        inv.setItem(27, this.item(Material.ARROW, "&#f5f5f5\u00ab Back", "&#bfbfbfReturn to crate settings."));
        return inv;
    }

    private String prettyBgName(String id) {
        if (id == null) {
            return "Transparent";
        }
        return switch (id.toUpperCase()) {
            case "TRANSPARENT" -> "Transparent";
            case "DARK" -> "Dark background";
            case "GRAY" -> "Soft dark gray";
            case "BLUE" -> "Blue accent";
            case "GREEN" -> "Green accent";
            case "RED" -> "Red accent";
            case "GOLD" -> "Gold accent";
            case "AQUA" -> "Aqua accent";
            default -> id.substring(0, 1).toUpperCase() + id.substring(1).toLowerCase();
        };
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
        i.setItemMeta(im);
        return i;
    }
}

