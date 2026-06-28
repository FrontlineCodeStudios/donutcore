package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ClearEnderChestCommand implements CommandExecutor, TabCompleter {
    private final DonutEnderChest plugin;

    public ClearEnderChestCommand(DonutEnderChest plugin) {
        this.plugin = plugin;
        plugin.getCommand("clearechest").setExecutor(this);
        plugin.getCommand("clearechest").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("enderchest.clear")) {
            sender.sendMessage(plugin.msgComponent("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.formatComponent("&cUsage: /" + label + " <player>"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && target.getPlayer() == null) {
            sender.sendMessage(plugin.formatComponent(plugin.msg("player-not-found").replace("<player>", targetName)));
            return true;
        }

        String displayName = target.getName() != null ? target.getName() : targetName;

        // Suppress the save that would fire from closing the inventory
        plugin.suppressSave(target.getUniqueId());

        // Close the chest if the owner has it open right now
        if (target.getPlayer() instanceof Player online) {
            plugin.runAtPlayer(online, () -> {
                if (online.getOpenInventory().getTopInventory().getHolder() instanceof EnderChestHolder) {
                    online.closeInventory();
                }
            });
        }

        plugin.runAsync(() -> {
            plugin.getDataManager().clearItems(target.getUniqueId());
            plugin.allowSave(target.getUniqueId());
            plugin.runSync(() ->
                    sender.sendMessage(plugin.formatComponent(plugin.msg("enderchest-cleared").replace("<player>", displayName))));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("enderchest.clear")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) result.add(p.getName());
            }
            return result;
        }
        return Collections.emptyList();
    }
}
