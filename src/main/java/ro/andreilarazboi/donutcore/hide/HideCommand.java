package ro.andreilarazboi.donutcore.hide;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /hide — toggles VIP / Streamer hide.
 *
 * <ul>
 *   <li>Name tag above head: hidden (Scoreboard team)</li>
 *   <li>Tab entry: blank</li>
 *   <li>Skin: changed to a random entry from {@code hide.vip-fake-skins} in config</li>
 * </ul>
 *
 * <p>Requires permission {@code donutcore.hide.vip}.</p>
 */
public class HideCommand implements CommandExecutor, TabCompleter {

    private final HideManager manager;

    public HideCommand(HideManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("donutcore.hide.vip")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        boolean nowHidden = manager.toggleVipHide(player);

        if (nowHidden) {
            player.sendMessage("§aYou are now §lhidden§r§a.");
            player.sendMessage("§7→ Name tag concealed  §8|  §7Tab entry cleared  §8|  §7Random skin being applied...");
        } else {
            player.sendMessage("§aYou are now §lvisible§r§a. Your original identity has been fully restored.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return null; // no args needed
    }
}
