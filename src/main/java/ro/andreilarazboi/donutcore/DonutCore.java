package ro.andreilarazboi.donutcore;

import org.bukkit.plugin.java.JavaPlugin;
import ro.andreilarazboi.donutcore.crates.CratesModule;
import ro.andreilarazboi.donutcore.enderchest.EnderChestModule;
import ro.andreilarazboi.donutcore.sell.SellModule;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class DonutCore extends JavaPlugin {

    private static DonutCore instance;
    private CratesModule cratesModule;
    private SellModule sellModule;
    private EnderChestModule enderChestModule;

    private static final String RESET = "[0m";
    private static final String BLUE  = "[94m";
    private static final String GREEN = "[92m";

    @Override
    public void onEnable() {
        instance = this;
        setupColoredLogger();

        boolean cratesOk = false, sellOk = false, enderChestOk = false;

        cratesModule = new CratesModule(this);
        try {
            cratesModule.enable();
            cratesOk = true;
        } catch (Throwable t) {
            getLogger().severe("Crates module failed to enable: " + t.getMessage());
        }

        sellModule = new SellModule(this);
        try {
            sellModule.enable();
            sellOk = true;
        } catch (Throwable t) {
            getLogger().severe("Sell module failed to enable: " + t.getMessage());
        }

        enderChestModule = new EnderChestModule(this);
        try {
            enderChestModule.enable();
            enderChestOk = true;
        } catch (Throwable t) {
            getLogger().severe("EnderChest module failed to enable: " + t.getMessage());
        }

        printBanner(cratesOk, sellOk, enderChestOk);
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

    private void printBanner(boolean cratesOk, boolean sellOk, boolean enderChestOk) {
        boolean hasPapi = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;

        int defaultRows = 6;
        if (enderChestOk) {
            defaultRows = enderChestModule.getEnderChest().getConfig().getInt("default-rows", 6);
        }

        String version = getPluginMeta().getVersion();

        log("");
        log("  #####    ####   ##  ##  ##  ##  ######   ####    ####   #####   ######");
        log("  ##  ##  ##  ##  ### ##  ##  ##    ##    ##      ##  ##  ##  ##  ##    ");
        log("  ##  ##  ##  ##  ######  ##  ##    ##    ##      ##  ##  #####   ##### ");
        log("  ##  ##  ##  ##  ## ###  ##  ##    ##    ##  ##  ##  ##  ## ##   ##    ");
        log("  #####    ####   ##  ##   ####     ##     ####    ####   ##  ##  ######");
        log("");
        log("    Version: " + version + "  |  Author: Andreilarazboi");
        log("");
        log("    Sell         » " + (sellOk       ? "Enabled" : "Disabled"));
        log("    Crates       » " + (cratesOk     ? "Enabled" : "Disabled"));
        log("    Enderchest   » " + (enderChestOk ? "Enabled (default: " + defaultRows + " rows)" : "Disabled"));
        log("");
        log("    PlaceholderAPI » " + (hasPapi  ? "YES" : "NO  (Install it for the plugin to work properly)"));
        log("    Vault          » " + (hasVault ? "YES" : "NO  (Install it for the plugin to work properly)"));
        log("");
        log("  ##############################################");
        log("");
    }

    private void log(String message) {
        getLogger().info(GREEN + message + RESET);
    }

    @Override
    public void onDisable() {
        if (enderChestModule != null) enderChestModule.disable();
        if (sellModule != null) sellModule.disable();
        if (cratesModule != null) cratesModule.disable();
        getLogger().info("DonutCore disabled.");
    }

    public static DonutCore getInstance() {
        return instance;
    }

    public CratesModule getCratesModule() {
        return cratesModule;
    }

    public SellModule getSellModule() {
        return sellModule;
    }

    public EnderChestModule getEnderChestModule() {
        return enderChestModule;
    }
}
