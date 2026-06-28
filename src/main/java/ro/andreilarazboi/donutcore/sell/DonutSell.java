package ro.andreilarazboi.donutcore.sell;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import ro.andreilarazboi.donutcore.DonutCore;

public final class DonutSell implements Listener {
    private final JavaPlugin parent;
    private FileConfiguration sellConfig;
    private File sellConfigFile;

    private SellPacketListener packetListenerInstance;
    private CleanupListener cleanupListener;
    private ViewTracker viewTracker;
    private ItemPricesMenu itemPricesMenu;
    private AdminPriceEditorMenu adminPriceEditorMenu;
    private SellGui sellGui;
    private ProgressGui progressGui;
    private HistoryTracker historyTracker;
    private SellHistoryGui sellHistoryGui;
    private ResetConfirmationGui resetConfirmationGui;
    private Economy econ;
    private final Map<UUID, Double> totalSold = new HashMap<>();
    private final Map<UUID, Map<String, Double>> soldByCategory = new HashMap<>();
    private final Map<UUID, Map<String, Stats>> itemHistory = new HashMap<>();
    public final Map<String, List<String>> categoryItems = new HashMap<>();
    private final Map<String, Double> itemValues = new HashMap<>();
    private File saveFile;
    private FileConfiguration saveConfig;
    private File worthFile;
    private FileConfiguration worthConfig;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private File mysqlFile;
    private FileConfiguration mysqlConfig;
    private File menusFile;
    private FileConfiguration menusConfig;
    private SellMySQL mysql;
    private boolean usingMySQL = false;
    private CancellableTask restartWarningTask;
    private Boolean lastKnownUseNewSellMenu;
    private NamespacedKey sellAxeKey;
    private NamespacedKey expiryKey;
    private final Set<UUID> toggleWorthDisabled = new HashSet<>();
    private final Set<String> disabledItemsUpper = new HashSet<>();

    public DonutSell(JavaPlugin parent) {
        this.parent = parent;
    }

    public JavaPlugin getPlugin() {
        return this.parent;
    }

    public java.util.logging.Logger getLogger() {
        return this.parent.getLogger();
    }

    public org.bukkit.Server getServer() {
        return this.parent.getServer();
    }

    public org.bukkit.command.PluginCommand getCommand(String name) {
        return this.parent.getCommand(name);
    }

    public io.papermc.paper.plugin.configuration.PluginMeta getPluginMeta() {
        return this.parent.getPluginMeta();
    }

    public FileConfiguration getConfig() {
        return this.sellConfig;
    }

    public void reloadConfig() {
        this.sellConfig = YamlConfiguration.loadConfiguration(this.sellConfigFile);
    }

    private void saveResource(String name, boolean replace) {
        File dest = new File(this.parent.getDataFolder(), name);
        if (dest.exists() && !replace) return;
        try (InputStream in = this.parent.getResource(name)) {
            if (in == null) {
                this.parent.getLogger().warning("[DonutCore] Resource '" + name + "' not found in jar.");
                return;
            }
            dest.getParentFile().mkdirs();
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            this.parent.getLogger().log(Level.WARNING, "[DonutCore] Failed to save resource " + name, e);
        }
    }

    private boolean isFolia() {
        return Bukkit.getServer().getName().equalsIgnoreCase("Folia");
    }

    public void runSync(Runnable runnable) {
        if (this.isFolia()) { Bukkit.getGlobalRegionScheduler().execute(this.parent, runnable); return; }
        Bukkit.getScheduler().runTask(this.parent, runnable);
    }

    public void runAsync(Runnable runnable) {
        if (this.isFolia()) { Bukkit.getAsyncScheduler().runNow(this.parent, task -> runnable.run()); return; }
        Bukkit.getScheduler().runTaskAsynchronously(this.parent, runnable);
    }

    public void runLaterGlobal(Runnable runnable, long delayTicks) {
        if (this.isFolia()) { Bukkit.getGlobalRegionScheduler().runDelayed(this.parent, task -> runnable.run(), delayTicks); return; }
        Bukkit.getScheduler().runTaskLater(this.parent, runnable, delayTicks);
    }

