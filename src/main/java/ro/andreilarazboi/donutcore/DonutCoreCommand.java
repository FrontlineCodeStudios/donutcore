package ro.andreilarazboi.donutcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class DonutCoreCommand implements CommandExecutor, TabCompleter {

    private final DonutCore plugin;

    public DonutCoreCommand(DonutCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutcore.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("module")) {
            sender.sendMessage("§eUsage: /donutcore module <crates|sell|enderchest> <enable|disable>");
            return true;
        }

        String moduleName = args[1].toLowerCase();
        String action     = args[2].toLowerCase();

        if (!action.equals("enable") && !action.equals("disable")) {
            sender.sendMessage("§eUsage: /donutcore module <crates|sell|enderchest> <enable|disable>");
            return true;
        }

        boolean enable = action.equals("enable");

        switch (moduleName) {
            case "arenas" -> {
                if (enable == plugin.getArenasModule().isActive()) {
                    sender.sendMessage("§eArenas module is already " + (enable ? "enabled" : "disabled") + "."); return true;
                }
                if (enable) { plugin.getArenasModule().enable();  sender.sendMessage("§aArenas module enabled."); }
                else        { plugin.getArenasModule().disable(); sender.sendMessage("§aArenas module disabled."); }
                plugin.getConfig().set("modules.arenas", enable);
                plugin.saveConfig();
            }
            case "fastcrystals" -> {
                if (enable == plugin.getFastCrystalsModule().isActive()) {
                    sender.sendMessage("§eFastCrystals module is already " + (enable ? "enabled" : "disabled") + "."); return true;
                }
                if (enable) { plugin.getFastCrystalsModule().enable();  sender.sendMessage("§aFastCrystals module enabled."); }
                else        { plugin.getFastCrystalsModule().disable(); sender.sendMessage("§aFastCrystals module disabled."); }
                plugin.getConfig().set("modules.fastcrystals", enable);
                plugin.saveConfig();
            }
            case "stash" -> {
                if (enable == plugin.getStashModule().isActive()) {
                    sender.sendMessage("§eStash module is already " + (enable ? "enabled" : "disabled") + "."); return true;
                }
                if (enable) { plugin.getStashModule().enable();  sender.sendMessage("§aStash module enabled."); }
                else        { plugin.getStashModule().disable(); sender.sendMessage("§aStash module disabled."); }
                plugin.getConfig().set("modules.stash", enable);
                plugin.saveConfig();
            }
            case "crates" -> {
                if (enable == plugin.getCratesModule().isActive()) {
                    sender.sendMessage("§eCrates module is already " + (enable ? "enabled" : "disabled") + ".");
                    return true;
                }
                if (enable) {
                    plugin.getCratesModule().enable();
                    sender.sendMessage("§aCrates module enabled.");
                } else {
                    plugin.getCratesModule().disable();
                    sender.sendMessage("§aCrates module disabled.");
                }
                plugin.getConfig().set("modules.crates", enable);
                plugin.saveConfig();
            }
            case "sell" -> {
                if (enable == plugin.getSellModule().isActive()) {
                    sender.sendMessage("§eSell module is already " + (enable ? "enabled" : "disabled") + ".");
                    return true;
                }
                if (enable) {
                    plugin.getSellModule().enable();
                    sender.sendMessage("§aSell module enabled.");
                } else {
                    plugin.getSellModule().disable();
                    sender.sendMessage("§aSell module disabled.");
                }
                plugin.getConfig().set("modules.sell", enable);
                plugin.saveConfig();
            }
            case "enderchest" -> {
                if (enable == plugin.getEnderChestModule().isActive()) {
                    sender.sendMessage("§eEnderChest module is already " + (enable ? "enabled" : "disabled") + ".");
                    return true;
                }
                if (enable) {
                    plugin.getEnderChestModule().enable();
                    sender.sendMessage("§aEnderChest module enabled.");
                } else {
                    plugin.getEnderChestModule().disable();
                    sender.sendMessage("§aEnderChest module disabled.");
                }
                plugin.getConfig().set("modules.enderchest", enable);
                plugin.saveConfig();
            }
            default -> sender.sendMessage("§eUnknown module. Use: crates, sell, enderchest, arenas, fastcrystals, stash");
        }

        plugin.syncModuleCommandVisibility();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutcore.admin")) return null;
        return switch (args.length) {
            case 1 -> List.of("module");
            case 2 -> List.of("crates", "sell", "enderchest", "arenas", "fastcrystals", "stash");
            case 3 -> List.of("enable", "disable");
            default -> null;
        };
    }
}
