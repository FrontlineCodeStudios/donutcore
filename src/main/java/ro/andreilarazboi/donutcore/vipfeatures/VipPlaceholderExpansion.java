package ro.andreilarazboi.donutcore.vipfeatures;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.DonutCore;

/**
 * PlaceholderAPI expansion registered under the identifier {@code donutcore}.
 *
 * <p>Available placeholders:</p>
 * <ul>
 *   <li>{@code %donutcore_chatcolor%} — returns the player's custom chat color
 *       (e.g. {@code "&c"} or {@code "&#FF5500"}) or {@code ""} if not set.</li>
 *   <li>{@code %donutcore_namecolor%} — returns the player's custom name color
 *       or {@code ""} if not set.</li>
 * </ul>
 *
 * <p>An empty return value signals EssentialsX (or any other chat plugin) to
 * apply no extra color, leaving the rank/group color intact.</p>
 */
public class VipPlaceholderExpansion extends PlaceholderExpansion {

    private final DonutCore      plugin;
    private final VipDataManager dataManager;

    public VipPlaceholderExpansion(DonutCore plugin, VipDataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
    }

    @Override public boolean persist()     { return true; }

    @Override
    public boolean canRegister() {
        return plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /** Identifier used in placeholders: {@code %donutcore_<identifier>%}. */
    @Override public String getIdentifier() { return "donutcore"; }

    @Override public String getAuthor()  { return plugin.getPluginMeta().getAuthors().toString(); }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        return switch (identifier) {
            case "chatcolor" -> dataManager.getChatColor(player.getUniqueId());
            case "namecolor" -> dataManager.getNameColor(player.getUniqueId());
            default          -> null;
        };
    }
}
