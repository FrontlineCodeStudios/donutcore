
package ro.andreilarazboi.donutcore.crates;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class DonutCratesPlaceholder
extends PlaceholderExpansion {
    public String getIdentifier() {
        return "donutcrate";
    }

    public String getAuthor() {
        return "DonutCratesPlugin";
    }

    public String getVersion() {
        return DonutCrates.instance.getPlugin().getPluginMeta().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null) {
            return "";
        }
        if (params.startsWith("key_")) {
            String crate = params.substring(4);
            Player online = player.getPlayer();
            int keys = online != null ? DonutCrates.instance.dataMgr.getKeys(online, crate) : 0;
            return String.valueOf(keys);
        }
        return null;
    }
}

