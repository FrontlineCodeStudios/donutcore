package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ProgressGui {
    private final DonutSell plugin;
    private String defaultTitle;
    private final Map<String, String> customTitles = new HashMap<>();
    private int size;
    private String loadingColor;
    private String completeLoadingColor;
    private String barSymbol;
    private int barLength;
    private Material incompleteMat;
    private String incompleteName;
    private List<String> incompleteLore;
    private Material workingMat;
    private String workingName;
    private List<String> workingLore;
    private Material completeMat;
    private String completeName;
    private List<String> completeLore;
    private boolean fillerEnabled;
    private Material fillerMat;
    private String fillerName;
    private List<String> fillerLore;
    private boolean backEnabled;
    private int backSlot;
    private Material backMat;
    private String backName;
    private List<String> backLore;
    private final List<Level> levels = new ArrayList<>();
    public final Map<String, CategoryIcon> categoryIcons = new HashMap<>();

    public ProgressGui(DonutSell plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection cfg = this.plugin.getMenusConfig().getConfigurationSection("progress-menu");
        if (cfg == null) return;
        this.defaultTitle = Utils.formatColors(cfg.getString("title", "%item% Progress"));
        this.customTitles.clear();
        ConfigurationSection titles = cfg.getConfigurationSection("titles");
        if (titles != null) {
            for (String cat : titles.getKeys(false)) {
                this.customTitles.put(cat.toLowerCase(), Utils.formatColors(titles.getString(cat)));
            }
        }
        this.size = cfg.getInt("rows", 3) * 9;
        this.loadingColor = Utils.formatColors(cfg.getString("loading-color", "&#20f706"));
        this.completeLoadingColor = Utils.formatColors(cfg.getString("complete-loading-color", this.loadingColor));
        this.barLength = cfg.getInt("bar-length", 10);
        this.barSymbol = cfg.getString("bar-symbol", "━");
        ConfigurationSection inc = cfg.getConfigurationSection("incomplete");
        this.incompleteMat = Material.valueOf(inc.getString("material"));
        this.incompleteName = Utils.formatColors(inc.getString("displayname"));
        this.incompleteLore = Utils.formatColors(inc.getStringList("lore"));
        ConfigurationSection wk = cfg.getConfigurationSection("working");
        this.workingMat = Material.valueOf(wk.getString("material"));
        this.workingName = Utils.formatColors(wk.getString("displayname"));
        this.workingLore = Utils.formatColors(wk.getStringList("lore"));
        ConfigurationSection com = cfg.getConfigurationSection("complete");
        this.completeMat = Material.valueOf(com.getString("material"));
        this.completeName = Utils.formatColors(com.getString("displayname"));
        this.completeLore = Utils.formatColors(com.getStringList("lore"));
        ConfigurationSection fill = cfg.getConfigurationSection("filler");
        this.fillerEnabled = fill.getBoolean("enabled", false);
        this.fillerMat = Material.valueOf(fill.getString("material"));
        this.fillerName = Utils.formatColors(fill.getString("displayname"));
        this.fillerLore = Utils.formatColors(fill.getStringList("lore"));
        ConfigurationSection back = cfg.getConfigurationSection("back-button");
        this.backEnabled = back.getBoolean("enabled", false);
        this.backSlot = back.getInt("slot", -1);
        this.backMat = Material.valueOf(back.getString("material"));
        this.backName = Utils.formatColors(back.getString("displayname"));
        this.backLore = Utils.formatColors(back.getStringList("lore"));
        this.levels.clear();
        ConfigurationSection lvlSec = cfg.getConfigurationSection("levels");
        if (lvlSec != null) {
            for (String key : lvlSec.getKeys(false)) {
                ConfigurationSection ls = lvlSec.getConfigurationSection(key);
                long amount = ls.getLong("amountNeeded");
                double multi = ls.getDouble("multi");
                int slot = ls.getInt("slot");
                this.levels.add(new Level(amount, multi, slot));
            }
        }
        this.levels.sort(Comparator.comparingLong(l -> l.amountNeeded));
        this.categoryIcons.clear();
        ConfigurationSection cats = cfg.getConfigurationSection("categories");
        if (cats != null) {
            for (String cat : cats.getKeys(false)) {
                ConfigurationSection ico = cats.getConfigurationSection(cat + ".icon");
                if (ico == null) continue;
                Material mat = Material.valueOf(ico.getString("material"));
                int slot = ico.getInt("slot");
                String dn = Utils.formatColors(ico.getString("displayname"));
                List<String> lore = Utils.formatColors(ico.getStringList("lore"));
                this.categoryIcons.put(cat.toLowerCase(), new CategoryIcon(mat, slot, dn, lore));
            }
        }
    }

    public Inventory open(Player p, String categoryKey) {
        String key = categoryKey.toLowerCase();
        String raw = this.customTitles.getOrDefault(key, this.defaultTitle);
        String title = raw.replace("%item%", this.capitalize(key));
        Inventory inv = Bukkit.createInventory(new GuiHolder(key, -1), this.size, title);
        double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key);
        int workingIndex = 0;
        while (workingIndex < this.levels.size() && sold >= this.levels.get(workingIndex).amountNeeded) {
            workingIndex++;
        }
        for (int i = 0; i < this.levels.size(); ++i) {
            Level lvl = this.levels.get(i);
            boolean isComplete = i < workingIndex;
            boolean isWorking = i == workingIndex;
            Material mat = isComplete ? this.completeMat : (isWorking ? this.workingMat : this.incompleteMat);
            ItemStack pane = new ItemStack(mat);
            ItemMeta meta = pane.getItemMeta();
            meta.displayName(Utils.toComponent(isComplete ? this.completeName : (isWorking ? this.workingName : this.incompleteName)));
            double progress = Math.min(sold, lvl.amountNeeded);
            double frac = lvl.amountNeeded > 0L ? progress / lvl.amountNeeded : 0.0;
            String needed = Utils.abbreviateNumber(lvl.amountNeeded);
            String done = Utils.abbreviateNumber(progress);
            String amtStr = isComplete ? needed + "/" + needed : done + "/" + needed;
            String bar;
            if (isComplete) {
                bar = this.completeLoadingColor + this.barSymbol.repeat(this.barLength);
            } else if (isWorking) {
                int filled = (int) Math.round(frac * this.barLength);
                bar = this.loadingColor + this.barSymbol.repeat(filled) + "&f" + this.barSymbol.repeat(this.barLength - filled);
            } else {
                bar = "&f" + this.barSymbol.repeat(this.barLength);
            }
            List<String> template = isComplete ? this.completeLore : (isWorking ? this.workingLore : this.incompleteLore);
            ArrayList<Component> finalLore = new ArrayList<>();
            for (String line : template) {
                finalLore.add(Utils.toComponent(line
                        .replace("%loading-bar%", bar)
                        .replace("%multi%", String.valueOf(lvl.multi))
                        .replace("%progress%", String.format("%.1f", frac * 100.0))
                        .replace("%amount-needed%", amtStr)));
            }
            meta.lore(finalLore);
            pane.setItemMeta(meta);
            inv.setItem(lvl.slot, pane);
        }
        CategoryIcon ci = this.categoryIcons.get(key);
        if (ci != null) {
            ItemStack icon = new ItemStack(ci.material);
            ItemMeta im = icon.getItemMeta();
            im.displayName(Utils.toComponent(ci.displayName.replace("%item%", this.capitalize(key)).replace("%sold%", Utils.abbreviateNumber(sold))));
            ArrayList<Component> lore = new ArrayList<>();
            for (String l : ci.lore) {
                lore.add(Utils.toComponent(l.replace("%item%", this.capitalize(key)).replace("%sold%", Utils.abbreviateNumber(sold))));
            }
            im.lore(lore);
            icon.setItemMeta(im);
            inv.setItem(ci.slot, icon);
        }
        if (this.fillerEnabled) {
            ItemStack f = new ItemStack(this.fillerMat);
            ItemMeta fm = f.getItemMeta();
            fm.displayName(Utils.toComponent(this.fillerName));
            fm.lore(Utils.toComponents(this.fillerLore));
            f.setItemMeta(fm);
            for (int i = 0; i < this.size; ++i) {
                if (inv.getItem(i) == null) inv.setItem(i, f.clone());
            }
        }
        if (this.backEnabled && this.backSlot >= 0 && this.backSlot < this.size) {
            ItemStack b = new ItemStack(this.backMat);
            ItemMeta bm = b.getItemMeta();
            bm.displayName(Utils.toComponent(this.backName));
            bm.lore(Utils.toComponents(this.backLore));
            b.setItemMeta(bm);
            inv.setItem(this.backSlot, b);
        }
        p.openInventory(inv);
        return inv;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static class Level {
        final long amountNeeded;
        final double multi;
        final int slot;

        Level(long a, double m, int s) {
            this.amountNeeded = a;
            this.multi = m;
            this.slot = s;
        }
    }

    public static class CategoryIcon {
        final Material material;
        final int slot;
        final String displayName;
        final List<String> lore;

        CategoryIcon(Material m, int s, String dn, List<String> l) {
            this.material = m;
            this.slot = s;
            this.displayName = dn;
            this.lore = l;
        }
    }
}
