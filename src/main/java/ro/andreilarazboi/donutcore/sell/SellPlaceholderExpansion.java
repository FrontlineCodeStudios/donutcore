package ro.andreilarazboi.donutcore.sell;

import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class SellPlaceholderExpansion extends PlaceholderExpansion {
    private final DonutSell plugin;

    public SellPlaceholderExpansion(DonutSell plugin) {
        this.plugin = plugin;
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return this.plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String getIdentifier() {
        return "sell";
    }

    public String getAuthor() {
        return this.plugin.getPluginMeta().getAuthors().toString();
    }

    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("totalsold")) {
            UUID uuid = player.getUniqueId();
            return this.plugin.getFormattedTotalSold(uuid);
        }
        return null;
    }
}
