package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SellGui {
    private final DonutSell plugin;
    private String title;
    private int rows;
    private int size;
    private Sound closeSound;
    private final boolean useNewMenuFlag;

    public SellGui(DonutSell plugin) {
        this.plugin = plugin;
        this.useNewMenuFlag = plugin.getConfig().getBoolean("use-new-sell-menu", false);
        this.loadConfig();
    }

    private void loadSection(String section) {
        ConfigurationSection cfg = this.plugin.getMenusConfig().getConfigurationSection(section);
        if (cfg == null) throw new IllegalStateException("Missing section '" + section + "' in menus.yml");
        this.title = Utils.formatColors(cfg.getString("title", "&aSell Items"));
        this.rows = cfg.getInt("rows", 5);
        this.size = this.rows * 9;
        this.closeSound = null;
        ConfigurationSection sounds = cfg.getConfigurationSection("sounds");
        if (sounds != null) {
            this.closeSound = Utils.resolveSound(sounds.getString("close-sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        }
    }

    public void loadConfig() {
        this.loadSection("sell-menu");
    }

    public Inventory open(Player p) {
        Inventory inv = Bukkit.createInventory(null, this.size, Utils.toComponent(this.title));
        List<LevelData> lvlList = this.loadLevels();
        if (!this.useNewMenuFlag && this.plugin.isUseMultipliers()) {
            this.populateBottomRow("sell-menu", inv, lvlList, p);
        }
        p.openInventory(inv);
        return inv;
    }

    public Inventory openNew(Player p) {
        this.loadSection("new-sell-menu");
        Inventory inv = Bukkit.createInventory(null, this.size, Utils.toComponent(this.title));
        List<LevelData> lvlList = this.loadLevels();
        if (this.plugin.isUseMultipliers()) {
            this.populateBottomRow("new-sell-menu", inv, lvlList, p);
        }
        p.openInventory(inv);
        this.loadConfig();
        return inv;
    }

    private List<LevelData> loadLevels() {
        ArrayList<LevelData> lvlList = new ArrayList<>();
        ConfigurationSection lvlSec = this.plugin.getMenusConfig().getConfigurationSection("progress-menu.levels");
        if (lvlSec != null) {
            for (String key : lvlSec.getKeys(false)) {
                ConfigurationSection ls = lvlSec.getConfigurationSection(key);
                if (ls == null) continue;
                lvlList.add(new LevelData(ls.getLong("amountNeeded", 0L), ls.getDouble("multi", 1.0)));
            }
            lvlList.sort(Comparator.comparingLong(ld -> ld.amountNeeded));
        }
        return lvlList;
    }

    private void populateBottomRow(String section, Inventory inv, List<LevelData> lvlList, Player p) {
        if (!this.plugin.isUseMultipliers()) return;
        List<String> items = this.plugin.getMenusConfig().getStringList(section + ".items");
        ConfigurationSection settings = this.plugin.getMenusConfig().getConfigurationSection(section + ".item-settings");
        int defaultStart = (this.rows - 1) * 9;
        for (int i = 0; i < items.size(); ++i) {
            String catKey = items.get(i);
            ConfigurationSection is = settings != null ? settings.getConfigurationSection(catKey) : null;
            int slot = defaultStart + i;
            if (is != null && is.isInt("slot")) slot = is.getInt("slot");
            String matName = is != null && is.getString("material") != null ? is.getString("material") : catKey;
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ItemStack button = new ItemStack(mat, 1);
            ItemMeta meta = button.getItemMeta();
            if (meta == null) continue;
            LevelData next = this.findNextLevel(lvlList, p, catKey);
            double pct = this.computePct(next, p, catKey);
            String bar = this.buildBar(pct);
            if (is != null) {
                meta.displayName(Utils.toComponent(is.getString("displayname", catKey)));
                ArrayList<Component> lore = new ArrayList<>();
                for (String line : is.getStringList("lore")) {
                    lore.add(Utils.toComponent(line
                            .replace("%next-multi%", String.format("%.1f", next.multi))
                            .replace("%progress%", String.format("%.1f", pct))
                            .replace("%progress-bar%", bar)));
                }
                meta.lore(lore);
            } else {
                meta.displayName(Utils.toComponent("&e" + catKey.toLowerCase(Locale.ROOT)));
            }
            button.setItemMeta(meta);
            inv.setItem(slot, button);
        }
    }

    private LevelData findNextLevel(List<LevelData> lvlList, Player p, String key) {
        double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
        for (LevelData ld : lvlList) {
            if (sold < ld.amountNeeded) return ld;
        }
        return lvlList.isEmpty() ? new LevelData(1L, 1.0) : lvlList.get(lvlList.size() - 1);
    }

    private double computePct(LevelData ld, Player p, String key) {
        double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
        return ld.amountNeeded > 0L ? Math.min(sold / ld.amountNeeded * 100.0, 100.0) : 100.0;
    }

    private String buildBar(double pct) {
        ConfigurationSection cfg = this.plugin.getMenusConfig().getConfigurationSection("sell-menu");
        int barLength = cfg != null ? cfg.getInt("bar-length", 10) : 10;
        String barSymbol = cfg != null ? Utils.formatColors(cfg.getString("bar-symbol", "#")) : "#";
        String loadingColor = cfg != null ? Utils.formatColors(cfg.getString("loading-color", "")) : "";
        String completeColor = cfg != null ? Utils.formatColors(cfg.getString("complete-loading-color", loadingColor)) : loadingColor;
        int filled = (int) Math.round(barLength * (pct / 100.0));
        if (pct >= 100.0) {
            return completeColor + barSymbol.repeat(barLength);
        }
        return loadingColor + barSymbol.repeat(filled) + "&f" + barSymbol.repeat(barLength - filled);
    }

    public void handleClose(Player p) {
        if (this.closeSound != null) {
            p.playSound(p.getLocation(), this.closeSound, 1.0f, 1.0f);
        }
    }

    public boolean matchesTitle(String openTitle) {
        return Utils.stripColor(openTitle).equals(Utils.stripColor(this.title));
    }

    public int getSize() {
        return this.size;
    }

    private static class LevelData {
        final long amountNeeded;
        final double multi;

        LevelData(long a, double m) {
            this.amountNeeded = a;
            this.multi = m;
        }
    }
}
