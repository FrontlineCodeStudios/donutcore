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
import net.kyori.adventure.text.Component;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

public class CategoryGui {
    private final DonutSell plugin;
    private final String categoryKey;
    private final List<ItemStack> items = new ArrayList<>();
    private final int rows;
    private final String defaultTitleTpl;
    private final Map<String, String> customTitles = new HashMap<>();
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

    static {
        ROMAN.put(1000, "M"); ROMAN.put(900, "CM"); ROMAN.put(500, "D");
        ROMAN.put(400, "CD"); ROMAN.put(100, "C"); ROMAN.put(90, "XC");
        ROMAN.put(50, "L"); ROMAN.put(40, "XL"); ROMAN.put(10, "X");
        ROMAN.put(9, "IX"); ROMAN.put(5, "V"); ROMAN.put(4, "IV"); ROMAN.put(1, "I");
    }

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
        this.prevMat = Material.matchMaterial(cfg.getString("previous-page.material"));
        this.prevName = Utils.formatColors(cfg.getString("previous-page.displayname", "&aPrevious"));
        this.prevLore = Utils.formatColors(cfg.getStringList("previous-page.lore"));
        this.nextSlot = cfg.getInt("next-page-slot", 51);
        this.nextMat = Material.matchMaterial(cfg.getString("next-page.material"));
        this.nextName = Utils.formatColors(cfg.getString("next-page.displayname", "&aNext"));
        this.nextLore = Utils.formatColors(cfg.getStringList("next-page.lore"));
        this.backSlot = cfg.getInt("back-button.slot", 45);
        this.backMat = Material.matchMaterial(cfg.getString("back-button.material"));
        this.backName = Utils.formatColors(cfg.getString("back-button.displayname", "&cBack"));
        this.backLore = Utils.formatColors(cfg.getStringList("back-button.lore"));
        this.itemNameTpl = Utils.formatColors(cfg.getString("item.displayname", "%item%"));
        this.itemLoreTpl = cfg.getStringList("item.lore");
        String configured = plugin.getConfig().getString("sounds.page-switch", "ITEM_BOOK_PAGE_TURN");
        this.pageSwitchSoundName = configured != null ? configured.toUpperCase(Locale.ROOT) : "ITEM_BOOK_PAGE_TURN";
        Object rawNode = plugin.getWorthConfig().get("categories." + this.categoryKey);
        if (rawNode instanceof ConfigurationSection catSec) {
            for (String entryKey : catSec.getKeys(false)) {
                double price = catSec.getDouble(entryKey, -1.0);
                if (price < 0.0) {
                    plugin.getPlugin().getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
                    continue;
                }
                this.handleEntry(entryKey, price);
            }
        } else if (rawNode instanceof List<?> rawList) {
            for (Object o : rawList) {
                if (!(o instanceof Map<?, ?> map)) continue;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String entryKey = (String) e.getKey();
                    double price;
                    try {
                        price = Double.parseDouble(e.getValue().toString());
                    } catch (NumberFormatException ex) {
                        plugin.getPlugin().getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
                        continue;
                    }
                    this.handleEntry(entryKey, price);
                }
            }
        } else {
            plugin.getPlugin().getLogger().warning("No entries found for categories." + categoryKey);
        }
    }

    private void handleEntry(String entryKey, double price) {
        Matcher spm = SPAWNER_PATTERN.matcher(entryKey);
        if (spm.matches()) {
            try {
                EntityType type = EntityType.valueOf(spm.group(1).toUpperCase(Locale.ROOT));
                ItemStack stk = new ItemStack(Material.SPAWNER);
                BlockStateMeta bsm = (BlockStateMeta) stk.getItemMeta();
                if (bsm != null) {
                    BlockState blockState = bsm.getBlockState();
                    if (blockState instanceof CreatureSpawner cs) {
                        cs.setSpawnedType(type);
                        bsm.setBlockState(cs);
                        stk.setItemMeta(bsm);
                    }
                }
                this.applyDisplayAndLore(stk, entryKey, price, null);
                this.items.add(stk);
            } catch (IllegalArgumentException ex) {
                this.plugin.getPlugin().getLogger().warning("Unknown spawner entity '" + spm.group(1) + "' in categories." + this.categoryKey);
            }
            return;
        }
        Matcher enchM = ENCH_PATTERN.matcher(entryKey);
        if (enchM.matches() && entryKey.endsWith("-value")) {
            String enKeyRaw = enchM.group(1);
            int lvl = Integer.parseInt(enchM.group(2));
            String prettyEn = this.prettyName(enKeyRaw);
            Enchantment found = Arrays.stream(Enchantment.values())
                    .filter(e -> e.getKey().getKey().equalsIgnoreCase(enKeyRaw))
                    .findFirst().orElse(null);
            ItemStack stk = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) stk.getItemMeta();
            if (esm != null) {
                if (found != null) {
                    esm.addStoredEnchant(found, lvl, true);
                } else {
                    esm.displayName(Utils.toComponent("Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")"));
                }
                esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
                stk.setItemMeta(esm);
            }
            this.applyDisplayAndLore(stk, entryKey, price,
                    found == null ? "Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")" : null);
            this.items.add(stk);
            return;
        }
        String matName = entryKey.replace("-value", "");
        Material mat = Material.matchMaterial(matName);
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
            // Build full PotionType name (e.g. long + swiftness -> LONG_SWIFTNESS)
            String fullTypeName = (longOrStrong != null ? longOrStrong.toUpperCase(Locale.ROOT) + "_" : "")
                    + effectName.toUpperCase(Locale.ROOT);
            PotionType type = this.resolvePotionType(fullTypeName);
            if (type == null) type = this.resolvePotionType(effectName);
            if (type == null) {
                this.plugin.getPlugin().getLogger().warning("Unknown potion effect '" + effectName + "' for key '" + entryKey + "' in categories." + this.categoryKey + " — skipping.");
                return;
            }
            Material potMat = Material.POTION;
            if ("splash".equalsIgnoreCase(splashOrLingering)) potMat = Material.SPLASH_POTION;
            else if ("lingering".equalsIgnoreCase(splashOrLingering)) potMat = Material.LINGERING_POTION;
            ItemStack stk = new ItemStack(potMat);
            PotionMeta pm = (PotionMeta) stk.getItemMeta();
            if (pm != null) {
                pm.setBasePotionType(type);
                pm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
                stk.setItemMeta(pm);
            }
            this.applyDisplayAndLore(stk, entryKey, price, null);
            this.items.add(stk);
            return;
        }
        this.plugin.getPlugin().getLogger().warning("Unrecognized entry key '" + entryKey + "' in categories." + this.categoryKey + " — skipping.");
    }

    public void open(Player p, int page) {
        int perPage = (this.rows - 1) * 9;
        int start = page * perPage;
        int end = Math.min(start + perPage, this.items.size());
        String titleTpl = this.customTitles.getOrDefault(this.categoryKey, this.defaultTitleTpl);
        String title = titleTpl.replace("%item%", this.prettyName(this.categoryKey));
        Inventory inv = Bukkit.createInventory(new GuiHolder(this.categoryKey, page), this.rows * 9, title);
        for (int i = start; i < end; ++i) {
            inv.setItem(i - start, this.items.get(i));
        }
        if (this.prevMat != null) inv.setItem(this.prevSlot, this.buildButton(this.prevMat, this.prevName, this.prevLore));
        if (this.nextMat != null) inv.setItem(this.nextSlot, this.buildButton(this.nextMat, this.nextName, this.nextLore));
        if (this.backMat != null) inv.setItem(this.backSlot, this.buildButton(this.backMat, this.backName, this.backLore));
        p.openInventory(inv);
        try {
            p.playSound(p.getLocation(), Sound.valueOf(this.pageSwitchSoundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            this.plugin.getPlugin().getLogger().warning("Invalid sound '" + this.pageSwitchSoundName + "', defaulting to ITEM_BOOK_PAGE_TURN");
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private void applyDisplayAndLore(ItemStack stk, String entryKey, double price, String customName) {
        ItemMeta im = stk.getItemMeta();
        if (im == null) return;
        if (customName != null) {
            im.displayName(Utils.toComponent(customName));
        } else if (!im.hasDisplayName()) {
            String base = this.prettyName(entryKey.replace("-value", ""));
            im.displayName(Utils.toComponent(this.itemNameTpl.replace("%item%", base)));
        }
        ArrayList<Component> lore = new ArrayList<>();
        for (String line : this.itemLoreTpl) {
            lore.add(Utils.toComponent(line.replace("%item-value%", Utils.abbreviateNumber(price))));
        }
        im.lore(lore);
        stk.setItemMeta(im);
    }

    private ItemStack buildButton(Material mat, String name, List<String> lore) {
        ItemStack b = new ItemStack(mat);
        ItemMeta m = b.getItemMeta();
        if (m != null) {
            m.displayName(Utils.toComponent(name));
            m.lore(Utils.toComponents(lore));
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

    private PotionType safePotionType(String... names) {
        for (String s : names) {
            try { return PotionType.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private PotionType resolvePotionType(String name) {
        String n = name.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (n) {
            case "leaping", "jump", "jump_boost" -> safePotionType("LEAPING");
            case "long_leaping", "long_jump", "long_jump_boost" -> safePotionType("LONG_LEAPING");
            case "strong_leaping", "strong_jump", "strong_jump_boost" -> safePotionType("STRONG_LEAPING");
            case "swiftness", "speed" -> safePotionType("SWIFTNESS");
            case "long_swiftness", "long_speed" -> safePotionType("LONG_SWIFTNESS");
            case "strong_swiftness", "strong_speed" -> safePotionType("STRONG_SWIFTNESS");
            case "healing", "instant_heal" -> safePotionType("HEALING");
            case "strong_healing", "strong_instant_heal" -> safePotionType("STRONG_HEALING");
            case "harming", "instant_damage" -> safePotionType("HARMING");
            case "strong_harming", "strong_instant_damage" -> safePotionType("STRONG_HARMING");
            case "regeneration", "regen" -> safePotionType("REGENERATION");
            case "long_regeneration", "long_regen" -> safePotionType("LONG_REGENERATION");
            case "strong_regeneration", "strong_regen" -> safePotionType("STRONG_REGENERATION");
            case "water", "potion" -> safePotionType("WATER");
            default -> safePotionType(n.toUpperCase(Locale.ROOT));
        };
    }

    private String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        int l = ROMAN.floorKey(number);
        if (number == l) return ROMAN.get(number);
        return ROMAN.get(l) + this.toRoman(number - l);
    }
}
