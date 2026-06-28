
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import java.util.ArrayList;
import java.util.List;
import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class SpiralEliminationAnimation
extends BaseAnimation {
    private List<Integer> path;
    private int winnerSlot = -1;
    private int pointer = 0;

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        this.path = this.buildInnerSpiralPath54();
        this.path.removeIf(s -> s == 22);
        this.winnerSlot = this.path.get(this.ctx.rng().nextInt(this.path.size()));
        for (int s2 : this.path) {
            this.inv.setItem(s2, this.randomFromPool());
        }
    }

    @Override
    protected void onTick() {
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        int remaining = this.path.size() - this.countBlackOnPath();
        if (remaining <= 1) {
            for (int s : this.path) {
                this.inv.setItem(s, this.pane(Material.BLACK_STAINED_GLASS_PANE));
            }
            this.inv.setItem(this.winnerSlot, this.safeClone(this.ctx.reward()));
            this.finishWin();
            return;
        }
        int guard = 0;
        Integer toRemove = null;
        while (guard++ < this.path.size()) {
            int s = this.path.get(this.pointer % this.path.size());
            ++this.pointer;
            if (s == this.winnerSlot || this.inv.getItem(s) != null && this.inv.getItem(s).getType() == Material.BLACK_STAINED_GLASS_PANE) continue;
            toRemove = s;
            break;
        }
        if (toRemove != null) {
            this.inv.setItem(toRemove.intValue(), this.pane(Material.BLACK_STAINED_GLASS_PANE));
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.6f, 1.05f);
        this.wait = this.spiralDelay(remaining);
    }

    private int countBlackOnPath() {
        int black = 0;
        for (int s : this.path) {
            if (this.inv.getItem(s) == null || this.inv.getItem(s).getType() != Material.BLACK_STAINED_GLASS_PANE) continue;
            ++black;
        }
        return black;
    }

    private int spiralDelay(int remaining) {
        if (remaining >= 24) {
            return 0;
        }
        if (remaining >= 16) {
            return 1;
        }
        if (remaining >= 10) {
            return 2;
        }
        if (remaining >= 6) {
            return 4;
        }
        return 6;
    }

    private List<Integer> buildInnerSpiralPath54() {
        int top = 1;
        int bottom = 4;
        int left = 1;
        int right = 7;
        ArrayList<Integer> out = new ArrayList<Integer>();
        while (top <= bottom && left <= right) {
            int r;
            int c;
            for (c = left; c <= right; ++c) {
                out.add(top * 9 + c);
            }
            for (r = ++top; r <= bottom; ++r) {
                out.add(r * 9 + right);
            }
            --right;
            if (top <= bottom) {
                for (c = right; c >= left; --c) {
                    out.add(bottom * 9 + c);
                }
                --bottom;
            }
            if (left > right) continue;
            for (r = bottom; r >= top; --r) {
                out.add(r * 9 + left);
            }
            ++left;
        }
        return out;
    }
}

