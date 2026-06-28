
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CircleSpinEatAnimation
extends BaseAnimation {
    private final int[] ring = new int[]{10, 11, 12, 13, 14, 15, 16, 25, 34, 43, 42, 41, 40, 39, 38, 37, 28, 19};
    private final Map<Integer, ItemStack> itemAt = new HashMap<Integer, ItemStack>();
    private final Set<Integer> covered = new HashSet<Integer>();
    private int winnerSlot = -1;

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        this.winnerSlot = this.ring[this.ctx.rng().nextInt(this.ring.length)];
        for (int s : this.ring) {
            this.itemAt.put(s, this.randomFromPool());
        }
        for (int s : this.ring) {
            this.inv.setItem(s, this.safeClone(this.itemAt.get(s)));
        }
    }

    @Override
    protected void onTick() {
        Integer eat;
        int s;
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        ArrayList<Integer> uncovered = new ArrayList<Integer>();
        for (int s2 : this.ring) {
            if (this.covered.contains(s2)) continue;
            uncovered.add(s2);
        }
        if (uncovered.size() <= 1) {
            for (int s2 : this.ring) {
                this.inv.setItem(s2, this.pane(Material.BLACK_STAINED_GLASS_PANE));
            }
            this.inv.setItem(this.winnerSlot, this.safeClone(this.ctx.reward()));
            this.finishWin();
            return;
        }
        if (uncovered.size() >= 2) {
            ItemStack lastItem = this.safeClone(this.itemAt.get(uncovered.get(uncovered.size() - 1)));
            for (int i = uncovered.size() - 1; i > 0; --i) {
                this.itemAt.put(uncovered.get(i), this.safeClone(this.itemAt.get(uncovered.get(i - 1))));
            }
            this.itemAt.put(uncovered.get(0), lastItem);
        }
        for (Iterator<Integer> iter = uncovered.iterator(); iter.hasNext(); ) {
            s = iter.next();
            this.inv.setItem(s, this.safeClone(this.itemAt.get(s)));
        }
        for (Iterator<Integer> iter = this.covered.iterator(); iter.hasNext(); ) {
            s = iter.next();
            if (this.inv.getItem(s) != null && this.inv.getItem(s).getType() != Material.BLACK_STAINED_GLASS_PANE) continue;
            this.inv.setItem(s, this.pane(Material.PURPLE_STAINED_GLASS_PANE));
        }
        if (this.ctx.rng().nextInt(2) == 0 && (eat = this.pickCoverSlot()) != null) {
            Material m = this.ctx.rng().nextBoolean() ? Material.MAGENTA_STAINED_GLASS_PANE : Material.PURPLE_STAINED_GLASS_PANE;
            this.inv.setItem(eat.intValue(), this.pane(m));
            this.covered.add(eat);
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.65f, 1.0f);
        this.wait = this.circleDelay(uncovered.size());
    }

    private Integer pickCoverSlot() {
        ArrayList<Integer> can = new ArrayList<Integer>();
        for (int s : this.ring) {
            if (s == this.winnerSlot || this.covered.contains(s)) continue;
            can.add(s);
        }
        if (can.isEmpty()) {
            return null;
        }
        return can.get(this.ctx.rng().nextInt(can.size()));
    }

    private int circleDelay(int remainingUncovered) {
        if (remainingUncovered >= 14) {
            return 0;
        }
        if (remainingUncovered >= 10) {
            return 1;
        }
        if (remainingUncovered >= 7) {
            return 2;
        }
        if (remainingUncovered >= 4) {
            return 4;
        }
        return 6;
    }
}

