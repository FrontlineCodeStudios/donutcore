package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings({ "deprecation", "unchecked", "null", "removal" })
public class AdminPriceEditorMenu
        implements Listener {
    private static final String FILTER_ALL = "ALL";
    private final DonutSell plugin;
    private final String editorTitlePrefix;
    private final String categoryTitle;
    private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
    private static final Pattern POTION_PATTERN = Pattern
            .compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)$");
    private final Map<UUID, Integer> pageByPlayer = new HashMap<UUID, Integer>();
    private final Map<UUID, SortMode> sortByPlayer = new HashMap<UUID, SortMode>();
    private final Map<UUID, String> filterByPlayer = new HashMap<UUID, String>();
    private final Map<UUID, PendingChat> awaitingChat = new ConcurrentHashMap<UUID, PendingChat>();
    private final Map<UUID, PendingCategory> awaitingCategory = new ConcurrentHashMap<UUID, PendingCategory>();
    private final Map<String, List<EntryData>> viewCache = new ConcurrentHashMap<String, List<EntryData>>();
    private final List<EntryData> baseEntries = new ArrayList<EntryData>();
    private volatile boolean loaded = false;
    private final Set<String> recentClicks = ConcurrentHashMap.newKeySet();

    public AdminPriceEditorMenu(DonutSell plugin) {
        this.plugin = plugin;
        this.editorTitlePrefix = Utils.formatColors("&#444444Price Editor (Page ");
        this.categoryTitle = Utils.formatColors("&#444444Select Category");
        this.reloadData();
    }

    public void reloadData() {
        this.loaded = false;
        this.plugin.runAsync(() -> {
            ArrayList<EntryData> computed = new ArrayList<EntryData>();
            for (Material material : Material.values()) {
                if (!material.isItem() || material == Material.AIR)
                    continue;
                String key = material.name().toLowerCase(Locale.ROOT) + "-value";
                boolean listed = this.plugin.getItemValues().containsKey(key);
                double price = listed ? this.plugin.getItemValues().get(key).doubleValue()
                        : this.plugin.getConfig().getDouble("default-value", 0.1);
                computed.add(new EntryData(material, key, this.prettify(material), listed, price));
            }
            List<EntryData> list = this.baseEntries;
            synchronized (list) {
                this.baseEntries.clear();
                this.baseEntries.addAll(computed);
            }
            this.viewCache.clear();
            this.loaded = true;
        });
    }

    public boolean isAwaitingPriceInput(UUID uuid) {
        return this.awaitingChat.containsKey(uuid);
    }

    public void open(Player player, int requestedPage) {
        if (!this.loaded) {
            player.sendMessage(Utils.formatColors("&ePrice editor is still loading. Please try again in a second."));
            return;
        }
        UUID uuid = player.getUniqueId();
        SortMode sort = this.sortByPlayer.getOrDefault(uuid, SortMode.HIGH_TO_LOW);
        String filter = this.filterByPlayer.getOrDefault(uuid, FILTER_ALL);
        this.plugin.runAsync(() -> {
            List<EntryData> snapshot = this.getSortedFilteredView(sort, filter);
            int perPage = 45;
            int maxPage = Math.max(1, (int) Math.ceil((double) snapshot.size() / (double) perPage));
            int page = Math.min(maxPage, Math.max(1, requestedPage));
            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, snapshot.size());
            List<EntryData> pageEntries = snapshot.subList(start, end);
            this.plugin.runAtPlayer(player, () -> {
                Inventory inv = Bukkit.createInventory(null, (int) 54,
                        (String) Utils.formatColors("&#444444Price Editor (Page " + page + ")"));
                for (int i = 0; i < pageEntries.size(); ++i) {
                    EntryData data = (EntryData) pageEntries.get(i);
                    ItemStack item = new ItemStack(data.material());
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(Utils.formatColors("&f" + data.display()));
                        ArrayList<String> lore = new ArrayList<String>();
                        lore.add(Utils.formatColors("&7Key: &f" + data.key()));
                        lore.add(Utils.formatColors("&7Price: &#34ee80$" + Utils.abbreviateNumber(data.price())));
                        lore.add(Utils.formatColors("&fClick to edit"));
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    inv.setItem(i, item);
                }
                inv.setItem(45, this.button(Material.ARROW, "&#34ee80\u0299\u1d00\u1d04\u1d0b",
                        Collections.singletonList("&fClick to go back")));
                inv.setItem(53, this.button(Material.ARROW, "&#34ee80\u0274\u1d07\u0445\u1d1b",
                        Collections.singletonList("&fClick to go forward")));
                inv.setItem(48, this.filterButton(filter));
                inv.setItem(49, this.button(Material.ANVIL, "&#34ee80\u0280\u1d07\ua730\u0280\u1d07\u0455\u029c",
                        Collections.singletonList("&fClick to refresh")));
                inv.setItem(50, this.sortButton(sort));
                player.openInventory(inv);
                this.pageByPlayer.put(uuid, page);
                this.playClick(player);
            });
        });
    }

    public void handlePriceChat(Player player, String message) {
        double price;
        UUID uuid = player.getUniqueId();
        PendingChat pending = this.awaitingChat.get(uuid);
        if (pending == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            this.awaitingChat.remove(uuid);
            this.open(player, 1);
            return;
        }
        try {
            price = Utils.parseShorthandNumber(message);
            if (price < 0.0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            player.sendMessage(Utils.formatColors(
                    "&cPlease type a valid number or 'cancel'. Supported formats: 30, 30k, 1.5m, 30b, 30t, 30q"));
            return;
        }
        this.awaitingChat.remove(uuid);
        this.awaitingCategory.put(uuid,
                new PendingCategory(pending.key(), pending.display(), pending.material(), price));
        this.openCategoryMenu(player);
    }

    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        String title = event.getView().getTitle();
        if (title == null) {
            return;
        }
        if (title.startsWith(this.editorTitlePrefix)) {
            event.setCancelled(true);
            this.handleEditorClick(player, event.getRawSlot(), event.getView().getTopInventory());
            return;
        }
        if (title.equals(this.categoryTitle)) {
            event.setCancelled(true);
            this.handleCategoryClick(player, event.getRawSlot(), event.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid;
        if (!this.plugin.isModuleActive()) return;
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        String title = event.getView().getTitle();
        if (title == null) {
            return;
        }
        if (title.startsWith(this.editorTitlePrefix)) {
            this.pageByPlayer.put(player.getUniqueId(), 1);
        }
        if (title.equals(this.categoryTitle) && this.awaitingCategory.containsKey(uuid = player.getUniqueId())) {
            this.awaitingCategory.remove(uuid);
        }
    }

    private void handleEditorClick(Player player, int slot, Inventory top) {
        UUID uuid = player.getUniqueId();
        int page = this.pageByPlayer.getOrDefault(uuid, 1);
        if (slot == 45) {
            this.open(player, page - 1);
            return;
        }
        if (slot == 53) {
            this.open(player, page + 1);
            return;
        }
        if (slot == 48) {
            this.cycleFilter(uuid);
            this.open(player, 1);
            return;
        }
        if (slot == 49) {
            this.open(player, 1);
            return;
        }
        if (slot == 50) {
            this.cycleSort(uuid);
            this.open(player, 1);
            return;
        }
        if (slot >= 0 && slot < 45) {
            ItemStack clicked = top.getItem(slot);
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }
            String key = clicked.getType().name().toLowerCase(Locale.ROOT) + "-value";
            String token = String.valueOf(uuid) + ":" + key;
            if (!this.recentClicks.add(token)) {
                return;
            }
            this.plugin.runLaterGlobal(() -> this.recentClicks.remove(token), 2L);
            this.awaitingChat.put(uuid, new PendingChat(key, this.prettify(clicked.getType()), clicked.getType()));
            player.closeInventory();
            player.sendMessage(Utils.formatColors(
                    "&c&l(!) &r&cType a new price for &f" + key + "&r&c in chat. Type 'cancel' to abort."));
        }
    }

    private void openCategoryMenu(Player player) {
        UUID uuid = player.getUniqueId();
        PendingCategory pending = this.awaitingCategory.get(uuid);
        if (pending == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, (int) 27, (String) this.categoryTitle);
        List<String> categories = this.plugin.getMenusConfig().getStringList("new-sell-menu.items");
        ConfigurationSection settings = this.plugin.getMenusConfig()
                .getConfigurationSection("new-sell-menu.item-settings");
        ItemStack selectedItem = new ItemStack(pending.material());
        ItemMeta selectedMeta = selectedItem.getItemMeta();
        if (selectedMeta != null) {
            selectedMeta.setDisplayName(Utils.formatColors("&#34ee80Selected Item"));
            selectedMeta.setLore(Utils.formatColors(Arrays.asList("&7Item: &f" + pending.display(),
                    "&7Price: &#34ee80$" + Utils.abbreviateNumber(pending.price()))));
            selectedItem.setItemMeta(selectedMeta);
            inv.setItem(4, selectedItem);
        }
        for (String category : categories) {
            ItemStack item;
            ItemMeta meta;
            ConfigurationSection sec;
            int slot;
            if (settings == null || !settings.isConfigurationSection(category)
                    || (slot = (sec = settings.getConfigurationSection(category)).getInt("slot", -1)) < 0
                    || slot >= inv.getSize())
                continue;
            Material material = Material.matchMaterial((String) sec.getString("material", category));
            if (material == null) {
                material = Material.PAPER;
            }
            if ((meta = (item = this.button(material, sec.getString("displayname", "&f" + category),
                    Collections.singletonList("&fClick to select this category for &e" + pending.display())))
                    .getItemMeta()) != null) {
                ArrayList<String> withCat = new ArrayList<String>(
                        meta.getLore() == null ? Collections.emptyList() : meta.getLore());
                withCat.add(Utils.formatColors("&8category:" + category));
                meta.setLore(withCat);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }
        player.openInventory(inv);
        this.playClick(player);
    }

    private void handleCategoryClick(Player player, int slot, Inventory top) {
        UUID uuid = player.getUniqueId();
        PendingCategory pending = this.awaitingCategory.get(uuid);
        if (pending == null) {
            player.closeInventory();
            return;
        }
        ItemStack clicked = top.getItem(slot);
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore == null) {
            return;
        }
        String category = null;
        for (String line : lore) {
            String stripped = ChatColor.stripColor((String) Utils.formatColors(line));
            if (stripped == null || !stripped.toLowerCase(Locale.ROOT).startsWith("category:"))
                continue;
            category = stripped.substring("category:".length()).trim();
            break;
        }
        if (category == null || category.isEmpty()) {
            return;
        }
        this.applyPriceToCategory(category, pending.key(), pending.price());
        this.awaitingCategory.remove(uuid);
        player.sendMessage(Utils.formatColors(
                "&aUpdated &f" + pending.key() + " &ato &#34ee80$" + pending.price() + " &ain category &f" + category));
        player.closeInventory();
        this.plugin.reloadPlugin();
    }

    private void applyPriceToCategory(String category, String key, double price) {
        ConfigurationSection cats = this.plugin.getWorthConfig().getConfigurationSection("categories");
        if (cats == null) {
            return;
        }
        for (String cat : cats.getKeys(false)) {
            String path = "categories." + cat;
            List<Map<?, ?>> rawList = this.plugin.getWorthConfig().getMapList(path);
            List<Map<String, Object>> rebuilt = new ArrayList<>();
            for (Map<?, ?> map : rawList) {
                Map<String, Object> copy = new HashMap<>();
                for (Map.Entry<?, ?> en : map.entrySet()) {
                    String k = String.valueOf(en.getKey());
                    if (k.equalsIgnoreCase(key))
                        continue;
                    copy.put(k, en.getValue());
                }
                if (copy.isEmpty())
                    continue;
                rebuilt.add(copy);
            }
            this.plugin.getWorthConfig().set(path, rebuilt);
        }
        String targetPath = "categories." + category;
        List<Map<?, ?>> targetRaw = this.plugin.getWorthConfig().getMapList(targetPath);
        List<Map<String, Object>> target = new ArrayList<>();
        boolean replaced = false;
        for (Map<?, ?> map : targetRaw) {
            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> en : map.entrySet()) {
                copy.put(String.valueOf(en.getKey()), en.getValue());
            }
            if (copy.containsKey(key)) {
                copy.put(key, price);
                replaced = true;
            }
            target.add(copy);
        }
        if (!replaced) {
            Map<String, Object> add = new HashMap<>();
            add.put(key, price);
            target.add(add);
        }
        this.plugin.getWorthConfig().set(targetPath, target);
        this.plugin.saveWorthConfig();
        this.viewCache.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private List<EntryData> getSortedFilteredView(SortMode sort, String filter) {
        ArrayList<EntryData> snapshot;
        String key = sort.name() + "|" + filter.toLowerCase(Locale.ROOT);
        List<EntryData> cached = this.viewCache.get(key);
        if (cached != null) {
            return cached;
        }
        List<EntryData> list = this.baseEntries;
        synchronized (list) {
            snapshot = new ArrayList<EntryData>(this.baseEntries);
        }
        if (!FILTER_ALL.equalsIgnoreCase(filter)) {
            String normalized = this.normalizeCategoryKey(filter);
            List<String> allowed = this.plugin.categoryItems.getOrDefault(normalized, Collections.emptyList());
            snapshot.removeIf(entry -> !this.isAllowedInCategory(entry, normalized, allowed));
        }
        switch (sort.ordinal()) {
            case 0: {
                snapshot.sort((a, b) -> Double.compare(b.price(), a.price()));
                break;
            }
            case 1: {
                snapshot.sort(Comparator.comparingDouble(EntryData::price));
                break;
            }
            case 2: {
                snapshot.sort(Comparator.comparing(EntryData::display, String.CASE_INSENSITIVE_ORDER));
                break;
            }
            case 3: {
                snapshot.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(b.display(), a.display()));
                break;
            }
            case 4: {
                snapshot.removeIf(EntryData::listed);
                snapshot.sort(Comparator.comparing(EntryData::display, String.CASE_INSENSITIVE_ORDER));
            }
        }
        List<EntryData> immutable = List.copyOf(snapshot);
        this.viewCache.put(key, immutable);
        return immutable;
    }

    private void cycleSort(UUID uuid) {
        SortMode current = this.sortByPlayer.getOrDefault(uuid, SortMode.HIGH_TO_LOW);
        SortMode next = switch (current.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> SortMode.LOW_TO_HIGH;
            case 1 -> SortMode.A_TO_Z;
            case 2 -> SortMode.Z_TO_A;
            case 3 -> SortMode.NO_LISTED_PRICE;
            case 4 -> SortMode.HIGH_TO_LOW;
        };
        this.sortByPlayer.put(uuid, next);
    }

    private void cycleFilter(UUID uuid) {
        List<String> opts = this.getOrderedFilterOptions();
        if (opts.size() <= 1) {
            this.filterByPlayer.put(uuid, FILTER_ALL);
            return;
        }
        String current = this.filterByPlayer.getOrDefault(uuid, FILTER_ALL);
        int idx = opts.indexOf(current);
        if (idx < 0) {
            idx = 0;
        }
        idx = (idx + 1) % opts.size();
        this.filterByPlayer.put(uuid, opts.get(idx));
    }

    private ItemStack filterButton(String activeFilter) {
        ArrayList<String> lore = new ArrayList<String>();
        List<String> opts = this.getOrderedFilterOptions();
        for (String opt : opts) {
            boolean selected = opt.equalsIgnoreCase(activeFilter);
            lore.add((selected ? "&#34ee80" : "&f") + this.getCategoryDisplay(opt));
        }
        return this.button(Material.CAULDRON, "&#34ee80\ua730\u026a\u029f\u1d1b\u1d07\u0280", lore);
    }

    private ItemStack sortButton(SortMode current) {
        List<String> names = Arrays.asList("Highest Price", "Lowest Price", "A-Z", "Z-A", "No Price Added");
        List<SortMode> modes = Arrays.asList(SortMode.HIGH_TO_LOW, SortMode.LOW_TO_HIGH, SortMode.A_TO_Z,
                SortMode.Z_TO_A, SortMode.NO_LISTED_PRICE);
        ArrayList<String> lore = new ArrayList<String>();
        for (int i = 0; i < names.size(); ++i) {
            lore.add((modes.get(i) == current ? "&#34ee80" : "&f") + names.get(i));
        }
        return this.button(Material.HOPPER, "&#34ee80\u0455\u1d0f\u0280\u1d1b", lore);
    }

    private List<String> getOrderedFilterOptions() {
        ArrayList<String> opts = new ArrayList<String>();
        opts.add(FILTER_ALL);
        List<String> configured = this.plugin.getMenusConfig().getStringList("item-prices-menu.filter.lore");
        for (String c : configured) {
            String lower;
            if (c == null || (lower = c.toLowerCase(Locale.ROOT)).equals("all")
                    || !this.plugin.categoryItems.containsKey(lower))
                continue;
            opts.add(lower);
        }
        return opts;
    }

    private String getCategoryDisplay(String categoryKey) {
        if (FILTER_ALL.equalsIgnoreCase(categoryKey)) {
            return "All";
        }
        return this.prettify(this.normalizeCategoryKey(categoryKey));
    }

    private String normalizeCategoryKey(String category) {
        String c;
        if (category == null) {
            return "";
        }
        return switch (c = category.toLowerCase(Locale.ROOT).replace(' ', '_')) {
            case "mob_drops" -> "bone";
            case "ores" -> "diamond";
            case "natural_items" -> "oak_leaves";
            case "armor_tools", "armor_and_tools" -> "netherite_helmet";
            case "fish" -> "tropical_fish";
            case "enchanted_books", "books" -> "book";
            case "potions" -> "brewing_stand";
            case "blocks" -> "brick";
            default -> c;
        };
    }

    private boolean isAllowedInCategory(EntryData entry, String normalizedFilter, List<String> allowed) {
        String type = entry.material().name();
        String key = entry.key();
        if ("book".equalsIgnoreCase(normalizedFilter)) {
            if (entry.material() == Material.ENCHANTED_BOOK || type.endsWith("_BOOK")) {
                return true;
            }
            if (ENCH_PATTERN.matcher(key).matches()) {
                return true;
            }
        }
        if ("brewing_stand".equalsIgnoreCase(normalizedFilter)) {
            String baseKey;
            if (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION")) {
                return true;
            }
            baseKey = key.endsWith("-value") ? key.substring(0, key.length() - 6) : key;
            if (POTION_PATTERN.matcher(baseKey).matches()) {
                return true;
            }
        }
        if (allowed != null && !allowed.isEmpty()) {
            if (allowed.contains(type)) {
                return true;
            }
            if ((this.containsIgnoreCase(allowed, "BOOKS") || this.containsIgnoreCase(allowed, "ANY_BOOK"))
                    && (type.endsWith("_BOOK") || entry.material() == Material.ENCHANTED_BOOK)) {
                return true;
            }
            if ((this.containsIgnoreCase(allowed, "POTIONS") || this.containsIgnoreCase(allowed, "ANY_POTION"))
                    && (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION"))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(List<String> list, String token) {
        for (String s : list) {
            if (!s.equalsIgnoreCase(token))
                continue;
            return true;
        }
        return false;
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            meta.setLore(Utils.formatColors(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String prettify(Material material) {
        return this.prettify(material.name());
    }

    private String prettify(String raw) {
        CharSequence[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < parts.length; ++i) {
            if (parts[i].isEmpty())
                continue;
            parts[i] = Character.toUpperCase(((String) parts[i]).charAt(0)) + ((String) parts[i]).substring(1);
        }
        return String.join((CharSequence) " ", parts);
    }

    private void playClick(Player p) {
        String raw = this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK");
        try {
            p.playSound(p.getLocation(), Sound.valueOf((String) raw.toUpperCase(Locale.ROOT)), 1.0f, 1.0f);
        } catch (Exception ex) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private static enum SortMode {
        HIGH_TO_LOW,
        LOW_TO_HIGH,
        A_TO_Z,
        Z_TO_A,
        NO_LISTED_PRICE;

    }

    private record PendingChat(String key, String display, Material material) {
    }

    private record PendingCategory(String key, String display, Material material, double price) {
    }

    private record EntryData(Material material, String key, String display, boolean listed, double price) {
    }
}
