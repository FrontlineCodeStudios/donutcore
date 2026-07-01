package ro.andreilarazboi.donutcore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.Set;

public class CommandVisibilityListener implements Listener {

    private final DonutCore plugin;

    private static final Set<String> CRATES_CMDS = Set.of(
        "donutcrate", "dcrate", "crate",
        "donutcore:donutcrate", "donutcore:dcrate", "donutcore:crate"
    );

    private static final Set<String> SELL_CMDS = Set.of(
        "sell", "sellmulti", "worth", "toggleworth", "sellhistory", "donutsell",
        "donutcore:sell", "donutcore:sellmulti", "donutcore:worth",
        "donutcore:toggleworth", "donutcore:sellhistory", "donutcore:donutsell"
    );

    private static final Set<String> ENDERCHEST_CMDS = Set.of(
        "enderchest", "echest", "clearechest", "clearenderchest",
        "donutcore:enderchest", "donutcore:echest",
        "donutcore:clearechest", "donutcore:clearenderchest"
    );

    private static final Set<String> ARENA_CMDS = Set.of(
        "donutarena", "donutcore:donutarena"
    );

    private static final Set<String> STASH_CMDS = Set.of(
        "donutstash", "stash",
        "donutcore:donutstash", "donutcore:stash"
    );

    private static final Set<String> VIP_CMDS = Set.of(
        "chatcolor", "namecolor",
        "donutcore:chatcolor", "donutcore:namecolor"
    );

    private static final Set<String> HIDE_CMDS = Set.of(
        "hide", "staffhide",
        "donutcore:hide", "donutcore:staffhide"
    );

    public CommandVisibilityListener(DonutCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Collection<String> cmds = event.getCommands();
        if (!plugin.getCratesModule().isActive())           cmds.removeAll(CRATES_CMDS);
        if (!plugin.getSellModule().isActive())             cmds.removeAll(SELL_CMDS);
        if (!plugin.getEnderChestModule().isActive())       cmds.removeAll(ENDERCHEST_CMDS);
        if (!plugin.getArenasModule().isActive())           cmds.removeAll(ARENA_CMDS);
        if (!plugin.getStashModule().isActive())            cmds.removeAll(STASH_CMDS);
        if (!plugin.getVipFeaturesModule().isActive())      cmds.removeAll(VIP_CMDS);
        if (!plugin.getHideModule().isActive())             cmds.removeAll(HIDE_CMDS);
    }
}
