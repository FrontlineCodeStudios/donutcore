
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class FlickerLockAnimation
extends BaseAnimation {
    private int frame = 0;

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
        if (this.frame < 34) {
            this.inv.setItem(13, this.randomFromPool());
        } else {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
        }
        if (this.frame % 2 == 0) {
            this.inv.setItem(12, this.pane(Material.YELLOW_STAINED_GLASS_PANE));
            this.inv.setItem(14, this.pane(Material.YELLOW_STAINED_GLASS_PANE));
        } else {
            this.inv.setItem(12, this.pane(Material.BLACK_STAINED_GLASS_PANE));
            this.inv.setItem(14, this.pane(Material.BLACK_STAINED_GLASS_PANE));
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.6f, (float)Math.min(2.0, 0.9 + (double)this.frame * 0.03));
        this.wait = this.genericSlow(this.frame, 40, 0, 6);
        ++this.frame;
        if (this.frame >= 40) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

}

