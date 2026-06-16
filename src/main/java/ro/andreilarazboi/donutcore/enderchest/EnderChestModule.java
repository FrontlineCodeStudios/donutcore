package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.plugin.java.JavaPlugin;

public class EnderChestModule {
    private final DonutEnderChest enderChest;
    private boolean active = false;

    public EnderChestModule(JavaPlugin plugin) {
        this.enderChest = new DonutEnderChest(plugin);
    }

    public void enable() {
        this.enderChest.enable();
        this.active = true;
    }

    public void disable() {
        this.enderChest.disable();
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public DonutEnderChest getEnderChest() {
        return enderChest;
    }
}
