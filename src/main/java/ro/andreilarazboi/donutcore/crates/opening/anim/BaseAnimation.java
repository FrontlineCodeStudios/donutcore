
package ro.andreilarazboi.donutcore.crates.opening.anim;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class BaseAnimation
implements OpeningAnimation {
    protected AnimationContext ctx;
    protected Inventory inv;
    protected int wait = 0;
    protected boolean finished = false;

    @Override
    public void init(AnimationContext ctx) {
        this.ctx = ctx;
        this.inv = ctx.inv();
        this.onInit();
    }

    protected abstract void onInit();

    @Override
    public boolean tick() {
        if (this.finished) {
            return true;
        }
        if (this.wait > 0) {
            --this.wait;
            return false;
        }
        this.onTick();
        return this.finished;
    }

    protected abstract void onTick();

    protected void finishWin() {
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.pling(), 1.0f, 1.8f);
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.win(), 0.9f, 1.2f);
        this.finished = true;
    }

    protected ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void fillAll(Material mat) {
        ItemStack it = this.pane(mat);
        for (int i = 0; i < this.inv.getSize(); ++i) {
            this.inv.setItem(i, it);
        }
    }

    protected ItemStack safeClone(ItemStack it) {
        if (it == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack c = it.clone();
        if (!c.getType().isAir()) {
            c.setAmount(1);
        }
        return c;
    }

    protected ItemStack randomFromPool() {
        List<ItemStack> list = this.ctx.pool();
        if (list == null || list.isEmpty()) {
            return new ItemStack(Material.BARRIER);
        }
        return this.safeClone(list.get(this.ctx.rng().nextInt(list.size())));
    }

    protected boolean isBorderSlot54(int slot) {
        if (slot < 0 || slot >= 54) {
            return false;
        }
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    protected void fillBorder54(Material mat) {
        if (this.inv.getSize() != 54) {
            return;
        }
        ItemStack it = this.pane(mat);
        for (int i = 0; i < 54; ++i) {
            if (!this.isBorderSlot54(i)) continue;
            this.inv.setItem(i, it);
        }
    }

    protected List<Integer> innerSlots54() {
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (int r = 1; r <= 4; ++r) {
            for (int c = 1; c <= 7; ++c) {
                out.add(r * 9 + c);
            }
        }
        return out;
    }

    protected int genericSlow(int frame, int total, int minDelay, int maxDelay) {
        int left = total - frame;
        if (left <= 0) return maxDelay;
        int quarter = Math.max(1, total / 4);
        if (left > quarter) return minDelay;
        double t = 1.0 - (double)left / (double)quarter;
        int d = (int)Math.round((double)minDelay + (double)(maxDelay - minDelay) * t);
        return Math.max(minDelay, Math.min(maxDelay, d));
    }
}

