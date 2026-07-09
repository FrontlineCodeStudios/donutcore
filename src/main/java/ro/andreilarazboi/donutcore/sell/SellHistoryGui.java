package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public class SellHistoryGui {
    private final DonutSell plugin;
    private int rows;
    private int size;
    private String titleTemplate;
    private String titlePrefix;
    private String itemNameTpl;
    private List<String> loreTpl;
    public int backSlot;
    private String backName;
    private Material backMat;
    private List<String> backLore;
    public int nextSlot;
    private String nextName;
    private Material nextMat;
    private List<String> nextLore;
    public int sortSlot;
    private String sortName;
    private Material sortMat;
    private String notCurrentColor;
    private String currentColor;
    private List<String> sortLoreBase;
    public int refreshSlot;
    private String refreshName;
    private Material refreshMat;
    private List<String> refreshLore;
    public int playerSlot;
    private String playerNameTpl;
    private Material playerMat;
    private List<String> playerLoreTpl;

    public SellHistoryGui(DonutSell plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection cfg = this.plugin.getMenusConfig().getConfigurationSection("sellhistory-menu");
        this.rows = cfg.getInt("rows", 6);
        this.size = this.rows * 9;
        String rawTitle = cfg.getString("title", "&8Sell history (Page %history-page%)");
        this.titleTemplate = Utils.formatColors(rawTitle);
        int idx = rawTitle.indexOf("%history-page%");
        if (idx < 0) {
            idx = rawTitle.length();
        }
        this.titlePrefix = ChatColor.stripColor((String)Utils.formatColors(rawTitle.substring(0, idx)));
        ConfigurationSection sold = cfg.getConfigurationSection("sold-items");
        this.itemNameTpl = Utils.formatColors(sold.getString("displayname", "%Item-Name%"));
        this.loreTpl = Utils.formatColors(sold.getStringList("lore"));
        ConfigurationSection prev = cfg.getConfigurationSection("history-back");
        this.backSlot = prev.getInt("slot", 45);
        this.backName = Utils.formatColors(prev.getString("displayname", "&fBack"));
        this.backMat = Material.valueOf((String)prev.getString("material", "ARROW").toUpperCase());
        this.backLore = Utils.formatColors(prev.getStringList("lore"));
        ConfigurationSection nxt = cfg.getConfigurationSection("history-next");
        this.nextSlot = nxt.getInt("slot", 53);
        this.nextName = Utils.formatColors(nxt.getString("displayname", "&fNext"));
        this.nextMat = Material.valueOf((String)nxt.getString("material", "ARROW").toUpperCase());
        this.nextLore = Utils.formatColors(nxt.getStringList("lore"));
        ConfigurationSection sort = cfg.getConfigurationSection("history-sort");
        this.sortSlot = sort.getInt("slot", 50);
        this.sortMat = Material.valueOf((String)sort.getString("material", "HOPPER").toUpperCase());
        this.sortName = Utils.formatColors(sort.getString("displayname", "&aSort"));
        this.notCurrentColor = sort.getString("NotCurrentColor", "&f");
        this.currentColor = sort.getString("CurrentColor", "&a");
        this.sortLoreBase = sort.getStringList("lore");
        ConfigurationSection ref = cfg.getConfigurationSection("history-refresh");
        this.refreshSlot = ref.getInt("slot", 49);
        this.refreshName = Utils.formatColors(ref.getString("displayname", "&aRefresh"));
        this.refreshMat = Material.valueOf((String)ref.getString("material", "ANVIL").toUpperCase());
        this.refreshLore = Utils.formatColors(ref.getStringList("lore"));
        ConfigurationSection pp = cfg.getConfigurationSection("history-player");
        this.playerSlot = pp.getInt("slot", 48);
        this.playerMat = Material.valueOf((String)pp.getString("material", "PLAYER_HEAD").toUpperCase());
        this.playerNameTpl = pp.getString("displayname", "%player%");
        this.playerLoreTpl = pp.getStringList("lore");
    }

    public void open(Player p, int page) {
        HistoryTracker tracker = this.plugin.getHistoryTracker();
        HistoryTracker.SortOrder order = tracker.getOrder(p.getUniqueId());
        Map<String, DonutSell.Stats> history = this.plugin.getHistory(p.getUniqueId());
        ArrayList<Map.Entry<String, DonutSell.Stats>> all = new ArrayList<Map.Entry<String, DonutSell.Stats>>(history.entrySet());
        switch (order) {
            case HIGH: {
                all.sort((a, b) -> Double.compare(b.getValue().revenue, a.getValue().revenue));
                break;
            }
            case LOW: {
                all.sort((a, b) -> Double.compare(a.getValue().revenue, b.getValue().revenue));
                break;
            }
            case NAME: {
                all.sort((a, b) -> a.getKey().compareTo(b.getKey()));
                break;
            }
        }
        int perPage = (this.rows - 1) * 9;
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, all.size());
        Inventory inv = Bukkit.createInventory(null, (int)this.size, (String)this.titleTemplate.replace("%history-page%", String.valueOf(page)));
        int slotIndex = 0;
        for (int i = from; i < to; i++) {
            Map.Entry<String, DonutSell.Stats> e = all.get(i);
            String key = e.getKey();
            DonutSell.Stats st = e.getValue();
            Material mat;
            try {
                mat = Material.valueOf(key.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                mat = Material.STONE;
            }
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                m.setDisplayName(this.itemNameTpl.replace("%Item-Name%", this.capitalize(key)));
                ArrayList<String> lore = new ArrayList<>();
                for (String line : this.loreTpl) {
                    lore.add(line.replace("%amount-sold%", String.valueOf((long)st.count)).replace("%price-sold%", Utils.abbreviateNumber(st.revenue)));
                }
                m.setLore(lore);
                it.setItemMeta(m);
            }
            inv.setItem(slotIndex, it);
            slotIndex++;
        }
        ItemStack back = new ItemStack(this.backMat);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(this.backName);
        bm.setLore(this.backLore);
        back.setItemMeta(bm);
        inv.setItem(this.backSlot, back);
        ItemStack refresh = new ItemStack(this.refreshMat);
        ItemMeta rm = refresh.getItemMeta();
        rm.setDisplayName(this.refreshName);
        rm.setLore(this.refreshLore);
        refresh.setItemMeta(rm);
        inv.setItem(this.refreshSlot, refresh);
        ItemStack sortBtn = new ItemStack(this.sortMat);
        ItemMeta sm = sortBtn.getItemMeta();
        sm.setDisplayName(this.sortName);
        ArrayList<String> slore = new ArrayList<String>();
        for (String line2 : this.sortLoreBase) {
            String trimmed = line2.trim();
            String prefix = order == HistoryTracker.SortOrder.HIGH && trimmed.toLowerCase().contains("highest") ? this.currentColor : (order == HistoryTracker.SortOrder.LOW && trimmed.toLowerCase().contains("lowest") ? this.currentColor : (order == HistoryTracker.SortOrder.NAME && trimmed.toLowerCase().contains("name") ? this.currentColor : this.notCurrentColor));
            slore.add(Utils.formatColors(prefix + trimmed));
        }
        sm.setLore(slore);
        sortBtn.setItemMeta(sm);
        inv.setItem(this.sortSlot, sortBtn);
        ItemStack head = new ItemStack(this.playerMat);
        SkullMeta sk = (SkullMeta)head.getItemMeta();
        sk.setOwningPlayer((OfflinePlayer)p);
        String display = Utils.formatColors(this.playerNameTpl).replace("%player%", p.getName());
        display = PlaceholderAPI.setPlaceholders((Player)p, (String)display);
        sk.setDisplayName(display);
        List plore = this.playerLoreTpl.stream().map(line -> Utils.formatColors(line)).map(line -> line.replace("%player%", p.getName())).map(line -> PlaceholderAPI.setPlaceholders((Player)p, (String)line)).collect(Collectors.toList());
        sk.setLore(plore);
        head.setItemMeta((ItemMeta)sk);
        inv.setItem(this.playerSlot, head);
        ItemStack next = new ItemStack(this.nextMat);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(this.nextName);
        nm.setLore(this.nextLore);
        next.setItemMeta(nm);
        inv.setItem(this.nextSlot, next);
        p.openInventory(inv);
        this.plugin.getHistoryTracker().setPage(p.getUniqueId(), page);
    }

    public boolean matchesTitle(String openTitle) {
        String plain = ChatColor.stripColor((String)openTitle);
        return plain.startsWith(this.titlePrefix);
    }

    public int getRows() {
        return this.rows;
    }

    public int getBackSlot() {
        return this.backSlot;
    }

    public int getNextSlot() {
        return this.nextSlot;
    }

    public int getSortSlot() {
        return this.sortSlot;
    }

    public int getRefreshSlot() {
        return this.refreshSlot;
    }

    public int getPlayerSlot() {
        return this.playerSlot;
    }

    private String capitalize(String key) {
        CharSequence[] parts = key.split("_");
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = ((String)parts[i]).substring(0, 1).toUpperCase() + ((String)parts[i]).substring(1).toLowerCase();
        }
        return String.join((CharSequence)" ", parts);
    }
}

