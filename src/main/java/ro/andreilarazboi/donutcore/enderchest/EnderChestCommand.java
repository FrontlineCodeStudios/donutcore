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

public final class EnderChestCommand implements CommandExecutor, TabCompleter {
    private final DonutEnderChest plugin;

    public EnderChestCommand(DonutEnderChest plugin) {
        this.plugin = plugin;
        plugin.getCommand("enderchest").setExecutor(this);
        plugin.getCommand("enderchest").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formatComponent("&cOnly players can use this command."));
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission("enderchest.command")) {
                player.sendMessage(plugin.msgComponent("no-permission"));
                return true;
            }
            plugin.getEnderChestManager().openOwnChest(player);
            return true;
        }

        if (!player.hasPermission("enderchest.command.others")) {
            player.sendMessage(plugin.msgComponent("no-permission"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && target.getPlayer() == null) {
            player.sendMessage(plugin.formatComponent(plugin.msg("player-not-found").replace("<player>", targetName)));
            return true;
        }

        plugin.getEnderChestManager().openOthersChest(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("enderchest.command.others")) {
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
