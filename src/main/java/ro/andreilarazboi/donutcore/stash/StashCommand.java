package ro.andreilarazboi.donutcore.stash;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.sell.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StashCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "donutcore.admin.stash";
    private final StashManager manager;
    private final StashSchematicManager schematics;

    public StashCommand(StashManager manager, StashSchematicManager schematics) {
        this.manager    = manager;
        this.schematics = schematics;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission(PERM) && !player.isOp()) {
            player.sendMessage(Utils.toComponent("&cNo permission."));
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) { player.sendMessage(Utils.toComponent("&cUsage: /donutstash save <name>")); return true; }
                String name  = args[1].toLowerCase();
                String error = schematics.save(player, name);
                if (error != null) player.sendMessage(Utils.toComponent(error));
                else player.sendMessage(Utils.toComponent(
                    "&aStash &e" + name + " &asaved from your WorldEdit selection &7(air skipped)&a."));
            }
            case "spawn" -> {
                if (args.length < 2) { player.sendMessage(Utils.toComponent("&cUsage: /donutstash spawn <name>")); return true; }
                String name = args[1].toLowerCase();
                if (!schematics.exists(name)) {
                    player.sendMessage(Utils.toComponent("&cNo stash schematic named &e" + name + "&c."));
                    return true;
                }
                try {
                    StashSchematicManager.PasteResult result = schematics.spawn(player, name);
                    if (result == null) {
                        player.sendMessage(Utils.toComponent("&cFailed to paste stash."));
                        return true;
                    }
                    int marked = markContainers(result, name);
                    player.sendMessage(Utils.toComponent("&aStash &e" + name + " &aspawned at your location &7("
                        + marked + " container(s) marked as traps)&a."));
                } catch (IOException e) {
                    player.sendMessage(Utils.toComponent("&cFailed to spawn stash: " + e.getMessage()));
                }
            }
            case "remove" -> {
                if (args.length < 2) { player.sendMessage(Utils.toComponent("&cUsage: /donutstash remove <name>")); return true; }
                String name = args[1].toLowerCase();
                if (schematics.delete(name)) player.sendMessage(Utils.toComponent("&aStash schematic &e" + name + " &adeleted."));
                else player.sendMessage(Utils.toComponent("&cNo stash schematic named &e" + name + "&c."));
            }
            case "list" -> {
                List<String> names = schematics.list();
                if (names.isEmpty()) player.sendMessage(Utils.toComponent("&7No stash schematics saved."));
                else player.sendMessage(Utils.toComponent("&eStash schematics: &f" + String.join(", ", names)));
            }
            case "mark" -> {
                if (args.length < 2) { player.sendMessage(Utils.toComponent("&cUsage: /donutstash mark <name>")); return true; }
                Block target = player.getTargetBlockExact(5);
                if (target == null || !(target.getState() instanceof Container)) {
                    player.sendMessage(Utils.toComponent("&cLook at a container block (chest, barrel, etc.) first."));
                    return true;
                }
                String name = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
                manager.markAsStash(target, name);
                player.sendMessage(Utils.toComponent("&aMarked the targeted container as stash trap &e" + name + "&a."));
            }
            case "unmark" -> {
                Block target = player.getTargetBlockExact(5);
                if (target == null || !manager.isStash(target)) {
                    player.sendMessage(Utils.toComponent("&cYou are not looking at a marked stash trap."));
                    return true;
                }
                String name = manager.getStashName(target);
                manager.removeStash(target);
                player.sendMessage(Utils.toComponent("&aStash trap &e" + name + " &aremoved."));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private int markContainers(StashSchematicManager.PasteResult result, String name) {
        int count = 0;
        World world = result.world();
        for (int x = result.min().x(); x <= result.max().x(); x++) {
            for (int y = result.min().y(); y <= result.max().y(); y++) {
                for (int z = result.min().z(); z <= result.max().z(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockState state = block.getState();
                    if (state instanceof Container && manager.markAsStash(block, name)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Utils.toComponent("&e&l--- Stash Commands ---"));
        player.sendMessage(Utils.toComponent("&7/donutstash save <name>    &8— save your WorldEdit selection (air skipped)"));
        player.sendMessage(Utils.toComponent("&7/donutstash spawn <name>   &8— paste it at your location, auto-marks containers"));
        player.sendMessage(Utils.toComponent("&7/donutstash remove <name>  &8— delete a saved schematic"));
        player.sendMessage(Utils.toComponent("&7/donutstash list           &8— list saved schematics"));
        player.sendMessage(Utils.toComponent("&7/donutstash mark <name>    &8— mark the container you're looking at"));
        player.sendMessage(Utils.toComponent("&7/donutstash unmark         &8— unmark the container you're looking at"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission(PERM) && !sender.isOp()) return List.of();
        if (args.length == 1) return filter(List.of("save", "spawn", "remove", "list", "mark", "unmark"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("remove")))
            return filter(schematics.list(), args[1]);
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String lo = prefix.toLowerCase();
        return opts.stream().filter(o -> o.toLowerCase().startsWith(lo)).toList();
    }
}
