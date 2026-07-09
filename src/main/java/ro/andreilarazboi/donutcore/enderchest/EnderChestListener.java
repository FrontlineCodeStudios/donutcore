package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ro.andreilarazboi.donutcore.DonutCore;

public class EnderChestListener implements Listener {
    private final DonutEnderChest plugin;

    public EnderChestListener(DonutEnderChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;

        Player player = event.getPlayer();

        if (!DonutCore.getInstance().getEnderChestModule().isActive()) {
            if (player.hasPermission("donutcore.admin") || player.isOp()) {
                player.sendMessage(plugin.formatComponent("&cEnderChest module is disabled &7— opening vanilla ender chest."));
                player.sendMessage(plugin.formatComponent("&e⚠ Warning: &fItems stored in the vanilla ender chest &ccannot be imported &fback to DonutCore's ender chest if the module is re-enabled."));
            }
            return;
        }

        event.setCancelled(true);
        plugin.getEnderChestManager().openOwnChest(player, event.getClickedBlock());
    }
}
