package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import ro.andreilarazboi.donutcore.sell.DonutOrderBridge;
import ro.andreilarazboi.donutcore.sell.DonutSell;
import ro.andreilarazboi.donutcore.sell.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand
implements CommandExecutor,
TabCompleter {
    private final DonutSell plugin;

    public SellCommand(DonutSell plugin) {
        this.plugin = plugin;
        plugin.getCommand("sell").setExecutor((CommandExecutor)this);
        plugin.getCommand("sell").setTabCompleter((TabCompleter)this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!this.plugin.isModuleActive()) {
            sender.sendMessage(Utils.formatColors("&cThis module is currently disabled."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.formatColors("&cOnly players may use /sell."));
            return true;
        }
        Player p = (Player)sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("hand")) {
            if (!this.plugin.getConfig().getBoolean("sell-command.hand-enabled", true)) {
                p.sendMessage(Utils.formatColors("&c/sell hand is disabled."));
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!this.plugin.isSellable(hand)) {
                p.sendMessage(Utils.formatColors(this.plugin.getMessagesConfig().getString("messages.cannot-sell", "&cThat item is not sellable.")));
                return true;
            }
            double worth = this.plugin.calculateItemWorth(hand);
            if (worth <= 0.0) {
                return true;
            }
            int originalAmount = hand.getAmount();
            double orderPayout = 0.0;
            DonutOrderBridge.Result orderResult = this.plugin.trySellToBetterOrder(p, hand, worth, false);
            if (orderResult.wasFilled()) {
                orderPayout = orderResult.totalPayout();
                if (orderResult.filledAmount() >= hand.getAmount()) {
                    p.getInventory().setItemInMainHand(null);
                    this.plugin.notifySale(p, orderPayout, originalAmount);
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    return true;
                }
                hand.setAmount(hand.getAmount() - orderResult.filledAmount());
                p.getInventory().setItemInMainHand(hand);
                worth = this.plugin.calculateItemWorth(hand);
                if (worth <= 0.0) {
                    this.plugin.notifySale(p, orderPayout, orderResult.filledAmount());
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    return true;
                }
            }
            HashMap<String, DonutSell.Stats> sold = new HashMap<String, DonutSell.Stats>();
            sold.put(hand.getType().name().toLowerCase(Locale.ROOT), new DonutSell.Stats(hand.getAmount(), worth));
            this.plugin.recordSale(p, sold);
            this.plugin.getEconomy().depositPlayer((OfflinePlayer)p, worth);
            p.getInventory().setItemInMainHand(null);
            this.plugin.notifySale(p, orderPayout + worth, originalAmount);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("inventory")) {
            if (!this.plugin.getConfig().getBoolean("sell-command.inventory-enabled", false)) {
                p.sendMessage(Utils.formatColors("&c/sell inventory is disabled."));
                return true;
            }
            HashMap<String, DonutSell.Stats> sold = new HashMap<String, DonutSell.Stats>();
            double total = 0.0;
            double orderPayout = 0.0;
            long orderItemsSold = 0L;
            for (int i = 0; i < p.getInventory().getSize(); ++i) {
                double worth;
                ItemStack item = p.getInventory().getItem(i);
                if (!this.plugin.isSellable(item) || (worth = this.plugin.calculateItemWorth(item)) <= 0.0) continue;
                DonutOrderBridge.Result orderResult = this.plugin.trySellToBetterOrder(p, item, worth, false);
                if (orderResult.wasFilled()) {
                    orderPayout += orderResult.totalPayout();
                    orderItemsSold += (long)orderResult.filledAmount();
                    if (orderResult.filledAmount() >= item.getAmount()) {
                        p.getInventory().setItem(i, null);
                        continue;
                    }
                    item.setAmount(item.getAmount() - orderResult.filledAmount());
                    p.getInventory().setItem(i, item);
                    worth = this.plugin.calculateItemWorth(item);
                    if (worth <= 0.0) continue;
                }
                total += worth;
                sold.merge(item.getType().name().toLowerCase(Locale.ROOT), new DonutSell.Stats(item.getAmount(), worth), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                p.getInventory().setItem(i, null);
            }
            if (total > 0.0) {
                this.plugin.recordSale(p, sold);
                this.plugin.getEconomy().depositPlayer((OfflinePlayer)p, total);
            }
            long normalItemsSold = Math.round(sold.values().stream().mapToDouble(stats -> stats.count).sum());
            if (total > 0.0 || orderPayout > 0.0) {
                this.plugin.notifySale(p, total + orderPayout, normalItemsSold + orderItemsSold);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            return true;
        }
        this.plugin.getSellGui().open(p);
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return this.filter(Arrays.asList("hand", "inventory"), args[0]);
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

