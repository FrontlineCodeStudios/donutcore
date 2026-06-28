package ro.andreilarazboi.donutcore.sell;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.DonutCore;

public final class ToggleWorthCommand implements CommandExecutor {
    private final DonutSell plugin;

    public ToggleWorthCommand(DonutSell plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("toggleworth"), "Command 'toggleworth' not found in plugin.yml").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!DonutCore.getInstance().getSellModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage(Utils.toComponent("&cThe Sell module is currently disabled."));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.toComponent("&cOnly players can use this command."));
            return true;
        }
        Player p = (Player) sender;
        UUID id = p.getUniqueId();
        boolean currentlyEnabled = this.plugin.isWorthEnabled(id);
        boolean targetEnabled = !currentlyEnabled;
        this.plugin.setWorthEnabled(id, targetEnabled);
        if (!targetEnabled) {
            this.plugin.getCleanupListener().stripAllLore(p);
        } else {
            p.updateInventory();
        }
        String path = targetEnabled ? "messages.worth-enabled" : "messages.worth-disabled";
        String fallback = targetEnabled ? "&#34ee80Worth lore: &aENABLED" : "&#34ee80Worth lore: &cDISABLED";
        p.sendMessage(Utils.toComponent(this.plugin.getMessagesConfig().getString(path, fallback)));
        return true;
    }
}
