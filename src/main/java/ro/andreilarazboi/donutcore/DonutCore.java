package ro.andreilarazboi.donutcore;

import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ro.andreilarazboi.donutcore.arenas.ArenasModule;
import ro.andreilarazboi.donutcore.crates.CratesModule;
import ro.andreilarazboi.donutcore.enderchest.EnderChestModule;
import ro.andreilarazboi.donutcore.fastcrystals.FastCrystalsModule;
import ro.andreilarazboi.donutcore.sell.SellModule;
import ro.andreilarazboi.donutcore.stash.StashModule;

import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class DonutCore extends JavaPlugin {

    private static DonutCore instance;
    private CratesModule cratesModule;
    private SellModule sellModule;
    private EnderChestModule enderChestModule;
    private ArenasModule arenasModule;
    private FastCrystalsModule fastCrystalsModule;
    private StashModule stashModule;

    private static final char   ESC   = 27;
    private static final String RESET = ESC + "[0m";
    private static final String BLUE  = ESC + "[94m";
    private static final String GREEN = ESC + "[92m";

    @Override
    public void onEnable() {
        instance = this;
        setupColoredLogger();
        saveDefaultConfig();

        boolean cratesOk = false, sellOk = false, enderChestOk = false,
                arenasOk = false, fastCrystalsOk = false, stashOk = false;

        cratesModule = new CratesModule(this);
        if (getConfig().getBoolean("modules.crates", true)) {
            try { cratesModule.enable(); cratesOk = true; }
            catch (Throwable t) { getLogger().severe("Crates module failed: " + t.getMessage()); }
        }

        sellModule = new SellModule(this);
        if (getConfig().getBoolean("modules.sell", true)) {
            try { sellModule.enable(); sellOk = true; }
            catch (Throwable t) { getLogger().severe("Sell module failed: " + t.getMessage()); }
        }

        enderChestModule = new EnderChestModule(this);
        if (getConfig().getBoolean("modules.enderchest", true)) {
            try { enderChestModule.enable(); enderChestOk = true; }
            catch (Throwable t) { getLogger().severe("EnderChest module failed: " + t.getMessage()); }
        }

        arenasModule = new ArenasModule(this);
        if (getConfig().getBoolean("modules.arenas", false)) {
            try { arenasModule.enable(); arenasOk = arenasModule.isActive(); }
            catch (Throwable t) { getLogger().severe("Arenas module failed: " + t.getMessage()); }
        }

        fastCrystalsModule = new FastCrystalsModule(this);
        if (getConfig().getBoolean("modules.fastcrystals", false)) {
            try { fastCrystalsModule.enable(); fastCrystalsOk = true; }
            catch (Throwable t) { getLogger().severe("FastCrystals module failed: " + t.getMessage()); }
        }

        stashModule = new StashModule(this);
        if (getConfig().getBoolean("modules.stash", false)) {
            try { stashModule.enable(); stashOk = true; }
            catch (Throwable t) { getLogger().severe("Stash module failed: " + t.getMessage()); }
        }

        DonutCoreCommand donutCoreCmd = new DonutCoreCommand(this);
        Objects.requireNonNull(getCommand("donutcore")).setExecutor(donutCoreCmd);
        Objects.requireNonNull(getCommand("donutcore")).setTabCompleter(donutCoreCmd);

        getServer().getPluginManager().registerEvents(new CommandVisibilityListener(this), this);

        syncModuleCommandVisibility();
        printBanner(cratesOk, sellOk, enderChestOk, arenasOk, fastCrystalsOk, stashOk);
    }

    public void syncModuleCommandVisibility() {
        boolean cratesOn = cratesModule.isActive();
        boolean sellOn   = sellModule.isActive();
        boolean ecOn     = enderChestModule.isActive();
        boolean arenaOn  = arenasModule != null && arenasModule.isActive();
        boolean stashOn  = stashModule != null && stashModule.isActive();

        applyPerm("donutcrate",  cratesOn ? "donutcore.crate.use"  : "donutcore.admin", !cratesOn);

        applyPerm("sell",        sellOn ? "donutcore.sell.use"      : "donutcore.admin", !sellOn);
        applyPerm("sellmulti",   sellOn ? "donutcore.sell.use"      : "donutcore.admin", !sellOn);
        applyPerm("worth",       sellOn ? "donutcore.sell.use"      : "donutcore.admin", !sellOn);
        applyPerm("toggleworth", sellOn ? "donutcore.sell.use"      : "donutcore.admin", !sellOn);
        applyPerm("sellhistory", sellOn ? "donutcore.sell.use"      : "donutcore.admin", !sellOn);
        applyPerm("donutsell",   sellOn ? "donutcore.sell.admin"    : "donutcore.admin", !sellOn);

        applyPerm("enderchest",  ecOn ? "enderchest.command"        : "donutcore.admin", !ecOn);
        applyPerm("clearechest", ecOn ? "enderchest.clear"          : "donutcore.admin", !ecOn);

        applyPerm("donutarena", arenaOn ? "donutcore.arenas.admin" : "donutcore.admin", !arenaOn);
        applyPerm("donutstash", stashOn ? "donutcore.admin.stash"  : "donutcore.admin", !stashOn);

        getServer().getOnlinePlayers().forEach(Player::updateCommands);
    }

    @SuppressWarnings("deprecation")
    private void applyPerm(String name, String perm, boolean silentDeny) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        cmd.setPermission(perm);
        cmd.permissionMessage(silentDeny ? Component.empty() : null);
    }

    // Intercepts each LogRecord after PluginLogger prepends "[DonutCore] "
    // and wraps the prefix with ANSI blue so it appears colored in console.
    private void setupColoredLogger() {
        final String prefix = "[DonutCore] ";
        getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                if (msg != null && msg.startsWith(prefix)) {
                    record.setMessage(BLUE + "[DonutCore]" + RESET + " " + msg.substring(prefix.length()));
                }
            }

            @Override public void flush() {}
            @Override public void close() throws SecurityException {}
        });
    }

    private void printBanner(boolean cratesOk, boolean sellOk, boolean enderChestOk,
                             boolean arenasOk, boolean fastCrystalsOk, boolean stashOk) {
        boolean hasPapi       = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean hasVault      = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasWorldGuard = getServer().getPluginManager().getPlugin("WorldGuard") != null;

        int defaultRows = 6;
        if (enderChestOk) {
            defaultRows = enderChestModule.getEnderChest().getConfig().getInt("default-rows", 6);
        }

        String version = getPluginMeta().getVersion();
        String on  = "Enabled";
        String off = "Disabled";

        log("");
        log("  #####    ####   ##  ##  ##  ##  ######   ####    ####   #####   ######");
        log("  ##  ##  ##  ##  ### ##  ##  ##    ##    ##      ##  ##  ##  ##  ##    ");
        log("  ##  ##  ##  ##  ######  ##  ##    ##    ##      ##  ##  #####   ##### ");
        log("  ##  ##  ##  ##  ## ###  ##  ##    ##    ##  ##  ##  ##  ## ##   ##    ");
        log("  #####    ####   ##  ##   ####     ##     ####    ####   ##  ##  ######");
        log("");
        log("    Version: " + version + "  |  Author: Andreilarazboi");
        log("");
        log("    Sell          » " + (sellOk         ? on : off));
        log("    Crates        » " + (cratesOk       ? on : off));
        log("    EnderChest    » " + (enderChestOk   ? on + " (default: " + defaultRows + " rows)" : off));
        log("    Arenas        » " + (arenasOk       ? on : off));
        log("    FastCrystals  » " + (fastCrystalsOk ? on : off));
        log("    Stash         » " + (stashOk        ? on : off));
        log("");
        log("    PlaceholderAPI » " + (hasPapi       ? "YES" : "NO  (Install it for the plugin to work properly)"));
        log("    Vault          » " + (hasVault      ? "YES" : "NO  (Install it for the plugin to work properly)"));
        log("    WorldGuard     » " + (hasWorldGuard ? "YES" : "NO  (Required for the Arenas module)"));
        log("");
        log("  ##############################################");
        log("");
    }

    private void log(String message) {
        getLogger().info(GREEN + message + RESET);
    }

    @Override
    public void onDisable() {
        if (arenasModule       != null && arenasModule.isActive())       arenasModule.disable();
        if (stashModule        != null && stashModule.isActive())        stashModule.disable();
        if (fastCrystalsModule != null && fastCrystalsModule.isActive()) fastCrystalsModule.disable();
        if (enderChestModule   != null && enderChestModule.isActive())   enderChestModule.disable();
        if (sellModule         != null && sellModule.isActive())         sellModule.disable();
        if (cratesModule       != null && cratesModule.isActive())       cratesModule.disable();
        getLogger().info("DonutCore disabled.");
    }

    public static DonutCore getInstance()                     { return instance; }
    public CratesModule getCratesModule()                     { return cratesModule; }
    public SellModule getSellModule()                         { return sellModule; }
    public EnderChestModule getEnderChestModule()             { return enderChestModule; }
    public ArenasModule getArenasModule()                     { return arenasModule; }
    public FastCrystalsModule getFastCrystalsModule()         { return fastCrystalsModule; }
    public StashModule getStashModule()                       { return stashModule; }
}
