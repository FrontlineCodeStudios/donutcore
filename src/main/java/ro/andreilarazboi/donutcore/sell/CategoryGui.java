package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

@SuppressWarnings({"deprecation", "unchecked", "removal"})
public class CategoryGui {
    private final DonutSell plugin;
    private final String categoryKey;
    private final List<ItemStack> items = new ArrayList<ItemStack>();
    private final int rows;
    private final String defaultTitleTpl;
    private final Map<String, String> customTitles = new HashMap<String, String>();
    private final int prevSlot;
    private final int nextSlot;
    private final int backSlot;
    private final Material prevMat;
    private final Material nextMat;
    private final Material backMat;
    private final String prevName;
    private final String nextName;
    private final String backName;
    private final List<String> prevLore;
    private final List<String> nextLore;
    private final List<String> backLore;
    private final String itemNameTpl;
    private final List<String> itemLoreTpl;
    private final String pageSwitchSoundName;
    private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
    private static final Pattern POTION_PATTERN = Pattern.compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)-value");
    private static final Pattern SPAWNER_PATTERN = Pattern.compile("([a-z_]+)_spawner-value");
    private final NamespacedKey PDC_CATEGORY;
    private static final TreeMap<Integer, String> ROMAN = new TreeMap<>();

    public CategoryGui(DonutSell plugin, String categoryKey) {
        this.plugin = plugin;
        this.categoryKey = categoryKey.toLowerCase(Locale.ROOT);
        this.PDC_CATEGORY = new NamespacedKey(plugin.getPlugin(), "category");
        ConfigurationSection cfg = plugin.getMenusConfig().getConfigurationSection("category-menu");
        this.rows = cfg.getInt("rows", 6);
        this.defaultTitleTpl = Utils.formatColors(cfg.getString("title", "%item% Items"));
        ConfigurationSection tsec = cfg.getConfigurationSection("titles");
        if (tsec != null) {
            for (String cat : tsec.getKeys(false)) {
                this.customTitles.put(cat.toLowerCase(), Utils.formatColors(tsec.getString(cat)));
            }
        }
        this.prevSlot = cfg.getInt("previous-page-slot", 49);
        this.prevMat = Material.matchMaterial((String)cfg.getString("previous-page.material"));
        this.prevName = Utils.formatColors(cfg.getString("previous-page.displayname", "&aPrevious"));
        this.prevLore = Utils.formatColors(cfg.getStringList("previous-page.lore"));
        this.nextSlot = cfg.getInt("next-page-slot", 51);
        this.nextMat = Material.matchMaterial((String)cfg.getString("next-page.material"));
        this.nextName = Utils.formatColors(cfg.getString("next-page.displayname", "&aNext"));
        this.nextLore = Utils.formatColors(cfg.getStringList("next-page.lore"));
        this.backSlot = cfg.getInt("back-button.slot", 45);
        this.backMat = Material.matchMaterial((String)cfg.getString("back-button.material"));
        this.backName = Utils.formatColors(cfg.getString("back-button.displayname", "&cBack"));
        this.backLore = Utils.formatColors(cfg.getStringList("back-button.lore"));
        this.itemNameTpl = Utils.formatColors(cfg.getString("item.displayname", "%item%"));
        this.itemLoreTpl = cfg.getStringList("item.lore");
        String configured = plugin.getConfig().getString("sounds.page-switch", "ITEM_BOOK_PAGE_TURN");
        this.pageSwitchSoundName = configured != null ? configured.toUpperCase(Locale.ROOT) : "ITEM_BOOK_PAGE_TURN";
        Object rawNode = plugin.getWorthConfig().get("categories." + this.categoryKey);
        if (rawNode instanceof ConfigurationSection) {
            ConfigurationSection catSec = (ConfigurationSection)rawNode;
            for (String entryKey : catSec.getKeys(false)) {
                double price = catSec.getDouble(entryKey, -1.0);
                if (price < 0.0) {
                    plugin.getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
                    continue;
                }
                this.handleEntry(entryKey, price);
            }
        } else if (rawNode instanceof List) {
            List<?> rawList = (List<?>)rawNode;
            for (Object o : rawList) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>)o;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    double price;
                    String entryKey = String.valueOf(e.getKey());
                    try {
                        price = Double.parseDouble(e.getValue().toString());
                    }
                    catch (NumberFormatException ex) {
                        plugin.getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
                        continue;
                    }
                    this.handleEntry(entryKey, price);
                }
            }
        } else {
            plugin.getLogger().warning("No entries found for categories." + categoryKey);
        }
    }

    private void handleEntry(String entryKey, double price) {
        Matcher spm = SPAWNER_PATTERN.matcher(entryKey);
        if (spm.matches()) {
            try {
                BlockState blockState;
                EntityType type = EntityType.valueOf((String)spm.group(1).toUpperCase(Locale.ROOT));
                ItemStack stk = new ItemStack(Material.SPAWNER);
                BlockStateMeta bsm = (BlockStateMeta)stk.getItemMeta();
                if (bsm != null && (blockState = bsm.getBlockState()) instanceof CreatureSpawner) {
                    CreatureSpawner cs = (CreatureSpawner)blockState;
                    cs.setSpawnedType(type);
                    bsm.setBlockState((BlockState)cs);
                    stk.setItemMeta((ItemMeta)bsm);
                }
                this.applyDisplayAndLore(stk, entryKey, price, null);
                this.items.add(stk);
            }
            catch (IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Unknown spawner entity '" + spm.group(1) + "' in categories." + this.categoryKey);
            }
            return;
        }
        Matcher enchM = ENCH_PATTERN.matcher(entryKey);
        if (enchM.matches() && entryKey.endsWith("-value")) {
            String enKeyRaw = enchM.group(1);
            int lvl = Integer.parseInt(enchM.group(2));
            String prettyEn = this.prettyName(enKeyRaw);
            Enchantment found = Arrays.stream(Enchantment.values()).filter(e -> e.getKey().getKey().equalsIgnoreCase(enKeyRaw)).findFirst().orElse(null);
            ItemStack stk = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta)stk.getItemMeta();
            if (esm != null) {
                if (found != null) {
                    esm.addStoredEnchant(found, lvl, true);
                } else {
                    esm.setDisplayName("Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")");
                }
                esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
                stk.setItemMeta((ItemMeta)esm);
            }
            this.applyDisplayAndLore(stk, entryKey, price, found == null ? "Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")" : null);
            this.items.add(stk);
            return;
        }
        String matName = entryKey.replace("-value", "");
        Material mat = Material.matchMaterial((String)matName);
        if (mat != null) {
            ItemStack stk = new ItemStack(mat);
            this.applyDisplayAndLore(stk, entryKey, price, null);
            this.items.add(stk);
            return;
        }
        Matcher potM = POTION_PATTERN.matcher(entryKey);
        if (potM.matches()) {
            String splashOrLingering = potM.group(1);
            String longOrStrong = potM.group(2);
            String effectName = potM.group(3);
            boolean extended = "long".equalsIgnoreCase(longOrStrong);
            boolean upgraded = "strong".equalsIgnoreCase(longOrStrong);
            PotionType type = this.resolvePotionType(effectName);
            if (type == null) {
                this.plugin.getLogger().warning("Unknown potion effect '" + effectName + "' for key '" + entryKey + "' in categories." + this.categoryKey + " \u2014 skipping potion item.");
                return;
            }
            Material potMat = Material.POTION;
            if ("splash".equalsIgnoreCase(splashOrLingering)) {
                potMat = Material.SPLASH_POTION;
            } else if ("lingering".equalsIgnoreCase(splashOrLingering)) {
                potMat = Material.LINGERING_POTION;
            }
            ItemStack stk = new ItemStack(potMat);
            PotionMeta pm = (PotionMeta)stk.getItemMeta();
            if (pm != null) {
                pm.setBasePotionData(new PotionData(type, extended, upgraded));
                pm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
                stk.setItemMeta((ItemMeta)pm);
            }
            this.applyDisplayAndLore(stk, entryKey, price, null);
            this.items.add(stk);
            return;
        }
        this.plugin.getLogger().warning("Unrecognized entry key '" + entryKey + "' in categories." + this.categoryKey + " \u2014 not a spawner, book, material, or known potion.");
    }

    public void open(Player p, int page) {
        int perPage = (this.rows - 1) * 9;
        int start = page * perPage;
        int end = Math.min(start + perPage, this.items.size());
        String titleTpl = this.customTitles.getOrDefault(this.categoryKey, this.defaultTitleTpl);
        String title = titleTpl.replace("%item%", this.prettyName(this.categoryKey));
        Inventory inv = Bukkit.createInventory((InventoryHolder)new GuiHolder(this.categoryKey, page), (int)(this.rows * 9), (String)title);
        for (int i = start; i < end; ++i) {
            inv.setItem(i - start, this.items.get(i));
        }
        if (this.prevMat != null) {
            inv.setItem(this.prevSlot, this.buildButton(this.prevMat, this.prevName, this.prevLore));
        }
        if (this.nextMat != null) {
            inv.setItem(this.nextSlot, this.buildButton(this.nextMat, this.nextName, this.nextLore));
        }
        if (this.backMat != null) {
            inv.setItem(this.backSlot, this.buildButton(this.backMat, this.backName, this.backLore));
        }
        p.openInventory(inv);
        try {
            Sound sound = Sound.valueOf((String)this.pageSwitchSoundName);
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }
        catch (IllegalArgumentException ex) {
            this.plugin.getLogger().warning("Invalid sound '" + this.pageSwitchSoundName + "', defaulting to ITEM_BOOK_PAGE_TURN");
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private void applyDisplayAndLore(ItemStack stk, String entryKey, double price, String customName) {
        ItemMeta im = stk.getItemMeta();
        if (im == null) {
            return;
        }
        if (customName != null) {
            im.setDisplayName(customName);
        } else if (!im.hasDisplayName()) {
            String base = this.prettyName(entryKey.replace("-value", ""));
            im.setDisplayName(this.itemNameTpl.replace("%item%", base));
        }
        ArrayList<String> lore = new ArrayList<String>();
        for (String line : this.itemLoreTpl) {
            lore.add(Utils.formatColors(line.replace("%item-value%", Utils.abbreviateNumber(price))));
        }
        im.setLore(lore);
        stk.setItemMeta(im);
    }

    private ItemStack buildButton(Material mat, String name, List<String> lore) {
        ItemStack b = new ItemStack(mat);
        ItemMeta m = b.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            m.setLore(lore);
            b.setItemMeta(m);
        }
        return b;
    }

    private String prettyName(String raw) {
        String s = raw.replace('-', '_');
        StringBuilder out = new StringBuilder();
        for (String part : s.split("_")) {
            if (part.isEmpty()) continue;
            String low = part.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(low.charAt(0))).append(low.substring(1)).append(" ");
        }
        return out.toString().trim();
    }

    private PotionType safePotionType(String ... names) {
        for (String s : names) {
            try {
                return PotionType.valueOf((String)s);
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
        }
        return null;
    }

    private PotionType resolvePotionType(String effectName) {
        String n;
        switch (n = effectName.toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "leaping": {
                return this.safePotionType("JUMP", "LEAPING");
            }
            case "swiftness": 
            case "speed": {
                return this.safePotionType("SPEED", "SWIFTNESS");
            }
            case "healing": {
                return this.safePotionType("INSTANT_HEAL", "HEALING");
            }
            case "harming": {
                return this.safePotionType("INSTANT_DAMAGE", "HARMING");
            }
            case "regeneration": {
                return this.safePotionType("REGENERATION", "REGEN");
            }
            case "water": 
            case "potion": {
                return this.safePotionType("WATER");
            }
        }
        PotionType direct = this.safePotionType(n.toUpperCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        if (n.equals("regen")) {
            return this.safePotionType("REGENERATION");
        }
        if (n.equals("instant_heal")) {
            return this.safePotionType("HEALING");
        }
        if (n.equals("instant_damage")) {
            return this.safePotionType("HARMING");
        }
        if (n.equals("jump_boost")) {
            return this.safePotionType("JUMP", "LEAPING");
        }
        return null;
    }

    private String toRoman(int number) {
        if (number <= 0) {
            return String.valueOf(number);
        }
        int l = ROMAN.floorKey(number);
        if (number == l) {
            return ROMAN.get(number);
        }
        return ROMAN.get(l) + this.toRoman(number - l);
    }

    static {
        ROMAN.put(1000, "M");
        ROMAN.put(900, "CM");
        ROMAN.put(500, "D");
        ROMAN.put(400, "CD");
        ROMAN.put(100, "C");
        ROMAN.put(90, "XC");
        ROMAN.put(50, "L");
        ROMAN.put(40, "XL");
        ROMAN.put(10, "X");
        ROMAN.put(9, "IX");
        ROMAN.put(5, "V");
        ROMAN.put(4, "IV");
        ROMAN.put(1, "I");
    }
}

