package ro.andreilarazboi.donutcore.hide;

import org.bukkit.event.HandlerList;
import ro.andreilarazboi.donutcore.DonutCore;

import java.util.Objects;

/**
 * Hide module — provides /hide (VIP/Streamer) and /staffhide (Staff camouflage).
 */
public class HideModule {

    private final DonutCore plugin;
    private HideManager  manager;
    private HideListener listener;
    private boolean active = false;

    public HideModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------

    public void enable() {
        manager  = new HideManager(plugin);
        listener = new HideListener(manager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        HideCommand hideCmd = new HideCommand(manager);
        Objects.requireNonNull(plugin.getCommand("hide")).setExecutor(hideCmd);
        Objects.requireNonNull(plugin.getCommand("hide")).setTabCompleter(hideCmd);

        StaffHideCommand staffCmd = new StaffHideCommand(manager);
        Objects.requireNonNull(plugin.getCommand("staffhide")).setExecutor(staffCmd);
        Objects.requireNonNull(plugin.getCommand("staffhide")).setTabCompleter(staffCmd);

        // Register all already-online staff as observers immediately
        for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            manager.addStaffObserver(online);
        }

        this.active = true;
    }

    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (manager != null) {
            manager.cleanup(); // unregister scoreboard team
            manager = null;
        }
        this.active = false;
    }

    // -------------------------------------------------------------------------

    public boolean isActive()             { return active; }
    public HideManager getManager()       { return manager; }
}
