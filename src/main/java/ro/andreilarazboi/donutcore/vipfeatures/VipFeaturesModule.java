package ro.andreilarazboi.donutcore.vipfeatures;

import org.bukkit.Bukkit;
import ro.andreilarazboi.donutcore.DonutCore;

import java.util.Objects;

/**
 * VIP Features module — provides /chatcolor and /namecolor commands plus
 * PlaceholderAPI expansions %donutcore_chatcolor% and %donutcore_namecolor%.
 *
 * <p>
 * Priority design: when a player has no custom color set, the placeholder
 * returns an empty string so that the external chat plugin (EssentialsX, etc.)
 * falls back to the rank/group color automatically.
 * </p>
 */
public class VipFeaturesModule {

    private final DonutCore plugin;

    private VipDataManager dataManager;
    private VipPlaceholderExpansion papiExpansion;
    private boolean active = false;

    public VipFeaturesModule(DonutCore plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------

    public void enable() {
        dataManager = new VipDataManager(plugin);

        // Register commands
        ChatColorCommand chatColorCmd = new ChatColorCommand(dataManager);
        Objects.requireNonNull(plugin.getCommand("chatcolor")).setExecutor(chatColorCmd);
        Objects.requireNonNull(plugin.getCommand("chatcolor")).setTabCompleter(chatColorCmd);

        NameColorCommand nameColorCmd = new NameColorCommand(dataManager);
        Objects.requireNonNull(plugin.getCommand("namecolor")).setExecutor(nameColorCmd);
        Objects.requireNonNull(plugin.getCommand("namecolor")).setTabCompleter(nameColorCmd);

        // Register PlaceholderAPI expansion if PAPI is present
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiExpansion = new VipPlaceholderExpansion(plugin, dataManager);
            papiExpansion.register();
            plugin.getLogger()
                    .info("[VipFeatures] Placeholders registered: %donutcore_chatcolor%, %donutcore_namecolor%");
        } else {
            plugin.getLogger().warning("[VipFeatures] PlaceholderAPI not found — placeholders will not work.");
        }

        this.active = true;
    }

    public void disable() {
        if (papiExpansion != null) {
            papiExpansion.unregister();
            papiExpansion = null;
        }
        dataManager = null;
        this.active = false;
    }

    // -------------------------------------------------------------------------

    public boolean isActive() {
        return active;
    }

    public VipDataManager getDataManager() {
        return dataManager;
    }
}
