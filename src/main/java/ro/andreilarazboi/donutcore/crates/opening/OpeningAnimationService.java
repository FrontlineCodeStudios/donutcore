
package ro.andreilarazboi.donutcore.crates.opening;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ro.andreilarazboi.donutcore.crates.opening.anim.AnimationContext;
import ro.andreilarazboi.donutcore.crates.opening.anim.OpeningAnimation;
import ro.andreilarazboi.donutcore.crates.opening.anim.OpeningAnimationRegistry;

public class OpeningAnimationService {
    private final DonutCrates plugin;
    private final OpeningAnimationRegistry registry;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<UUID, BukkitTask>();

    public OpeningAnimationService(DonutCrates plugin) {
        this.plugin = plugin;
        this.registry = new OpeningAnimationRegistry();
    }

    public void stop(Player p) {
        if (p == null) {
            return;
        }
        BukkitTask t = this.activeTasks.remove(p.getUniqueId());
        if (t != null) {
            t.cancel();
        }
    }

    public void play(Player p, String crate, OpeningAnimationType type, List<ItemStack> candidates, ItemStack finalReward, Runnable onFinish) {
        OpeningAnimation anim;
        if (p == null || crate == null || type == null || onFinish == null) {
            return;
        }
        this.stop(p);
        ItemStack reward = finalReward == null || finalReward.getType().isAir() ? new ItemStack(Material.BARRIER) : finalReward.clone();
        reward.setAmount(1);
        ArrayList<ItemStack> pool = new ArrayList<ItemStack>();
        if (candidates != null) {
            for (ItemStack it : candidates) {
                if (it == null || it.getType().isAir()) continue;
                ItemStack c = it.clone();
                c.setAmount(1);
                pool.add(c);
            }
        }
        if (pool.isEmpty()) {
            pool.add(reward.clone());
        }
        if ((anim = this.registry.create(type)) == null) {
            onFinish.run();
            return;
        }
        OpeningAnimationHolder holder = new OpeningAnimationHolder(crate);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)anim.inventorySize(), (String)Utils.formatColors("&#444444" + crate + " Opening..."));
        holder.setInventory(inv);
        ItemStack filler = this.pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, filler);
        }
        Sound tick = this.safeSound("UI_BUTTON_CLICK", Sound.UI_BUTTON_CLICK);
        Sound pling = this.safeSound("BLOCK_NOTE_BLOCK_PLING", Sound.BLOCK_NOTE_BLOCK_PLING);
        Sound win = this.safeSound(this.plugin.cfg.config.getString("sounds.claim", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP);
        AnimationContext ctx = new AnimationContext(this.plugin, p, crate, type, inv, pool, reward, new Random(), tick, pling, win);
        p.openInventory(inv);
        anim.init(ctx);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin.getPlugin(), () -> {
            boolean done;
            if (!p.isOnline() || !(p.getOpenInventory().getTopInventory().getHolder() instanceof OpeningAnimationHolder)) {
                this.stop(p);
                onFinish.run();
                return;
            }
            try {
                done = anim.tick();
            }
            catch (Throwable t1) {
                this.stop(p);
                onFinish.run();
                return;
            }
            if (done) {
                this.stop(p);
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.getPlugin(), onFinish, anim.postWinDelayTicks());
            }
        }, 0L, 1L);
        this.activeTasks.put(p.getUniqueId(), task);
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Utils.toComponent(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Sound safeSound(String name, Sound fallback) {
        return Utils.resolveSound(name, fallback);
    }
}

