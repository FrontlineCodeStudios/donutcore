package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

@SuppressWarnings({"deprecation", "removal"})
public class SellGui {
    private final DonutSell plugin;
    private String title;
    private int rows;
    private int size;
    private String actionbarFmt;
    private String chatFmt;
    private int barLength;
    private String barSymbol;
    private String loadingColor;
    private String completeLoadingColor;
    private Sound closeSound;

    public SellGui(DonutSell plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    private void loadSection(String section) {
        ConfigurationSection cfg = this.plugin.getMenusConfig().getConfigurationSection(section);
        if (cfg == null) {
            throw new IllegalStateException("Missing section '" + section + "' in config.yml");
        }
        this.title = Utils.formatColors(cfg.getString("title", "&aSell Items"));
        this.rows = cfg.getInt("rows", 5);
        this.size = this.rows * 9;
        this.actionbarFmt = Utils.formatColors(cfg.getString("actionbar-message", ""));
        this.chatFmt = Utils.formatColors(cfg.getString("chat-message", ""));
        this.barLength = cfg.getInt("bar-length", 10);
        this.barSymbol = Utils.formatColors(cfg.getString("bar-symbol", "#"));
        this.loadingColor = Utils.formatColors(cfg.getString("loading-color", ""));
        this.completeLoadingColor = Utils.formatColors(cfg.getString("complete-loading-color", this.loadingColor));
        this.closeSound = null;
        ConfigurationSection sounds = cfg.getConfigurationSection("sounds");
        if (sounds != null) {
            try {
                this.closeSound = Sound.valueOf((String)sounds.getString("close-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Invalid close-sound in " + section + ": " + sounds.getString("close-sound"));
            }
        }
    }

    public void loadConfig() {
        this.loadSection("sell-menu");
    }

    public Inventory open(Player p) {
        if (this.plugin.isActionSellMenuEnabled()) {
            return this.openActionSell(p);
        }
        Inventory inv = Bukkit.createInventory((InventoryHolder)null, (int)this.size, (String)this.title);
        List<LevelData> lvlList = this.loadLevels();
        if (!this.plugin.isSellMultiMenuEnabled() && this.plugin.isUseMultipliers()) {
            this.populateBottomRow("sell-menu", inv, lvlList, p);
        }
        p.openInventory(inv);
        return inv;
    }

    public Inventory openActionSell(Player p) {
        this.loadSection("action-sell-menu");
        Inventory inv = Bukkit.createInventory((InventoryHolder)null, (int)this.size, (String)this.title);
        this.refreshActionSellButton(p, inv);
        p.openInventory(inv);
        this.loadConfig();
        return inv;
    }

    public void refreshActionSellButton(Player p, Inventory inv) {
        if (inv == null) {
            return;
        }
        ConfigurationSection buttonCfg = this.plugin.getMenusConfig().getConfigurationSection("action-sell-menu.sell-button");
        int slot = this.plugin.getActionSellButtonSlot();
        Material mat = Material.EMERALD;
        if (buttonCfg != null) {
            try {
                mat = Material.valueOf((String)buttonCfg.getString("material", "EMERALD").toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {
                mat = Material.EMERALD;
            }
        }
        DonutSell.SellQuote quote = this.plugin.quoteSellInventory(p, inv, slot);
        ItemStack button = new ItemStack(mat, 1);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin.getPlugin(), "no_worth_lore"), PersistentDataType.BYTE, (byte) 1);
            String name = buttonCfg != null ? buttonCfg.getString("displayname", "&8~&a$ &f{amount}") : "&8~&a$ &f{amount}";
            meta.setDisplayName(Utils.formatColors(name.replace("{amount}", Utils.abbreviateNumber(quote.payout()))));
            List<String> loreTemplate = buttonCfg != null ? buttonCfg.getStringList("lore") : Collections.singletonList("&7Click to sell items");
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : loreTemplate) {
                lore.add(Utils.formatColors(line.replace("{amount}", Utils.abbreviateNumber(quote.payout()))));
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        inv.setItem(slot, button);
    }

    public Inventory openNew(Player p) {
        this.loadSection("new-sell-menu");
        Inventory inv = Bukkit.createInventory((InventoryHolder)null, (int)this.size, (String)this.title);
        List<LevelData> lvlList = this.loadLevels();
        if (this.plugin.isUseMultipliers()) {
            this.populateBottomRow("new-sell-menu", inv, lvlList, p);
        }
        p.openInventory(inv);
        this.loadConfig();
        return inv;
    }

    private List<LevelData> loadLevels() {
        ArrayList<LevelData> lvlList = new ArrayList<LevelData>();
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
        if (!this.plugin.isUseMultipliers()) {
            return;
        }
        List items = this.plugin.getMenusConfig().getStringList(section + ".items");
        ConfigurationSection settings = this.plugin.getMenusConfig().getConfigurationSection(section + ".item-settings");
        int defaultStart = (this.rows - 1) * 9;
        for (int i = 0; i < items.size(); ++i) {
            Material mat;
            String catKey = (String)items.get(i);
            ConfigurationSection is = settings != null ? settings.getConfigurationSection(catKey) : null;
            int slot = defaultStart + i;
            if (is != null && is.isInt("slot")) {
                slot = is.getInt("slot");
            }
            String matName = is != null && is.getString("material") != null ? is.getString("material") : catKey;
            try {
                mat = Material.valueOf((String)matName.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ex) {
                continue;
            }
            ItemStack button = new ItemStack(mat, 1);
            ItemMeta meta = button.getItemMeta();
            if (meta == null) continue;
            LevelData next = this.findNextLevel(lvlList, p, catKey);
            double pct = this.computePct(next, p, catKey);
            String bar = this.buildBar(pct);
            if (is != null) {
                meta.setDisplayName(Utils.formatColors(is.getString("displayname", catKey)));
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : is.getStringList("lore")) {
                    lore.add(Utils.formatColors(line.replace("%next-multi%", String.format("%.1f", next.multi)).replace("%progress%", String.format("%.1f", pct)).replace("%progress-bar%", bar)));
                }
                meta.setLore(lore);
            } else {
                meta.setDisplayName(Utils.formatColors("&e" + catKey.toLowerCase(Locale.ROOT)));
            }
            button.setItemMeta(meta);
            inv.setItem(slot, button);
        }
    }

    private LevelData findNextLevel(List<LevelData> lvlList, Player p, String key) {
        double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
        for (LevelData ld : lvlList) {
            if (!(sold < (double)ld.amountNeeded)) continue;
            return ld;
        }
        return lvlList.isEmpty() ? new LevelData(1L, 1.0) : lvlList.get(lvlList.size() - 1);
    }

    private double computePct(LevelData ld, Player p, String key) {
        double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
        return ld.amountNeeded > 0L ? Math.min(sold / (double)ld.amountNeeded * 100.0, 100.0) : 100.0;
    }

    private String buildBar(double pct) {
        int i;
        int filled = (int)Math.round((double)this.barLength * (pct / 100.0));
        StringBuilder b = new StringBuilder();
        b.append(this.loadingColor);
        for (i = 0; i < filled; ++i) {
            b.append(this.barSymbol);
        }
        b.append("&f");
        for (i = filled; i < this.barLength; ++i) {
            b.append(this.barSymbol);
        }
        if (pct >= 100.0) {
            b = new StringBuilder(this.completeLoadingColor);
            for (i = 0; i < this.barLength; ++i) {
                b.append(this.barSymbol);
            }
        }
        return b.toString();
    }

    public void handleClose(Player p) {
        if (this.closeSound != null) {
            p.playSound(p.getLocation(), this.closeSound, 1.0f, 1.0f);
        }
    }

    public boolean matchesTitle(String openTitle) {
        return ChatColor.stripColor((String)openTitle).equals(ChatColor.stripColor((String)this.title));
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

