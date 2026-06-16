package ro.andreilarazboi.donutcore.arenas;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final Plugin plugin;
    private final File   dataFile;
    private FileConfiguration cfg;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;

        File dir = new File(plugin.getDataFolder(), "arenas");
        dir.mkdirs();

        // Copy default arenas/config.yml if absent
        plugin.saveResource("arenas/config.yml", false);

        this.dataFile = new File(dir, "arenas.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(dataFile);
        arenas.clear();

        ConfigurationSection sec = cfg.getConfigurationSection("arenas");
        if (sec == null) return;

        for (String name : sec.getKeys(false)) {
            String region = sec.getString(name + ".region", "");
            String world  = sec.getString(name + ".world", "world");
            Arena  a      = new Arena(name, region, world);

            // AFK Timer
            a.setAfkEnabled(sec.getBoolean(name + ".afk.enabled", false));
            a.setAfkIntervalSeconds(sec.getInt(name + ".afk.interval", 60));
            a.setAfkCommand(sec.getString(name + ".afk.command", ""));
            a.setAfkDisplayType(sec.getString(name + ".afk.display.type", "actionbar"));
            a.setAfkActionbarText(sec.getString(name + ".afk.display.actionbar", "&aAFK Zone &7| Reward in &e{time}"));
            a.setAfkTitleText(sec.getString(name + ".afk.display.title", "&aAFK Zone"));
            a.setAfkSubtitleText(sec.getString(name + ".afk.display.subtitle", "&7Reward in &e{time}"));
            a.setAfkChatText(sec.getString(name + ".afk.display.chat", "&7[Zone] Reward in &e{time}"));

            // Enter action
            a.setEnterCommandEnabled(sec.getBoolean(name + ".enter.command-enabled", false));
            a.setEnterCommand(sec.getString(name + ".enter.command", ""));
            a.setEnterMessageType(sec.getString(name + ".enter.message.type", "chat"));
            a.setEnterMessage(sec.getString(name + ".enter.message.text", "&aYou entered the &e{zone} &azone."));
            a.setEnterTitle(sec.getString(name + ".enter.message.title", "&a{zone}"));
            a.setEnterSubtitle(sec.getString(name + ".enter.message.subtitle", "&7Welcome!"));

            // Exit action
            a.setExitCommandEnabled(sec.getBoolean(name + ".exit.command-enabled", false));
            a.setExitCommand(sec.getString(name + ".exit.command", ""));
            a.setExitMessageType(sec.getString(name + ".exit.message.type", "chat"));
            a.setExitMessage(sec.getString(name + ".exit.message.text", "&7You left the &e{zone} &7zone."));
            a.setExitTitle(sec.getString(name + ".exit.message.title", "&7{zone}"));
            a.setExitSubtitle(sec.getString(name + ".exit.message.subtitle", "&7Goodbye!"));

            arenas.put(name.toLowerCase(), a);
        }
    }

    public void save() {
        cfg.set("arenas", null);
        for (Arena a : arenas.values()) {
            String k = "arenas." + a.getName();
            cfg.set(k + ".region", a.getRegion());
            cfg.set(k + ".world",  a.getWorldName());

            // AFK Timer
            cfg.set(k + ".afk.enabled",           a.isAfkEnabled());
            cfg.set(k + ".afk.interval",           a.getAfkIntervalSeconds());
            cfg.set(k + ".afk.command",            a.getAfkCommand());
            cfg.set(k + ".afk.display.type",       a.getAfkDisplayType());
            cfg.set(k + ".afk.display.actionbar",  a.getAfkActionbarText());
            cfg.set(k + ".afk.display.title",      a.getAfkTitleText());
            cfg.set(k + ".afk.display.subtitle",   a.getAfkSubtitleText());
            cfg.set(k + ".afk.display.chat",       a.getAfkChatText());

            // Enter action
            cfg.set(k + ".enter.command-enabled",      a.isEnterCommandEnabled());
            cfg.set(k + ".enter.command",              a.getEnterCommand());
            cfg.set(k + ".enter.message.type",         a.getEnterMessageType());
            cfg.set(k + ".enter.message.text",         a.getEnterMessage());
            cfg.set(k + ".enter.message.title",        a.getEnterTitle());
            cfg.set(k + ".enter.message.subtitle",     a.getEnterSubtitle());

            // Exit action
            cfg.set(k + ".exit.command-enabled",       a.isExitCommandEnabled());
            cfg.set(k + ".exit.command",               a.getExitCommand());
            cfg.set(k + ".exit.message.type",          a.getExitMessageType());
            cfg.set(k + ".exit.message.text",          a.getExitMessage());
            cfg.set(k + ".exit.message.title",         a.getExitTitle());
            cfg.set(k + ".exit.message.subtitle",      a.getExitSubtitle());
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public void addArena(Arena arena)    { arenas.put(arena.getName().toLowerCase(), arena); save(); }
    public void removeArena(String name) { arenas.remove(name.toLowerCase()); save(); }
    public Arena getArena(String name)   { return name == null ? null : arenas.get(name.toLowerCase()); }
    public Collection<Arena> getArenas() { return arenas.values(); }

    public boolean isPlayerInArena(Player player, Arena arena) {
        if (!player.getWorld().getName().equals(arena.getWorldName())) return false;
        try {
            RegionManager rm = WorldGuard.getInstance()
                .getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
            if (rm == null) return false;
            ProtectedRegion region = rm.getRegion(arena.getRegion());
            if (region == null) return false;
            Location loc = player.getLocation();
            return region.contains(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean regionExists(String worldName, String regionName) {
        try {
            org.bukkit.World w = Bukkit.getWorld(worldName);
            if (w == null) return false;
            RegionManager rm = WorldGuard.getInstance()
                .getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(w));
            return rm != null && rm.getRegion(regionName) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
