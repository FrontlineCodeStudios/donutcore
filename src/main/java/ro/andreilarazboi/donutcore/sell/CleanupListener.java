package ro.andreilarazboi.donutcore.sell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings({"deprecation", "removal"})
public class CleanupListener
implements Listener {
    private final DonutSell plugin;
    private final List<String> lorePrefixes;

    public CleanupListener(DonutSell plugin) {
        this.plugin = plugin;
        this.lorePrefixes = plugin.getConfig().getStringList("lore").stream().map(line -> ChatColor.stripColor((String)Utils.formatColors(line.replace("%amount%", ""))).toLowerCase(Locale.ROOT).trim()).toList();
    }

    public void stripAllLore(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            ItemMeta meta;
            if (it == null || !it.hasItemMeta() || (meta = it.getItemMeta()) == null || !meta.hasLore()) continue;
            ArrayList<String> filtered = new ArrayList<String>();
            for (String line : meta.getLore()) {
                String plain = ChatColor.stripColor((String)line).toLowerCase(Locale.ROOT).trim();
                if (!this.lorePrefixes.stream().noneMatch(plain::startsWith)) continue;
                filtered.add(line);
            }
            meta.setLore(filtered.isEmpty() ? null : filtered);
            it.setItemMeta(meta);
        }
        p.updateInventory();
    }

    private boolean worthLoreActiveFor(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        if (!this.plugin.getConfig().getBoolean("display-worth-lore", true)) {
            return false;
        }
        return this.plugin.isWorthEnabled(p.getUniqueId());
    }

    private void resyncNextTick(Player p) {
        this.plugin.runAtPlayerLater(p, () -> {
            if (this.worthLoreActiveFor(p)) {
                p.updateInventory();
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!this.plugin.isModuleActive()) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) {
            this.stripAllLore(p);
        } else {
            this.resyncNextTick(p);
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!this.plugin.isModuleActive()) return;
        Player p = event.getPlayer();
        GameMode newMode = event.getNewGameMode();
        if (newMode == GameMode.CREATIVE) {
            this.stripAllLore(p);
        } else {
            this.resyncNextTick(p);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!this.plugin.isModuleActive()) return;
        LivingEntity livingEntity = e.getEntity();
        if (livingEntity instanceof Player p && p.getGameMode() == GameMode.CREATIVE) {
            e.getItem().setItemStack(this.stripLoreFromStack(e.getItem().getItemStack()));
        }
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent e) {
        if (!this.plugin.isModuleActive()) return;
        e.getTarget().setItemStack(this.stripLoreFromStack(e.getTarget().getItemStack()));
    }

    private ItemStack stripLoreFromStack(ItemStack original) {
        if (original == null || !original.hasItemMeta()) {
            return original;
        }
        ItemStack copy = original.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return copy;
        }
        ArrayList<String> keep = new ArrayList<String>();
        for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor((String)line).toLowerCase(Locale.ROOT).trim();
            if (!this.lorePrefixes.stream().noneMatch(plain::startsWith)) continue;
            keep.add(line);
        }
        meta.setLore(keep.isEmpty() ? null : keep);
        copy.setItemMeta(meta);
        return copy;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!this.plugin.isModuleActive()) return;
        Player p = (Player)e.getWhoClicked();
        if (this.worthLoreActiveFor(p)) {
            this.resyncNextTick(p);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!this.plugin.isModuleActive()) return;
        Player p = (Player)e.getWhoClicked();
        if (this.worthLoreActiveFor(p)) {
            this.resyncNextTick(p);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!this.plugin.isModuleActive()) return;
        Player p = (Player)e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) {
            this.stripAllLore(p);
        } else if (this.worthLoreActiveFor(p)) {
            this.resyncNextTick(p);
        }
    }
}

