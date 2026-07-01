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
            "crates", "sell", "enderchest", "arenas", "fastcrystals", "stash", "vipfeatures", "hide"
    );

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
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("  §6§lDonutCore §7— Module Status");
        sender.sendMessage("§8§m════════════════════════════════════════");
        for (String name : ALL_MODULES) {
            boolean active  = isActive(name);
            String  icon    = active ? "§a✔" : "§c✗";
            String  status  = active ? "§aEnabled" : "§cDisabled";
            String  display = padRight(capitalize(name), 14);
            sender.sendMessage("  " + icon + " §f" + display + "§8» " + status);
        }
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("  §7Use §f/donutcore modules enable/disable <module|all>");
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
            if (enable) getModule(name).enable();
            else        getModule(name).disable();
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
                if (enable) getModule(name).enable();
                else        getModule(name).disable();
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
        sender.sendMessage("§8§m════════════════════════════════════════");
        sender.sendMessage("  §6§lDonutCore §7— Command Reference");
        sender.sendMessage("§8§m════════════════════════════════════════");

        // --- Admin ---
        sender.sendMessage("  §e§lAdmin §8| §7donutcore.admin");
        sender.sendMessage("    §f/donutcore modules list");
        sender.sendMessage("    §f/donutcore modules enable §8<module|all>");
        sender.sendMessage("    §f/donutcore modules disable §8<module|all>");
        sender.sendMessage("    §f/donutcore help");
        sender.sendMessage("");

        // --- Sell ---
        helpLine(sender, "Sell", "donutcore.sell.use",
                plugin.getSellModule().isActive(),
                "/sell §8· §f/sellmulti §8· §f/worth §8· §f/toggleworth §8· §f/sellhistory");
        helpLine(sender, "Sell Admin", "donutcore.sell.admin",
                plugin.getSellModule().isActive(),
                "/donutsell");

        // --- Crates ---
        helpLine(sender, "Crates", "donutcore.crate.use",
                plugin.getCratesModule().isActive(),
                "/donutcrate §8[aliases: /dcrate, /crate]");
        helpLine(sender, "Crates Admin", "donutcore.crate.admin",
                plugin.getCratesModule().isActive(),
                "/donutcrate give/set/…");

        // --- EnderChest ---
        helpLine(sender, "EnderChest", "enderchest.command",
                plugin.getEnderChestModule().isActive(),
                "/enderchest §8[aliases: /ec, /echest]");
        helpLine(sender, "EnderChest Admin", "enderchest.clear",
                plugin.getEnderChestModule().isActive(),
                "/clearechest §8<player>");

        // --- Arenas ---
        helpLine(sender, "Arenas Admin", "donutcore.arenas.admin",
                plugin.getArenasModule().isActive(),
                "/donutarena");

        // --- FastCrystals ---
        helpSection(sender, "FastCrystals", plugin.getFastCrystalsModule().isActive(),
                "§7Passive — no commands. Grants immunity to own crystal explosions.");

        // --- Stash ---
        helpLine(sender, "Stash Admin", "donutcore.admin.stash",
                plugin.getStashModule().isActive(),
                "/donutstash save/spawn/remove/list/mark/unmark §8[alias: /stash]");

        // --- VIP Features ---
        helpLine(sender, "VIP Features", "donutcore.vip.chatcolor",
                plugin.getVipFeaturesModule().isActive(),
                "/chatcolor §8<&X|&#RRGGBB|reset>");
        helpLine(sender, "VIP Features", "donutcore.vip.namecolor",
                plugin.getVipFeaturesModule().isActive(),
                "/namecolor §8<&X|&#RRGGBB|reset>");

        // --- Hide ---
        helpLine(sender, "Hide VIP", "donutcore.hide.vip",
                plugin.getHideModule().isActive(),
                "/hide §8— toggle streamer mode (name tag + random skin)");
        helpLine(sender, "Hide Staff", "donutcore.hide.staff",
                plugin.getHideModule().isActive(),
                "/staffhide §8<FakeName> [SkinName|UUID]  ·  no args = deactivate");

        sender.sendMessage("§8§m════════════════════════════════════════");
    }

    /** Prints a module section header + command line (coloured by active/inactive). */
    private void helpLine(CommandSender sender, String section, String perm,
                          boolean moduleActive, String commands) {
        String status = moduleActive ? "" : " §c[Disabled]";
        sender.sendMessage("  §e§l" + section + status + " §8| §7" + perm);
        sender.sendMessage("    §f" + commands);
        sender.sendMessage("");
    }

    /** Prints a passive module section (no command). */
    private void helpSection(CommandSender sender, String section, boolean moduleActive, String note) {
        String status = moduleActive ? "" : " §c[Disabled]";
        sender.sendMessage("  §e§l" + section + status);
        sender.sendMessage("    " + note);
        sender.sendMessage("");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns whether the named module is currently active. */
    private boolean isActive(String name) {
        return switch (name) {
            case "crates"       -> plugin.getCratesModule().isActive();
            case "sell"         -> plugin.getSellModule().isActive();
            case "enderchest"   -> plugin.getEnderChestModule().isActive();
            case "arenas"       -> plugin.getArenasModule().isActive();
            case "fastcrystals" -> plugin.getFastCrystalsModule().isActive();
            case "stash"        -> plugin.getStashModule().isActive();
            case "vipfeatures"  -> plugin.getVipFeaturesModule().isActive();
            case "hide"         -> plugin.getHideModule().isActive();
            default             -> false;
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
            case "crates"       -> new Togglable() {
                public void enable()  { plugin.getCratesModule().enable(); }
                public void disable() { plugin.getCratesModule().disable(); }
            };
            case "sell"         -> new Togglable() {
                public void enable()  { plugin.getSellModule().enable(); }
                public void disable() { plugin.getSellModule().disable(); }
            };
            case "enderchest"   -> new Togglable() {
                public void enable()  { plugin.getEnderChestModule().enable(); }
                public void disable() { plugin.getEnderChestModule().disable(); }
            };
            case "arenas"       -> new Togglable() {
                public void enable()  { plugin.getArenasModule().enable(); }
                public void disable() { plugin.getArenasModule().disable(); }
            };
            case "fastcrystals" -> new Togglable() {
                public void enable()  { plugin.getFastCrystalsModule().enable(); }
                public void disable() { plugin.getFastCrystalsModule().disable(); }
            };
            case "stash"        -> new Togglable() {
                public void enable()  { plugin.getStashModule().enable(); }
                public void disable() { plugin.getStashModule().disable(); }
            };
            case "vipfeatures"  -> new Togglable() {
                public void enable()  { plugin.getVipFeaturesModule().enable(); }
                public void disable() { plugin.getVipFeaturesModule().disable(); }
            };
            case "hide"         -> new Togglable() {
                public void enable()  { plugin.getHideModule().enable(); }
                public void disable() { plugin.getHideModule().disable(); }
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

    private static String padRight(String s, int len) {
        return s.length() >= len ? s : s + " ".repeat(len - s.length());
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // =========================================================================
    // Tab completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutcore.admin")) return null;

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
                String sub    = args[0].toLowerCase();
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
