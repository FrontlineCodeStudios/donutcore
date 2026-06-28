
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class ExplosionEliminationAnimation
extends BaseAnimation {
    private List<Integer> slots;
    private final Map<Integer, ItemStack> itemAt = new HashMap<Integer, ItemStack>();
    private final Set<Integer> exploded = new HashSet<Integer>();
    private int winnerSlot = -1;
    private int phase = 0;
    private int flashHold = 0;

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
        this.fillBorder54(Material.RED_STAINED_GLASS_PANE);
        List<Integer> inner = this.innerSlots54();
        inner.removeIf(s -> s == 22);
        Collections.shuffle(inner, this.ctx.rng());
        this.slots = new ArrayList<Integer>(inner.subList(0, Math.min(21, inner.size())));
        this.winnerSlot = this.slots.get(this.ctx.rng().nextInt(this.slots.size()));
        for (int s2 : this.slots) {
            this.itemAt.put(s2, this.randomFromPool());
        }
        for (int i = 0; i < this.inv.getSize(); ++i) {
            if (this.isBorderSlot54(i)) continue;
            if (i == 22) {
                this.inv.setItem(i, this.pane(Material.BLACK_STAINED_GLASS_PANE));
                continue;
            }
            if (this.slots.contains(i)) {
                this.inv.setItem(i, this.safeClone(this.itemAt.get(i)));
                continue;
            }
            this.inv.setItem(i, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        }
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        this.phase = 0;
        this.flashHold = 0;
    }

    @Override
    protected void onTick() {
        this.fillBorder54(Material.RED_STAINED_GLASS_PANE);
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        int alive = 0;
        for (int s : this.slots) {
            if (this.exploded.contains(s)) continue;
            ++alive;
        }
        if (alive <= 1) {
            for (int s : this.slots) {
                if (s == this.winnerSlot) continue;
                this.inv.setItem(s, this.pane(Material.RED_STAINED_GLASS_PANE));
            }
            this.inv.setItem(this.winnerSlot, this.safeClone(this.ctx.reward()));
            this.finishWin();
            return;
        }
        if (this.phase == 0) {
            Integer s;
            for (int i = 0; i < 6 && (s = this.pickAliveNonWinner()) != null; ++i) {
                this.inv.setItem(s.intValue(), this.pane(Material.RED_STAINED_GLASS_PANE));
            }
            this.ctx.player().playSound(this.ctx.player().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.22f, 1.25f);
            this.flashHold = 0;
            this.phase = 1;
            this.wait = 0;
            return;
        }
        ++this.flashHold;
        if (this.flashHold < 2) {
            this.wait = 0;
            return;
        }
        Integer dead = this.pickAliveNonWinner();
        if (dead != null) {
            this.exploded.add(dead);
            this.inv.setItem(dead.intValue(), this.pane(Material.RED_STAINED_GLASS_PANE));
        }
        for (int s : this.slots) {
            if (s == this.winnerSlot || this.exploded.contains(s) || this.inv.getItem(s) == null || this.inv.getItem(s).getType() != Material.RED_STAINED_GLASS_PANE) continue;
            this.inv.setItem(s, this.safeClone(this.itemAt.get(s)));
        }
        if (!this.exploded.contains(this.winnerSlot)) {
            this.inv.setItem(this.winnerSlot, this.safeClone(this.itemAt.get(this.winnerSlot)));
        }
        int remainingAlive = 0;
        for (int s : this.slots) {
            if (this.exploded.contains(s)) continue;
            ++remainingAlive;
        }
        this.wait = this.explosionDelay(remainingAlive);
        this.phase = 0;
    }

    private Integer pickAliveNonWinner() {
        ArrayList<Integer> can = new ArrayList<Integer>();
        for (int s : this.slots) {
            if (s == this.winnerSlot || this.exploded.contains(s)) continue;
            can.add(s);
        }
        if (can.isEmpty()) {
            return null;
        }
        return can.get(this.ctx.rng().nextInt(can.size()));
    }

    private int explosionDelay(int aliveCount) {
        if (aliveCount >= 18) {
            return 0;
        }
        if (aliveCount >= 12) {
            return 1;
        }
        if (aliveCount >= 8) {
            return 2;
        }
        if (aliveCount >= 5) {
            return 4;
        }
        return 6;
    }
}

