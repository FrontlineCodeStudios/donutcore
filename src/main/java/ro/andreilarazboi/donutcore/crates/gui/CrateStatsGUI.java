
package ro.andreilarazboi.donutcore.crates.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ro.andreilarazboi.donutcore.crates.CrateStatsHolder;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateStatsGUI {
    private final DonutCrates plugin;

    public CrateStatsGUI(DonutCrates plugin) {
        this.plugin = plugin;
    }

    public Inventory buildMain(Player player) {
        CrateStatsHolder holder = new CrateStatsHolder(null, false);
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#444444Crate Stats"));
        holder.setInventory(inv);
        ArrayList<String> crates = new ArrayList<String>(this.plugin.crateMgr.crateBlocks.keySet());
        crates.sort(String::compareToIgnoreCase);
        for (int slot = 0; slot < 54 && slot < crates.size(); ++slot) {
            String crate = (String)crates.get(slot);
            int opened = this.plugin.dataMgr.getCrateOpenCount(player.getUniqueId(), crate);
            Material material = this.resolveCrateMaterial(crate);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String crateDisplay = this.plugin.getCrateDisplayNameRaw(crate);
            meta.displayName(Utils.toComponent("&r" + crateDisplay));
            meta.lore(List.of(Utils.toComponent("&7Totaled Opened: &f" + opened), Utils.toComponent("&8Click to view last 45 rewards")));
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }
        return inv;
    }

    public Inventory buildHistory(Player player, String crate) {
        CrateStatsHolder holder = new CrateStatsHolder(crate, true);
        Inventory inv = Bukkit.createInventory(holder, 54, Utils.toComponent("&#0fe30f" + crate + " Rewards"));
        holder.setInventory(inv);
        List<ItemStack> rewards = this.plugin.dataMgr.getRecentRewardItems(player.getUniqueId(), crate, 45);
        if (rewards.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta noneMeta = none.getItemMeta();
            if (noneMeta != null) {
                noneMeta.displayName(Utils.toComponent("&#ff5555No rewards yet"));
                noneMeta.lore(List.of(Utils.toComponent("&7Open this crate to build your history.")));
                none.setItemMeta(noneMeta);
            }
            inv.setItem(22, none);
        } else {
            for (int i = 0; i < rewards.size() && i < 45; ++i) {
                ItemStack reward = rewards.get(i);
                if (reward == null || reward.getType().isAir()) continue;
                inv.setItem(i, reward.clone());
            }
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Utils.toComponent("&#0f99e3Back"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(53, back);
        return inv;
    }

    private Material resolveCrateMaterial(String crate) {
        Block block = this.plugin.crateMgr.crateBlocks.get(crate);
        if (block != null && block.getType() != null && block.getType().isItem()) {
            return block.getType();
        }
        String configured = this.plugin.cfg.crates.getString("Crates." + crate + ".stats-material", "EMERALD_ORE");
        if (configured == null || configured.isBlank()) {
            return Material.EMERALD_ORE;
        }
        try {
            return Material.valueOf((String)configured.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored) {
            return Material.EMERALD_ORE;
        }
    }
}

