package ro.andreilarazboi.donutcore.sell;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.DonutCore;

public class SellHistoryCommand implements CommandExecutor {
    private final DonutSell plugin;

    public SellHistoryCommand(DonutSell plugin) {
        this.plugin = plugin;
        plugin.getCommand("sellhistory").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!DonutCore.getInstance().getSellModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage(Utils.toComponent("&cThe Sell module is currently disabled."));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.toComponent("&cOnly players can view sell history."));
            return true;
        }
        Player p = (Player) sender;
        this.plugin.getSellHistoryGui().open(p, 1);
        return true;
    }
}
