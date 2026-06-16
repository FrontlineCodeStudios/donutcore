
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class GuiUtil {
    private GuiUtil() {
    }

    public static void playClick(DonutCrates plugin, Player p) {
        p.playSound(p.getLocation(), Utils.resolveSound(plugin.cfg.config.getString("sounds.click", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK), 1.0f, 1.0f);
    }

    public static void playNoKeySound(DonutCrates plugin, Player p) {
        p.playSound(p.getLocation(), Utils.resolveSound(plugin.cfg.config.getString("sounds.no-key", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
    }

    public static void sendNoKeysMessage(DonutCrates plugin, Player p, String crate) {
        String raw = plugin.cfg.config.getString("messages.no-keys", "&#ff5555You don't have any keys for this crate!").replace("%crate%", crate);
        plugin.msg(p, raw);
        GuiUtil.playNoKeySound(plugin, p);
    }

    public static boolean isDeleteConfirmTitle(DonutCrates plugin, String title) {
        ConfigurationSection c = plugin.cfg.config.getConfigurationSection("delete-confirm-menu");
        String expected = Utils.formatColors(c != null ? c.getString("title", "&#8b0000\u1d05\u1d07\u029f\u1d07\u1d1b\u1d07 \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d") : "&#8b0000\u1d05\u1d07\u029f\u1d07\u1d1b\u1d07 \u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d");
        return title.equals(expected);
    }
}

