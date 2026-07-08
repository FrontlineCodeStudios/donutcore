package ro.andreilarazboi.donutcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class DonutCoreCommand implements CommandExecutor, TabCompleter {

    private final DonutCore plugin;

    /** Canonical module names in display order. */
    private static final List<String> ALL_MODULES = List.of(
            "crates", "sell", "enderchest");

    public DonutCoreCommand(DonutCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // onCommand
    // =========================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutcore.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /donutcore help
        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        // /donutcore modules (also accepts legacy "module")
        if (sub.equals("modules") || sub.equals("module")) {
            return handleModules(sender, args);
        }

        sendUsage(sender);
        return true;
    }

    // =========================================================================
    // /donutcore modules …
    // =========================================================================

    private boolean handleModules(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUsage: /donutcore modules <list|enable|disable> [module|all]");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {

            case "list" -> {
                sendModuleList(sender);
            }

            case "enable", "disable" -> {
                boolean enable = action.equals("enable");

                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /donutcore modules " + action + " <module|all>");
                    return true;
                }

                String target = args[2].toLowerCase();

                if (target.equals("all")) {
                    toggleAllModules(sender, enable);
                } else if (ALL_MODULES.contains(target)) {
                    toggleModule(sender, target, enable, true);
                } else {
                    sender.sendMessage("§cUnknown module §e" + target
                            + "§c. Available: " + String.join(", ", ALL_MODULES));
                }

                plugin.syncModuleCommandVisibility();
            }

            default -> sender.sendMessage("§eUsage: /donutcore modules <list|enable|disable> [module|all]");
        }

        return true;
    }

    // =========================================================================
    // Module list
    // =========================================================================

    private void sendModuleList(CommandSender sender) {
        sendHelpLine(sender, "&7&m----------------------------");
        sendHelpLine(sender, "&#FF073ADonutCore Modules:");
        for (String name : ALL_MODULES) {
            boolean active = isActive(name);
            String status = active ? "&#FF073AEnabled" : "&cDisabled";
            sendHelpLine(sender, "&#FF073A" + capitalize(name) + " &7- " + status);
        }
        sendHelpLine(sender, "&7&m----------------------------");
    }

    // =========================================================================
    // Toggle — single module
    // =========================================================================

    /**
     * @param sendFeedback if {@code true}, sends a result message to the sender
     *                     and calls {@link DonutCore#saveConfig()}.
     */
    private void toggleModule(CommandSender sender, String name, boolean enable, boolean sendFeedback) {
        boolean currentlyActive = isActive(name);

        if (currentlyActive == enable) {
            if (sendFeedback) {
                sender.sendMessage("§e" + capitalize(name)
                        + " is already " + (enable ? "enabled" : "disabled") + ".");
            }
            return;
        }

        try {
            if (enable)
                getModule(name).enable();
            else
                getModule(name).disable();
        } catch (Throwable t) {
            sender.sendMessage("§c" + capitalize(name) + " failed: " + t.getMessage());
            return;
        }

        plugin.getConfig().set("modules." + name, enable);

        if (sendFeedback) {
            boolean nowActive = isActive(name);
            if (enable && !nowActive) {
                // Module enable() silently failed (e.g. missing dependency)
                sender.sendMessage("§e" + capitalize(name)
                        + " could not be enabled (check console — missing dependency?).");
            } else {
                sender.sendMessage("§a" + capitalize(name) + " " + (enable ? "enabled" : "disabled") + ".");
            }
            plugin.saveConfig();
        }
    }

    // =========================================================================
    // Toggle — all modules
    // =========================================================================

    private void toggleAllModules(CommandSender sender, boolean enable) {
        int changed = 0;
        int skipped = 0;

        for (String name : ALL_MODULES) {
            if (isActive(name) == enable) {
                skipped++;
                continue;
            }
            try {
                if (enable)
                    getModule(name).enable();
                else
                    getModule(name).disable();
                plugin.getConfig().set("modules." + name, enable);
                changed++;
            } catch (Throwable t) {
                sender.sendMessage("§c" + capitalize(name) + " failed: " + t.getMessage());
            }
        }

        plugin.saveConfig();

        if (changed == 0) {
            sender.sendMessage("§eAll modules are already "
                    + (enable ? "enabled" : "disabled") + ".");
        } else {
            sender.sendMessage("§a" + changed + " module(s) " + (enable ? "enabled" : "disabled")
                    + (skipped > 0 ? " §7(" + skipped + " already in that state)" : "") + ".");
        }
    }

    // =========================================================================
    // /donutcore help
    // =========================================================================

    private void sendHelp(CommandSender sender) {
        sendHelpLine(sender, "&7&m----------------------------");
        sendHelpLine(sender, "&#FF073ADonutCore Help Menu:");
        
        // --- Admin ---
        if (sender.hasPermission("donutcore.admin")) {
            sendHelpLine(sender, "&#FF073A/donutcore modules list &7- List all modules.");
            sendHelpLine(sender, "&#FF073A/donutcore modules enable <module|all> &7- Enable a module.");
            sendHelpLine(sender, "&#FF073A/donutcore modules disable <module|all> &7- Disable a module.");
            sendHelpLine(sender, "&#FF073A/donutcore help &7- Show this help menu.");
        }

        // --- Sell ---
        if (plugin.getSellModule().isActive() && (sender.hasPermission("donutcore.sell.use") || sender.hasPermission("donutcore.admin"))) {
            sendHelpLine(sender, "&#FF073A/sell &7- Open the sell GUI.");
            sendHelpLine(sender, "&#FF073A/sellmulti &7- Sell multiple items.");
            sendHelpLine(sender, "&#FF073A/worth &7- Check item worth.");
            sendHelpLine(sender, "&#FF073A/toggleworth &7- Toggle worth messages.");
            sendHelpLine(sender, "&#FF073A/sellhistory &7- View sell history.");
        }

        // --- Crates ---
        if (plugin.getCratesModule().isActive()) {
            sendHelpLine(sender, "&#FF073A/crate stats &7- Open crate stats GUI.");
            if (sender.hasPermission("donutcrate.admin") || sender.hasPermission("donutcore.admin")) {
                sendHelpLine(sender, "&#FF073A/crate editor &7- Open crate editor.");
                sendHelpLine(sender, "&#FF073A/crate preview <crate> &7- Preview a crate.");
                sendHelpLine(sender, "&#FF073A/crate reload &7- Reload crate data.");
                sendHelpLine(sender, "&#FF073A/crate key <give|giveall|remove|reset> ...");
            }
        }

        // --- EnderChest ---
        if (plugin.getEnderChestModule().isActive()) {
            if (sender.hasPermission("enderchest.command") || sender.hasPermission("donutcore.admin")) {
                sendHelpLine(sender, "&#FF073A/enderchest &7- Open your enderchest.");
            }
            if (sender.hasPermission("enderchest.clear") || sender.hasPermission("donutcore.admin")) {
                sendHelpLine(sender, "&#FF073A/clearechest <player> &7- Clear a player's enderchest.");
            }
        }

        sendHelpLine(sender, "&7&m----------------------------");
    }

    private void sendHelpLine(CommandSender sender, String raw) {
        sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(formatColors(raw)));
    }

    private String formatColors(String input) {
        if (input == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(input);
        StringBuffer buffer = new StringBuffer(input.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder repl = new StringBuilder("§x");
            for (char c : hex.toCharArray()) repl.append('§').append(c);
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(repl.toString()));
        }
        matcher.appendTail(buffer);
        char[] chars = buffer.toString().toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length && "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(chars[i + 1])) >= 0) {
                sb.append('§').append(Character.toLowerCase(chars[i + 1]));
                i++;
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns whether the named module is currently active. */
    private boolean isActive(String name) {
        return switch (name) {
            case "crates" -> plugin.getCratesModule().isActive();
            case "sell" -> plugin.getSellModule().isActive();
            case "enderchest" -> plugin.getEnderChestModule().isActive();
            default -> false;
        };
    }

    /** Small interface to unify module enable/disable calls. */
    private interface Togglable {
        void enable();

        void disable();
    }

    /** Returns a {@link Togglable} adapter for the named module. */
    private Togglable getModule(String name) {
        return switch (name) {
            case "crates" -> new Togglable() {
                public void enable() {
                    plugin.getCratesModule().enable();
                }

                public void disable() {
                    plugin.getCratesModule().disable();
                }
            };
            case "sell" -> new Togglable() {
                public void enable() {
                    plugin.getSellModule().enable();
                }

                public void disable() {
                    plugin.getSellModule().disable();
                }
            };
            case "enderchest" -> new Togglable() {
                public void enable() {
                    plugin.getEnderChestModule().enable();
                }

                public void disable() {
                    plugin.getEnderChestModule().disable();
                }
            };
            default -> throw new IllegalArgumentException("Unknown module: " + name);
        };
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eUsage:");
        sender.sendMessage("  §f/donutcore modules list");
        sender.sendMessage("  §f/donutcore modules enable §8<module|all>");
        sender.sendMessage("  §f/donutcore modules disable §8<module|all>");
        sender.sendMessage("  §f/donutcore help");
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // =========================================================================
    // Tab completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutcore.admin"))
            return null;

        return switch (args.length) {
            case 1 -> filter(List.of("modules", "help"), args[0]);
            case 2 -> {
                String sub = args[0].toLowerCase();
                if (sub.equals("modules") || sub.equals("module")) {
                    yield filter(List.of("list", "enable", "disable"), args[1]);
                }
                yield null;
            }
            case 3 -> {
                String sub = args[0].toLowerCase();
                String action = args[1].toLowerCase();
                if ((sub.equals("modules") || sub.equals("module"))
                        && (action.equals("enable") || action.equals("disable"))) {
                    List<String> opts = new ArrayList<>(ALL_MODULES);
                    opts.add(0, "all");
                    yield filter(opts, args[2]);
                }
                yield null;
            }
            default -> null;
        };
    }

    private static List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
