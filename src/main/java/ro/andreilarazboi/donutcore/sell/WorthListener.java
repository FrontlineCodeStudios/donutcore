package ro.andreilarazboi.donutcore.sell;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ro.andreilarazboi.donutcore.sell.SellPacketListener;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WorthListener
implements Listener {
    private final SellPacketListener packetLoreService;
    private final Set<UUID> refreshQueued = ConcurrentHashMap.newKeySet();

    public WorthListener(SellPacketListener packetLoreService) {
        this.packetLoreService = packetLoreService;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        Player player = event.getPlayer();
        this.packetLoreService.inject(player);
        this.scheduleRefresh(player);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        Player player = event.getPlayer();
        this.refreshQueued.remove(player.getUniqueId());
        this.packetLoreService.uninject(player);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onOpen(InventoryOpenEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            this.packetLoreService.setNoWorthOpen(player, this.packetLoreService.isNoWorthInventory(event.getView()));
            this.scheduleRefresh(player);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            this.packetLoreService.setNoWorthOpen(player, false);
            this.scheduleRefresh(player);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onClick(InventoryClickEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            if (player.getGameMode() == GameMode.CREATIVE) {
                return;
            }
            this.scheduleRefresh(player);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDrag(InventoryDragEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            this.scheduleRefresh(player);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!this.packetLoreService.plugin().isModuleActive()) return;
        this.scheduleRefresh(event.getPlayer());
    }

    private void scheduleRefresh(Player player) {
        UUID uniqueId = player.getUniqueId();
        if (!this.refreshQueued.add(uniqueId)) {
            return;
        }
        this.packetLoreService.plugin().runAtPlayer(player, () -> {
            try {
                if (player.isOnline()) {
                    player.updateInventory();
                }
            }
            finally {
                this.refreshQueued.remove(uniqueId);
            }
        });
    }
}

