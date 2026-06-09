package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class EnderChestInventoryListener implements Listener {
    private final DonutEnderChest plugin;

    public EnderChestInventoryListener(DonutEnderChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;

        if (holder.isReadOnly()) {
            if (event.getAction() != InventoryAction.NOTHING) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.sendMessage(plugin.msgComponent("cannot-modify"));
                }
            }
            return;
        }

        ItemStack toPlace = getItemBeingPlacedInTop(event);
        if (toPlace != null && !toPlace.getType().isAir() && plugin.getEnderChestManager().isBlacklisted(toPlace)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(plugin.formatComponent(
                        plugin.getConfig().getString("blacklisted-message",
                                "&4You cannot put that item into an ender chest.")));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;

        if (holder.isReadOnly()) {
            event.setCancelled(true);
            return;
        }

        int topSize = event.getInventory().getSize();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (affectsTop && plugin.getEnderChestManager().isBlacklisted(event.getOldCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(plugin.formatComponent(
                        plugin.getConfig().getString("blacklisted-message",
                                "&4You cannot put that item into an ender chest.")));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnderChestHolder holder)) return;
        if (holder.isReadOnly()) return;

        UUID ownerUUID = holder.getOwnerUUID();
        if (plugin.isSuppressSave(ownerUUID)) return;

        // Snapshot inventory contents on the main thread before going async
        Map<Integer, ItemStack> snapshot = plugin.getEnderChestManager()
                .buildSaveSnapshot(holder, event.getInventory());

        plugin.runAsync(() -> plugin.getDataManager().saveItems(ownerUUID, snapshot));
    }

    private ItemStack getItemBeingPlacedInTop(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        Inventory clicked = event.getClickedInventory();
        InventoryAction action = event.getAction();

        if (clicked == top) {
            return switch (action) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
                case HOTBAR_SWAP -> {
                    int hotbarSlot = event.getHotbarButton();
                    if (hotbarSlot >= 0) yield event.getWhoClicked().getInventory().getItem(hotbarSlot);
                    yield event.getWhoClicked().getInventory().getItemInOffHand();
                }
                default -> null;
            };
        } else if (clicked != null && action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return event.getCurrentItem();
        }
        return null;
    }
}
