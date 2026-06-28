
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class DoubleSpinMatchAnimation
extends BaseAnimation {
    private final int[] top = new int[]{10, 11, 12, 13, 14, 15, 16};
    private final int[] bottom = new int[]{37, 38, 39, 40, 41, 42, 43};
    private int frame = 0;

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        for (int s : this.top) {
            this.inv.setItem(s, this.randomFromPool());
        }
        for (int s : this.bottom) {
            this.inv.setItem(s, this.randomFromPool());
        }
    }

    @Override
    protected void onTick() {
        int i;
        this.inv.setItem(22, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        int left = 56 - this.frame;
        for (i = 0; i < this.top.length - 1; ++i) {
            this.inv.setItem(this.top[i], this.safeClone(this.inv.getItem(this.top[i + 1])));
        }
        this.inv.setItem(this.top[this.top.length - 1], this.randomFromPool());
        for (i = this.bottom.length - 1; i > 0; --i) {
            this.inv.setItem(this.bottom[i], this.safeClone(this.inv.getItem(this.bottom[i - 1])));
        }
        this.inv.setItem(this.bottom[0], this.randomFromPool());
        if (left == 3) {
            this.inv.setItem(16, this.safeClone(this.ctx.reward()));
            this.inv.setItem(37, this.safeClone(this.ctx.reward()));
        }
        if (left <= 4) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.inv.setItem(40, this.safeClone(this.ctx.reward()));
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.7f, (float)Math.min(2.0, 0.95 + (double)this.frame * 0.018));
        this.wait = this.genericSlow(this.frame, 56, 0, 8);
        ++this.frame;
        if (this.frame >= 56) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.inv.setItem(40, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

}

