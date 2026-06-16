package ro.andreilarazboi.donutcore.fastcrystals;

import org.bukkit.event.HandlerList;
import ro.andreilarazboi.donutcore.DonutCore;

public class FastCrystalsModule {

    private final DonutCore plugin;
    private FastCrystalsListener listener;
    private boolean active = false;

    public FastCrystalsModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        long immunityMs = plugin.getConfig().getLong("fastcrystals.self-damage-immunity-ms", 500L);
        listener = new FastCrystalsListener(immunityMs);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        this.active = true;
    }

    public void disable() {
        if (listener != null) {
            listener.cleanup();
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
