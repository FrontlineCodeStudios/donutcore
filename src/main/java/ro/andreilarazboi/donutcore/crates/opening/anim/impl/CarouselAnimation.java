
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;
import org.bukkit.Material;

public class CarouselAnimation
extends BaseAnimation {
    private final int[] strip = new int[]{11, 12, 13, 14, 15};
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
        int left = 48 - this.frame;
        if (left == 2) {
            this.inv.setItem(this.strip[this.strip.length - 1], this.safeClone(this.ctx.reward()));
        } else {
            this.inv.setItem(this.strip[this.strip.length - 1], this.randomFromPool());
        }
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.75f, (float)Math.min(2.0, 1.0 + (double)this.frame * 0.02));
        this.wait = this.genericSlow(this.frame, 48, 0, 6);
        ++this.frame;
        if (this.frame >= 48) {
            this.inv.setItem(13, this.safeClone(this.ctx.reward()));
            this.finishWin();
        }
    }

}

