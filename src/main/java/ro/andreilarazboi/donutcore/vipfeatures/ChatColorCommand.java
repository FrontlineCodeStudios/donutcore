package ro.andreilarazboi.donutcore.vipfeatures;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

/**
 * /chatcolor <color|reset>
 *
 * <p>Accepted formats:</p>
 * <ul>
 *   <li>Classic: {@code &0}–{@code &9}, {@code &a}–{@code &f}</li>
 *   <li>Hex:     {@code &#RRGGBB} (e.g. {@code &#FF5500})</li>
 * </ul>
 *
 * <p>Requires permission {@code donutcore.vip.chatcolor}.</p>
 */
public class ChatColorCommand implements CommandExecutor, TabCompleter {

    private static final Pattern CLASSIC = Pattern.compile("^&[0-9a-fA-F]$");
    private static final Pattern HEX     = Pattern.compile("^&#[0-9a-fA-F]{6}$");

    private static final List<String> SUGGESTIONS = List.of(
            "reset",
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
            "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
            "&#FF5500", "&#00AAFF", "&#AA00FF"
    );

    private final VipDataManager dataManager;

    public ChatColorCommand(VipDataManager dataManager) {
        this.dataManager = dataManager;
    }

    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("donutcore.vip.chatcolor")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String input = args[0];

        if (input.equalsIgnoreCase("reset")) {
            dataManager.resetChatColor(player.getUniqueId());
            player.sendMessage("§aChat color §rhas been §areset §rto your rank's default.");
            return true;
        }

        if (!CLASSIC.matcher(input).matches() && !HEX.matcher(input).matches()) {
            player.sendMessage("§cInvalid color format.");
            sendUsage(player);
            return true;
        }

        dataManager.setChatColor(player.getUniqueId(), input);

        // Build a colored preview using Adventure
        Component preview = buildColoredText(input, "■■■ This is your new chat color");
        player.sendMessage(
                Component.text("§aChat color set! Preview: ").append(preview)
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("donutcore.vip.chatcolor")) return null;
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return SUGGESTIONS.stream()
                    .filter(s -> s.toLowerCase().startsWith(typed))
                    .toList();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendUsage(Player player) {
        player.sendMessage("§eUsage: /chatcolor <color|reset>");
        player.sendMessage("§7Classic codes: §r&0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f");
        player.sendMessage("§7Hex format:    §r&#RRGGBB  §8(e.g. &#FF5500)");
    }

    /**
     * Returns an Adventure {@link Component} with the given text colored by {@code colorCode}.
     * Supports both classic {@code &X} and hex {@code &#RRGGBB} codes.
     */
    static Component buildColoredText(String colorCode, String text) {
        if (CLASSIC.matcher(colorCode).matches()) {
            return LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(colorCode + text);
        } else if (HEX.matcher(colorCode).matches()) {
            // &#RRGGBB -> TextColor
            String hex = "#" + colorCode.substring(2); // &#RRGGBB → #RRGGBB
            TextColor color = TextColor.fromHexString(hex);
            return color != null
                    ? Component.text(text, color)
                    : Component.text(text);
        }
        return Component.text(text);
    }
}
