package ro.andreilarazboi.donutcore.crates;

import org.bukkit.plugin.java.JavaPlugin;

public class CratesModule {
    private final DonutCrates crates;
    private boolean active = false;

    public CratesModule(JavaPlugin plugin) {
        this.crates = new DonutCrates(plugin);
    }

    public void enable() {
        crates.enable();
        this.active = true;
    }

    public void disable() {
        crates.disable();
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public DonutCrates getCrates() {
        return crates;
    }
}
