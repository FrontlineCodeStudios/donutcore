package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ro.andreilarazboi.donutcore.DonutCore;

public final class SellCommand implements CommandExecutor, TabCompleter {
    private final DonutSell plugin;

    public SellCommand(DonutSell plugin) {
        this.plugin = plugin;
        plugin.getCommand("sell").setExecutor(this);
        plugin.getCommand("sell").setTabCompleter(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!DonutCore.getInstance().getSellModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage(Utils.toComponent("&cThe Sell module is currently disabled."));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.toComponent("&cOnly players may use /sell."));
            return true;
        }
        if (this.plugin.getEconomy() == null) {
            sender.sendMessage(Utils.toComponent("&cVault economy is not set up. Please install Vault + an economy plugin."));
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("hand")) {
            if (!this.plugin.getConfig().getBoolean("sell-command.hand-enabled", true)) {
                p.sendMessage(Utils.toComponent("&c/sell hand is disabled."));
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!this.plugin.isSellable(hand)) {
                p.sendMessage(Utils.toComponent(this.plugin.getMessagesConfig().getString("messages.cannot-sell", "&cThat item is not sellable.")));
                return true;
            }
            double worth = this.plugin.calculateItemWorth(hand);
            if (worth <= 0.0) {
                return true;
            }
            HashMap<String, DonutSell.Stats> sold = new HashMap<>();
            sold.put(hand.getType().name().toLowerCase(Locale.ROOT), new DonutSell.Stats(hand.getAmount(), worth));
            this.plugin.recordSale(p, sold);
            this.plugin.getEconomy().depositPlayer((OfflinePlayer) p, worth);
            p.getInventory().setItemInMainHand(null);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("inventory")) {
            if (!this.plugin.getConfig().getBoolean("sell-command.inventory-enabled", false)) {
                p.sendMessage(Utils.toComponent("&c/sell inventory is disabled."));
                return true;
            }
            HashMap<String, DonutSell.Stats> sold = new HashMap<>();
            double total = 0.0;
            for (int i = 0; i < p.getInventory().getSize(); ++i) {
                ItemStack item = p.getInventory().getItem(i);
                double worth;
                if (!this.plugin.isSellable(item) || (worth = this.plugin.calculateItemWorth(item)) <= 0.0) continue;
                total += worth;
                sold.merge(item.getType().name().toLowerCase(Locale.ROOT), new DonutSell.Stats(item.getAmount(), worth), (a, b) -> new DonutSell.Stats(a.count + b.count, a.revenue + b.revenue));
                p.getInventory().setItem(i, null);
            }
            if (total > 0.0) {
                this.plugin.recordSale(p, sold);
                this.plugin.getEconomy().depositPlayer((OfflinePlayer) p, total);
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
        ArrayList<String> out = new ArrayList<>();
        for (String o : options) {
            if (!o.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            out.add(o);
        }
        return out;
    }
}
