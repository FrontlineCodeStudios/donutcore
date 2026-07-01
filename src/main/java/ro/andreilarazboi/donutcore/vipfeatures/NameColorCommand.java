package ro.andreilarazboi.donutcore.vipfeatures;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

/**
 * /namecolor <color|reset>
 *
 * <p>Accepted formats:</p>
 * <ul>
 *   <li>Classic: {@code &0}–{@code &9}, {@code &a}–{@code &f}</li>
 *   <li>Hex:     {@code &#RRGGBB} (e.g. {@code &#FF5500})</li>
 * </ul>
 *
 * <p>Requires permission {@code donutcore.vip.namecolor}.</p>
 */
public class NameColorCommand implements CommandExecutor, TabCompleter {

    private static final Pattern CLASSIC = Pattern.compile("^&[0-9a-fA-F]$");
    private static final Pattern HEX     = Pattern.compile("^&#[0-9a-fA-F]{6}$");

    private static final List<String> SUGGESTIONS = List.of(
            "reset",
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
            "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
            "&#FF5500", "&#00AAFF", "&#AA00FF"
    );

    private final VipDataManager dataManager;

    public NameColorCommand(VipDataManager dataManager) {
        this.dataManager = dataManager;
    }

    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("donutcore.vip.namecolor")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String input = args[0];

        if (input.equalsIgnoreCase("reset")) {
            dataManager.resetNameColor(player.getUniqueId());
            player.sendMessage("§aName color §rhas been §areset §rto your rank's default.");
            return true;
        }

        if (!CLASSIC.matcher(input).matches() && !HEX.matcher(input).matches()) {
            player.sendMessage("§cInvalid color format.");
            sendUsage(player);
            return true;
        }

        dataManager.setNameColor(player.getUniqueId(), input);

        // Build a colored preview using Adventure (reuse helper from ChatColorCommand)
        Component preview = ChatColorCommand.buildColoredText(input, player.getName());
        player.sendMessage(
                Component.text("§aName color set! Preview: ").append(preview)
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("donutcore.vip.namecolor")) return null;
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return SUGGESTIONS.stream()
                    .filter(s -> s.toLowerCase().startsWith(typed))
                    .toList();
        }
        return null;
    }

    // -------------------------------------------------------------------------

    private void sendUsage(Player player) {
        player.sendMessage("§eUsage: /namecolor <color|reset>");
        player.sendMessage("§7Classic codes: §r&0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f");
        player.sendMessage("§7Hex format:    §r&#RRGGBB  §8(e.g. &#FF5500)");
    }
}
