package ro.andreilarazboi.donutcore.sell;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.plugin.Plugin;

@SuppressWarnings({"deprecation", "removal"})
public class SellAxe
implements Listener {
    private final DonutSell plugin;
    private final NamespacedKey sellAxeKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey usesRemainingKey;

    public SellAxe(DonutSell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey, NamespacedKey usesRemainingKey) {
        this.plugin = plugin;
        this.sellAxeKey = sellAxeKey;
        this.expiryKey = expiryKey;
        this.usesRemainingKey = usesRemainingKey;
        plugin.getServer().getPluginManager().registerEvents((Listener)this, plugin.getPlugin());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!this.plugin.isModuleActive()) return;
        Long expiry;
        if (event.isCancelled()) {
            return;
        }
        Block block = event.getBlock();
        BlockState state = block.getState();
        boolean isChest = state instanceof Chest;
        boolean isShulker = state instanceof ShulkerBox;
        if (!isChest && !isShulker) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType().isAir()) {
            return;
        }
        ItemMeta meta = inHand.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte marker = (Byte)pdc.get(this.sellAxeKey, PersistentDataType.BYTE);
        if (marker == null || marker != 1) {
            return;
        }
        Integer remainingUses = (Integer)pdc.get(this.usesRemainingKey, PersistentDataType.INTEGER);
        if (remainingUses == null) {
            remainingUses = this.plugin.getConfig().getInt("sell-axe.amount", -1);
            pdc.set(this.usesRemainingKey, PersistentDataType.INTEGER, remainingUses);
            this.plugin.updateSellAxeMeta(inHand, meta, this.currentCountdown(meta));
            inHand.setItemMeta(meta);
        }
        if (remainingUses == 0) {
            this.destroyWand(player, inHand);
            event.setCancelled(true);
            player.sendMessage(Utils.formatColors(this.plugin.getMessagesConfig().getString("messages.expired-wand", "&cYour Sell Wand has no uses left and has been removed.")));
            return;
        }
        if (this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true) && ((expiry = (Long)pdc.get(this.expiryKey, PersistentDataType.LONG)) == null || System.currentTimeMillis() >= expiry)) {
            player.getInventory().remove(inHand);
            event.setCancelled(true);
            String msg = this.plugin.getMessagesConfig().getString("messages.expired-wand", "&cYour Sell Wand has expired and been removed.");
            player.sendMessage(Utils.formatColors(msg));
            return;
        }
        Inventory containerInv = isChest ? ((Chest)state).getInventory() : ((ShulkerBox)state).getInventory();
        HashMap<String, DonutSell.Stats> sold = new HashMap<String, DonutSell.Stats>();
        HashMap<String, Double> revCats = new HashMap<String, Double>();
        for (ItemStack item : containerInv.getContents()) {
            BlockStateMeta bsm;
            BlockState blockState;
            if (item == null || item.getType().isAir()) continue;
            ItemMeta im = item.getItemMeta();
            if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta)im;
                for (Map.Entry entry : esm.getStoredEnchants().entrySet()) {
                    String key = ((Enchantment)entry.getKey()).getKey().getKey().toLowerCase() + String.valueOf(entry.getValue());
                    double total = this.plugin.getPrice(key + "-value") * (double)item.getAmount();
                    sold.merge(key, new DonutSell.Stats(item.getAmount(), total), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(key.toUpperCase(Locale.ROOT))) continue;
                        revCats.merge(cat.getKey(), total, Double::sum);
                    }
                }
                continue;
            }
            if (im instanceof BlockStateMeta && (blockState = (bsm = (BlockStateMeta)im).getBlockState()) instanceof ShulkerBox) {
                ShulkerBox nested = (ShulkerBox)blockState;
                String string = item.getType().name().toLowerCase();
                double boxValue = this.plugin.getPrice(string + "-value") * (double)item.getAmount();
                sold.merge(string, new DonutSell.Stats(item.getAmount(), boxValue), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                    if (!cat.getValue().contains(item.getType().name())) continue;
                    revCats.merge(cat.getKey(), boxValue, Double::sum);
                }
                for (ItemStack inside : nested.getInventory().getContents()) {
                    if (inside == null || inside.getType().isAir()) continue;
                    ItemMeta innerMeta = inside.getItemMeta();
                    if (inside.getType() == Material.ENCHANTED_BOOK && innerMeta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta esm = (EnchantmentStorageMeta)innerMeta;
                        for (Map.Entry e3 : esm.getStoredEnchants().entrySet()) {
                            String key = ((Enchantment)e3.getKey()).getKey().getKey().toLowerCase() + String.valueOf(e3.getValue());
                            double val = this.plugin.getPrice(key + "-value") * (double)inside.getAmount();
                            sold.merge(key, new DonutSell.Stats(inside.getAmount(), val), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                            for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                if (!cat.getValue().contains(key.toUpperCase(Locale.ROOT))) continue;
                                revCats.merge(cat.getKey(), val, Double::sum);
                            }
                        }
                        continue;
                    }
                    String innerKey = inside.getType().name().toLowerCase();
                    double val = this.plugin.calculateItemWorth(inside);
                    sold.merge(innerKey, new DonutSell.Stats(inside.getAmount(), val), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(inside.getType().name())) continue;
                        revCats.merge(cat.getKey(), val, Double::sum);
                    }
                }
                continue;
            }
            String key = item.getType().name().toLowerCase();
            double value = this.plugin.calculateItemWorth(item);
            sold.merge(key, new DonutSell.Stats(item.getAmount(), value), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
            for (Map.Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                if (!cat.getValue().contains(item.getType().name())) continue;
                revCats.merge(cat.getKey(), value, Double::sum);
            }
        }
        if (sold.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(Utils.formatColors(this.plugin.getMessagesConfig().getString("messages.empty-chest", "&7Chest is empty \u2013 nothing to sell.")));
            return;
        }
        this.plugin.recordSale(player, sold);
        double payout = revCats.entrySet().stream().mapToDouble(e -> (Double)e.getValue() * this.plugin.getSellMultiplier(player.getUniqueId(), (String)e.getKey())).sum();
        double uncategorized = sold.entrySet().stream().filter(e -> this.plugin.categoryItems.values().stream().noneMatch(list -> list.contains(((String)e.getKey()).toUpperCase(Locale.ROOT)))).mapToDouble(e -> ((DonutSell.Stats)e.getValue()).revenue).sum();
        this.plugin.getEconomy().depositPlayer((OfflinePlayer)player, payout += uncategorized);
        player.playSound(player.getLocation(), Sound.valueOf((String)this.plugin.getMenusConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP")), 1.0f, 1.0f);
        String actionbar = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.actionbar-message", "&aSold $%amount%")).replace("%amount%", Utils.abbreviateNumber(payout));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)actionbar));
        String chatMsg = Utils.formatColors(this.plugin.getMenusConfig().getString("sell-menu.chat-message", "&7[Sell]&r $%amount%")).replace("%amount%", Utils.abbreviateNumber(payout));
        player.sendMessage(chatMsg);
        event.setCancelled(true);
        containerInv.clear();
        this.consumeUse(player, inHand);
    }

    private void consumeUse(Player player, ItemStack wand) {
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int remaining = this.plugin.getSellAxeUsesRemaining(meta);
        if (remaining < 0) {
            this.plugin.updateSellAxeMeta(wand, meta, this.currentCountdown(meta));
            wand.setItemMeta(meta);
            return;
        }
        if ((remaining = Math.max(0, remaining - 1)) <= 0) {
            this.destroyWand(player, wand);
            player.sendMessage(Utils.formatColors(this.plugin.getMessagesConfig().getString("messages.expired-wand", "&cYour Sell Wand has no uses left and has been removed.")));
            return;
        }
        pdc.set(this.usesRemainingKey, PersistentDataType.INTEGER, remaining);
        this.plugin.updateSellAxeMeta(wand, meta, this.currentCountdown(meta));
        wand.setItemMeta(meta);
    }

    private String currentCountdown(ItemMeta meta) {
        if (!this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true) || meta == null) {
            return "";
        }
        Long expiry = (Long)meta.getPersistentDataContainer().get(this.expiryKey, PersistentDataType.LONG);
        if (expiry == null) {
            return "";
        }
        return this.formatDuration(Math.max(0L, expiry - System.currentTimeMillis()));
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000L;
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    private void destroyWand(Player player, ItemStack wand) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == wand || hand.isSimilar(wand)) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().remove(wand);
        }
    }
}

