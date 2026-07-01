package ro.andreilarazboi.donutcore.vipfeatures;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ro.andreilarazboi.donutcore.DonutCore;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages per-player VIP color data stored in plugins/DonutCore/vipcolors.yml.
 *
 * <p>If a player has no color set, {@link #getChatColor} / {@link #getNameColor}
 * return an empty string — this signals the chat formatter (EssentialsX, etc.)
 * to fall back to the rank/group color instead of overriding it.</p>
 */
public class VipDataManager {

    private static final String KEY_CHATCOLOR = "chatcolor";
    private static final String KEY_NAMECOLOR = "namecolor";

    private final DonutCore plugin;
    private final File      dataFile;
    private FileConfiguration data;

    public VipDataManager(DonutCore plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "vipcolors.yml");
        load();
    }

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    private void load() {
        if (!dataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("[VipFeatures] Could not create vipcolors.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[VipFeatures] Could not save vipcolors.yml: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the stored chat color code (e.g. {@code "&c"} or {@code "&#FF5500"}).
     * Returns an empty string if not set, telling the chat formatter to use the rank color.
     */
    public String getChatColor(UUID uuid) {
        return data.getString("players." + uuid + "." + KEY_CHATCOLOR, "");
    }

    /**
     * Returns the stored name color code.
     * Returns an empty string if not set, telling the chat formatter to use the rank color.
     */
    public String getNameColor(UUID uuid) {
        return data.getString("players." + uuid + "." + KEY_NAMECOLOR, "");
    }

    /** Persists a chat color for the given player. */
    public void setChatColor(UUID uuid, String color) {
        data.set("players." + uuid + "." + KEY_CHATCOLOR, color);
        save();
    }

    /** Persists a name color for the given player. */
    public void setNameColor(UUID uuid, String color) {
        data.set("players." + uuid + "." + KEY_NAMECOLOR, color);
        save();
    }

    /**
     * Removes the chat color for the given player (falls back to rank color).
     * Also removes the player's YAML section entirely if no other data remains.
     */
    public void resetChatColor(UUID uuid) {
        data.set("players." + uuid + "." + KEY_CHATCOLOR, null);
        cleanupIfEmpty(uuid);
        save();
    }

    /**
     * Removes the name color for the given player (falls back to rank color).
     * Also removes the player's YAML section entirely if no other data remains.
     */
    public void resetNameColor(UUID uuid) {
        data.set("players." + uuid + "." + KEY_NAMECOLOR, null);
        cleanupIfEmpty(uuid);
        save();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void cleanupIfEmpty(UUID uuid) {
        String path = "players." + uuid;
        if (data.getConfigurationSection(path) == null
                || data.getConfigurationSection(path).getKeys(false).isEmpty()) {
            data.set(path, null);
        }
    }
}
