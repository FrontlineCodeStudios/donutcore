package ro.andreilarazboi.donutcore.stash;

import org.bukkit.event.HandlerList;
import ro.andreilarazboi.donutcore.DonutCore;

public class StashModule {

    private final DonutCore plugin;
    private StashManager manager;
    private StashListener listener;
    private boolean active = false;

    public StashModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        manager  = new StashManager(plugin);
        listener = new StashListener(manager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        StashCommand cmd = new StashCommand(manager);
        plugin.getCommand("spawnstash").setExecutor(cmd);
        plugin.getCommand("spawnstash").setTabCompleter(cmd);
        plugin.getCommand("unspawnstash").setExecutor(cmd);
        plugin.getCommand("unspawnstash").setTabCompleter(cmd);

        this.active = true;
    }

    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
