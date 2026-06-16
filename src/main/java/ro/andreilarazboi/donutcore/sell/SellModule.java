package ro.andreilarazboi.donutcore.sell;

import org.bukkit.plugin.java.JavaPlugin;

public class SellModule {
    private final DonutSell sell;
    private boolean active = false;

    public SellModule(JavaPlugin plugin) {
        this.sell = new DonutSell(plugin);
    }

    public void enable() {
        this.sell.enable();
        this.active = true;
    }

    public void disable() {
        this.sell.disable();
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public DonutSell getSell() {
        return this.sell;
    }
}
