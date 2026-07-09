package ro.andreilarazboi.donutcore.sell;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes", "unused", "removal"})
public class ItemPricesMenu
implements Listener {
    private static final String FILTER_ALL = "all";
    private final DonutSell plugin;
    private List<Map.Entry<ItemStack, Double>> masterEntries;
    private final Set<String> disabledSet;
    private final String titlePrefix;
    private final String titleTemplate;
    private final int rows;
    private final Material prevMat;
    private final int prevSlot;
    private final String prevName;
    private final List<String> prevLore;
    private final Material nextMat;
    private final int nextSlot;
    private final String nextName;
    private final List<String> nextLore;
    private final Material refreshMat;
    private final int refreshSlot;
    private final String refreshName;
    private final List<String> refreshLore;
    private final Material sortMat;
    private final int sortSlot;
    private final String sortName;
    private final String sortNotCurColor;
    private final String sortCurColor;
    private final List<String> sortOptions;
    private final Material filterMat;
    private final int filterSlot;
    private final String filterName;
    private final String filterNotCurColor;
    private final String filterCurColor;
    private final List<String> filterOptions;
    private final String itemDisplayTemplate;
    private final List<String> itemLoreTemplate;
    private final String pageSwitchSoundName;
    private final Map<String, List<Map.Entry<ItemStack, Double>>> sortedViewCache = new HashMap<String, List<Map.Entry<ItemStack, Double>>>();
    private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
    private static final Pattern POTION_PATTERN = Pattern.compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)$");
    private static final Pattern SPAWNER_PATTERN = Pattern.compile("([a-z_]+)_spawner-value");
    private final NamespacedKey PDC_CATEGORY;

    public ItemPricesMenu(DonutSell plugin) {
        this.plugin = plugin;
        this.PDC_CATEGORY = new NamespacedKey(plugin.getPlugin(), "category");
        this.disabledSet = new HashSet<String>();
        for (String s : plugin.getConfig().getStringList("disabled-items")) {
            this.disabledSet.add(s.toUpperCase(Locale.ROOT));
        }
        ConfigurationSection menu = plugin.getMenusConfig().getConfigurationSection("item-prices-menu");
        this.titleTemplate = menu.getString("title");
        this.titlePrefix = this.titleTemplate.split("%page%", 2)[0];
        this.rows = menu.getInt("rows", 6);
        ConfigurationSection prev = menu.getConfigurationSection("previous");
        this.prevMat = Material.valueOf((String)prev.getString("previous-page-material").toUpperCase());
        this.prevSlot = prev.getInt("previous-page-slot");
        this.prevName = prev.getString("previous-page-displayname");
        this.prevLore = prev.getStringList("previous-page-lore");
        ConfigurationSection nxt = menu.getConfigurationSection("next");
        this.nextMat = Material.valueOf((String)nxt.getString("next-page-material").toUpperCase());
        this.nextSlot = nxt.getInt("next-page-slot");
        this.nextName = nxt.getString("next-page-displayname");
        this.nextLore = nxt.getStringList("next-page-lore");
        ConfigurationSection rf = menu.getConfigurationSection("refresh");
        this.refreshMat = Material.valueOf((String)rf.getString("material").toUpperCase());
        this.refreshSlot = rf.getInt("slot");
        this.refreshName = rf.getString("displayname");
        this.refreshLore = rf.getStringList("lore");
        ConfigurationSection st = menu.getConfigurationSection("sort");
        this.sortMat = Material.valueOf((String)st.getString("material").toUpperCase());
        this.sortSlot = st.getInt("slot");
        this.sortName = st.getString("displayname");
        this.sortNotCurColor = st.getString("NotCurrentColor");
        this.sortCurColor = st.getString("CurrentColor");
        this.sortOptions = st.getStringList("lore");
        ConfigurationSection fl = menu.getConfigurationSection("filter");
        this.filterMat = Material.valueOf((String)fl.getString("material").toUpperCase());
        this.filterSlot = fl.getInt("slot");
        this.filterName = fl.getString("displayname");
        this.filterNotCurColor = fl.getString("NotCurrentColor");
        this.filterCurColor = fl.getString("CurrentColor");
        this.filterOptions = fl.getStringList("lore");
        ConfigurationSection items = menu.getConfigurationSection("items");
        this.itemDisplayTemplate = items.getString("displayname");
        this.itemLoreTemplate = items.getStringList("lore");
        String configured = plugin.getConfig().getString("sounds.page-switch", "ITEM_BOOK_PAGE_TURN");
        this.pageSwitchSoundName = configured != null ? configured.toUpperCase(Locale.ROOT) : "ITEM_BOOK_PAGE_TURN";
        this.buildEntries();
    }

    private void buildEntries() {
        double val;
        ArrayList<Map.Entry<ItemStack, Double>> list = new ArrayList<Map.Entry<ItemStack, Double>>();
        HashSet<Object> seen = new HashSet<Object>();
        boolean disableAllSpawnEggs = this.disabledSet.contains("SPAWN_EGG");
        for (Material m : Material.values()) {
            if (!m.isItem() || m == Material.AIR || this.disabledSet.contains(m.name()) || disableAllSpawnEggs && m.name().endsWith("_SPAWN_EGG")) continue;
            if (m == Material.SPAWNER) {
                double val2 = this.plugin.getPrice("spawner-value");
                ItemStack spawner = new ItemStack(Material.SPAWNER);
                list.add(new AbstractMap.SimpleEntry<ItemStack, Double>(spawner, val2));
                seen.add("spawner-value");
                continue;
            }
            String key = m.name().toLowerCase(Locale.ROOT) + "-value";
            val = this.plugin.getPrice(key);
            list.add(new AbstractMap.SimpleEntry<ItemStack, Double>(new ItemStack(m), val));
            seen.add(key);
        }
        for (String key : this.plugin.getItemValues().keySet()) {
            ItemStack book;
            EnchantmentStorageMeta esm;
            Matcher mk;
            if (!key.endsWith("-value") || seen.contains(key) || !(mk = ENCH_PATTERN.matcher(key)).matches()) continue;
            String enKey = mk.group(1);
            int lvl = Integer.parseInt(mk.group(2));
            val = this.plugin.getPrice(key);
            Enchantment ench = Arrays.stream(Enchantment.values()).filter(e -> e.getKey().getKey().equalsIgnoreCase(enKey)).findFirst().orElse(null);
            if (ench == null || (esm = (EnchantmentStorageMeta)(book = new ItemStack(Material.ENCHANTED_BOOK)).getItemMeta()) == null) continue;
            esm.addStoredEnchant(ench, lvl, true);
            esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
            book.setItemMeta((ItemMeta)esm);
            list.add(new AbstractMap.SimpleEntry<ItemStack, Double>(book, val));
            seen.add(key);
        }
        for (String rawKey : this.plugin.getItemValues().keySet()) {
            ItemStack pot;
            PotionMeta pmMeta;
            Matcher pm;
            if (!rawKey.endsWith("-value") || seen.contains(rawKey) || !(pm = POTION_PATTERN.matcher(rawKey.substring(0, rawKey.length() - 6))).matches()) continue;
            String splashOrLingering = pm.group(1);
            String longOrStrong = pm.group(2);
            String effectName = pm.group(3);
            if (!this.looksLikePotion(effectName)) continue;
            boolean extended = "long".equalsIgnoreCase(longOrStrong);
            boolean upgraded = "strong".equalsIgnoreCase(longOrStrong);
            Material potMat = Material.POTION;
            if ("splash".equalsIgnoreCase(splashOrLingering)) {
                potMat = Material.SPLASH_POTION;
            } else if ("lingering".equalsIgnoreCase(splashOrLingering)) {
                potMat = Material.LINGERING_POTION;
            }
            if ((pmMeta = (PotionMeta)(pot = new ItemStack(potMat)).getItemMeta()) == null) continue;
            PotionType type = this.resolvePotionType(effectName);
            if (type != null) {
                pmMeta.setBasePotionData(new PotionData(type, extended, upgraded));
            } else {
                pmMeta.setBasePotionData(new PotionData(PotionType.AWKWARD, false, false));
                String pretty = this.prettify(effectName) + " Potion";
                pmMeta.setDisplayName(pretty);
            }
            pmMeta.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
            pot.setItemMeta((ItemMeta)pmMeta);
            list.add(new AbstractMap.SimpleEntry<ItemStack, Double>(pot, this.plugin.getPrice(rawKey)));
            seen.add(rawKey);
        }
        for (String rawKey : this.plugin.getItemValues().keySet()) {
            BlockState blockState;
            EntityType et;
            Matcher sm;
            if (!rawKey.endsWith("-value") || seen.contains(rawKey) || !(sm = SPAWNER_PATTERN.matcher(rawKey)).matches()) continue;
            String entityName = sm.group(1);
            try {
                et = EntityType.valueOf((String)entityName.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ex) {
                continue;
            }
            double val3 = this.plugin.getPrice(rawKey);
            ItemStack sp = new ItemStack(Material.SPAWNER);
            BlockStateMeta bsm = (BlockStateMeta)sp.getItemMeta();
            if (bsm == null || !((blockState = bsm.getBlockState()) instanceof CreatureSpawner)) continue;
            CreatureSpawner cs = (CreatureSpawner)blockState;
            cs.setSpawnedType(et);
            bsm.setBlockState((BlockState)cs);
            sp.setItemMeta((ItemMeta)bsm);
            list.add(new AbstractMap.SimpleEntry<ItemStack, Double>(sp, val3));
            seen.add(rawKey);
        }
        this.masterEntries = list;
        this.sortedViewCache.clear();
    }

    public void reloadData() {
        this.buildEntries();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!this.plugin.isModuleActive()) return;
        ItemStack clicked;
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        String title = e.getView().getTitle();
        if (!title.startsWith(this.titlePrefix)) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        e.setCancelled(true);
        Player player = (Player)e.getWhoClicked();
        if (slot >= 0 && slot < this.rows * 9 - 9 && (clicked = top.getItem(slot)) != null && !clicked.getType().isAir()) {
            this.plugin.promptPriceEdit(player, clicked);
        }
    }

    public void open(Player player, int reqPage) {
        ItemStack filterItem;
        ItemMeta fm;
        ViewTracker vt = this.plugin.getViewTracker();
        String filterCategory = vt.getFilter(player.getUniqueId());
        String cacheKey = vt.getOrder(player.getUniqueId()).name() + "|" + (filterCategory == null ? FILTER_ALL : filterCategory.toLowerCase(Locale.ROOT));
        List<Map.Entry<ItemStack, Double>> sorted = this.sortedViewCache.get(cacheKey);
        if (sorted == null) {
            sorted = new ArrayList<Map.Entry<ItemStack, Double>>(this.masterEntries);
            switch (vt.getOrder(player.getUniqueId())) {
                case HIGH_TO_LOW: {
                    sorted.sort((a, b) -> Double.compare((Double)b.getValue(), (Double)a.getValue()));
                    break;
                }
                case LOW_TO_HIGH: {
                    sorted.sort(Comparator.comparingDouble(Map.Entry::getValue));
                    break;
                }
                case A_TO_Z: 
                case NAME: {
                    sorted.sort(Comparator.comparing(e -> {
                        ItemMeta m = ((ItemStack)e.getKey()).getItemMeta();
                        return m != null && m.hasDisplayName() ? m.getDisplayName() : this.prettify(((ItemStack)e.getKey()).getType());
                    }, String.CASE_INSENSITIVE_ORDER));
                    break;
                }
                case Z_TO_A: {
                    sorted.sort((a, b) -> {
                        ItemMeta ma = ((ItemStack)a.getKey()).getItemMeta();
                        ItemMeta mb = ((ItemStack)b.getKey()).getItemMeta();
                        String da = ma != null && ma.hasDisplayName() ? ma.getDisplayName() : this.prettify(((ItemStack)a.getKey()).getType());
                        String db = mb != null && mb.hasDisplayName() ? mb.getDisplayName() : this.prettify(((ItemStack)b.getKey()).getType());
                        return String.CASE_INSENSITIVE_ORDER.compare(db, da);
                    });
                }
            }
            if (filterCategory != null && !filterCategory.equalsIgnoreCase(FILTER_ALL)) {
                String normalized = this.normalizeCategoryKey(filterCategory);
                List<String> allowed = this.plugin.categoryItems.getOrDefault(normalized, Collections.emptyList());
                sorted.removeIf(e -> !this.isAllowedInCategory(e.getKey(), filterCategory, allowed));
            }
            this.sortedViewCache.put(cacheKey, sorted);
        }
        int size = this.rows * 9;
        int per = size - 9;
        int maxPage = Math.max(1, (int)Math.ceil((double)sorted.size() / (double)per));
        int page = Math.min(maxPage, Math.max(1, reqPage));
        Inventory inv = Bukkit.createInventory(null, (int)size, (String)Utils.formatColors(this.titleTemplate.replace("%page%", String.valueOf(page))));
        int start = (page - 1) * per;
        int end = Math.min(start + per, sorted.size());
        for (int i = start; i < end; ++i) {
            Map.Entry<ItemStack, Double> ent = sorted.get(i);
            ItemStack is = ent.getKey().clone();
            ItemMeta m = is.getItemMeta();
            if (m == null) continue;
            String base;
            if (is.getType() == Material.SPAWNER && m instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) m;
                BlockState blockState = bsm.getBlockState();
                if (blockState instanceof CreatureSpawner) {
                    CreatureSpawner csSpawner = (CreatureSpawner) blockState;
                    EntityType spawned = csSpawner.getSpawnedType();
                    base = spawned != null ? this.prettify(spawned.name() + "_SPAWNER") : this.prettify("SPAWNER");
                } else {
                    base = m.hasDisplayName() ? m.getDisplayName() : this.prettify(is.getType());
                }
            } else {
                base = m.hasDisplayName() ? m.getDisplayName() : this.prettify(is.getType());
            }
            m.setDisplayName(Utils.formatColors(this.itemDisplayTemplate.replace("%ItemName%", base).replace("%amount%", Utils.abbreviateNumber(ent.getValue()))));
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : this.itemLoreTemplate) {
                lore.add(Utils.formatColors(line.replace("%ItemName%", base).replace("%amount%", Utils.abbreviateNumber(ent.getValue()))));
            }
            m.setLore(lore);
            is.setItemMeta(m);
            inv.setItem(i - start, is);
        }
        this.place(inv, this.prevMat, this.prevSlot, this.prevName, this.prevLore);
        this.place(inv, this.nextMat, this.nextSlot, this.nextName, this.nextLore);
        this.place(inv, this.refreshMat, this.refreshSlot, this.refreshName, this.refreshLore);
        ItemStack sortItem = new ItemStack(this.sortMat);
        ItemMeta sm = sortItem.getItemMeta();
        if (sm != null) {
            sm.setDisplayName(Utils.formatColors(this.sortName));
            ArrayList<String> lore = new ArrayList<String>();
            ViewTracker.SortOrder cur = vt.getOrder(player.getUniqueId());
            for (String option : this.sortOptions) {
                ViewTracker.SortOrder mode = this.sortOrderFromLabel(option);
                String col = mode == cur ? this.sortCurColor : this.sortNotCurColor;
                lore.add(Utils.formatColors(col + "\u2022 " + option));
            }
            sm.setLore(lore);
            sortItem.setItemMeta(sm);
            inv.setItem(this.sortSlot, sortItem);
        }
        if ((fm = (filterItem = new ItemStack(this.filterMat)).getItemMeta()) != null) {
            fm.setDisplayName(Utils.formatColors(this.filterName));
            ArrayList<String> lore = new ArrayList<String>();
            String curFilt = filterCategory == null ? FILTER_ALL : filterCategory;
            List<String> dynamic = this.filterOptions.isEmpty() ? Collections.singletonList(FILTER_ALL) : this.filterOptions;
            for (String opt : dynamic) {
                String col = opt.equalsIgnoreCase(curFilt) ? this.filterCurColor : this.filterNotCurColor;
                lore.add(Utils.formatColors(col + this.prettyCategoryName(opt)));
            }
            fm.setLore(lore);
            filterItem.setItemMeta(fm);
            inv.setItem(this.filterSlot, filterItem);
        }
        player.openInventory(inv);
        vt.setPage(player.getUniqueId(), page);
        try {
            Sound sound = Sound.valueOf((String)this.pageSwitchSoundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private void place(Inventory inv, Material mat, int slot, String name, List<String> lore) {
        ItemStack b = new ItemStack(mat);
        ItemMeta m = b.getItemMeta();
        if (m != null) {
            m.setDisplayName(Utils.formatColors(name));
            m.setLore(Utils.formatColors(lore));
            b.setItemMeta(m);
            inv.setItem(slot, b);
        }
    }

    private String prettify(Material m) {
        return this.prettify(m.name());
    }

    private String prettify(String raw) {
        CharSequence[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = Character.toUpperCase(((String)parts[i]).charAt(0)) + ((String)parts[i]).substring(1);
        }
        return String.join((CharSequence)" ", parts);
    }

    private boolean isAllowedInCategory(ItemStack is, String filterCategory, List<String> allowed) {
        String catTag;
        String normalizedFilter = this.normalizeCategoryKey(filterCategory);
        String type = is.getType().name();
        ItemMeta meta = is.getItemMeta();
        PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
        String string = catTag = pdc != null && pdc.has(this.PDC_CATEGORY, PersistentDataType.STRING) ? (String)pdc.get(this.PDC_CATEGORY, PersistentDataType.STRING) : null;
        if ("book".equalsIgnoreCase(normalizedFilter)) {
            if ("book".equalsIgnoreCase(catTag)) {
                return true;
            }
            if (meta instanceof EnchantmentStorageMeta) {
                return true;
            }
            if (type.endsWith("_BOOK")) {
                return true;
            }
        }
        if ("brewing_stand".equalsIgnoreCase(normalizedFilter)) {
            if ("brewing_stand".equalsIgnoreCase(catTag)) {
                return true;
            }
            if (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION")) {
                return true;
            }
        }
        if (allowed != null && !allowed.isEmpty()) {
            if (allowed.contains(type)) {
                return true;
            }
            if ((this.containsIgnoreCase(allowed, "BOOKS") || this.containsIgnoreCase(allowed, "ANY_BOOK")) && (type.endsWith("_BOOK") || meta instanceof EnchantmentStorageMeta)) {
                return true;
            }
            if ((this.containsIgnoreCase(allowed, "POTIONS") || this.containsIgnoreCase(allowed, "ANY_POTION")) && (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION"))) {
                return true;
            }
        }
        return false;
    }

    private String prettyCategoryName(String key) {
        if (key == null) {
            return "";
        }
        if (key.equalsIgnoreCase(FILTER_ALL)) {
            return "All";
        }
        return this.prettify(this.normalizeCategoryKey(key));
    }

    private ViewTracker.SortOrder sortOrderFromLabel(String label) {
        if (label == null) {
            return ViewTracker.SortOrder.HIGH_TO_LOW;
        }
        return switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "highest price" -> ViewTracker.SortOrder.HIGH_TO_LOW;
            case "lowest price" -> ViewTracker.SortOrder.LOW_TO_HIGH;
            case "a-z" -> ViewTracker.SortOrder.A_TO_Z;
            case "z-a" -> ViewTracker.SortOrder.Z_TO_A;
            default -> ViewTracker.SortOrder.HIGH_TO_LOW;
        };
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

    private boolean containsIgnoreCase(List<String> list, String token) {
        for (String s : list) {
            if (!s.equalsIgnoreCase(token)) continue;
            return true;
        }
        return false;
    }

    private boolean looksLikePotion(String effectName) {
        return effectName.contains("potion") || effectName.contains("vision") || effectName.contains("invisibility") || effectName.contains("leaping") || effectName.contains("jump") || effectName.contains("fire_resistance") || effectName.contains("swiftness") || effectName.contains("speed") || effectName.contains("slowness") || effectName.contains("water_breathing") || effectName.contains("healing") || effectName.contains("harming") || effectName.contains("poison") || effectName.contains("regeneration") || effectName.contains("strength") || effectName.contains("weakness") || effectName.contains("turtle_master") || effectName.contains("slow_falling") || effectName.contains("mundane") || effectName.contains("thick") || effectName.contains("awkward") || effectName.contains("water") || effectName.contains("wind_charged") || effectName.contains("weaving") || effectName.contains("oozing") || effectName.contains("infested");
    }

    private PotionType resolvePotionType(String effectName) {
        String n;
        switch (n = effectName.toLowerCase(Locale.ROOT)) {
            case "leaping": {
                return PotionType.LEAPING;
            }
            case "swiftness": {
                return PotionType.SWIFTNESS;
            }
            case "healing": {
                return PotionType.HEALING;
            }
            case "harming": {
                return PotionType.HARMING;
            }
            case "water": {
                return PotionType.WATER;
            }
            case "potion": {
                return PotionType.WATER;
            }
        }
        String enumName = n.toUpperCase(Locale.ROOT);
        enumName = enumName.replace('-', '_');
        try {
            return PotionType.valueOf((String)enumName);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

