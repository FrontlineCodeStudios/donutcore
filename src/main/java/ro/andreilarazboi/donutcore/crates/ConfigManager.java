
package ro.andreilarazboi.donutcore.crates;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigManager {
    public FileConfiguration config;
    public FileConfiguration crates;
    public FileConfiguration saves;
    private final File cfgFile;
    private final File cratesFile;
    private final File savesFile;
    private final DonutCrates plugin;

    public ConfigManager(DonutCrates plugin) {
        this.plugin = plugin;
        this.cfgFile = new File(plugin.getDataFolder(), "crates/config.yml");
        this.cratesFile = new File(plugin.getDataFolder(), "crates/crates.yml");
        this.savesFile = new File(plugin.getDataFolder(), "crates/saves.yml");
        this.ensureFileExists("crates/config.yml", this.cfgFile);
        this.ensureFileExists("crates/crates.yml", this.cratesFile);
        this.ensureFileExists("crates/saves.yml", this.savesFile);
        this.reloadAll();
    }

    public void reloadAll() {
        this.plugin.getLogger().info("[DonutCrates][ConfigIO] Reloading config files from disk...");
        this.config = YamlConfiguration.loadConfiguration(this.cfgFile);
        this.crates = YamlConfiguration.loadConfiguration(this.cratesFile);
        this.saves = YamlConfiguration.loadConfiguration(this.savesFile);
    }

    public void saveAll() {
        this.saveCrates();
        this.saveSaves();
    }

    public void saveCrates() {
        try {
            this.crates.save(this.cratesFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "[DonutCrates] Failed to save crates.yml", e);
        }
    }

    public void saveSaves() {
        try {
            this.saves.save(this.savesFile);
            this.plugin.getLogger().info("[DonutCrates][ConfigIO] Saved saves.yml");
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "[DonutCrates] Failed to save saves.yml", e);
        }
    }

    private void ensureFileExists(String resourceName, File file) {
        if (file.exists()) {
            return;
        }
        file.getParentFile().mkdirs();
        try (InputStream in = this.plugin.getResource(resourceName);){
            if (in != null) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                file.createNewFile();
            }
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "[DonutCrates] Failed to create default " + resourceName, e);
        }
    }
}

