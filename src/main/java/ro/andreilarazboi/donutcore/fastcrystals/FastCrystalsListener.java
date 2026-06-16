package ro.andreilarazboi.donutcore.fastcrystals;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FastCrystalsListener implements Listener {

    private final long immunityMs;
    private final Map<UUID, Long> recentPlacements = new ConcurrentHashMap<>();

    public FastCrystalsListener(long immunityMs) {
        this.immunityMs = immunityMs;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCrystalPlace(EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.END_CRYSTAL) return;
        Player player = event.getPlayer();
        if (player == null) return;
        recentPlacements.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        Long placedAt = recentPlacements.get(player.getUniqueId());
        if (placedAt == null) return;
        if (System.currentTimeMillis() - placedAt <= immunityMs) {
            event.setCancelled(true);
        }
    }

    public void cleanup() {
        recentPlacements.clear();
    }
}
