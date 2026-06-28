
package ro.andreilarazboi.donutcore.crates.opening.anim.impl;

import ro.andreilarazboi.donutcore.crates.opening.anim.BaseAnimation;

public class MatrixFillAnimation
extends BaseAnimation {
    private int frame = 0;

    @Override
    public int inventorySize() {
        return 54;
    }

    @Override
    protected void onInit() {
        for (int i = 0; i < this.inv.getSize(); ++i) {
            this.inv.setItem(i, this.randomFromPool());
        }
    }

    @Override
    protected void onTick() {
        double progress = Math.min(1.0, (double)this.frame / 70.0);
        double winChance = Math.pow(progress, 2.2);
        int updates = (int)Math.round(6.0 + progress * 44.0);
        for (int i = 0; i < updates; ++i) {
            int slot = this.ctx.rng().nextInt(this.inv.getSize());
            if (this.ctx.rng().nextDouble() < winChance) {
                this.inv.setItem(slot, this.safeClone(this.ctx.reward()));
                continue;
            }
            this.inv.setItem(slot, this.randomFromPool());
        }
        float pitch = (float)Math.min(2.0, 0.75 + progress * 1.1);
        this.ctx.player().playSound(this.ctx.player().getLocation(), this.ctx.tick(), 0.45f, pitch);
        this.wait = progress < 0.25 ? 3 : (progress < 0.45 ? 2 : (progress < 0.7 ? 1 : 0));
        ++this.frame;
        if (this.frame >= 70) {
            for (int i = 0; i < this.inv.getSize(); ++i) {
                this.inv.setItem(i, this.safeClone(this.ctx.reward()));
            }
            this.finishWin();
        }
    }

    @Override
    public long postWinDelayTicks() {
        return 80L;
    }
}

