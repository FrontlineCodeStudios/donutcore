package ro.andreilarazboi.donutcore.arenas;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import ro.andreilarazboi.donutcore.DonutCore;
import ro.andreilarazboi.donutcore.arenas.gui.ArenaGUIListener;

import java.util.Objects;

public class ArenasModule {

    private final DonutCore plugin;
    private ArenaManager    manager;
    private ArenaTracker    tracker;
    private ArenaGUIListener guiListener;
    private boolean         active = false;

    public ArenasModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().warning("Arenas module requires WorldGuard — module will not start.");
            return;
        }

        manager     = new ArenaManager(plugin);
        guiListener = new ArenaGUIListener(plugin, manager);
        tracker     = new ArenaTracker(manager);

        tracker.runTaskTimer(plugin, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);

        ArenasCommand cmd = new ArenasCommand(guiListener);
        Objects.requireNonNull(plugin.getCommand("donutarena")).setExecutor(cmd);

        this.active = true;
    }

    public void disable() {
        if (tracker != null && !tracker.isCancelled()) {
            tracker.cancel();
            tracker = null;
        }
        if (guiListener != null) {
            HandlerList.unregisterAll(guiListener);
            guiListener = null;
        }
        this.active = false;
    }

    public boolean isActive()              { return active; }
    public ArenaManager getManager()       { return manager; }
    public ArenaGUIListener getGuiListener() { return guiListener; }
}
