package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
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
import ro.andreilarazboi.donutcore.DonutCore;

public final class SellAxeCommand implements CommandExecutor, TabCompleter {
    private final DonutSell plugin;
    private final NamespacedKey sellAxeKey;
    private final NamespacedKey expiryKey;

    public SellAxeCommand(DonutSell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey) {
        this.plugin = plugin;
        this.sellAxeKey = sellAxeKey;
        this.expiryKey = expiryKey;
        plugin.getPlugin().getCommand("donutsell").setExecutor(this);
        plugin.getPlugin().getCommand("donutsell").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!DonutCore.getInstance().getSellModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage(Utils.toComponent("&cThe Sell module is currently disabled."));
            }
            return true;
        }
        if (!sender.hasPermission("sell.admin")) {
            sender.sendMessage(Utils.toComponent("&cYou do not have permission."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Utils.toComponent("&cUsage: /donutsell <givesellwand|reload|resetall|addhanditem|prices>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadPlugin();
            sender.sendMessage(Utils.toComponent("&aSell config reloaded."));
            return true;
        }
        if (args[0].equalsIgnoreCase("prices")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Utils.toComponent("&cOnly players can open the price editor."));
                return true;
            }
            this.plugin.getAdminPriceEditorMenu().open(p, 1);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetall")) {
            String targetName = args[1];
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact(targetName) == null) {
                sender.sendMessage(Utils.toComponent("&cPlayer &e" + targetName + " &cnot found."));
                return true;
            }
            if (sender instanceof Player adminPlayer) {
                this.plugin.getResetConfirmationGui().open(adminPlayer, offline);
            } else {
                sender.sendMessage(Utils.toComponent("&cOnly in-game players can confirm a reset."));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Utils.toComponent("&cOnly players can use this."));
                return true;
            }
            String category = args[1].toLowerCase(Locale.ROOT);
            FileConfiguration cfg = this.plugin.getWorthConfig();
            ConfigurationSection catsSection = cfg.getConfigurationSection("categories");
            if (catsSection == null || !catsSection.getKeys(false).contains(category)) {
                sender.sendMessage(Utils.toComponent("&cUnknown category: " + category));
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[2]);
                if (price < 0.0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Utils.toComponent("&cPlease specify a valid non-negative number for price."));
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                sender.sendMessage(Utils.toComponent("&cHold an item to set its price."));
                return true;
            }
            String entryKey;
            ItemMeta handMeta = hand.getItemMeta();
            if (hand.getType() == Material.SPAWNER && handMeta instanceof BlockStateMeta bsm) {
                BlockState blockState = bsm.getBlockState();
                if (blockState instanceof CreatureSpawner cs) {
                    entryKey = cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
                } else {
                    entryKey = "spawner-value";
                }
            } else if (hand.getType() == Material.ENCHANTED_BOOK && handMeta instanceof EnchantmentStorageMeta esm) {
                if (esm.getStoredEnchants().size() != 1) {
                    sender.sendMessage(Utils.toComponent("&cHold an enchanted book with exactly one enchantment."));
                    return true;
                }
                Map.Entry<Enchantment, Integer> e = esm.getStoredEnchants().entrySet().iterator().next();
                entryKey = e.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + e.getValue() + "-value";
            } else if (handMeta instanceof PotionMeta pm) {
                String base = pm.getBasePotionType().getKey().getKey();
                if (hand.getType() == Material.SPLASH_POTION) base = "splash_" + base;
                else if (hand.getType() == Material.LINGERING_POTION) base = "lingering_" + base;
                entryKey = base + "-value";
            } else {
                entryKey = hand.getType().name().toLowerCase(Locale.ROOT) + "-value";
            }
            String path = "categories." + category;
            List<Map<?, ?>> rawList = cfg.getMapList(path);
            ArrayList<Map<String, Object>> newList = new ArrayList<>();
            for (Map<?, ?> m : rawList) {
                HashMap<String, Object> copy = new HashMap<>();
                for (Map.Entry<?, ?> en : m.entrySet()) {
                    copy.put(String.valueOf(en.getKey()), en.getValue());
                }
                newList.add(copy);
            }
            boolean replaced = false;
            for (Map<String, Object> map : newList) {
                if (!map.containsKey(entryKey)) continue;
                map.put(entryKey, price);
                replaced = true;
                break;
            }
            if (!replaced) {
                HashMap<String, Object> toAdd = new HashMap<>();
                toAdd.put(entryKey, price);
                newList.add(toAdd);
            }
            cfg.set(path, newList);
            this.plugin.saveWorthConfig();
            this.plugin.reloadPlugin();
            sender.sendMessage(Utils.toComponent("&aSet &e" + entryKey + " &ain category &e" + category + " &ato &e" + price));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givesellwand")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Utils.toComponent("&cPlayer not found: " + args[1]));
                return true;
            }
            ItemStack sellAxe = new ItemStack(Material.NETHERITE_AXE, 1);
            ItemMeta meta = sellAxe.getItemMeta();
            if (meta == null) {
                this.plugin.getPlugin().getLogger().warning("Failed to create Sell Wand item!");
                return true;
            }
            String rawName = this.plugin.getConfig().getString("sell-axe.display-name", "&aSell Wand");
            meta.displayName(Utils.toComponent(rawName));
            List<String> loreTemplate = new ArrayList<>(this.plugin.getConfig().getStringList("sell-axe.lore"));
            boolean useCountdown = this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true);
            long expiryMillis = 0L;
            List<Component> loreComponents = new ArrayList<>();
            if (useCountdown) {
                long durationSeconds = this.plugin.getConfig().getLong("sell-axe.duration-seconds", 259200L);
                expiryMillis = System.currentTimeMillis() + durationSeconds * 1000L;
                String initialCountdown = this.formatDuration(durationSeconds * 1000L);
                for (String line : loreTemplate) {
                    loreComponents.add(Utils.toComponent(line.replace("%countdown%", initialCountdown)));
                }
            } else {
                for (String line : loreTemplate) {
                    loreComponents.add(Utils.toComponent(line));
                }
            }
            meta.lore(loreComponents);
            List<String> enchList = this.plugin.getConfig().getStringList("sell-axe.enchantments");
            for (String enchEntry : enchList) {
                String[] parts = enchEntry.split(":");
                if (parts.length != 2) continue;
                try {
                    Enchantment ench = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                            .get(NamespacedKey.minecraft(parts[0].toLowerCase(Locale.ROOT).trim()));
                    if (ench == null) continue;
                    meta.addEnchant(ench, Integer.parseInt(parts[1].trim()), true);
                } catch (NumberFormatException ignored) {}
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(this.sellAxeKey, PersistentDataType.BYTE, (byte) 1);
            if (useCountdown) {
                pdc.set(this.expiryKey, PersistentDataType.LONG, expiryMillis);
            }
            sellAxe.setItemMeta(meta);
            target.getInventory().addItem(sellAxe);
            return true;
        }
        sender.sendMessage(Utils.toComponent("&cUsage: /donutsell <givesellwand|reload|resetall|addhanditem|prices>"));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("sell.admin")) return Collections.emptyList();
        if (args.length == 1) {
            return filter(Arrays.asList("givesellwand", "reload", "resetall", "addhanditem", "prices"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givesellwand") || args[0].equalsIgnoreCase("resetall")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("addhanditem")) {
                ConfigurationSection cats = this.plugin.getWorthConfig().getConfigurationSection("categories");
                if (cats == null) return Collections.emptyList();
                return filter(new ArrayList<>(cats.getKeys(false)), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
            return Collections.singletonList("0.1");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        if (prefix.isEmpty()) return options;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(o);
        }
        return out;
    }
}
