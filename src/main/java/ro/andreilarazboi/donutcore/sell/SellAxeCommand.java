package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@SuppressWarnings({"deprecation", "removal"})
public class SellAxeCommand
implements CommandExecutor,
TabCompleter {
    private final DonutSell plugin;
    private final NamespacedKey sellAxeKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey usesRemainingKey;

    public SellAxeCommand(DonutSell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey, NamespacedKey usesRemainingKey) {
        this.plugin = plugin;
        this.sellAxeKey = sellAxeKey;
        this.expiryKey = expiryKey;
        this.usesRemainingKey = usesRemainingKey;
        plugin.getCommand("donutsell").setExecutor((CommandExecutor)this);
        plugin.getCommand("donutsell").setTabCompleter((TabCompleter)this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!this.plugin.isModuleActive()) {
            sender.sendMessage(Utils.formatColors("&cThis module is currently disabled."));
            return true;
        }
        if (!sender.hasPermission("sell.admin")) {
            sender.sendMessage(Utils.formatColors("&cYou do not have permission."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Utils.formatColors("&cUsage: /donutsell <givesellwand|reload|resetall|addhanditem|prices>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadPlugin();
            sender.sendMessage(Utils.formatColors("&aSell config reloaded."));
            return true;
        }
        if (args[0].equalsIgnoreCase("prices")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Utils.formatColors("&cOnly players can open the price editor."));
                return true;
            }
            Player p = (Player)sender;
            this.plugin.getAdminPriceEditorMenu().open(p, 1);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetall")) {
            String targetName = args[1];
            OfflinePlayer offline = Bukkit.getOfflinePlayer((String)targetName);
            if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact((String)targetName) == null) {
                sender.sendMessage(Utils.formatColors("&cPlayer &e" + targetName + " &cnot found."));
                return true;
            }
            if (sender instanceof Player) {
                Player adminPlayer = (Player)sender;
                this.plugin.getResetConfirmationGui().open(adminPlayer, offline);
            } else {
                sender.sendMessage(Utils.formatColors("&cOnly in-game players can confirm a reset."));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
            double price;
            if (!(sender instanceof Player)) {
                sender.sendMessage(Utils.formatColors("&cOnly players can use this."));
                return true;
            }
            Player player = (Player)sender;
            String category = args[1].toLowerCase(Locale.ROOT);
            FileConfiguration cfg = this.plugin.getWorthConfig();
            ConfigurationSection catsSection = cfg.getConfigurationSection("categories");
            if (catsSection == null || !catsSection.getKeys(false).contains(category)) {
                sender.sendMessage(Utils.formatColors("&cUnknown category: " + category));
                return true;
            }
            try {
                price = Double.parseDouble(args[2]);
                if (price < 0.0) {
                    throw new NumberFormatException();
                }
            }
            catch (NumberFormatException e) {
                sender.sendMessage(Utils.formatColors("&cPlease specify a valid non-negative number for price."));
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                sender.sendMessage(Utils.formatColors("&cHold an item to set its price."));
                return true;
            }
            String entryKey;
            if (hand.getType() == Material.SPAWNER && hand.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) hand.getItemMeta();
                BlockState blockState = bsm.getBlockState();
                if (blockState instanceof CreatureSpawner) {
                    CreatureSpawner cs = (CreatureSpawner) blockState;
                    entryKey = cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
                } else {
                    entryKey = "spawner-value";
                }
            } else if (hand.getType() == Material.ENCHANTED_BOOK && hand.getItemMeta() instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) hand.getItemMeta();
                if (esm.getStoredEnchants().size() != 1) {
                    sender.sendMessage(Utils.formatColors("&cHold an enchanted book with exactly one enchantment."));
                    return true;
                }
                Map.Entry<Enchantment, Integer> entry = esm.getStoredEnchants().entrySet().iterator().next();
                entryKey = entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(entry.getValue()) + "-value";
            } else {
                ItemMeta meta = hand.getItemMeta();
                if (meta instanceof PotionMeta) {
                    PotionMeta pm = (PotionMeta) meta;
                    String base = pm.getBasePotionData().getType().name().toLowerCase(Locale.ROOT);
                    if (pm.getBasePotionData().isExtended()) {
                        base = "long_" + base;
                    }
                    if (pm.getBasePotionData().isUpgraded()) {
                        base = "strong_" + base;
                    }
                    if (hand.getType() == Material.SPLASH_POTION) {
                        base = "splash_" + base;
                    } else if (hand.getType() == Material.LINGERING_POTION) {
                        base = "lingering_" + base;
                    }
                    entryKey = base + "-value";
                } else {
                    entryKey = hand.getType().name().toLowerCase(Locale.ROOT) + "-value";
                }
            }
            String path = "categories." + category;
            List<Map<?, ?>> rawList = cfg.getMapList(path);
            List<Map<String, Object>> newList = new ArrayList<>();
            for (Map<?, ?> m : rawList) {
                Map<String, Object> hashMap = new HashMap<>();
                for (Map.Entry<?, ?> en : m.entrySet()) {
                    hashMap.put(String.valueOf(en.getKey()), en.getValue());
                }
                newList.add(hashMap);
            }
            boolean replaced = false;
            for (Map<String, Object> map : newList) {
                if (!map.containsKey(entryKey)) continue;
                map.put(entryKey, price);
                replaced = true;
                break;
            }
            if (!replaced) {
                Map<String, Object> toAdd = new HashMap<>();
                toAdd.put(entryKey, price);
                newList.add(toAdd);
            }
            cfg.set(path, newList);
            this.plugin.saveWorthConfig();
            this.plugin.reloadPlugin();
            sender.sendMessage(Utils.formatColors("&aSet &e" + entryKey + " &ain category &e" + category + " &ato &e" + price));
            return true;
        }
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("givesellwand")) {
            List<String> enchList;
            ItemStack sellAxe;
            ItemMeta meta;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Utils.formatColors("&cPlayer not found: " + args[1]));
                return true;
            }
            int uses = this.plugin.getConfig().getInt("sell-axe.amount", -1);
            if (args.length == 3) {
                try {
                    uses = Integer.parseInt(args[2]);
                    if (uses < -1) {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException ex) {
                    sender.sendMessage(Utils.formatColors("&cUses must be -1 or a positive whole number."));
                    return true;
                }
            }
            if ((meta = (sellAxe = new ItemStack(Material.NETHERITE_AXE, 1)).getItemMeta()) == null) {
                this.plugin.getLogger().warning("Failed to create Sell Wand item!");
                return true;
            }
            boolean useCountdown = this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true);
            long expiryMillis = 0L;
            String initialCountdown = "";
            if (useCountdown) {
                long durationSeconds = this.plugin.getConfig().getLong("sell-axe.duration-seconds", 259200L);
                expiryMillis = System.currentTimeMillis() + durationSeconds * 1000L;
                initialCountdown = this.formatDuration(durationSeconds * 1000L);
            }
            if ((enchList = this.plugin.getConfig().getStringList("sell-axe.enchantments")) != null) {
                for (String enchEntry : enchList) {
                    String[] parts = enchEntry.split(":");
                    if (parts.length != 2) continue;
                    try {
                        String enchName = parts[0].toUpperCase().trim();
                        int n = Integer.parseInt(parts[1].trim());
                        Enchantment ench = Enchantment.getByName((String)enchName);
                        if (ench == null) continue;
                        meta.addEnchant(ench, n, true);
                    }
                    catch (NumberFormatException numberFormatException) {}
                }
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(this.sellAxeKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(this.usesRemainingKey, PersistentDataType.INTEGER, uses);
            if (useCountdown) {
                pdc.set(this.expiryKey, PersistentDataType.LONG, expiryMillis);
            }
            this.plugin.updateSellAxeMeta(sellAxe, meta, initialCountdown);
            sellAxe.setItemMeta(meta);
            target.getInventory().addItem(new ItemStack[]{sellAxe});
            sender.sendMessage(Utils.formatColors("&aGave &e" + target.getName() + " &aa Sell Wand with &e" + this.plugin.formatSellAxeUses(uses) + " &auses."));
            return true;
        }
        sender.sendMessage(Utils.formatColors("&cUsage: /donutsell <givesellwand|reload|resetall|addhanditem|prices>"));
        return true;
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000L;
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("sell.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return this.filter(Arrays.asList("givesellwand", "reload", "resetall", "addhanditem", "prices"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givesellwand") || args[0].equalsIgnoreCase("resetall")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("addhanditem")) {
                ConfigurationSection cats = this.plugin.getWorthConfig().getConfigurationSection("categories");
                if (cats == null) {
                    return Collections.emptyList();
                }
                return this.filter(new ArrayList<String>(cats.getKeys(false)), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givesellwand")) {
            return this.filter(Arrays.asList("-1", "10", "100", "1000"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
            return Collections.singletonList("0.1");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        if (prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String o : options) {
            if (!o.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            out.add(o);
        }
        return out;
    }
}

