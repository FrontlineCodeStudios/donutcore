package ro.andreilarazboi.donutcore.sell;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public class VaultMoneyPlaceholder
extends PlaceholderExpansion {
    private final DonutSell plugin;
    private final Economy econ;
    private final DecimalFormat twoDecimals = new DecimalFormat("#.##");
    private final LinkedHashMap<String, Double> suffixes = new LinkedHashMap<>();

    public VaultMoneyPlaceholder(DonutSell plugin) {
        this.plugin = plugin;
        this.econ = plugin.getEconomy();
        this.loadSuffixes();
    }

    private void loadSuffixes() {
        this.suffixes.clear();
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("money-suffixes");
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                double value = section.getDouble(key);
                this.suffixes.put((String)key, value);
            });
        } else {
            this.suffixes.put("K", 1000.0);
            this.suffixes.put("M", 1000000.0);
            this.suffixes.put("B", 1.0E9);
            this.suffixes.put("T", 1.0E12);
        }
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getIdentifier() {
        return "vaultmoney";
    }

    public String getAuthor() {
        return String.join((CharSequence)", ", this.plugin.getDescription().getAuthors());
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        double balance = this.econ.getBalance((OfflinePlayer)player);
        switch (identifier) {
            case "sell_money": {
                return this.twoDecimals.format(balance);
            }
            case "sell_money_formatted": {
                return this.abbreviate(balance);
            }
        }
        return null;
    }

    private String abbreviate(double value) {
        double abs = Math.abs(value);
        String bestSuffix = "";
        double shortVal = value;
        for (Map.Entry<String, Double> entry : this.suffixes.entrySet()) {
            double threshold = entry.getValue();
            if (!(abs >= threshold)) continue;
            bestSuffix = entry.getKey();
            shortVal = value / threshold;
        }
        return this.twoDecimals.format(shortVal) + bestSuffix;
    }

    public void reload() {
        this.loadSuffixes();
    }
}

