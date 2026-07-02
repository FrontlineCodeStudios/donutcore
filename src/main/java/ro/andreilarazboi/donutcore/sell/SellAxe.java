package ro.andreilarazboi.donutcore.sell;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SellAxe implements Listener {
    private final DonutSell plugin;
    private final NamespacedKey sellAxeKey;
    private final NamespacedKey expiryKey;

    public SellAxe(DonutSell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey) {
        this.plugin = plugin;
        this.sellAxeKey = sellAxeKey;
        this.expiryKey = expiryKey;
        plugin.getPlugin().getServer().getPluginManager().registerEvents(this, plugin.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        BlockState state = block.getState();
        boolean isChest = state instanceof Chest;
        boolean isShulker = state instanceof ShulkerBox;
        if (!isChest && !isShulker) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType().isAir()) return;
        ItemMeta meta = inHand.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte marker = pdc.get(this.sellAxeKey, PersistentDataType.BYTE);
        if (marker == null || marker != 1) return;
        if (this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true)) {
            Long expiry = pdc.get(this.expiryKey, PersistentDataType.LONG);
            if (expiry == null || System.currentTimeMillis() >= expiry) {
                player.getInventory().remove(inHand);
                event.setCancelled(true);
                String msg = this.plugin.getMessagesConfig().getString("messages.expired-wand", "&cYour Sell Wand has expired and been removed.");
                player.sendMessage(Utils.toComponent(msg));
                return;
            }
        }
        Inventory containerInv = isChest ? ((Chest) state).getInventory() : ((ShulkerBox) state).getInventory();
        HashMap<String, DonutSell.Stats> sold = new HashMap<>();
        HashMap<String, Double> revCats = new HashMap<>();
        for (ItemStack item : containerInv.getContents()) {
            if (item == null || item.getType().isAir()) continue;
            ItemMeta im = item.getItemMeta();
            if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta esm) {
                for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                    String key = entry.getKey().getKey().getKey().toLowerCase() + entry.getValue();
                    double total = this.plugin.getPrice(key + "-value") * item.getAmount();
                    sold.merge(key, new DonutSell.Stats(item.getAmount(), total),
                            (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(key.toUpperCase(Locale.ROOT))) continue;
                        revCats.merge(cat.getKey(), total, (a, b) -> a + b);
                    }
                }
                continue;
            }
            if (im instanceof BlockStateMeta bsm) {
                BlockState blockState = bsm.getBlockState();
                if (blockState instanceof ShulkerBox nested) {
                    String string = item.getType().name().toLowerCase();
                    double boxValue = this.plugin.getPrice(string + "-value") * item.getAmount();
                    sold.merge(string, new DonutSell.Stats(item.getAmount(), boxValue),
                            (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(item.getType().name())) continue;
                        revCats.merge(cat.getKey(), boxValue, (a, b) -> a + b);
                    }
                    for (ItemStack inside : nested.getInventory().getContents()) {
                        if (inside == null || inside.getType().isAir()) continue;
                        ItemMeta innerMeta = inside.getItemMeta();
                        if (inside.getType() == Material.ENCHANTED_BOOK && innerMeta instanceof EnchantmentStorageMeta innerEsm) {
                            for (Map.Entry<Enchantment, Integer> e3 : innerEsm.getStoredEnchants().entrySet()) {
                                String key = e3.getKey().getKey().getKey().toLowerCase() + e3.getValue();
                                double val = this.plugin.getPrice(key + "-value") * inside.getAmount();
                                sold.merge(key, new DonutSell.Stats(inside.getAmount(), val),
                                        (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                                for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                    if (!cat.getValue().contains(key.toUpperCase(Locale.ROOT))) continue;
                                    revCats.merge(cat.getKey(), val, (a, b) -> a + b);
                                }
                            }
                            continue;
                        }
                        String innerKey = inside.getType().name().toLowerCase();
                        double val = this.plugin.calculateItemWorth(inside);
                        sold.merge(innerKey, new DonutSell.Stats(inside.getAmount(), val),
                                (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                        for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                            if (!cat.getValue().contains(inside.getType().name())) continue;
                            revCats.merge(cat.getKey(), val, (a, b) -> a + b);
                        }
                    }
                    continue;
                }
            }
            String key = item.getType().name().toLowerCase();
            double value = this.plugin.calculateItemWorth(item);
            sold.merge(key, new DonutSell.Stats(item.getAmount(), value),
                    (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
            for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                if (!cat.getValue().contains(item.getType().name())) continue;
                revCats.merge(cat.getKey(), value, (a, b) -> a + b);
            }
        }
        if (sold.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(Utils.toComponent(this.plugin.getMessagesConfig().getString("messages.empty-chest", "&7Chest is empty – nothing to sell.")));
            return;
        }
        this.plugin.recordSale(player, sold);
        double payout = revCats.entrySet().stream()
                .mapToDouble(e -> e.getValue() * this.plugin.getSellMultiplier(player.getUniqueId(), e.getKey()))
                .sum();
        double uncategorized = sold.entrySet().stream()
                .filter(e -> this.plugin.categoryItems.values().stream()
                        .noneMatch(list -> list.contains(e.getKey().toUpperCase(Locale.ROOT))))
                .mapToDouble(e -> e.getValue().revenue)
                .sum();
        payout += uncategorized;
        this.plugin.getEconomy().depositPlayer((OfflinePlayer) player, payout);
        player.playSound(player.getLocation(),
                Utils.resolveSound(this.plugin.getMenusConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
                1.0f, 1.0f);
        String actionbar = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.actionbar-message", "&aSold $%amount%"))
                .replace("%amount%", Utils.abbreviateNumber(payout));
        player.sendActionBar(Utils.toComponent(actionbar));
        String chatMsg = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.chat-message", "&7[DonutCore]&r $%amount%"))
                .replace("%amount%", Utils.abbreviateNumber(payout));
        player.sendMessage(Utils.toComponent(chatMsg));
        event.setCancelled(true);
        containerInv.clear();
    }
}
