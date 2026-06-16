package ro.andreilarazboi.donutcore.stash;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import ro.andreilarazboi.donutcore.sell.Utils;

public class StashListener implements Listener {

    private static final String STASH_PERM = "donutcore.admin.stash";
    private final StashManager manager;

    public StashListener(StashManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (player.hasPermission(STASH_PERM) || player.isOp()) return;

        Block block = resolveBlock(event.getInventory().getHolder());
        if (block == null || !manager.isStash(block)) return;

        String stashName = manager.getStashName(block);
        String loc = block.getWorld().getName() + " " + block.getX() + " " + block.getY() + " " + block.getZ();
        String alert = "&c[StashAlert] &f" + player.getName() + " &copened stash &e"
                + stashName + " &cat &7(" + loc + ")";

        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission(STASH_PERM) || admin.isOp()) {
                admin.sendMessage(Utils.toComponent(alert));
            }
        }
        Bukkit.getLogger().warning("[StashAlert] " + player.getName() + " opened stash: " + stashName + " at " + loc);
    }

    private Block resolveBlock(InventoryHolder holder) {
        if (holder instanceof Chest chest) return chest.getBlock();
        if (holder instanceof DoubleChest dc) {
            if (dc.getLeftSide() instanceof Chest l && manager.isStash(l.getBlock())) return l.getBlock();
            if (dc.getRightSide() instanceof Chest r && manager.isStash(r.getBlock())) return r.getBlock();
        }
        return null;
    }
}
