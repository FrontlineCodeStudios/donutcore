package ro.andreilarazboi.donutcore.stash;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import ro.andreilarazboi.donutcore.DonutCore;

import java.util.Objects;

public class StashModule {

    private final DonutCore plugin;
    private StashManager           manager;
    private StashSchematicManager  schematics;
    private StashListener          listener;
    private boolean active = false;

    public StashModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            plugin.getLogger().warning("Stash module requires WorldEdit for schematic save/spawn — module will not start.");
            return;
        }

        manager    = new StashManager(plugin);
        schematics = new StashSchematicManager(plugin);
        listener   = new StashListener(manager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        StashCommand cmd = new StashCommand(manager, schematics);
        Objects.requireNonNull(plugin.getCommand("donutstash")).setExecutor(cmd);
        Objects.requireNonNull(plugin.getCommand("donutstash")).setTabCompleter(cmd);

        this.active = true;
    }

    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        this.active = false;
    }

    public boolean isActive() { return active; }
}
