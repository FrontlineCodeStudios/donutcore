package ro.andreilarazboi.donutcore.enderchest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DonutEnderChest {
    private final JavaPlugin parent;
    private FileConfiguration config;
    private File configFile;
    private EnderChestDataManager dataManager;
    private EnderChestManager enderChestManager;
    private final Set<UUID> suppressSave = ConcurrentHashMap.newKeySet();

    public DonutEnderChest(JavaPlugin parent) {
        this.parent = parent;
    }

    public void enable() {
        this.configFile = new File(parent.getDataFolder(), "enderchest/config.yml");
        if (!configFile.exists()) saveResource("enderchest/config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.dataManager = new EnderChestDataManager(this);
        this.enderChestManager = new EnderChestManager(this);

        Bukkit.getPluginManager().registerEvents(new EnderChestListener(this), parent);
        Bukkit.getPluginManager().registerEvents(new EnderChestInventoryListener(this), parent);
        new EnderChestCommand(this);
        new ClearEnderChestCommand(this);

        parent.getLogger().info("[DonutCore] EnderChest module enabled.");
    }

    public void disable() {
        if (dataManager != null) dataManager.shutdown();
        parent.getLogger().info("[DonutCore] EnderChest module disabled.");
    }

    public void reloadPlugin() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveResource(String name) {
        File dest = new File(parent.getDataFolder(), name);
        if (dest.exists()) return;
        try (InputStream in = parent.getResource(name)) {
            if (in == null) {
                parent.getLogger().warning("[DonutEnderChest] Missing resource: " + name);
                return;
            }
            dest.getParentFile().mkdirs();
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            parent.getLogger().log(Level.WARNING, "[DonutEnderChest] Failed to save resource " + name, e);
        }
    }

    public String formatColors(String s) {
        if (s == null) return "";
        char[] chars = s.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                if ("0123456789abcdefklmnorx".indexOf(code) >= 0) {
                    sb.append('§').append(code);
                    i++;
                    continue;
                }
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public Component msgComponent(String key) {
        String raw = config.getString("messages." + key, "&c[EnderChest] Missing message: " + key);
        return LegacyComponentSerializer.legacySection().deserialize(formatColors(raw));
    }

    public Component formatComponent(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(formatColors(s));
    }

    public String msg(String key) {
        return formatColors(config.getString("messages." + key,
                "&c[EnderChest] Missing message: " + key));
    }

    public boolean isSuppressSave(UUID ownerUUID) {
        return suppressSave.contains(ownerUUID);
    }

    public void suppressSave(UUID ownerUUID) {
        suppressSave.add(ownerUUID);
    }

    public void allowSave(UUID ownerUUID) {
        suppressSave.remove(ownerUUID);
    }

    public boolean isFolia() {
        return Bukkit.getServer().getName().equalsIgnoreCase("Folia");
    }

    public void runAsync(Runnable runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(parent, t -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(parent, runnable);
    }

    public void runAtPlayer(Player player, Runnable runnable) {
        if (isFolia()) {
            player.getScheduler().execute(parent, runnable, null, 1L);
            return;
        }
        Bukkit.getScheduler().runTask(parent, runnable);
    }

    public void runSync(Runnable runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(parent, runnable);
            return;
        }
        Bukkit.getScheduler().runTask(parent, runnable);
    }

    public JavaPlugin getPlugin() { return parent; }
    public FileConfiguration getConfig() { return config; }
    public File getDataFolder() { return parent.getDataFolder(); }
    public java.util.logging.Logger getLogger() { return parent.getLogger(); }
    public org.bukkit.command.PluginCommand getCommand(String name) { return parent.getCommand(name); }
    public EnderChestDataManager getDataManager() { return dataManager; }
    public EnderChestManager getEnderChestManager() { return enderChestManager; }
}
