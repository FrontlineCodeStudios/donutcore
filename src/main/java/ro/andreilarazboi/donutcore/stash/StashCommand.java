package ro.andreilarazboi.donutcore.stash;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.sell.Utils;

import java.util.List;

public class StashCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "donutcore.admin.stash";
    private final StashManager manager;

    public StashCommand(StashManager manager) {
        this.manager = manager;
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

        boolean isSpawn = label.equalsIgnoreCase("spawnstash");

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(Utils.toComponent("&cLook at a container block (chest, barrel, etc.) first."));
            return true;
        }

        if (isSpawn) {
            if (args.length == 0) {
                player.sendMessage(Utils.toComponent("&cUsage: /spawnstash <name>"));
                return true;
            }
            String name = String.join("_", args).toLowerCase();
            if (!manager.markAsStash(target, name)) {
                player.sendMessage(Utils.toComponent("&cThis block does not support persistent data."));
                return true;
            }
            player.sendMessage(Utils.toComponent("&aStash trap &e" + name + " &aplaced at &7"
                    + target.getX() + " " + target.getY() + " " + target.getZ() + "&a."));
        } else {
            // unspawnstash
            if (!manager.isStash(target)) {
                player.sendMessage(Utils.toComponent("&cThis block is not a stash trap."));
                return true;
            }
            String name = manager.getStashName(target);
            manager.removeStash(target);
            player.sendMessage(Utils.toComponent("&aStash trap &e" + name + " &aremoved."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }
}