    public CancellableTask runRepeatingGlobal(Runnable runnable, long delayTicks, long periodTicks) {
        if (this.isFolia()) {
            long fd = Math.max(1L, delayTicks);
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.parent, t -> runnable.run(), fd, periodTicks);
            return task::cancel;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.parent, runnable, delayTicks, periodTicks);
        return task::cancel;
    }

    public void runAtPlayer(Player player, Runnable runnable) {
        if (this.isFolia()) { player.getScheduler().execute(this.parent, runnable, null, 1L); return; }
        Bukkit.getScheduler().runTask(this.parent, runnable);
    }

    public void runAtPlayerLater(Player player, Runnable runnable, long delayTicks) {
        if (this.isFolia()) { player.getScheduler().runDelayed(this.parent, task -> runnable.run(), null, delayTicks); return; }
        Bukkit.getScheduler().runTaskLater(this.parent, runnable, delayTicks);
    }

    public CleanupListener getCleanupListener() { return this.cleanupListener; }
    public ResetConfirmationGui getResetConfirmationGui() { return this.resetConfirmationGui; }
    public boolean isUsingMySQL() { return this.usingMySQL; }

    public void enable() {
        this.sellConfigFile = new File(this.parent.getDataFolder(), "sell/config.yml");
        if (!this.sellConfigFile.exists()) this.saveResource("sell/config.yml", false);
        this.sellConfig = YamlConfiguration.loadConfiguration(this.sellConfigFile);

        this.setupMessagesFile();
        this.setupMysqlFile();
        this.setupMenusFile();
        this.setupWorthFile();
        this.buildCategoryItems();
        this.setupStorage();
        this.rebuildDisabledItemsCache();

        this.cleanupListener = new CleanupListener(this);
        for (Player p : this.parent.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
        }

        this.viewTracker = new ViewTracker();
        this.itemPricesMenu = new ItemPricesMenu(this);
        this.adminPriceEditorMenu = new AdminPriceEditorMenu(this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this.parent);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this.parent);
        this.sellGui = new SellGui(this);
        this.progressGui = new ProgressGui(this);
        Bukkit.getPluginManager().registerEvents(this, this.parent);
        Bukkit.getPluginManager().registerEvents(new SellMenuClickListener(this), this.parent);
        this.historyTracker = new HistoryTracker();
        this.sellHistoryGui = new SellHistoryGui(this);
        Bukkit.getPluginManager().registerEvents(new HistoryClickListener(this), this.parent);
        new SellHistoryCommand(this);
        new SellPlaceholderExpansion(this).register();
        new SellCommand(this);
        new WorthCommand(this);
        this.resetConfirmationGui = new ResetConfirmationGui(this);
        Bukkit.getPluginManager().registerEvents(new GuiClickListener(this), this.parent);
        this.sellAxeKey = new NamespacedKey(this.parent, "sell_wand");
        this.expiryKey = new NamespacedKey(this.parent, "sell_wand_expiry");
        new SellAxeCommand(this, this.sellAxeKey, this.expiryKey);
        new SellAxe(this, this.sellAxeKey, this.expiryKey);
        new VaultMoneyPlaceholder(this).register();
        this.unregisterOtherSellCommands();
        new ToggleWorthCommand(this);

        if (this.getConfig().getBoolean("use-new-sell-menu", false)) {
            Objects.requireNonNull(this.parent.getCommand("sellmulti")).setExecutor((sender, cmd, lbl, args) -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(Utils.toComponent("&cOnly players can use this command.")); return true; }
                this.getSellGui().openNew(p);
                return true;
            });
        } else {
            this.unregisterSellMultiCommand();
        }

        if (!this.setupVault()) {
            this.parent.getLogger().severe("[DonutCore] Vault not found — economy features disabled. Install Vault + an economy plugin.");
        }

        if (this.getConfig().getBoolean("sell-axe.use-countdown", true)) {
            this.runRepeatingGlobal(() -> {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.runAtPlayer(player, () -> {
                        PlayerInventory inv = player.getInventory();
                        for (int slot = 0; slot < inv.getSize(); ++slot) {
                            ItemStack item = inv.getItem(slot);
                            if (item == null || item.getType().isAir()) continue;
                            ItemMeta meta = item.getItemMeta();
                            if (meta == null) continue;
                            PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            Byte marker = pdc.get(this.sellAxeKey, PersistentDataType.BYTE);
                            if (marker == null || marker != 1) continue;
                            Long expiry = pdc.get(this.expiryKey, PersistentDataType.LONG);
                            if (expiry == null) continue;
                            if (now >= expiry) {
                                inv.setItem(slot, null);
                                player.sendMessage(Utils.toComponent("&cYour Sell Wand has expired and been removed."));
                                continue;
                            }
                            String formatted = this.formatDuration(expiry - now);
                            List<String> template = this.getConfig().getStringList("sell-axe.lore");
                            ArrayList<Component> newLore = new ArrayList<>();
                            for (String line : template) newLore.add(Utils.toComponent(line.replace("%countdown%", formatted)));
                            meta.lore(newLore);
                            item.setItemMeta(meta);
                        }
                    });
                }
            }, 0L, 200L);
        }

        this.setupPacketLoreHook();
        this.scheduleRestartWarningIfNeeded();
        this.parent.getLogger().info("[DonutCore] Sell module enabled.");
    }

    public void disable() {
        if (this.restartWarningTask != null) {
            this.restartWarningTask.cancel();
            this.restartWarningTask = null;
        }
        for (Player p : this.parent.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
        }
        this.teardownPacketLoreHook();
        if (this.usingMySQL && this.mysql != null) {
            this.mysql.closeQuietly();
        } else {
            this.saveHistory();
            this.saveToggleWorthToSave();
        }
        this.parent.getLogger().info("[DonutCore] Sell module disabled.");
    }

    public void reloadPlugin() {
        this.reloadConfig();
        this.reloadWorthConfig();
        this.reloadMessagesConfig();
        this.reloadMysqlConfig();
        this.reloadMenusConfig();
        this.rebuildDisabledItemsCache();
        this.reloadPacketLoreConfig();
        this.itemPricesMenu.reloadData();
        this.adminPriceEditorMenu.reloadData();
        this.sellGui.loadConfig();
        this.progressGui.loadConfig();
        this.sellHistoryGui.loadConfig();
        this.buildCategoryItems();
        for (Player p : this.parent.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
        }
        this.scheduleRestartWarningIfNeeded();
        this.parent.getLogger().info("[DonutCore] Sell config reloaded.");
    }

    private void scheduleRestartWarningIfNeeded() {
        boolean current = this.getConfig().getBoolean("use-new-sell-menu", false);
        if (this.lastKnownUseNewSellMenu == null) { this.lastKnownUseNewSellMenu = current; return; }
        if (!this.lastKnownUseNewSellMenu.equals(current)) {
            if (this.restartWarningTask != null) this.restartWarningTask.cancel();
            this.restartWarningTask = this.runRepeatingGlobal(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    this.runAtPlayer(online, () -> {
                        if (online.hasPermission("sell.admin"))
                            online.sendMessage(Utils.toComponent("&cWARNING: You changed use-new-sell-menu. Please restart your server!"));
                    });
                }
            }, 0L, 100L);
        }
        this.lastKnownUseNewSellMenu = current;
    }

    private boolean setupVault() {
        if (this.parent.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = this.parent.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        this.econ = rsp.getProvider();
        return this.econ != null;
    }

    private void setupSaveFile() {
        this.saveFile = new File(this.parent.getDataFolder(), "sell/saves.yml");
        if (!this.saveFile.exists()) { this.saveFile.getParentFile().mkdirs(); this.saveResource("sell/saves.yml", false); }
        this.saveConfig = YamlConfiguration.loadConfiguration(this.saveFile);
    }

    private void setupWorthFile() {
        this.worthFile = new File(this.parent.getDataFolder(), "sell/worth.yml");
        boolean created = !this.worthFile.exists();
        if (created) { this.worthFile.getParentFile().mkdirs(); this.saveResource("sell/worth.yml", false); }
        this.worthConfig = YamlConfiguration.loadConfiguration(this.worthFile);
        if (created && this.getConfig().isConfigurationSection("categories") && !this.worthConfig.isConfigurationSection("categories")) {
            this.worthConfig.set("categories", this.getConfig().get("categories"));
            this.saveWorthConfig();
        }
    }

    private void setupMessagesFile() {
        this.messagesFile = new File(this.parent.getDataFolder(), "sell/messages.yml");
        boolean created = !this.messagesFile.exists();
        if (created) { this.messagesFile.getParentFile().mkdirs(); this.saveResource("sell/messages.yml", false); }
        this.messagesConfig = YamlConfiguration.loadConfiguration(this.messagesFile);
        if (created && this.getConfig().isConfigurationSection("messages") && !this.messagesConfig.isConfigurationSection("messages")) {
            ConfigurationSection sec = this.getConfig().getConfigurationSection("messages");
            if (sec != null) this.messagesConfig.set("messages", sec.getValues(true));
            this.saveYaml(this.messagesConfig, this.messagesFile, "sell/messages.yml");
        }
    }

    private void setupMysqlFile() {
        this.mysqlFile = new File(this.parent.getDataFolder(), "sell/mysql.yml");
        boolean created = !this.mysqlFile.exists();
        if (created) { this.mysqlFile.getParentFile().mkdirs(); this.saveResource("sell/mysql.yml", false); }
        this.mysqlConfig = YamlConfiguration.loadConfiguration(this.mysqlFile);
        if (created && this.getConfig().isConfigurationSection("mysql") && !this.mysqlConfig.isConfigurationSection("mysql")) {
            ConfigurationSection sec = this.getConfig().getConfigurationSection("mysql");
            if (sec != null) this.mysqlConfig.set("mysql", sec.getValues(true));
            this.saveYaml(this.mysqlConfig, this.mysqlFile, "sell/mysql.yml");
        }
    }

    private void setupMenusFile() {
        this.menusFile = new File(this.parent.getDataFolder(), "sell/menus.yml");
        boolean created = !this.menusFile.exists();
        if (created) { this.menusFile.getParentFile().mkdirs(); this.saveResource("sell/menus.yml", false); }
        this.menusConfig = YamlConfiguration.loadConfiguration(this.menusFile);
        if (created) {
            this.copyMissingSection(this.getConfig(), this.menusConfig, "sell-menu");
            this.copyMissingSection(this.getConfig(), this.menusConfig, "new-sell-menu");
            this.copyMissingSection(this.getConfig(), this.menusConfig, "category-menu");
            this.copyMissingSection(this.getConfig(), this.menusConfig, "progress-menu");
            this.copyMissingSection(this.getConfig(), this.menusConfig, "sellhistory-menu");
            this.copyMissingSection(this.getConfig(), this.menusConfig, "item-prices-menu");
            this.saveYaml(this.menusConfig, this.menusFile, "sell/menus.yml");
        }
    }

    public FileConfiguration getWorthConfig() { return this.worthConfig; }
    public FileConfiguration getMessagesConfig() { return this.messagesConfig; }
    public FileConfiguration getMysqlConfig() { return this.mysqlConfig; }
    public FileConfiguration getMenusConfig() { return this.menusConfig; }

    public void saveWorthConfig() {
        if (this.worthFile == null || this.worthConfig == null) return;
        try { this.worthConfig.save(this.worthFile); }
        catch (IOException e) { this.parent.getLogger().log(Level.WARNING, "Failed to save worth.yml: " + e.getMessage(), e); }
    }

    private void reloadWorthConfig() {
        if (this.worthFile == null) { this.setupWorthFile(); return; }
        this.worthConfig = YamlConfiguration.loadConfiguration(this.worthFile);
    }
    private void reloadMessagesConfig() { this.messagesConfig = YamlConfiguration.loadConfiguration(this.messagesFile); }
    private void reloadMysqlConfig() { this.mysqlConfig = YamlConfiguration.loadConfiguration(this.mysqlFile); }
    private void reloadMenusConfig() { this.menusConfig = YamlConfiguration.loadConfiguration(this.menusFile); }

    private void copyMissingSection(FileConfiguration src, FileConfiguration dst, String path) {
        if (!src.isSet(path) || dst.isSet(path)) return;
        dst.set(path, src.get(path));
    }

    private void saveYaml(FileConfiguration cfg, File file, String name) {
        try { cfg.save(file); }
        catch (IOException e) { this.parent.getLogger().log(Level.WARNING, "Failed to save " + name + ": " + e.getMessage(), e); }
    }

    private void setupStorage() {
        this.setupSaveFile();
        String storageType = this.getConfig().getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT);
        if ("MYSQL".equals(storageType)) {
            this.mysql = new SellMySQL(this, SellMySQL.StorageMode.MYSQL);
            if (this.mysql.init()) {
                this.usingMySQL = true;
                this.mysql.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
                this.parent.getLogger().info("[DonutCore] MySQL connection established.");
                return;
            }
            this.parent.getLogger().severe("[DonutCore] MySQL connection failed. Falling back to SQLite/YAML.");
        }
        if (!"YAML".equals(storageType)) {
            this.mysql = new SellMySQL(this, SellMySQL.StorageMode.SQLITE);
            if (this.mysql.init()) {
                this.usingMySQL = true;
                this.mysql.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
                this.parent.getLogger().info("[DonutCore] SQLite initialized.");
                return;
            }
            this.parent.getLogger().severe("[DonutCore] SQLite connection failed. Falling back to saves.yml.");
        }
        this.loadHistory();
        this.loadToggleWorthFromSave();
        this.parent.getLogger().info("[DonutCore] Using saves.yml storage.");
    }

    @EventHandler
    public void onPlayerJoinStrip(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        this.cleanupListener.stripAllLore(p);
        p.updateInventory();
        this.runAtPlayerLater(p, p::updateInventory, 1L);
        if (this.usingMySQL && this.mysql != null) {
            UUID id = p.getUniqueId();
            this.runAsync(() -> {
                SellMySQL.PlayerSnapshot snap = this.mysql.loadPlayerData(id);
                if (snap == null) return;
                this.runAtPlayer(p, () -> {
                    this.totalSold.put(id, snap.total);
                    this.soldByCategory.put(id, snap.categories);
                    this.itemHistory.put(id, snap.items);
                    if (snap.toggleDisabled) this.toggleWorthDisabled.add(id);
                    else this.toggleWorthDisabled.remove(id);
                });
            });
        }
    }

    @EventHandler
    public void onPlayerQuitStrip(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        this.cleanupListener.stripAllLore(p);
        p.updateInventory();
    }

    private void unregisterOtherSellCommands() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            SimpleCommandMap cmdMap = (SimpleCommandMap) cmdMapField.get(Bukkit.getServer());
            Field knownCmdsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCmdsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> known = (Map<String, Command>) knownCmdsField.get(cmdMap);
            PluginCommand ourSell = this.parent.getCommand("sell");
            Iterator<Map.Entry<String, Command>> it = known.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Command> entry = it.next();
                String key = entry.getKey();
                Command cmd = entry.getValue();
                if (cmd == ourSell) continue;
                if (key.equalsIgnoreCase("sell") || key.toLowerCase(Locale.ROOT).endsWith(":sell")) it.remove();
            }
            if (ourSell != null) known.putIfAbsent("sell", ourSell);
        } catch (Exception e) {
            this.parent.getLogger().warning("Failed to unregister other /sell commands: " + e.getMessage());
        }
    }

    private void unregisterSellMultiCommand() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            SimpleCommandMap cmdMap = (SimpleCommandMap) cmdMapField.get(Bukkit.getServer());
            Field knownCmdsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCmdsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> known = (Map<String, Command>) knownCmdsField.get(cmdMap);
            Iterator<Map.Entry<String, Command>> it = known.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Command> entry = it.next();
                String key = entry.getKey();
                Command cmd = entry.getValue();
                if (key.equalsIgnoreCase("sellmulti") || key.toLowerCase(Locale.ROOT).endsWith(":sellmulti")
                        || (cmd instanceof PluginCommand pc && pc.getName().equalsIgnoreCase("sellmulti"))) {
                    it.remove();
                }
            }
        } catch (Exception e) {
            this.parent.getLogger().warning("Failed to unregister /sellmulti command: " + e.getMessage());
        }
    }

    private void loadHistory() {
        for (String root : this.saveConfig.getKeys(false)) {
            if ("toggleworth-disabled".equalsIgnoreCase(root)) continue;
            UUID uuid;
            try { uuid = UUID.fromString(root); } catch (IllegalArgumentException e) { continue; }
            this.totalSold.put(uuid, this.saveConfig.getDouble(root + ".total", 0.0));
            HashMap<String, Double> catMap = new HashMap<>();
            if (this.saveConfig.isConfigurationSection(root + ".categories")) {
                ConfigurationSection sec = this.saveConfig.getConfigurationSection(root + ".categories");
                for (String cat : sec.getKeys(false)) catMap.put(cat, sec.getDouble(cat, 0.0));
            }
            this.soldByCategory.put(uuid, catMap);
            HashMap<String, Stats> itemMap = new HashMap<>();
            if (this.saveConfig.isConfigurationSection(root + ".items")) {
                ConfigurationSection sec = this.saveConfig.getConfigurationSection(root + ".items");
                for (String key : sec.getKeys(false)) {
                    double cnt = this.saveConfig.getDouble(root + ".items." + key + ".count", 0.0);
                    double rev = this.saveConfig.getDouble(root + ".items." + key + ".revenue", 0.0);
                    itemMap.put(key, new Stats(cnt, rev));
                }
            }
            this.itemHistory.put(uuid, itemMap);
        }
    }

    private void saveHistory() {
        for (Map.Entry<UUID, Double> entry : this.totalSold.entrySet())
            this.saveConfig.set(entry.getKey().toString() + ".total", entry.getValue());
        for (Map.Entry<UUID, Map<String, Double>> entry : this.soldByCategory.entrySet()) {
            String u = entry.getKey().toString();
            for (Map.Entry<String, Double> ec : entry.getValue().entrySet())
                this.saveConfig.set(u + ".categories." + ec.getKey(), ec.getValue());
        }
        for (Map.Entry<UUID, Map<String, Stats>> entry : this.itemHistory.entrySet()) {
            String u = entry.getKey().toString();
            for (Map.Entry<String, Stats> es : entry.getValue().entrySet()) {
                this.saveConfig.set(u + ".items." + es.getKey() + ".count", es.getValue().count);
                this.saveConfig.set(u + ".items." + es.getKey() + ".revenue", es.getValue().revenue);
            }
        }
        try { this.saveConfig.save(this.saveFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadToggleWorthFromSave() {
        this.toggleWorthDisabled.clear();
        for (String s : this.saveConfig.getStringList("toggleworth-disabled")) {
            try { this.toggleWorthDisabled.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveToggleWorthToSave() {
        this.saveConfig.set("toggleworth-disabled", this.toggleWorthDisabled.stream().map(UUID::toString).toList());
        try { this.saveConfig.save(this.saveFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isWorthEnabled(UUID id) { return !this.toggleWorthDisabled.contains(id); }

    public void setWorthEnabled(UUID id, boolean enabled) {
        if (enabled) this.toggleWorthDisabled.remove(id);
        else this.toggleWorthDisabled.add(id);
        if (this.usingMySQL && this.mysql != null) this.mysql.setToggleWorthDisabled(id, !enabled);
        else this.saveToggleWorthToSave();
    }

    private void buildCategoryItems() {
        this.itemValues.clear();
        this.categoryItems.clear();
        ConfigurationSection cats = this.getWorthConfig().getConfigurationSection("categories");
        if (cats == null) return;
        for (String cat : cats.getKeys(false)) {
            List<?> raw = this.getWorthConfig().getList("categories." + cat);
            ArrayList<String> mats = new ArrayList<>();
            if (raw != null) {
                for (Object o : raw) {
                    if (!(o instanceof Map<?, ?> map)) continue;
                    for (Map.Entry<?, ?> me : map.entrySet()) {
                        String entryKey = me.getKey().toString().trim();
                        double price;
                        try { price = Double.parseDouble(me.getValue().toString()); }
                        catch (NumberFormatException ex) {
                            this.parent.getLogger().warning("Invalid price for '" + entryKey + "' in category '" + cat + "'");
                            continue;
                        }
                        String lowerKey = entryKey.toLowerCase(Locale.ROOT);
                        this.itemValues.put(lowerKey, price);
                        mats.add(lowerKey.replaceAll("(?i)-value$", "").toUpperCase(Locale.ROOT));
                    }
                }
            }
            this.categoryItems.put(cat, mats);
        }
    }

    public double getPrice(String key) {
        return this.itemValues.getOrDefault(key.toLowerCase(Locale.ROOT), this.getConfig().getDouble("default-value", 0.1));
    }

    public String getLookupKey(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta im = item.getItemMeta();
        if (item.getType() == Material.SPAWNER && im instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof CreatureSpawner cs) {
            return cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
        }
        if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta esm && esm.getStoredEnchants().size() == 1) {
            Map.Entry<Enchantment, Integer> e = esm.getStoredEnchants().entrySet().iterator().next();
            return e.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + e.getValue() + "-value";
        }
        String potionKey = this.getPotionKey(item);
        if (potionKey != null) return potionKey + "-value";
        return item.getType().name().toLowerCase(Locale.ROOT) + "-value";
    }

    public boolean hasListedPrice(ItemStack item) {
        String key = this.getLookupKey(item);
        return key != null && this.itemValues.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public void promptPriceEdit(Player player, ItemStack clicked) {
        if (this.adminPriceEditorMenu == null || !player.hasPermission("sell.admin")) return;
        if (this.getLookupKey(clicked) == null) return;
        player.closeInventory();
        player.sendMessage(Utils.toComponent("&ePlease use &f/donutsell prices &eto edit prices."));
    }

    public boolean isSellable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (this.disabledItemsUpper.contains(item.getType().name().toUpperCase(Locale.ROOT))) return false;
        return !this.getConfig().getBoolean("missing-price-not-sellable", false) || this.hasListedPrice(item);
    }

    public Map<String, Double> getItemValues() { return Collections.unmodifiableMap(this.itemValues); }

    public void recordSale(Player p, Map<String, Stats> sold) {
        UUID id = p.getUniqueId();
        this.itemHistory.computeIfAbsent(id, k -> new HashMap<>());
        sold.forEach((k, st) -> {
            Stats old = this.itemHistory.get(id).getOrDefault(k, new Stats(0.0, 0.0));
            this.itemHistory.get(id).put(k, new Stats(old.count + st.count, old.revenue + st.revenue));
        });
        double sum = sold.values().stream().mapToDouble(s -> s.revenue).sum();
        this.totalSold.merge(id, sum, Double::sum);
        Map<String, Double> cmap = this.soldByCategory.computeIfAbsent(id, k -> new HashMap<>());
        HashMap<String, Double> catDelta = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : this.categoryItems.entrySet()) {
            String catName = entry.getKey();
            double catSum = sold.entrySet().stream()
                    .filter(e -> entry.getValue().contains(e.getKey().toUpperCase(Locale.ROOT)))
                    .mapToDouble(e -> e.getValue().revenue).sum();
            if (!(catSum > 0.0)) continue;
            cmap.merge(catName, catSum, Double::sum);
            catDelta.put(catName, catSum);
        }
        if (this.usingMySQL && this.mysql != null && sum > 0.0) {
            HashMap<String, Double> catDeltaCopy = new HashMap<>(catDelta);
            HashMap<String, Stats> soldCopy = new HashMap<>();
            sold.forEach((k, st) -> soldCopy.put(k, new Stats(st.count, st.revenue)));
            this.runAsync(() -> this.mysql.applySaleDelta(id, sum, catDeltaCopy, soldCopy));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!DonutCore.getInstance().getSellModule().isActive()) return;
        Player p = (Player) event.getPlayer();
        String openTitle = Utils.stripColor(event.getView().title());
        String classicTitle = Utils.stripColor(Utils.formatColors(this.getMenusConfig().getString("sell-menu.title", "&aSell Items")));
        if (!openTitle.equals(classicTitle)) return;

        Inventory inv = event.getInventory();
        boolean useNewFlag = this.getConfig().getBoolean("use-new-sell-menu", false);
        boolean excludeBottomRow = !useNewFlag && this.isUseMultipliers();
        int sellableSlots = inv.getSize() - (excludeBottomRow ? 9 : 0);
        Set<String> disabledSet = this.disabledItemsUpper;
        Sound declineSound = Utils.resolveSound(this.getConfig().getString("sounds.declined", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
        String declineMsg = Utils.formatColors(this.getMessagesConfig().getString("messages.cannot-sell", "&cYou cannot sell that item!"));
        HashMap<String, Stats> sold = new HashMap<>();
        HashMap<String, Double> revCats = new HashMap<>();
        boolean notifiedDecline = false;

        // Pass 1: return blocked items
        for (int i = 0; i < sellableSlots; ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            String mat = item.getType().name();
            boolean isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
            boolean missingPriceBlocked = this.getConfig().getBoolean("missing-price-not-sellable", false) && !this.hasListedPrice(item);
            if ((!disabledSet.contains(mat) && !missingPriceBlocked) || isShulkerBox) continue;
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
            leftover.values().forEach(d -> p.getWorld().dropItemNaturally(p.getLocation(), d));
            inv.setItem(i, null);
            if (!notifiedDecline) {
                p.playSound(p.getLocation(), declineSound, 1.0f, 1.0f);
                p.sendMessage(declineMsg);
                notifiedDecline = true;
            }
        }

        // Pass 2: process sellable items
        for (int i = 0; i < sellableSlots; ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            String mat = item.getType().name();
            boolean isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
            if (this.getConfig().getBoolean("missing-price-not-sellable", false) && !this.hasListedPrice(item)) continue;

            if (isShulkerBox && disabledSet.contains(mat)) {
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof BlockStateMeta bsm) || !(bsm.getBlockState() instanceof ShulkerBox boxState)) { inv.setItem(i, null); continue; }
                for (ItemStack inside : boxState.getInventory().getContents()) {
                    if (inside == null || inside.getType().isAir() || disabledSet.contains(inside.getType().name())) continue;
                    ItemMeta insideMeta = inside.getItemMeta();
                    if (inside.getType() == Material.ENCHANTED_BOOK && insideMeta instanceof EnchantmentStorageMeta innerESM) {
                        for (Map.Entry<Enchantment, Integer> entry : innerESM.getStoredEnchants().entrySet()) {
                            String keyName = entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + entry.getValue();
                            double totalRev = this.getPrice(keyName + "-value") * inside.getAmount();
                            sold.merge(keyName, new Stats(inside.getAmount(), totalRev), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                                if (!cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) continue;
                                revCats.merge(cat.getKey(), totalRev, Double::sum);
                            }
                        }
                        continue;
                    }
                    String insideKey = inside.getType().name().toLowerCase(Locale.ROOT);
                    double d = this.calculateItemWorth(inside);
                    sold.merge(insideKey, new Stats(inside.getAmount(), d), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(inside.getType().name())) continue;
                        revCats.merge(cat.getKey(), d, Double::sum);
                    }
                }
                ItemStack emptyBox = new ItemStack(item.getType(), item.getAmount());
                inv.setItem(i, null);
                Map<Integer, ItemStack> returned = p.getInventory().addItem(emptyBox);
                returned.values().forEach(d -> p.getWorld().dropItemNaturally(p.getLocation(), d));
                continue;
            }

            if (isShulkerBox) {
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof BlockStateMeta bsm) || !(bsm.getBlockState() instanceof ShulkerBox boxState)) continue;
                String boxKey = item.getType().name().toLowerCase(Locale.ROOT);
                double boxValue = this.getPrice(boxKey + "-value") * item.getAmount();
                sold.merge(boxKey, new Stats(item.getAmount(), boxValue), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                for (Map.Entry<String, List<String>> entry : this.categoryItems.entrySet()) {
                    if (!entry.getValue().contains(item.getType().name())) continue;
                    revCats.merge(entry.getKey(), boxValue, Double::sum);
                }
                for (ItemStack inside : boxState.getInventory().getContents()) {
                    if (inside == null || inside.getType().isAir()) continue;
                    ItemMeta insideMeta = inside.getItemMeta();
                    if (inside.getType() == Material.ENCHANTED_BOOK && insideMeta instanceof EnchantmentStorageMeta esm) {
                        for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                            String keyName = entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + entry.getValue();
                            double totalRev = this.getPrice(keyName + "-value") * inside.getAmount();
                            sold.merge(keyName, new Stats(inside.getAmount(), totalRev), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                                if (!cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) continue;
                                revCats.merge(cat.getKey(), totalRev, Double::sum);
                            }
                        }
                        continue;
                    }
                    String insideKey = inside.getType().name().toLowerCase(Locale.ROOT);
                    double d = this.calculateItemWorth(inside);
                    sold.merge(insideKey, new Stats(inside.getAmount(), d), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(inside.getType().name())) continue;
                        revCats.merge(cat.getKey(), d, Double::sum);
                    }
                }
                continue;
            }

            if (disabledSet.contains(mat)) continue;
            ItemMeta im = item.getItemMeta();
            if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta esm) {
                for (Map.Entry<Enchantment, Integer> eEntry : esm.getStoredEnchants().entrySet()) {
                    String keyName = eEntry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + eEntry.getValue();
                    double totalRev = this.getPrice(keyName + "-value") * item.getAmount();
                    sold.merge(keyName, new Stats(item.getAmount(), totalRev), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) continue;
                        revCats.merge(cat.getKey(), totalRev, Double::sum);
                    }
                }
                continue;
            }
            String key = item.getType().name().toLowerCase(Locale.ROOT);
            double raw = this.calculateItemWorth(item);
            sold.merge(key, new Stats(item.getAmount(), raw), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                if (!cat.getValue().contains(item.getType().name())) continue;
                revCats.merge(cat.getKey(), raw, Double::sum);
            }
        }

        if (sold.isEmpty()) return;
        this.recordSale(p, sold);
        double payout;
        if (this.isUseMultipliers()) {
            double categorized = revCats.entrySet().stream().mapToDouble(e -> e.getValue() * this.getSellMultiplier(p.getUniqueId(), e.getKey())).sum();
            double uncategorized = sold.entrySet().stream()
                    .filter(e -> this.categoryItems.values().stream().noneMatch(list -> list.contains(e.getKey().toUpperCase(Locale.ROOT))))
                    .mapToDouble(e -> e.getValue().revenue).sum();
            payout = categorized + uncategorized;
        } else {
            payout = sold.values().stream().mapToDouble(s -> s.revenue).sum();
        }
        this.getEconomy().depositPlayer((OfflinePlayer) p, payout);
        Sound soundOnClose = Utils.resolveSound(this.getMenusConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        p.playSound(p.getLocation(), soundOnClose, 1.0f, 1.0f);
        long itemsSold = Math.round(sold.values().stream().mapToDouble(s -> s.count).sum());
        this.notifySale(p, payout, itemsSold);
    }

    private EnumSet<SellNotifyMode> getNotifyModes() {
        EnumSet<SellNotifyMode> modes = EnumSet.noneOf(SellNotifyMode.class);
        Object raw = this.getConfig().get("sell-notify-mode");
        if (raw == null) raw = this.getConfig().get("sell-shower");
        if (raw instanceof String s) this.addModesFromString(s, modes);
        else if (raw instanceof List<?> list) { for (Object o : list) this.addModesFromString(String.valueOf(o), modes); }
        else if (raw != null) this.addModesFromString(String.valueOf(raw), modes);
        if (modes.isEmpty()) modes.add(SellNotifyMode.ACTIONBAR);
        return modes;
    }

    private void addModesFromString(String s, EnumSet<SellNotifyMode> modes) {
        if (s == null) return;
        for (String p : s.split("[,;\\s]+")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try { modes.add(SellNotifyMode.valueOf(t.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void notifySale(Player p, double moneyEarned, long itemsSold) {
        EnumSet<SellNotifyMode> modes = this.getNotifyModes();
        String amt = Utils.abbreviateNumber(moneyEarned);
        String itemsStr = String.valueOf(itemsSold);
        if (modes.contains(SellNotifyMode.CHAT)) {
            String chatMsg = Utils.formatColors(this.getMenusConfig().getString("sell-menu.chat-message", "&#34ee80+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            p.sendMessage(Utils.toComponent(chatMsg));
        }
        if (modes.contains(SellNotifyMode.ACTIONBAR)) {
            String actionbar = Utils.formatColors(this.getMenusConfig().getString("sell-menu.actionbar-message", "&#34ee80+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            p.sendActionBar(Utils.toComponent(actionbar));
        }
        if (modes.contains(SellNotifyMode.TITLE)) {
            String title = Utils.formatColors(this.getConfig().getString("sell-notify.screen.title", "&a+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            String subtitle = Utils.formatColors(this.getConfig().getString("sell-notify.screen.subtitle", "&7You sold %items% items")).replace("%amount%", amt).replace("%items%", itemsStr);
            p.showTitle(Title.title(
                Utils.toComponent(title),
                Utils.toComponent(subtitle),
                Title.Times.times(
                    Duration.ofMillis(this.getConfig().getInt("sell-notify.screen.fade-in", 5) * 50L),
                    Duration.ofMillis(this.getConfig().getInt("sell-notify.screen.stay", 40) * 50L),
                    Duration.ofMillis(this.getConfig().getInt("sell-notify.screen.fade-out", 10) * 50L)
                )
            ));
        }
    }

    private String getPotionKey(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta pm)) return null;
        PotionType potionType = pm.getBasePotionType();
        if (potionType == null) return null;
        String base = potionType.name().toLowerCase(Locale.ROOT);
        if (item.getType() == Material.SPLASH_POTION) base = "splash_" + base;
        else if (item.getType() == Material.LINGERING_POTION) base = "lingering_" + base;
        return base;
    }

    public double calculateItemWorth(ItemStack item) {
        ItemMeta im = item.getItemMeta();
        boolean missingNotSellable = this.getConfig().getBoolean("missing-price-not-sellable", false);
        double base;
        if (item.getType() == Material.SPAWNER && im instanceof BlockStateMeta bsm) {
            BlockState blockState = bsm.getBlockState();
            if (blockState instanceof CreatureSpawner cs) {
                String spawned = cs.getSpawnedType().name().toLowerCase(Locale.ROOT);
                String key = spawned + "_spawner-value";
                base = this.getPrice(key);
                if (missingNotSellable && !this.itemValues.containsKey(key.toLowerCase(Locale.ROOT))) return 0.0;
            } else {
                base = this.getPrice("spawner-value");
                if (missingNotSellable && !this.itemValues.containsKey("spawner-value")) return 0.0;
            }
        } else {
            String potionKey = this.getPotionKey(item);
            if (potionKey != null) {
                base = this.getPrice(potionKey + "-value");
                if (missingNotSellable && !this.itemValues.containsKey((potionKey + "-value").toLowerCase(Locale.ROOT))) return 0.0;
            } else {
                String baseKey = item.getType().name().toLowerCase(Locale.ROOT) + "-value";
                base = this.getPrice(baseKey);
                if (missingNotSellable && !this.itemValues.containsKey(baseKey)) return 0.0;
            }
        }
        double ench = 0.0;
        if (im instanceof EnchantmentStorageMeta esm) {
            for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet())
                ench += this.getPrice(entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + entry.getValue() + "-value");
        }
        if (im != null) {
            for (Map.Entry<Enchantment, Integer> entry : im.getEnchants().entrySet())
                ench += this.getPrice(entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + entry.getValue() + "-value");
        }
        double total = (base + ench) * item.getAmount();
        if (im instanceof BlockStateMeta bsmTotal && bsmTotal.getBlockState() instanceof ShulkerBox box) {
            for (ItemStack inside : box.getInventory().getContents()) {
                if (inside == null || inside.getType() == Material.AIR) continue;
                total += this.calculateItemWorth(inside);
            }
        }
        return total;
    }

    public void resetPlayerData(UUID uuid) {
        this.totalSold.remove(uuid);
        this.soldByCategory.remove(uuid);
        this.itemHistory.remove(uuid);
        this.toggleWorthDisabled.remove(uuid);
        this.saveConfig.set(uuid.toString(), null);
        if (!this.usingMySQL) {
            try { this.saveConfig.save(this.saveFile); }
            catch (IOException e) { this.parent.getLogger().severe("Could not save resets for " + uuid + ": " + e.getMessage()); }
        }
        if (this.usingMySQL && this.mysql != null) this.mysql.resetPlayerData(uuid);
    }

    public double getSellMultiplier(UUID u, String cat) {
        ConfigurationSection levels = this.getMenusConfig().getConfigurationSection("progress-menu.levels");
        if (levels == null) return 1.0;
        double soldInCategory = this.soldByCategory.getOrDefault(u, Collections.emptyMap()).getOrDefault(cat, 0.0);
        double multiplier = 1.0;
        for (String key : levels.getKeys(false)) {
            ConfigurationSection lvlSec = levels.getConfigurationSection(key);
            if (lvlSec == null) continue;
            double needed = lvlSec.getDouble("amountNeeded", Double.MAX_VALUE);
            double multi = lvlSec.getDouble("multi", 1.0);
            if (soldInCategory >= needed) multiplier = multi;
        }
        return multiplier;
    }

    public ViewTracker getViewTracker() { return this.viewTracker; }
    public ItemPricesMenu getItemPricesMenu() { return this.itemPricesMenu; }
    public AdminPriceEditorMenu getAdminPriceEditorMenu() { return this.adminPriceEditorMenu; }
    public SellGui getSellGui() { return this.sellGui; }
    public ProgressGui getProgressGui() { return this.progressGui; }
    public Economy getEconomy() { return this.econ; }
    public HistoryTracker getHistoryTracker() { return this.historyTracker; }
    public SellHistoryGui getSellHistoryGui() { return this.sellHistoryGui; }
    public Map<String, Stats> getHistory(UUID u) { return this.itemHistory.getOrDefault(u, Collections.emptyMap()); }
    public String getFormattedTotalSold(UUID u) { return Utils.abbreviateNumber(this.totalSold.getOrDefault(u, 0.0)); }
    public double getRawTotalSold(UUID u, String cat) { return this.soldByCategory.getOrDefault(u, Collections.emptyMap()).getOrDefault(cat, 0.0); }

    public double sumInventory(Inventory inv) {
        double t = 0.0;
        for (ItemStack i : inv.getContents()) {
            if (i == null || i.getType().isAir()) continue;
            t += this.calculateItemWorth(i);
        }
        return t;
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000L;
        return (totalSeconds / 86400L) + "d " + (totalSeconds % 86400L / 3600L) + "h "
                + (totalSeconds % 3600L / 60L) + "m " + (totalSeconds % 60L) + "s";
    }

    public boolean isUseMultipliers() { return this.getConfig().getBoolean("use-multipliers", true); }

    private void rebuildDisabledItemsCache() {
        this.disabledItemsUpper.clear();
        for (String s : this.getConfig().getStringList("disabled-items")) {
            if (s != null) this.disabledItemsUpper.add(s.toUpperCase(Locale.ROOT));
        }
    }

    private void setupPacketLoreHook() {
        try {
            this.packetListenerInstance = new SellPacketListener(this);
            Bukkit.getPluginManager().registerEvents(new WorthListener(this.packetListenerInstance), this.parent);
            this.packetListenerInstance.injectOnlinePlayers();
            this.parent.getLogger().info("[DonutCore] Worth-lore packet hook enabled.");
        } catch (Throwable t) {
            this.packetListenerInstance = null;
            this.parent.getLogger().log(Level.WARNING, "[DonutCore] Failed to initialize worth-lore packet hook: " + t.getMessage(), t);
        }
    }

    private void teardownPacketLoreHook() {
        if (this.packetListenerInstance == null) return;
        try { this.packetListenerInstance.shutdown(); }
        catch (Throwable t) { this.parent.getLogger().log(Level.WARNING, "[DonutCore] Failed to tear down packet hook: " + t.getMessage(), t); }
        finally { this.packetListenerInstance = null; }
    }

    private void reloadPacketLoreConfig() {
        if (this.packetListenerInstance == null) return;
        try { this.packetListenerInstance.reloadConfigData(); }
        catch (Throwable t) { this.parent.getLogger().log(Level.WARNING, "[DonutCore] Failed to reload packet lore config: " + t.getMessage(), t); }
    }

    @FunctionalInterface
    interface CancellableTask { void cancel(); }

    private enum SellNotifyMode { TITLE, ACTIONBAR, CHAT }

    public static class Stats {
        public double count;
        public double revenue;
        public Stats(double count, double revenue) { this.count = count; this.revenue = revenue; }
    }
}
