
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class CenterPulseAnimation
extends BaseAnimation {
    private int frame = 0;
    private final int[] ring = new int[]{4, 12, 14, 22, 3, 5, 21, 23};

    @Override
    public int inventorySize() {
        return 27;
    }

    @Override
    protected void onInit() {
        this.fillAll(Material.BLACK_STAINED_GLASS_PANE);
    }

    @Override
    protected void onTick() {
        Material mat = this.frame % 3 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : (this.frame % 3 == 1 ? Material.MAGENTA_STAINED_GLASS_PANE : Material.PINK_STAINED_GLASS_PANE);
        for (int s : this.ring) {
            this.inv.setItem(s, this.pane(mat));
        }
        if (this.frame < 38) {
            this.inv.setItem(13, this.randomFromPool());
        } else {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.65f, (float)Math.min(2.0, 0.95 + (double)this.frame * 0.02));
        this.wait = this.genericSlow(this.frame, 44, 0, 7);
        ++this.frame;
        if (this.frame >= 44) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

}

