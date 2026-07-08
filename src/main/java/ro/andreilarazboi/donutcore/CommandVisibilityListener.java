package ro.andreilarazboi.donutcore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.Set;

public class CommandVisibilityListener implements Listener {

    private final DonutCore plugin;

    private static final Set<String> CRATES_CMDS = Set.of(
            "crate", "donutcore:crate");

    private static final Set<String> SELL_CMDS = Set.of(
            "sell", "worth", "toggleworth", "sellhistory", "donutsell",
            "donutcore:sell", "donutcore:worth",
            "donutcore:toggleworth", "donutcore:sellhistory", "donutcore:donutsell");

    private static final Set<String> ENDERCHEST_CMDS = Set.of(
            "enderchest", "echest", "clearechest", "clearenderchest",
            "donutcore:enderchest", "donutcore:echest",
            "donutcore:clearechest", "donutcore:clearenderchest");

    public CommandVisibilityListener(DonutCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Collection<String> cmds = event.getCommands();
        if (!plugin.getCratesModule().isActive())
            cmds.removeAll(CRATES_CMDS);
        if (!plugin.getSellModule().isActive())
            cmds.removeAll(SELL_CMDS);
        if (!plugin.getEnderChestModule().isActive())
            cmds.removeAll(ENDERCHEST_CMDS);
    }
}
