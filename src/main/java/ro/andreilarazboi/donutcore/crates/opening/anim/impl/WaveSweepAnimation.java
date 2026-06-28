
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class WaveSweepAnimation
extends BaseAnimation {
    private int frame = 0;
    private final int[] waveSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
    }

    @Override
    protected void onTick() {
        int wavePos = this.frame % this.waveSlots.length;
        for (int i = 0; i < this.waveSlots.length; ++i) {
            if (i <= wavePos) {
                this.inv.setItem(this.waveSlots[i], this.pane(this.getWaveColor(i)));
                continue;
            }
            this.inv.setItem(this.waveSlots[i], this.randomFromPool());
        }
        if (this.frame >= 40) {
            this.inv.setItem(22, this.safeClone(this.ctx.reward()));
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.6f, (float)Math.min(2.0, 0.9 + (double)wavePos / (double)this.waveSlots.length));
        this.wait = this.genericSlow(this.frame, 46, 0, 6);
        ++this.frame;
        if (this.frame >= 46) {
            this.inv.setItem(22, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

    private Material getWaveColor(int i) {
        Material[] wave = new Material[]{Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE};
        return wave[i % wave.length];
    }

}

