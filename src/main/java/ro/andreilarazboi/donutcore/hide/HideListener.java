package ro.andreilarazboi.donutcore.hide;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

/**
 * Restores a player's original identity silently when they disconnect
 * while VIP-hide or staff-hide is active.
 * This prevents any persistent skin / tab-name corruption.
 */
public class HideListener implements Listener {

    private final HideManager manager;

    public HideListener(HideManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // If the joining player is staff, add them as an observer so they can
        // see hidden players' name tags above their heads.
        manager.addStaffObserver(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Silently restore identity on disconnect
        if (manager.isStaffHidden(player.getUniqueId())) {
            manager.staffShowSilent(player);
        } else if (manager.isVipHidden(player.getUniqueId())) {
            manager.vipShowSilent(player);
        }
        // Remove from staff observer role
        manager.removeStaffObserver(player);
    }
}
