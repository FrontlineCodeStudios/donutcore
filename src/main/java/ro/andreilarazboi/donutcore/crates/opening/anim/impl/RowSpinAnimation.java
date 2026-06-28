
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class RowSpinAnimation
extends BaseAnimation {
    private final int[] strip = new int[]{10, 11, 12, 13, 14, 15, 16};
    private int frame = 0;

    @Override
    public int inventorySize() {
        return 27;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
        for (int s : this.strip) {
            this.inv.setItem(s, this.randomFromPool());
        }
        this.inv.setItem(4, this.pane(Material.LIME_STAINED_GLASS_PANE));
        this.inv.setItem(22, this.pane(Material.LIME_STAINED_GLASS_PANE));
    }

    @Override
    protected void onTick() {
        this.inv.setItem(4, this.pane(Material.LIME_STAINED_GLASS_PANE));
        this.inv.setItem(22, this.pane(Material.LIME_STAINED_GLASS_PANE));
        for (int i = 0; i < this.strip.length - 1; ++i) {
            this.inv.setItem(this.strip[i], this.safeClone(this.inv.getItem(this.strip[i + 1])));
        }
        int left = 56 - this.frame;
        if (left == 3) {
            this.inv.setItem(this.strip[this.strip.length - 1], this.safeClone(this.ctx.reward()));
        } else {
            this.inv.setItem(this.strip[this.strip.length - 1], this.randomFromPool());
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.8f, (float)Math.min(2.0, 0.95 + (double)this.frame * 0.018));
        this.wait = this.rowDelay(this.frame, 56);
        ++this.frame;
        if (this.frame >= 56) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

    private int rowDelay(int frame, int totalFrames) {
        int left = totalFrames - frame;
        if (frame < 12) {
            return 0;
        }
        if (frame < 26) {
            return 1;
        }
        if (frame < 38) {
            return 2;
        }
        if (left > 9) {
            return 3;
        }
        if (left > 6) {
            return 4;
        }
        if (left > 3) {
            return 6;
        }
        return 8;
    }

    @Override
    public long postWinDelayTicks() {
        return 90L;
    }
}

