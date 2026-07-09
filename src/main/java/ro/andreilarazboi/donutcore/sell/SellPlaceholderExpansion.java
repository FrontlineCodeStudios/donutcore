package ro.andreilarazboi.donutcore.sell;

import java.util.UUID;
import ro.andreilarazboi.donutcore.sell.DonutSell;
import ro.andreilarazboi.donutcore.sell.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class SellPlaceholderExpansion
extends PlaceholderExpansion {
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
        return this.plugin.getDescription().getAuthors().toString();
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("totalsold")) {
            UUID uuid = player.getUniqueId();
            return this.plugin.getFormattedTotalSold(uuid);
        }
        if (identifier.equalsIgnoreCase("toggleworth_status") || identifier.equalsIgnoreCase("worth_status")) {
            boolean enabled = this.plugin.isWorthEnabled(player.getUniqueId());
            String path = enabled ? "placeholders.toggleworth-status.enabled" : "placeholders.toggleworth-status.disabled";
            String fallback = enabled ? "&aON" : "&cOFF";
            return Utils.formatColors(this.plugin.getConfig().getString(path, fallback));
        }
        return null;
    }
}

