
package ro.andreilarazboi.donutcore.crates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import ro.andreilarazboi.donutcore.DonutCore;

public class DonutCrateCommand
        implements CommandExecutor,
        TabCompleter {
    private final DonutCrates plugin;

    public DonutCrateCommand(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!DonutCore.getInstance().getCratesModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage("§cThe Crates module is currently disabled.");
            }
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.plugin.msg(sender, "&#0fe30f/crate stats &7- Open crate stats GUI.");
            if (sender.hasPermission("donutcrate.admin") || sender.hasPermission("donutcore.admin")) {
                this.plugin.msg(sender, "&#0fe30f/crate editor &7- Open crate editor.");
                this.plugin.msg(sender, "&#0fe30f/crate preview <crate> &7- Preview a crate.");
                this.plugin.msg(sender, "&#0fe30f/crate reload &7- Reload crate data.");
                this.plugin.msg(sender, "&#0fe30f/crate key <give|giveall|remove|reset> ...");
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("pay")) {
            return this.handleKeyPay(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("stats")) {
            if (!(sender instanceof Player)) {
                this.plugin.msg(sender, "&#d61111Only players may use this command.");
                return true;
            }
            Player player = (Player) sender;
            player.openInventory(this.plugin.guiCrateStats.buildMain(player));
            return true;
        }
        if (!sender.hasPermission("donutcrate.admin")) {
            String msg = this.plugin.cfg.config.getString("no-permission", "&cYou lack permission to use this.");
            this.plugin.msg(sender, msg);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "editor": {
                if (!(sender instanceof Player)) {
                    this.plugin.msg(sender, "&#d61111Only players may use this command.");
                    return true;
                }
                Player p = (Player) sender;
                p.openInventory(this.plugin.guiRootEditor.build());
                return true;
            }
            case "preview": {
                if (!(sender instanceof Player)) {
                    this.plugin.msg(sender, "&#d61111Only players may use this command.");
                    return true;
                }
                Player p = (Player) sender;
                if (args.length != 2) {
                    return this.syntax(sender);
                }
                String crate = args[1];
                if (!this.plugin.crateMgr.crateExists(crate)) {
                    this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
                    return true;
                }
                p.openInventory(this.plugin.guiCrate.build(crate, true));
                return true;
            }
            case "key": {
                if (args.length < 2) {
                    return this.syntax(sender);
                }
                switch (args[1].toLowerCase()) {
                    case "give": {
                        int amount;
                        if (args.length != 5) {
                            return this.syntax(sender);
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            this.plugin.msg(sender, "&#d61111Player not found.");
                            return true;
                        }
                        String crate = args[3];
                        if (!this.plugin.crateMgr.crateExists(crate)) {
                            this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
                            return true;
                        }
                        try {
                            amount = Integer.parseInt(args[4]);
                        } catch (NumberFormatException ex) {
                            return this.syntax(sender);
                        }
                        if (amount <= 0) {
                            return this.syntax(sender);
                        }
                        String keyId = this.plugin.getKeyIdForCrate(crate);
                        this.plugin.ensureKeyConfig(keyId);
                        boolean virt = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true);
                        if (virt) {
                            this.plugin.dataMgr.modifyKeys(target, keyId, amount);
                        } else {
                            ItemStack stack = this.plugin.buildKeyItemById(keyId, amount);
                            HashMap<Integer, ItemStack> left = target.getInventory().addItem(new ItemStack[] { stack });
                            if (!left.isEmpty()) {
                                left.values()
                                        .forEach(is -> target.getWorld().dropItemNaturally(target.getLocation(), is));
                            }
                        }
                        String perTpl = this.plugin.cfg.config.getString("messages.keyreceive",
                                "&#0fe30fYou received %amount% %crate% keys!");
                        perTpl = perTpl.replace("%amount%", String.valueOf(amount)).replace("%crate%", crate);
                        this.plugin.msg(target, perTpl);
                        this.plugin.msg(sender, "&#0fe30fGave &#f5f5f5" + amount + " &#0fe30fkeys (&f" + keyId
                                + "&#0fe30f) for crate &#f5f5f5" + crate + " &#0fe30fto &#f5f5f5" + target.getName());
                        return true;
                    }
                    case "giveall": {
                        int amount;
                        if (args.length != 4) {
                            return this.syntax(sender);
                        }
                        String crate = args[2];
                        if (!this.plugin.crateMgr.crateExists(crate)) {
                            this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
                            return true;
                        }
                        try {
                            amount = Integer.parseInt(args[3]);
                        } catch (NumberFormatException ex) {
                            return this.syntax(sender);
                        }
                        if (amount <= 0) {
                            return this.syntax(sender);
                        }
                        String keyId = this.plugin.getKeyIdForCrate(crate);
                        this.plugin.ensureKeyConfig(keyId);
                        boolean virt = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true);
                        int players = 0;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            ++players;
                            if (virt) {
                                this.plugin.dataMgr.modifyKeys(p, keyId, amount);
                            } else {
                                ItemStack stack = this.plugin.buildKeyItemById(keyId, amount);
                                HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[] { stack });
                                if (!left.isEmpty()) {
                                    left.values().forEach(is -> p.getWorld().dropItemNaturally(p.getLocation(), is));
                                }
                            }
                            String perTpl = this.plugin.cfg.config.getString("messages.keyreceive",
                                    "&#0fe30fYou received %amount% %crate% keys!");
                            perTpl = perTpl.replace("%amount%", String.valueOf(amount)).replace("%crate%", crate);
                            this.plugin.msg(p, perTpl);
                        }
                        this.plugin.msg(sender,
                                "&#0fe30fGave &#f5f5f5" + amount + " &#0fe30fkeys (&f" + keyId
                                        + "&#0fe30f) for crate &#f5f5f5" + crate + " &#0fe30fto &#f5f5f5" + players
                                        + " &#0fe30fonline players.");
                        return true;
                    }
                    case "remove": {
                        int amount;
                        if (args.length != 5) {
                            return this.syntax(sender);
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            this.plugin.msg(sender, "&#d61111Player not found.");
                            return true;
                        }
                        String crate = args[3];
                        if (!this.plugin.crateMgr.crateExists(crate)) {
                            this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
                            return true;
                        }
                        try {
                            amount = Integer.parseInt(args[4]);
                        } catch (NumberFormatException ex) {
                            return this.syntax(sender);
                        }
                        if (amount <= 0) {
                            return this.syntax(sender);
                        }
                        String keyId = this.plugin.getKeyIdForCrate(crate);
                        this.plugin.ensureKeyConfig(keyId);
                        this.plugin.dataMgr.modifyKeys(target, keyId, -amount);
                        this.plugin.msg(sender, "&#d61111Removed &#f5f5f5" + amount + " &#d61111keys (&f" + keyId
                                + "&#d61111) for crate &#f5f5f5" + crate + " &#d61111from &#f5f5f5" + target.getName());
                        return true;
                    }
                    case "reset": {
                        if (args.length != 4) {
                            return this.syntax(sender);
                        }
                        String who = args[2];
                        String crate = args[3];
                        if (!this.plugin.crateMgr.crateExists(crate)) {
                            this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
                            return true;
                        }
                        String keyId = this.plugin.getKeyIdForCrate(crate);
                        this.plugin.ensureKeyConfig(keyId);
                        if (who.equalsIgnoreCase("all")) {
                            this.plugin.dataMgr.resetKeysForAll(keyId);
                            this.plugin.msg(sender, "&#0fe30fReset virtual keys for all players for crate &#f5f5f5"
                                    + crate + " &#0fe30f(key &f" + keyId + "&#0fe30f)");
                            return true;
                        }
                        OfflinePlayer off = Bukkit.getOfflinePlayer(who);
                        if (off.getName() == null) {
                            this.plugin.msg(sender, "&#d61111Player not found.");
                            return true;
                        }
                        this.plugin.dataMgr.resetKeysForPlayer(off.getUniqueId(), keyId);
                        this.plugin.msg(sender, "&#0fe30fReset virtual keys for &#f5f5f5" + off.getName()
                                + " &#0fe30ffor crate &#f5f5f5" + crate + " &#0fe30f(key &f" + keyId + "&#0fe30f)");
                        return true;
                    }
                }
                return this.syntax(sender);
            }
            case "reload": {
                this.plugin.cfg.reloadAll();
                this.plugin.reloadPrefix();
                this.plugin.crateMgr.saveBlocks();
                this.plugin.dataMgr.saveAll();
                this.plugin.holoMgr.despawnAll();
                for (String crate : this.plugin.crateMgr.crateBlocks.keySet()) {
                    this.plugin.holoMgr.refreshCrate(crate);
                }
                this.plugin.msg(sender, "&#0fe30fReloaded configuration and data.");
                return true;
            }
        }
        return this.syntax(sender);
    }

    private boolean syntax(CommandSender sender) {
        this.plugin.msg(sender, "&#0fe30f/crate stats &7- Open crate stats GUI.");
        if (sender.hasPermission("donutcrate.admin") || sender.hasPermission("donutcore.admin")) {
            this.plugin.msg(sender, "&#0fe30f/crate editor &7- Open crate editor.");
            this.plugin.msg(sender, "&#0fe30f/crate preview <crate> &7- Preview a crate.");
            this.plugin.msg(sender, "&#0fe30f/crate reload &7- Reload crate data.");
            this.plugin.msg(sender, "&#0fe30f/crate key <give|giveall|remove|reset> ...");
        }
        return true;
    }

    private boolean handleKeyPay(CommandSender sender, String[] args) {
        int amount;
        boolean hasPerm;
        boolean keyPayEnabled = this.plugin.cfg.config.getBoolean("enable-key-pay", false);
        if (!keyPayEnabled) {
            this.plugin.msg(sender, this.plugin.cfg.config.getString("messages.key-pay-disabled",
                    "&#ff5555Key paying is disabled on this server."));
            return true;
        }
        hasPerm = sender.hasPermission("donutcrate.key.pay") || sender.hasPermission("donutcrate.admin");
        if (!hasPerm) {
            this.plugin.msg(sender,
                    this.plugin.cfg.config.getString("no-permission", "&cYou lack permission to use this."));
            return true;
        }
        if (!(sender instanceof Player)) {
            this.plugin.msg(sender, "&#d61111Only players may use this command.");
            return true;
        }
        Player payer = (Player) sender;
        if (args.length != 5) {
            return this.syntax(sender);
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            this.plugin.msg(sender, "&#d61111Player not found.");
            return true;
        }
        if (target.getUniqueId().equals(payer.getUniqueId())) {
            this.plugin.msg(payer, this.plugin.cfg.config.getString("messages.key-pay-self",
                    "&#ff5555You cannot pay keys to yourself."));
            return true;
        }
        String crate = args[3];
        if (!this.plugin.crateMgr.crateExists(crate)) {
            this.plugin.msg(sender, "&#d61111No crate named &#f5f5f5" + crate + " &#d61111exists.");
            return true;
        }
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException ex) {
            return this.syntax(sender);
        }
        if (amount <= 0) {
            this.plugin.msg(sender, "&#d61111Amount must be positive.");
            return true;
        }
        String keyId = this.plugin.getKeyIdForCrate(crate);
        this.plugin.ensureKeyConfig(keyId);
        boolean virt = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true);
        if (!virt) {
            String msg = this.plugin.cfg.config.getString("messages.key-pay-virtual-only",
                    "&#ff5555Key paying only works for virtual keys for this crate.").replace("%crate%", crate);
            this.plugin.msg(sender, msg);
            return true;
        }
        int payerKeys = this.plugin.dataMgr.getKeys(payer, keyId);
        if (payerKeys < amount) {
            String msg = this.plugin.cfg.config.getString("messages.key-pay-not-enough",
                    "&#ff5555You do not have &f%amount% &#ff5555keys for crate &f%crate%&#ff5555.");
            this.plugin.msg(payer, msg.replace("%amount%", String.valueOf(amount)).replace("%crate%", crate));
            return true;
        }
        this.plugin.dataMgr.modifyKeys(payer, keyId, -amount);
        this.plugin.dataMgr.modifyKeys(target, keyId, amount);
        this.plugin.msg(payer,
                this.plugin.cfg.config
                        .getString("messages.key-pay-sent",
                                "&#0fe30fYou paid &f%amount% %crate% keys &#0fe30fto &f%target%&#0fe30f.")
                        .replace("%amount%", String.valueOf(amount)).replace("%crate%", crate)
                        .replace("%target%", target.getName()));
        this.plugin.msg(target,
                this.plugin.cfg.config
                        .getString("messages.key-pay-received",
                                "&#0fe30fYou received &f%amount% %crate% keys &#0fe30ffrom &f%player%&#0fe30f.")
                        .replace("%amount%", String.valueOf(amount)).replace("%crate%", crate)
                        .replace("%player%", payer.getName()));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean keyPayEnabled = this.plugin.cfg.config.getBoolean("enable-key-pay", false);
        boolean hasPayPerm = keyPayEnabled
                && (sender.hasPermission("donutcrate.key.pay") || sender.hasPermission("donutcrate.admin"));
        boolean isAdmin = sender.hasPermission("donutcrate.admin");
        if (!isAdmin && !hasPayPerm) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], List.of("stats"), new ArrayList<>());
            }
            return List.of();
        }
        if (hasPayPerm && !isAdmin) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], List.of("key", "stats"), new ArrayList<>());
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("key")) {
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], List.of("pay"), new ArrayList<>());
                }
                if (args[1].equalsIgnoreCase("pay")) {
                    if (args.length == 3) {
                        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .collect(Collectors.toList());
                        return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
                    }
                    if (args.length == 4) {
                        return StringUtil.copyPartialMatches(args[3],
                                new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                    }
                    if (args.length == 5) {
                        return StringUtil.copyPartialMatches(args[4], List.of("1", "5", "10", "64"), new ArrayList<>());
                    }
                }
            }
            return List.of();
        }
        if (!isAdmin) {
            return List.of();
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("editor", "preview", "key", "reload", "stats"),
                    new ArrayList<>());
        }
        if (args[0].equalsIgnoreCase("preview")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1],
                        new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
            }
            return List.of();
        }
        if (args[0].equalsIgnoreCase("key")) {
            if (args.length == 2) {
                ArrayList<String> subs = new ArrayList<String>(List.of("give", "giveall", "remove", "reset"));
                if (keyPayEnabled) {
                    subs.add("pay");
                }
                return StringUtil.copyPartialMatches(args[1], subs, new ArrayList<>());
            }
            switch (args[1].toLowerCase()) {
                case "give": {
                    if (args.length == 3) {
                        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .collect(Collectors.toList());
                        return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
                    }
                    if (args.length == 4) {
                        return StringUtil.copyPartialMatches(args[3],
                                new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                    }
                    if (args.length != 5)
                        break;
                    return StringUtil.copyPartialMatches(args[4], List.of("1", "5", "10", "64"), new ArrayList<>());
                }
                case "giveall": {
                    if (args.length == 3) {
                        return StringUtil.copyPartialMatches(args[2],
                                new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                    }
                    if (args.length != 4)
                        break;
                    return StringUtil.copyPartialMatches(args[3], List.of("1", "5", "10", "64"), new ArrayList<>());
                }
                case "remove": {
                    if (args.length == 3) {
                        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .collect(Collectors.toList());
                        return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
                    }
                    if (args.length == 4) {
                        return StringUtil.copyPartialMatches(args[3],
                                new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                    }
                    if (args.length != 5)
                        break;
                    return StringUtil.copyPartialMatches(args[4], List.of("1", "5", "10", "64"), new ArrayList<>());
                }
                case "reset": {
                    if (args.length == 3) {
                        ArrayList<String> opts = new ArrayList<String>();
                        opts.add("all");
                        Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
                        return StringUtil.copyPartialMatches(args[2], opts, new ArrayList<>());
                    }
                    if (args.length != 4)
                        break;
                    return StringUtil.copyPartialMatches(args[3],
                            new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                }
                case "pay": {
                    if (!keyPayEnabled) {
                        return List.of();
                    }
                    if (args.length == 3) {
                        List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .collect(Collectors.toList());
                        return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
                    }
                    if (args.length == 4) {
                        return StringUtil.copyPartialMatches(args[3],
                                new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet()), new ArrayList<>());
                    }
                    if (args.length != 5)
                        break;
                    return StringUtil.copyPartialMatches(args[4], List.of("1", "5", "10", "64"), new ArrayList<>());
                }
            }
        }
        return List.of();
    }
}
