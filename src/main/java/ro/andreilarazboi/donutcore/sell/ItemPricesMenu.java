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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

public class ItemPricesMenu implements Listener {
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
    private final Map<String, List<Map.Entry<ItemStack, Double>>> sortedViewCache = new HashMap<>();
    private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
    private static final Pattern POTION_PATTERN = Pattern.compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)$");
    private static final Pattern SPAWNER_PATTERN = Pattern.compile("([a-z_]+)_spawner-value");
    private final NamespacedKey PDC_CATEGORY;

    public ItemPricesMenu(DonutSell plugin) {
        this.plugin = plugin;
        this.PDC_CATEGORY = new NamespacedKey(plugin.getPlugin(), "category");
        this.disabledSet = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("disabled-items")) {
            this.disabledSet.add(s.toUpperCase(Locale.ROOT));
        }
        ConfigurationSection menu = plugin.getMenusConfig().getConfigurationSection("item-prices-menu");
        this.titleTemplate = menu.getString("title");
        this.titlePrefix = this.titleTemplate.split("%page%", 2)[0];
        this.rows = menu.getInt("rows", 6);
        ConfigurationSection prev = menu.getConfigurationSection("previous");
        this.prevMat = Material.valueOf(prev.getString("previous-page-material").toUpperCase());
        this.prevSlot = prev.getInt("previous-page-slot");
        this.prevName = prev.getString("previous-page-displayname");
        this.prevLore = prev.getStringList("previous-page-lore");
        ConfigurationSection nxt = menu.getConfigurationSection("next");
        this.nextMat = Material.valueOf(nxt.getString("next-page-material").toUpperCase());
        this.nextSlot = nxt.getInt("next-page-slot");
        this.nextName = nxt.getString("next-page-displayname");
        this.nextLore = nxt.getStringList("next-page-lore");
        ConfigurationSection rf = menu.getConfigurationSection("refresh");
        this.refreshMat = Material.valueOf(rf.getString("material").toUpperCase());
        this.refreshSlot = rf.getInt("slot");
        this.refreshName = rf.getString("displayname");
        this.refreshLore = rf.getStringList("lore");
        ConfigurationSection st = menu.getConfigurationSection("sort");
        this.sortMat = Material.valueOf(st.getString("material").toUpperCase());
        this.sortSlot = st.getInt("slot");
        this.sortName = st.getString("displayname");
        this.sortNotCurColor = st.getString("NotCurrentColor");
        this.sortCurColor = st.getString("CurrentColor");
        this.sortOptions = st.getStringList("lore");
        ConfigurationSection fl = menu.getConfigurationSection("filter");
        this.filterMat = Material.valueOf(fl.getString("material").toUpperCase());
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
        ArrayList<Map.Entry<ItemStack, Double>> list = new ArrayList<>();
        HashSet<Object> seen = new HashSet<>();
        boolean disableAllSpawnEggs = this.disabledSet.contains("SPAWN_EGG");
        for (Material m : Material.values()) {
            if (!m.isItem() || m == Material.AIR || this.disabledSet.contains(m.name())
                    || (disableAllSpawnEggs && m.name().endsWith("_SPAWN_EGG"))) continue;
            if (m == Material.SPAWNER) {
                double val = this.plugin.getPrice("spawner-value");
                list.add(new AbstractMap.SimpleEntry<>(new ItemStack(Material.SPAWNER), val));
                seen.add("spawner-value");
                continue;
            }
            String key = m.name().toLowerCase(Locale.ROOT) + "-value";
            double val = this.plugin.getPrice(key);
            list.add(new AbstractMap.SimpleEntry<>(new ItemStack(m), val));
            seen.add(key);
        }
        for (String key : this.plugin.getItemValues().keySet()) {
            if (!key.endsWith("-value") || seen.contains(key)) continue;
            Matcher mk = ENCH_PATTERN.matcher(key);
            if (!mk.matches()) continue;
            String enKey = mk.group(1);
            int lvl = Integer.parseInt(mk.group(2));
            double val = this.plugin.getPrice(key);
            Enchantment ench = Arrays.stream(Enchantment.values())
                    .filter(e -> e.getKey().getKey().equalsIgnoreCase(enKey))
                    .findFirst().orElse(null);
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) book.getItemMeta();
            if (ench == null || esm == null) continue;
            esm.addStoredEnchant(ench, lvl, true);
            esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
            book.setItemMeta(esm);
            list.add(new AbstractMap.SimpleEntry<>(book, val));
            seen.add(key);
        }
        for (String rawKey : this.plugin.getItemValues().keySet()) {
            if (!rawKey.endsWith("-value") || seen.contains(rawKey)) continue;
            String baseKey = rawKey.substring(0, rawKey.length() - 6);
            Matcher pm = POTION_PATTERN.matcher(baseKey);
            if (!pm.matches()) continue;
            String splashOrLingering = pm.group(1);
            String longOrStrong = pm.group(2);
            String effectName = pm.group(3);
            if (!this.looksLikePotion(effectName)) continue;
            boolean extended = "long".equalsIgnoreCase(longOrStrong);
            boolean upgraded = "strong".equalsIgnoreCase(longOrStrong);
            Material potMat = Material.POTION;
            if ("splash".equalsIgnoreCase(splashOrLingering)) potMat = Material.SPLASH_POTION;
            else if ("lingering".equalsIgnoreCase(splashOrLingering)) potMat = Material.LINGERING_POTION;
            ItemStack pot = new ItemStack(potMat);
            PotionMeta pmMeta = (PotionMeta) pot.getItemMeta();
            if (pmMeta == null) continue;
            PotionType type = this.resolvePotionType(effectName);
            if (type != null) {
                pmMeta.setBasePotionData(new PotionData(type, extended, upgraded));
            } else {
                pmMeta.setBasePotionData(new PotionData(PotionType.AWKWARD, false, false));
                pmMeta.displayName(Utils.toComponent(this.prettify(effectName) + " Potion"));
            }
            pmMeta.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
            pot.setItemMeta(pmMeta);
            list.add(new AbstractMap.SimpleEntry<>(pot, this.plugin.getPrice(rawKey)));
            seen.add(rawKey);
        }
        for (String rawKey : this.plugin.getItemValues().keySet()) {
            if (!rawKey.endsWith("-value") || seen.contains(rawKey)) continue;
            Matcher sm = SPAWNER_PATTERN.matcher(rawKey);
            if (!sm.matches()) continue;
            String entityName = sm.group(1);
            EntityType et;
            try {
                et = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            double val = this.plugin.getPrice(rawKey);
            ItemStack sp = new ItemStack(Material.SPAWNER);
            BlockStateMeta bsm = (BlockStateMeta) sp.getItemMeta();
            if (bsm == null) continue;
            BlockState blockState = bsm.getBlockState();
            if (!(blockState instanceof CreatureSpawner cs)) continue;
            cs.setSpawnedType(et);
            bsm.setBlockState(cs);
            sp.setItemMeta(bsm);
            list.add(new AbstractMap.SimpleEntry<>(sp, val));
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
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Inventory top = e.getView().getTopInventory();
        String title = e.getView().getTitle();
        if (!title.startsWith(this.titlePrefix)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        e.setCancelled(true);
        if (slot < this.rows * 9 - 9) {
            ItemStack clicked = top.getItem(slot);
            if (clicked != null && !clicked.getType().isAir()) {
                this.plugin.promptPriceEdit(player, clicked);
            }
        }
    }

    public void open(Player player, int reqPage) {
        ViewTracker vt = this.plugin.getViewTracker();
        String filterCategory = vt.getFilter(player.getUniqueId());
        String cacheKey = vt.getOrder(player.getUniqueId()).name() + "|"
                + (filterCategory == null ? FILTER_ALL : filterCategory.toLowerCase(Locale.ROOT));
        List<Map.Entry<ItemStack, Double>> sorted = this.sortedViewCache.get(cacheKey);
        if (sorted == null) {
            sorted = new ArrayList<>(this.masterEntries);
            switch (vt.getOrder(player.getUniqueId())) {
                case HIGH_TO_LOW -> sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                case LOW_TO_HIGH -> sorted.sort(Comparator.comparingDouble(Map.Entry::getValue));
                case A_TO_Z, NAME -> sorted.sort(Comparator.comparing(en -> {
                    ItemMeta m = en.getKey().getItemMeta();
                    return m != null && m.hasDisplayName() && m.displayName() != null
                            ? LegacyComponentSerializer.legacySection().serialize(m.displayName())
                            : this.prettify(en.getKey().getType());
                }, String.CASE_INSENSITIVE_ORDER));
                case Z_TO_A -> sorted.sort((a, b) -> {
                    ItemMeta ma = a.getKey().getItemMeta();
                    ItemMeta mb = b.getKey().getItemMeta();
                    String da = ma != null && ma.hasDisplayName() && ma.displayName() != null
                            ? LegacyComponentSerializer.legacySection().serialize(ma.displayName())
                            : this.prettify(a.getKey().getType());
                    String db = mb != null && mb.hasDisplayName() && mb.displayName() != null
                            ? LegacyComponentSerializer.legacySection().serialize(mb.displayName())
                            : this.prettify(b.getKey().getType());
                    return String.CASE_INSENSITIVE_ORDER.compare(db, da);
                });
            }
            if (filterCategory != null && !filterCategory.equalsIgnoreCase(FILTER_ALL)) {
                String normalized = this.normalizeCategoryKey(filterCategory);
                List<String> allowed = this.plugin.categoryItems.getOrDefault(normalized, Collections.emptyList());
                sorted.removeIf(en -> !this.isAllowedInCategory(en.getKey(), filterCategory, allowed));
            }
            this.sortedViewCache.put(cacheKey, sorted);
        }
        int size = this.rows * 9;
        int per = size - 9;
        int maxPage = Math.max(1, (int) Math.ceil((double) sorted.size() / per));
        int page = Math.min(maxPage, Math.max(1, reqPage));
        Inventory inv = Bukkit.createInventory(null, size,
                Utils.formatColors(this.titleTemplate.replace("%page%", String.valueOf(page))));
        int start = (page - 1) * per;
        int end = Math.min(start + per, sorted.size());
        for (int i = start; i < end; ++i) {
            Map.Entry<ItemStack, Double> ent = sorted.get(i);
            ItemStack is = ent.getKey().clone();
            ItemMeta m = is.getItemMeta();
            if (m == null) continue;
            String base;
            if (is.getType() == Material.SPAWNER && m instanceof BlockStateMeta bsm) {
                BlockState bs = bsm.getBlockState();
                if (bs instanceof CreatureSpawner cs && cs.getSpawnedType() != null) {
                    base = this.prettify(cs.getSpawnedType().name() + "_SPAWNER");
                } else {
                    base = this.prettify("SPAWNER");
                }
            } else {
                base = m.hasDisplayName() && m.displayName() != null
                        ? LegacyComponentSerializer.legacySection().serialize(m.displayName())
                        : this.prettify(is.getType());
            }
            m.displayName(Utils.toComponent(this.itemDisplayTemplate
                    .replace("%ItemName%", base)
                    .replace("%amount%", Utils.abbreviateNumber(ent.getValue()))));
            ArrayList<Component> lore = new ArrayList<>();
            for (String line : this.itemLoreTemplate) {
                lore.add(Utils.toComponent(line.replace("%ItemName%", base).replace("%amount%", Utils.abbreviateNumber(ent.getValue()))));
            }
            m.lore(lore);
            is.setItemMeta(m);
            inv.setItem(i - start, is);
        }
        this.place(inv, this.prevMat, this.prevSlot, this.prevName, this.prevLore);
        this.place(inv, this.nextMat, this.nextSlot, this.nextName, this.nextLore);
        this.place(inv, this.refreshMat, this.refreshSlot, this.refreshName, this.refreshLore);
        ItemStack sortItem = new ItemStack(this.sortMat);
        ItemMeta sm = sortItem.getItemMeta();
        if (sm != null) {
            sm.displayName(Utils.toComponent(this.sortName));
            ArrayList<Component> sortLore = new ArrayList<>();
            ViewTracker.SortOrder cur = vt.getOrder(player.getUniqueId());
            for (String option : this.sortOptions) {
                ViewTracker.SortOrder mode = this.sortOrderFromLabel(option);
                String col = mode == cur ? this.sortCurColor : this.sortNotCurColor;
                sortLore.add(Utils.toComponent(col + "• " + option));
            }
            sm.lore(sortLore);
            sortItem.setItemMeta(sm);
            inv.setItem(this.sortSlot, sortItem);
        }
        ItemStack filterItem = new ItemStack(this.filterMat);
        ItemMeta fm = filterItem.getItemMeta();
        if (fm != null) {
            fm.displayName(Utils.toComponent(this.filterName));
            ArrayList<Component> filterLore = new ArrayList<>();
            String curFilt = filterCategory == null ? FILTER_ALL : filterCategory;
            List<String> dynamic = this.filterOptions.isEmpty() ? Collections.singletonList(FILTER_ALL) : this.filterOptions;
            for (String opt : dynamic) {
                String col = opt.equalsIgnoreCase(curFilt) ? this.filterCurColor : this.filterNotCurColor;
                filterLore.add(Utils.toComponent(col + this.prettyCategoryName(opt)));
            }
            fm.lore(filterLore);
            filterItem.setItemMeta(fm);
            inv.setItem(this.filterSlot, filterItem);
        }
        player.openInventory(inv);
        vt.setPage(player.getUniqueId(), page);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(this.pageSwitchSoundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private void place(Inventory inv, Material mat, int slot, String name, List<String> lore) {
        ItemStack b = new ItemStack(mat);
        ItemMeta m = b.getItemMeta();
        if (m != null) {
            m.displayName(Utils.toComponent(name));
            m.lore(Utils.toComponents(lore));
            b.setItemMeta(m);
            inv.setItem(slot, b);
        }
    }

    private String prettify(Material m) { return this.prettify(m.name()); }

    private String prettify(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private boolean isAllowedInCategory(ItemStack is, String filterCategory, List<String> allowed) {
        String normalizedFilter = this.normalizeCategoryKey(filterCategory);
        String type = is.getType().name();
        ItemMeta meta = is.getItemMeta();
        PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
        String catTag = pdc != null && pdc.has(this.PDC_CATEGORY, PersistentDataType.STRING)
                ? pdc.get(this.PDC_CATEGORY, PersistentDataType.STRING) : null;
        if ("book".equalsIgnoreCase(normalizedFilter)) {
            if ("book".equalsIgnoreCase(catTag) || meta instanceof EnchantmentStorageMeta || type.endsWith("_BOOK")) return true;
        }
        if ("brewing_stand".equalsIgnoreCase(normalizedFilter)) {
            if ("brewing_stand".equalsIgnoreCase(catTag) || type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION")) return true;
        }
        if (allowed != null && !allowed.isEmpty()) {
            if (allowed.contains(type)) return true;
            if ((containsIgnoreCase(allowed, "BOOKS") || containsIgnoreCase(allowed, "ANY_BOOK"))
                    && (type.endsWith("_BOOK") || meta instanceof EnchantmentStorageMeta)) return true;
            if ((containsIgnoreCase(allowed, "POTIONS") || containsIgnoreCase(allowed, "ANY_POTION"))
                    && (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION"))) return true;
        }
        return false;
    }

    private String prettyCategoryName(String key) {
        if (key == null) return "";
        if (key.equalsIgnoreCase(FILTER_ALL)) return "All";
        return this.prettify(this.normalizeCategoryKey(key));
    }

    private ViewTracker.SortOrder sortOrderFromLabel(String label) {
        if (label == null) return ViewTracker.SortOrder.HIGH_TO_LOW;
        return switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "highest price" -> ViewTracker.SortOrder.HIGH_TO_LOW;
            case "lowest price" -> ViewTracker.SortOrder.LOW_TO_HIGH;
            case "a-z" -> ViewTracker.SortOrder.A_TO_Z;
            case "z-a" -> ViewTracker.SortOrder.Z_TO_A;
            default -> ViewTracker.SortOrder.HIGH_TO_LOW;
        };
    }

    private String normalizeCategoryKey(String category) {
        if (category == null) return "";
        String c = category.toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (c) {
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
        for (String s : list) { if (s.equalsIgnoreCase(token)) return true; }
        return false;
    }

    private boolean looksLikePotion(String effectName) {
        return effectName.contains("potion") || effectName.contains("vision") || effectName.contains("invisibility")
                || effectName.contains("leaping") || effectName.contains("jump") || effectName.contains("fire_resistance")
                || effectName.contains("swiftness") || effectName.contains("speed") || effectName.contains("slowness")
                || effectName.contains("water_breathing") || effectName.contains("healing") || effectName.contains("harming")
                || effectName.contains("poison") || effectName.contains("regeneration") || effectName.contains("strength")
                || effectName.contains("weakness") || effectName.contains("turtle_master") || effectName.contains("slow_falling")
                || effectName.contains("mundane") || effectName.contains("thick") || effectName.contains("awkward")
                || effectName.contains("water") || effectName.contains("wind_charged") || effectName.contains("weaving")
                || effectName.contains("oozing") || effectName.contains("infested");
    }

    private PotionType resolvePotionType(String effectName) {
        String n = effectName.toLowerCase(Locale.ROOT);
        return switch (n) {
            case "leaping" -> PotionType.LEAPING;
            case "swiftness" -> PotionType.SWIFTNESS;
            case "healing" -> PotionType.HEALING;
            case "harming" -> PotionType.HARMING;
            case "water", "potion" -> PotionType.WATER;
            default -> {
                String enumName = n.toUpperCase(Locale.ROOT).replace('-', '_');
                try { yield PotionType.valueOf(enumName); } catch (IllegalArgumentException ignored) { yield null; }
            }
        };
    }
}
