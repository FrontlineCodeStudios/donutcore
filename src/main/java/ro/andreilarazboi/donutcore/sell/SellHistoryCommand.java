package ro.andreilarazboi.donutcore.sell;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public class SellHistoryCommand
implements CommandExecutor {
    private final DonutSell plugin;

    public SellHistoryCommand(DonutSell plugin) {
        this.plugin = plugin;
        plugin.getCommand("sellhistory").setExecutor((CommandExecutor)this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!this.plugin.isModuleActive()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "This module is currently disabled.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Only players can view sell history.");
            return true;
        }
        Player p = (Player)sender;
        this.plugin.getSellHistoryGui().open(p, 1);
        return true;
    }
}

