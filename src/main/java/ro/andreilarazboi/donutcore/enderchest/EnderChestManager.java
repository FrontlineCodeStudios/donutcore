package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestManager {
    private final DonutEnderChest plugin;

    public EnderChestManager(DonutEnderChest plugin) {
        this.plugin = plugin;
    }

    public int getRows(Player player) {
        for (int i = 6; i >= 1; i--) {
            if (player.hasPermission("enderchest.size." + i)) return i;
        }
        return plugin.getConfig().getInt("default-rows", 6);
    }

    public boolean isBlacklisted(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String name = item.getType().name();
        for (String entry : plugin.getConfig().getStringList("blacklisted-items")) {
            if (entry.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public String getTitle(int rows, String ownerName) {
        String template = plugin.getConfig().getString("enderchest-names." + rows + "-rows",
                "&7<player>'s Enderchest");
        return plugin.formatColors(template.replace("<player>", ownerName));
    }

    public void openOwnChest(Player player) {
        int rows = getRows(player);
        if (rows == 0) {
            player.sendMessage(plugin.msgComponent("no-enderchest"));
            return;
        }
        openAsync(player, player.getUniqueId(), player.getName(), rows, false);
    }

    public void openOthersChest(Player viewer, OfflinePlayer target) {
        boolean readOnly = !viewer.hasPermission("enderchest.modify.others");
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        openAsync(viewer, target.getUniqueId(), name, 6, readOnly);
    }

    private void openAsync(Player viewer, UUID ownerUUID, String ownerName, int rows, boolean readOnly) {
        plugin.runAsync(() -> {
            Map<Integer, ItemStack> fullItems = plugin.getDataManager().loadItems(ownerUUID);
            plugin.runAtPlayer(viewer, () -> {
                if (!viewer.isOnline()) return;
                String title = getTitle(rows, ownerName);
                EnderChestHolder holder = new EnderChestHolder(ownerUUID, viewer.getUniqueId(), readOnly, rows, fullItems);
                Inventory inv = Bukkit.createInventory(holder, rows * 9, plugin.formatComponent(title));
                holder.setInventory(inv);
                for (Map.Entry<Integer, ItemStack> entry : fullItems.entrySet()) {
                    if (entry.getKey() < rows * 9) inv.setItem(entry.getKey(), entry.getValue());
                }
                viewer.openInventory(inv);
                if (!viewer.getUniqueId().equals(ownerUUID)) {
                    viewer.sendMessage(plugin.formatComponent(plugin.msg("viewing-others").replace("<player>", ownerName)));
                }
            });
        });
    }

    public Map<Integer, ItemStack> buildSaveSnapshot(EnderChestHolder holder, Inventory inventory) {
        Map<Integer, ItemStack> items = new HashMap<>(holder.getFullItemsMap());
        int visibleSlots = holder.getRows() * 9;
        for (int i = 0; i < visibleSlots; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                items.remove(i);
            } else {
                items.put(i, item.clone());
            }
        }
        return items;
    }
}
