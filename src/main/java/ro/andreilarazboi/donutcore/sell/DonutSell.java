package ro.andreilarazboi.donutcore.sell;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.event.server.PluginEnableEvent;
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
import org.bukkit.scheduler.BukkitTask;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes", "unused", "null", "removal"})
public final class DonutSell implements Listener {
    private final JavaPlugin parent;
    private FileConfiguration sellConfig;
    private File sellConfigFile;

    public DonutSell(JavaPlugin parent) {
        this.parent = parent;
        instance = this;
    }

    public JavaPlugin getPlugin() {
        return this.parent;
    }

    public FileConfiguration getConfig() {
        return this.sellConfig;
    }

    public void reloadConfig() {
        this.sellConfig = YamlConfiguration.loadConfiguration(this.sellConfigFile);
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

    public File getDataFolder() {
        return this.parent.getDataFolder();
    }

    public org.bukkit.plugin.PluginDescriptionFile getDescription() {
        return this.parent.getDescription();
    }
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
    private final Map<UUID, Double> totalSold = new HashMap<UUID, Double>();
    private final Map<UUID, Map<String, Double>> soldByCategory = new HashMap<UUID, Map<String, Double>>();
    private final Map<UUID, Map<String, Stats>> itemHistory = new HashMap<UUID, Map<String, Stats>>();
    public final Map<String, List<String>> categoryItems = new HashMap<String, List<String>>();
    private final Map<String, Double> itemValues = new HashMap<String, Double>();
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
    private String lastKnownSellMenuOption;
    private final Set<UUID> actionMenuTransfers = new HashSet<UUID>();
    private NamespacedKey sellAxeKey;
    private NamespacedKey expiryKey;
    private NamespacedKey usesRemainingKey;
    private final Set<UUID> toggleWorthDisabled = new HashSet<UUID>();
    private final Set<String> disabledItemsUpper = new HashSet<String>();
    private static DonutSell instance;

    public static DonutSell getInstance() {
        return instance;
    }

    /**
     * Returns {@code true} if the sell module is currently enabled and active.
     * Used as a guard in commands and listeners to prevent execution when the module is disabled.
     */
    public boolean isModuleActive() {
        try {
            return ro.andreilarazboi.donutcore.DonutCore.getInstance().getSellModule().isActive();
        } catch (Exception e) {
            return false;
        }
    }

    public CleanupListener getCleanupListener() {
        return this.cleanupListener;
    }

    public ResetConfirmationGui getResetConfirmationGui() {
        return this.resetConfirmationGui;
    }

    public boolean isUsingMySQL() {
        return this.usingMySQL;
    }

    private boolean isFolia() {
        String serverName = Bukkit.getServer().getName();
        return serverName.equalsIgnoreCase("Folia") || serverName.equalsIgnoreCase("Canvas");
    }

    private void runSyncRegionized(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(this.parent, runnable);
    }

    private void runAsyncRegionized(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(this.parent, task -> runnable.run());
    }

    private void runLaterGlobalRegionized(Runnable runnable, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(this.parent, task -> runnable.run(), Math.max(1L, delayTicks));
    }

    public void runSync(Runnable runnable) {
        if (this.isFolia()) {
            this.runSyncRegionized(runnable);
            return;
        }
        try {
            Bukkit.getScheduler().runTask(this.parent, runnable);
        }
        catch (UnsupportedOperationException ex) {
            this.runSyncRegionized(runnable);
        }
    }

    public void runAsync(Runnable runnable) {
        if (this.isFolia()) {
            this.runAsyncRegionized(runnable);
            return;
        }
        try {
            Bukkit.getScheduler().runTaskAsynchronously(this.parent, runnable);
        }
        catch (UnsupportedOperationException ex) {
            this.runAsyncRegionized(runnable);
        }
    }

    public void runLaterGlobal(Runnable runnable, long delayTicks) {
        if (this.isFolia()) {
            this.runLaterGlobalRegionized(runnable, delayTicks);
            return;
        }
        try {
            Bukkit.getScheduler().runTaskLater(this.parent, runnable, delayTicks);
        }
        catch (UnsupportedOperationException ex) {
            this.runLaterGlobalRegionized(runnable, delayTicks);
        }
    }

    public CancellableTask runRepeatingGlobal(Runnable runnable, long delayTicks, long periodTicks) {
        if (this.isFolia()) {
            long foliaDelayTicks = Math.max(1L, delayTicks);
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.parent, t -> runnable.run(), foliaDelayTicks, periodTicks);
            return () -> ((ScheduledTask)task).cancel();
        }
        try {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.parent, runnable, delayTicks, periodTicks);
            return () -> ((BukkitTask)task).cancel();
        }
        catch (UnsupportedOperationException ex) {
            long foliaDelayTicks = Math.max(1L, delayTicks);
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.parent, t -> runnable.run(), foliaDelayTicks, periodTicks);
            return () -> ((ScheduledTask)task).cancel();
        }
    }

    public void runAtPlayer(Player player, Runnable runnable) {
        if (this.isFolia()) {
            player.getScheduler().execute(this.parent, runnable, null, 1L);
            return;
        }
        try {
            Bukkit.getScheduler().runTask(this.parent, runnable);
        }
        catch (UnsupportedOperationException ex) {
            player.getScheduler().execute(this.parent, runnable, null, 1L);
        }
    }

    public void runAtPlayerLater(Player player, Runnable runnable, long delayTicks) {
        if (this.isFolia()) {
            player.getScheduler().runDelayed(this.parent, task -> runnable.run(), null, Math.max(1L, delayTicks));
            return;
        }
        try {
            Bukkit.getScheduler().runTaskLater(this.parent, runnable, delayTicks);
        }
        catch (UnsupportedOperationException ex) {
            player.getScheduler().runDelayed(this.parent, task -> runnable.run(), null, Math.max(1L, delayTicks));
        }
    }

    private EnumSet<SellNotifyMode> getNotifyModes() {
        EnumSet<SellNotifyMode> modes = EnumSet.noneOf(SellNotifyMode.class);
        Object raw = this.sellConfig.get("sell-notify-mode");
        if (raw == null) {
            raw = this.sellConfig.get("sell-shower");
        }
        if (raw instanceof String) {
            String s = (String)raw;
            this.addModesFromString(s, modes);
        } else if (raw instanceof List) {
            List<?> list = (List<?>)raw;
            for (Object o : list) {
                this.addModesFromString(String.valueOf(o), modes);
            }
        } else if (raw != null) {
            this.addModesFromString(String.valueOf(raw), modes);
        }
        if (modes.isEmpty()) {
            modes.add(SellNotifyMode.ACTIONBAR);
        }
        return modes;
    }

    private void addModesFromString(String s, EnumSet<SellNotifyMode> modes) {
        if (s == null) {
            return;
        }
        for (String p : s.split("[,;\\s]+")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try {
                modes.add(SellNotifyMode.valueOf(t.toUpperCase(Locale.ROOT)));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
    }

    public void notifySale(Player p, double moneyEarned, long itemsSold) {
        EnumSet<SellNotifyMode> modes = this.getNotifyModes();
        String amt = Utils.abbreviateNumber(moneyEarned);
        String itemsStr = String.valueOf(itemsSold);
        if (modes.contains((Object)SellNotifyMode.CHAT)) {
            String chatMsg = Utils.formatColors(this.getMenusConfig().getString("sell-menu.chat-message", "&#34ee80+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            p.sendMessage(chatMsg);
        }
        if (modes.contains((Object)SellNotifyMode.ACTIONBAR)) {
            String actionbar = Utils.formatColors(this.getMenusConfig().getString("sell-menu.actionbar-message", "&#34ee80+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)actionbar));
        }
        if (modes.contains((Object)SellNotifyMode.TITLE)) {
            String title = Utils.formatColors(this.sellConfig.getString("sell-notify.screen.title", "&a+$%amount%")).replace("%amount%", amt).replace("%items%", itemsStr);
            String subtitle = Utils.formatColors(this.sellConfig.getString("sell-notify.screen.subtitle", "&7You sold %items% items")).replace("%amount%", amt).replace("%items%", itemsStr);
            int fadeIn = this.sellConfig.getInt("sell-notify.screen.fade-in", 5);
            int stay = this.sellConfig.getInt("sell-notify.screen.stay", 40);
            int fadeOut = this.sellConfig.getInt("sell-notify.screen.fade-out", 10);
            p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void enable() {
        instance = this;
        try {
            this.parent.getServer().getServicesManager().register(
                    andreilarazboi.lifestealorder.api.DonutOrderApi.class,
                    new andreilarazboi.lifestealorder.api.DonutOrderApi(),
                    this.parent,
                    org.bukkit.plugin.ServicePriority.Normal
            );
            this.parent.getLogger().info("Successfully registered DonutOrderApi service internally.");
        } catch (Throwable t) {
            this.parent.getLogger().log(java.util.logging.Level.SEVERE, "Failed to register DonutOrderApi service", t);
        }
        this.sellConfigFile = new File(this.parent.getDataFolder(), "sell/config.yml"); if (!this.sellConfigFile.exists()) this.saveResource("sell/config.yml", false); this.sellConfig = YamlConfiguration.loadConfiguration(this.sellConfigFile);
        this.setupMessagesFile();
        this.setupMysqlFile();
        this.setupMenusFile();
        this.setupWorthFile();
        this.buildCategoryItems();
        this.setupStorage();
        this.rebuildDisabledItemsCache();
        if (!this.setupVault()) {
            this.parent.getLogger().severe("Vault not found, disabling plugin.");
            this.parent.getServer().getPluginManager().disablePlugin(this.parent);
            return;
        }
        if (this.isSellMultiMenuEnabled()) {
            Objects.requireNonNull(this.parent.getCommand("sellmulti")).setExecutor((sender, cmd, lbl, args) -> {
                if (!this.isModuleActive()) {
                    sender.sendMessage("\u00a7cThis module is currently disabled.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("\u00a7cOnly players can use this command.");
                    return true;
                }
                this.getSellGui().openNew((Player)sender);
                return true;
            });
        } else {
            this.unregisterSellMultiCommand();
        }
        this.cleanupListener = new CleanupListener(this);
        this.parent.getServer().getPluginManager().registerEvents((Listener)this.cleanupListener, this.parent);
        for (Player p : this.parent.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
        }
        this.viewTracker = new ViewTracker();
        this.itemPricesMenu = new ItemPricesMenu(this);
        this.adminPriceEditorMenu = new AdminPriceEditorMenu(this);
        this.parent.getServer().getPluginManager().registerEvents((Listener)this.itemPricesMenu, this.parent);
        this.parent.getServer().getPluginManager().registerEvents((Listener)this.adminPriceEditorMenu, this.parent);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new InventoryClickListener(this), this.parent);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new ChatInputListener(this), this.parent);
        this.sellGui = new SellGui(this);
        this.progressGui = new ProgressGui(this);
        this.parent.getServer().getPluginManager().registerEvents((Listener)this, this.parent);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new SellMenuClickListener(this), this.parent);
        this.historyTracker = new HistoryTracker();
        this.sellHistoryGui = new SellHistoryGui(this);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new HistoryClickListener(this), this.parent);
        new SellHistoryCommand(this);
        new SellPlaceholderExpansion(this).register();
        new SellCommand(this);
        new WorthCommand(this);
        this.resetConfirmationGui = new ResetConfirmationGui(this);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new SellConfirmationGui(this), this.parent);
        this.parent.getServer().getPluginManager().registerEvents((Listener)new GuiClickListener(this), this.parent);
        this.sellAxeKey = new NamespacedKey(this.parent, "sell_wand");
        this.expiryKey = new NamespacedKey(this.parent, "sell_wand_expiry");
        this.usesRemainingKey = new NamespacedKey(this.parent, "sell_wand_uses_remaining");
        new SellAxeCommand(this, this.sellAxeKey, this.expiryKey, this.usesRemainingKey);
        new SellAxe(this, this.sellAxeKey, this.expiryKey, this.usesRemainingKey);
        new VaultMoneyPlaceholder(this).register();
        this.unregisterOtherSellCommands();
        new ToggleWorthCommand(this);
        boolean useCountdown = this.sellConfig.getBoolean("sell-axe.use-countdown", true);
        if (useCountdown) {
            this.runRepeatingGlobal(() -> {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.runAtPlayer(player, () -> {
                        PlayerInventory inv = player.getInventory();
                        for (int slot = 0; slot < inv.getSize(); ++slot) {
                            Long expiry;
                            PersistentDataContainer pdc;
                            Byte marker;
                            ItemMeta meta;
                            ItemStack item = inv.getItem(slot);
                            if (item == null || item.getType().isAir() || (meta = item.getItemMeta()) == null || (marker = (Byte)(pdc = meta.getPersistentDataContainer()).get(this.sellAxeKey, PersistentDataType.BYTE)) == null || marker != 1 || (expiry = (Long)pdc.get(this.expiryKey, PersistentDataType.LONG)) == null) continue;
                            if (now >= expiry) {
                                inv.setItem(slot, null);
                                player.sendMessage(Utils.formatColors("&cYour Sell Wand has expired and been removed."));
                                continue;
                            }
                            long remainingMillis = expiry - now;
                            String formatted = this.formatDuration(remainingMillis);
                            this.updateSellAxeMeta(item, meta, formatted);
                            item.setItemMeta(meta);
                        }
                    });
                }
            }, 0L, 200L);
        }
        this.setupPacketLoreHook();
        this.scheduleRestartWarningIfNeeded();
        this.parent.getLogger().info("Sell plugin enabled.");
    }

    private void setupStorage() {
        this.setupSaveFile();
        String storageType = this.sellConfig.getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT);
        if ("MYSQL".equals(storageType)) {
            this.mysql = new SellMySQL(this, SellMySQL.StorageMode.MYSQL);
            if (this.mysql.init()) {
                this.usingMySQL = true;
                this.mysql.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
                this.parent.getLogger().info("[DonutSell] MySQL connection established. Using database storage.");
                return;
            }
            this.parent.getLogger().severe("[DonutSell] MySQL connection failed. Falling back to SQLite/YAML.");
        }
        if (!"YAML".equals(storageType)) {
            this.mysql = new SellMySQL(this, SellMySQL.StorageMode.SQLITE);
            if (this.mysql.init()) {
                this.usingMySQL = true;
                this.mysql.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
                this.parent.getLogger().info("[DonutSell] SQLite initialized. Using database storage.");
                return;
            }
            this.parent.getLogger().severe("[DonutSell] SQLite connection failed. Falling back to saves.yml.");
        }
        this.loadHistory();
        this.loadToggleWorthFromSave();
        this.parent.getLogger().info("[DonutSell] Using saves.yml storage.");
    }

    @EventHandler
    public void onPluginEnableNormalize(PluginEnableEvent e) {
        if (!this.isModuleActive()) return;
        if (!e.getPlugin().getName().equals(this.parent.getName())) {
            return;
        }
        for (Player p : this.parent.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
            this.runAtPlayerLater(p, () -> ((Player)p).updateInventory(), 1L);
        }
    }

    @EventHandler
    public void onPlayerJoinStrip(PlayerJoinEvent e) {
        if (!this.isModuleActive()) return;
        Player p = e.getPlayer();
        this.cleanupListener.stripAllLore(p);
        p.updateInventory();
        this.runAtPlayerLater(p, () -> ((Player)p).updateInventory(), 1L);
        if (this.usingMySQL && this.mysql != null) {
            UUID id = p.getUniqueId();
            this.runAsync(() -> {
                SellMySQL.PlayerSnapshot snap = this.mysql.loadPlayerData(id);
                if (snap == null) {
                    return;
                }
                this.runAtPlayer(p, () -> {
                    this.totalSold.put(id, snap.total);
                    this.soldByCategory.put(id, snap.categories);
                    this.itemHistory.put(id, snap.items);
                    if (snap.toggleDisabled) {
                        this.toggleWorthDisabled.add(id);
                    } else {
                        this.toggleWorthDisabled.remove(id);
                    }
                });
            });
        }
    }

    @EventHandler
    public void onPlayerQuitStrip(PlayerQuitEvent e) {
        if (!this.isModuleActive()) return;
        Player p = e.getPlayer();
        this.cleanupListener.stripAllLore(p);
        p.updateInventory();
    }

    private void unregisterOtherSellCommands() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            SimpleCommandMap cmdMap = (SimpleCommandMap)cmdMapField.get(Bukkit.getServer());
            Field knownCmdsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCmdsField.setAccessible(true);
            Map<?, ?> known = (Map<?, ?>)knownCmdsField.get(cmdMap);
            Iterator<?> it = known.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)it.next();
                String key = (String)entry.getKey();
                Command cmd = (Command)entry.getValue();
                if (key.equalsIgnoreCase("sell") || cmd instanceof PluginCommand && ((PluginCommand)cmd).getName().equalsIgnoreCase("sell")) {
                    it.remove();
                }
                if (!key.toLowerCase(Locale.ROOT).endsWith(":sell")) continue;
                it.remove();
            }
        }
        catch (Exception e) {
            this.parent.getLogger().warning("Failed to unregister other /sell commands: " + e.getMessage());
        }
    }

    private void unregisterSellMultiCommand() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            SimpleCommandMap cmdMap = (SimpleCommandMap)cmdMapField.get(Bukkit.getServer());
            Field knownCmdsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCmdsField.setAccessible(true);
            Map<?, ?> known = (Map<?, ?>)knownCmdsField.get(cmdMap);
            Iterator<?> it = known.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)it.next();
                String key = (String)entry.getKey();
                Command cmd = (Command)entry.getValue();
                if (!key.equalsIgnoreCase("sellmulti") && !key.toLowerCase(Locale.ROOT).endsWith(":sellmulti") && (!(cmd instanceof PluginCommand) || !((PluginCommand)cmd).getName().equalsIgnoreCase("sellmulti"))) continue;
                it.remove();
            }
        }
        catch (Exception e) {
            this.parent.getLogger().warning("Failed to unregister /sellmulti command: " + e.getMessage());
        }
    }

    private void saveResource(String name, boolean replace) {
        File dest = new File(this.parent.getDataFolder(), name);
        if (dest.exists() && !replace) return;
        java.io.InputStream in = this.parent.getResource(name);
        if (in == null && !name.startsWith("sell/")) {
            in = this.parent.getResource("sell/" + name);
        }
        if (in == null) {
            this.parent.getLogger().warning("[DonutCore] Resource '" + name + "' not found in jar.");
            return;
        }
        try {
            dest.getParentFile().mkdirs();
            java.nio.file.Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            this.parent.getLogger().log(java.util.logging.Level.WARNING, "[DonutCore] Failed to save resource " + name, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
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
        this.parent.getLogger().info("Sell plugin disabled.");
    }

    public void reloadPlugin() {
        this.sellConfig = YamlConfiguration.loadConfiguration(this.sellConfigFile);
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
        this.parent.getLogger().info("Sell config reloaded.");
    }

    private void scheduleRestartWarningIfNeeded() {
        String current = this.getSellMenuOption();
        if (this.lastKnownSellMenuOption == null) {
            this.lastKnownSellMenuOption = current;
            return;
        }
        if (!this.lastKnownSellMenuOption.equalsIgnoreCase(current)) {
            if (this.restartWarningTask != null) {
                this.restartWarningTask.cancel();
            }
            this.restartWarningTask = this.runRepeatingGlobal(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    this.runAtPlayer(online, () -> {
                        if (online.hasPermission("sell.admin")) {
                            online.sendMessage(Utils.formatColors("&cWARNING: You changed sell-menu-option. Please restart your server now!"));
                        }
                    });
                }
            }, 0L, 100L);
        }
        this.lastKnownSellMenuOption = current;
    }

    private boolean setupVault() {
        if (this.parent.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = this.parent.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.econ = (Economy)rsp.getProvider();
        return this.econ != null;
    }

    private void setupSaveFile() {
        this.saveFile = new File(this.parent.getDataFolder(), "saves.yml");
        if (!this.saveFile.exists()) {
            this.saveFile.getParentFile().mkdirs();
            this.saveResource("saves.yml", false);
        }
        this.saveConfig = YamlConfiguration.loadConfiguration((File)this.saveFile);
    }

    private void setupWorthFile() {
        this.worthFile = new File(this.parent.getDataFolder(), "worth.yml");
        boolean created = false;
        if (!this.worthFile.exists()) {
            this.worthFile.getParentFile().mkdirs();
            this.saveResource("worth.yml", false);
            created = true;
        }
        this.worthConfig = YamlConfiguration.loadConfiguration((File)this.worthFile);
        if (created && this.sellConfig.isConfigurationSection("categories") && !this.worthConfig.isConfigurationSection("categories")) {
            this.worthConfig.set("categories", this.sellConfig.get("categories"));
            this.saveWorthConfig();
        }
    }

    private void setupMessagesFile() {
        this.messagesFile = new File(this.parent.getDataFolder(), "messages.yml");
        boolean created = false;
        if (!this.messagesFile.exists()) {
            this.messagesFile.getParentFile().mkdirs();
            this.saveResource("messages.yml", false);
            created = true;
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration((File)this.messagesFile);
        if (created && this.sellConfig.isConfigurationSection("messages") && !this.messagesConfig.isConfigurationSection("messages")) {
            this.messagesConfig.set("messages", this.sellConfig.getConfigurationSection("messages").getValues(true));
            this.saveYaml(this.messagesConfig, this.messagesFile, "messages.yml");
        }
    }

    private void setupMysqlFile() {
        this.mysqlFile = new File(this.parent.getDataFolder(), "mysql.yml");
        boolean created = false;
        if (!this.mysqlFile.exists()) {
            this.mysqlFile.getParentFile().mkdirs();
            this.saveResource("mysql.yml", false);
            created = true;
        }
        this.mysqlConfig = YamlConfiguration.loadConfiguration((File)this.mysqlFile);
        if (created && this.sellConfig.isConfigurationSection("mysql") && !this.mysqlConfig.isConfigurationSection("mysql")) {
            this.mysqlConfig.set("mysql", this.sellConfig.getConfigurationSection("mysql").getValues(true));
            this.saveYaml(this.mysqlConfig, this.mysqlFile, "mysql.yml");
        }
    }

    private void setupMenusFile() {
        this.menusFile = new File(this.parent.getDataFolder(), "menus.yml");
        boolean created = false;
        if (!this.menusFile.exists()) {
            this.menusFile.getParentFile().mkdirs();
            this.saveResource("menus.yml", false);
            created = true;
        }
        this.menusConfig = YamlConfiguration.loadConfiguration((File)this.menusFile);
        java.io.InputStream defStream = this.parent.getResource("sell/menus.yml");
        if (defStream != null) {
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8));
            boolean dirty = false;
            String[] sections = {"sell-menu", "new-sell-menu", "category-menu", "progress-menu", "sellhistory-menu", "item-prices-menu"};
            for (String section : sections) {
                if (!this.menusConfig.isSet(section)) {
                    this.copyMissingSection(defConfig, this.menusConfig, section);
                    dirty = true;
                }
            }
            if (dirty) {
                this.saveYaml(this.menusConfig, this.menusFile, "menus.yml");
            }
        }
    }

    public FileConfiguration getWorthConfig() {
        return this.worthConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    public FileConfiguration getMysqlConfig() {
        return this.mysqlConfig;
    }

    public FileConfiguration getMenusConfig() {
        return this.menusConfig;
    }

    public void saveWorthConfig() {
        if (this.worthFile == null || this.worthConfig == null) {
            return;
        }
        try {
            this.worthConfig.save(this.worthFile);
        }
        catch (IOException e) {
            this.parent.getLogger().log(Level.WARNING, "Failed to save worth.yml: " + e.getMessage(), e);
        }
    }

    private void reloadWorthConfig() {
        if (this.worthFile == null) {
            this.setupWorthFile();
            return;
        }
        this.worthConfig = YamlConfiguration.loadConfiguration((File)this.worthFile);
    }

    private void reloadMessagesConfig() {
        this.messagesConfig = YamlConfiguration.loadConfiguration((File)this.messagesFile);
    }

    private void reloadMysqlConfig() {
        this.mysqlConfig = YamlConfiguration.loadConfiguration((File)this.mysqlFile);
    }

    private void reloadMenusConfig() {
        this.menusConfig = YamlConfiguration.loadConfiguration((File)this.menusFile);
    }

    private void copyMissingSection(FileConfiguration src, FileConfiguration dst, String path) {
        if (!src.isSet(path) || dst.isSet(path)) {
            return;
        }
        dst.set(path, src.get(path));
    }

    private void saveYaml(FileConfiguration cfg, File file, String name) {
        try {
            cfg.save(file);
        }
        catch (IOException e) {
            this.parent.getLogger().log(Level.WARNING, "Failed to save " + name + ": " + e.getMessage(), e);
        }
    }

    private void loadHistory() {
        for (String root : this.saveConfig.getKeys(false)) {
            UUID uuid;
            if ("toggleworth-disabled".equalsIgnoreCase(root)) continue;
            try {
                uuid = UUID.fromString(root);
            }
            catch (IllegalArgumentException e) {
                continue;
            }
            this.totalSold.put(uuid, this.saveConfig.getDouble(root + ".total", 0.0));
            HashMap<String, Double> catMap = new HashMap<String, Double>();
            if (this.saveConfig.isConfigurationSection(root + ".categories")) {
                ConfigurationSection sec = this.saveConfig.getConfigurationSection(root + ".categories");
                for (String cat : sec.getKeys(false)) {
                    catMap.put(cat, sec.getDouble(cat, 0.0));
                }
            }
            this.soldByCategory.put(uuid, catMap);
            HashMap<String, Stats> itemMap = new HashMap<String, Stats>();
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
        String u;
        for (Map.Entry<UUID, Double> entry : this.totalSold.entrySet()) {
            this.saveConfig.set(String.valueOf(entry.getKey()) + ".total", entry.getValue());
        }
        for (Map.Entry<UUID, Map<String, Double>> entry : this.soldByCategory.entrySet()) {
            u = entry.getKey().toString();
            for (Map.Entry<String, Double> ec : entry.getValue().entrySet()) {
                this.saveConfig.set(u + ".categories." + ec.getKey(), ec.getValue());
            }
        }
        for (Map.Entry<UUID, Map<String, Stats>> entry : this.itemHistory.entrySet()) {
            u = entry.getKey().toString();
            for (Map.Entry<String, Stats> es : entry.getValue().entrySet()) {
                this.saveConfig.set(u + ".items." + es.getKey() + ".count", es.getValue().count);
                this.saveConfig.set(u + ".items." + es.getKey() + ".revenue", es.getValue().revenue);
            }
        }
        try {
            this.saveConfig.save(this.saveFile);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadToggleWorthFromSave() {
        this.toggleWorthDisabled.clear();
        List<String> raw = this.saveConfig.getStringList("toggleworth-disabled");
        if (raw == null) {
            return;
        }
        for (String s : raw) {
            try {
                this.toggleWorthDisabled.add(UUID.fromString(s));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
    }

    private void saveToggleWorthToSave() {
        List<String> raw = this.toggleWorthDisabled.stream().map(UUID::toString).toList();
        this.saveConfig.set("toggleworth-disabled", raw);
        try {
            this.saveConfig.save(this.saveFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isWorthEnabled(UUID id) {
        return !this.toggleWorthDisabled.contains(id);
    }

    public void setWorthEnabled(UUID id, boolean enabled) {
        if (enabled) {
            this.toggleWorthDisabled.remove(id);
        } else {
            this.toggleWorthDisabled.add(id);
        }
        if (this.usingMySQL && this.mysql != null) {
            this.mysql.setToggleWorthDisabled(id, !enabled);
        } else {
            this.saveToggleWorthToSave();
        }
    }

    private void buildCategoryItems() {
        this.itemValues.clear();
        this.categoryItems.clear();
        ConfigurationSection cats = this.getWorthConfig().getConfigurationSection("categories");
        if (cats == null) {
            return;
        }
        for (String cat : cats.getKeys(false)) {
            List<?> raw = this.getWorthConfig().getList("categories." + cat);
            ArrayList<String> mats = new ArrayList<String>();
            if (raw != null) {
                for (Object o : raw) {
                    if (!(o instanceof Map)) continue;
                    Map<?, ?> map = (Map<?, ?>)o;
                    for (Map.Entry<?, ?> me : map.entrySet()) {
                        double price;
                        String entryKey = me.getKey().toString().trim();
                        try {
                            price = Double.parseDouble(me.getValue().toString());
                        }
                        catch (NumberFormatException ex) {
                            this.parent.getLogger().warning("Invalid price for '" + entryKey + "' in category '" + cat + "'");
                            continue;
                        }
                        String lowerKey = entryKey.toLowerCase(Locale.ROOT);
                        this.itemValues.put(lowerKey, price);
                        String matName = lowerKey.replaceAll("(?i)-value$", "").toUpperCase(Locale.ROOT);
                        mats.add(matName);
                    }
                }
            }
            this.categoryItems.put(cat, mats);
        }
    }

    public double getPrice(String key) {
        return this.itemValues.getOrDefault(key.toLowerCase(Locale.ROOT), this.sellConfig.getDouble("default-value", 0.1));
    }

    public String getLookupKey(ItemStack item) {
        BlockState blockState;
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta im = item.getItemMeta();
        if (item.getType() == Material.SPAWNER && im instanceof BlockStateMeta bsm && (blockState = bsm.getBlockState()) instanceof CreatureSpawner) {
            CreatureSpawner cs = (CreatureSpawner)blockState;
            return cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
        }
        if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta esm && esm.getStoredEnchants().size() == 1) {
            Map.Entry<Enchantment, Integer> e = esm.getStoredEnchants().entrySet().iterator().next();
            return e.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(e.getValue()) + "-value";
        }
        String potionKey = this.getPotionKey(item);
        if (potionKey != null) {
            return potionKey + "-value";
        }
        return item.getType().name().toLowerCase(Locale.ROOT) + "-value";
    }

    public boolean hasListedPrice(ItemStack item) {
        String key = this.getLookupKey(item);
        return key != null && this.itemValues.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public void promptPriceEdit(Player player, ItemStack clicked) {
        if (this.adminPriceEditorMenu == null) {
            return;
        }
        if (!player.hasPermission("sell.admin")) {
            return;
        }
        String key = this.getLookupKey(clicked);
        if (key == null) {
            return;
        }
        player.closeInventory();
        player.sendMessage(Utils.formatColors("&ePlease use &f/donutsell prices &eto edit prices."));
    }

    public boolean isSellable(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (this.disabledItemsUpper.contains(item.getType().name().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return !this.sellConfig.getBoolean("missing-price-not-sellable", false) || this.hasListedPrice(item);
    }

    public String getSellMenuOption() {
        String configured = this.sellConfig.getString("sell-menu-option", null);
        if (configured == null || configured.isBlank()) {
            return this.sellConfig.getBoolean("use-new-sell-menu", false) ? "Option2" : "Option1";
        }
        String normalized = configured.trim();
        if (normalized.equalsIgnoreCase("1") || normalized.equalsIgnoreCase("option1")) {
            return "Option1";
        }
        if (normalized.equalsIgnoreCase("2") || normalized.equalsIgnoreCase("option2")) {
            return "Option2";
        }
        if (normalized.equalsIgnoreCase("3") || normalized.equalsIgnoreCase("option3")) {
            return "Option3";
        }
        this.parent.getLogger().warning("Unknown sell-menu-option '" + configured + "', falling back to Option1.");
        return "Option1";
    }

    public boolean isSellMultiMenuEnabled() {
        String option = this.getSellMenuOption();
        return option.equalsIgnoreCase("Option2") || option.equalsIgnoreCase("Option3");
    }

    public boolean isActionSellMenuEnabled() {
        return this.getSellMenuOption().equalsIgnoreCase("Option3");
    }

    public int getActionSellButtonSlot() {
        int rows = this.getMenusConfig().getInt("action-sell-menu.rows", 6);
        int max = Math.max(9, rows * 9) - 1;
        int slot = this.getMenusConfig().getInt("action-sell-menu.sell-button.slot", max);
        return Math.max(0, Math.min(max, slot));
    }

    public boolean isActionSellTitle(String title) {
        String actionTitle = Utils.formatColors(this.getMenusConfig().getString("action-sell-menu.title", ""));
        return title != null && title.equals(actionTitle);
    }

    public boolean isSellConfirmationTitle(String title) {
        String confirmTitle = Utils.formatColors(this.getMenusConfig().getString("action-sell-menu.confirmation.title", "Confirm sale"));
        return title != null && title.equals(confirmTitle);
    }

    public void markActionMenuTransfer(UUID uuid) {
        if (uuid != null) {
            this.actionMenuTransfers.add(uuid);
        }
    }

    public boolean consumeActionMenuTransfer(UUID uuid) {
        return uuid != null && this.actionMenuTransfers.remove(uuid);
    }

    public String formatSellAxeUses(int remainingUses) {
        if (remainingUses < 0) {
            return this.sellConfig.getString("sell-axe.infinite-placeholder", "\u221e");
        }
        return String.valueOf(remainingUses);
    }

    public String replaceSellAxePlaceholders(String line, String countdown, int remainingUses) {
        String formattedUses = this.formatSellAxeUses(remainingUses);
        String result = line == null ? "" : line;
        result = result.replace("%countdown%", countdown == null ? "" : countdown).replace("{countdown}", countdown == null ? "" : countdown).replace("{amount-used}", formattedUses).replace("%amount-used%", formattedUses).replace("{amount_remaining}", formattedUses).replace("%amount_remaining%", formattedUses);
        return result;
    }

    public void updateSellAxeMeta(ItemStack item, ItemMeta meta, String countdown) {
        if (item == null || meta == null) {
            return;
        }
        int remainingUses = this.getSellAxeUsesRemaining(meta);
        String rawName = this.sellConfig.getString("sell-axe.display-name", "&aSell Wand");
        meta.setDisplayName(Utils.formatColors(this.replaceSellAxePlaceholders(rawName, countdown, remainingUses)));
        ArrayList<String> newLore = new ArrayList<String>();
        for (String line : this.sellConfig.getStringList("sell-axe.lore")) {
            newLore.add(Utils.formatColors(this.replaceSellAxePlaceholders(line, countdown, remainingUses)));
        }
        meta.setLore(newLore);
    }

    public int getSellAxeUsesRemaining(ItemMeta meta) {
        if (meta == null || this.usesRemainingKey == null) {
            return this.sellConfig.getInt("sell-axe.amount", -1);
        }
        Integer value = (Integer)meta.getPersistentDataContainer().get(this.usesRemainingKey, PersistentDataType.INTEGER);
        return value == null ? this.sellConfig.getInt("sell-axe.amount", -1) : value.intValue();
    }

    public NamespacedKey getSellAxeUsesRemainingKey() {
        return this.usesRemainingKey;
    }

    public DonutOrderBridge.Result trySellToBetterOrder(Player player, ItemStack item, double normalWorth, boolean requireFullAmount) {
        String category;
        if (player == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return DonutOrderBridge.Result.empty();
        }
        if (!Double.isFinite(normalWorth) || normalWorth <= 0.0) {
            return DonutOrderBridge.Result.empty();
        }
        double multiplier = 1.0;
        if (this.isUseMultipliers() && (category = (String)this.categoryItems.entrySet().stream().filter(entry -> ((List)entry.getValue()).contains(item.getType().name())).map(Map.Entry::getKey).findFirst().orElse(null)) != null) {
            multiplier = this.getSellMultiplier(player.getUniqueId(), category);
        }
        double minimumPriceEach = normalWorth / (double)Math.max(1, item.getAmount()) * multiplier;
        return DonutOrderBridge.fillBestOrder(this, player, item, minimumPriceEach, requireFullAmount);
    }

    public double getWorthLoreValue(UUID playerId, ItemStack item, double normalUnitValue, boolean perItem) {
        if (item == null || item.getAmount() <= 0) {
            return 0.0;
        }
        ItemStack quotedStack = item.clone();
        if (perItem) {
            quotedStack.setAmount(1);
        }
        int amount = quotedStack.getAmount();
        DonutOrderBridge.Result quote = DonutOrderBridge.quoteBestOrder(this, playerId, quotedStack, normalUnitValue);
        int normalAmount = Math.max(0, amount - quote.filledAmount());
        return quote.totalPayout() + (double)normalAmount * normalUnitValue;
    }

    public SellQuote quoteSellInventory(Player player, Inventory inv, int reservedSlot) {
        if (player == null || inv == null) {
            return new SellQuote(0.0, 0L);
        }
        double total = 0.0;
        long items = 0L;
        for (int slot = 0; slot < inv.getSize(); ++slot) {
            double rawWorth;
            ItemStack item;
            if (slot == reservedSlot || !this.isSellable(item = inv.getItem(slot)) || !Double.isFinite(rawWorth = this.calculateItemWorth(item)) || rawWorth <= 0.0) continue;
            double multiplier = this.multiplierForItem(player.getUniqueId(), item);
            double normalUnitValue = rawWorth / (double)Math.max(1, item.getAmount()) * multiplier;
            total += this.getWorthLoreValue(player.getUniqueId(), item, normalUnitValue, false);
            items += (long)item.getAmount();
        }
        return new SellQuote(total, items);
    }

    public SellQuote sellActionInventory(Player player, Inventory inv, int reservedSlot) {
        double payout;
        if (player == null || inv == null) {
            return new SellQuote(0.0, 0L);
        }
        HashMap<String, Stats> sold = new HashMap<String, Stats>();
        HashMap<String, Double> revCats = new HashMap<String, Double>();
        double orderPayout = 0.0;
        long orderItemsSold = 0L;
        for (int slot = 0; slot < inv.getSize(); ++slot) {
            double normalRaw;
            double rawWorth;
            ItemStack item;
            if (slot == reservedSlot || !this.isSellable(item = inv.getItem(slot)) || !Double.isFinite(rawWorth = this.calculateItemWorth(item)) || rawWorth <= 0.0) continue;
            ItemStack remaining = item.clone();
            DonutOrderBridge.Result orderResult = this.trySellToBetterOrder(player, remaining, rawWorth, false);
            if (orderResult.wasFilled()) {
                orderPayout += orderResult.totalPayout();
                orderItemsSold += (long)orderResult.filledAmount();
                if (orderResult.filledAmount() >= remaining.getAmount()) {
                    inv.setItem(slot, null);
                    continue;
                }
                remaining.setAmount(remaining.getAmount() - orderResult.filledAmount());
            }
            if ((normalRaw = this.calculateItemWorth(remaining)) <= 0.0) {
                inv.setItem(slot, null);
                continue;
            }
            String key = remaining.getType().name().toLowerCase(Locale.ROOT);
            sold.merge(key, new Stats(remaining.getAmount(), normalRaw), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                if (!cat.getValue().contains(remaining.getType().name())) continue;
                revCats.merge(cat.getKey(), normalRaw, Double::sum);
            }
            inv.setItem(slot, null);
        }
        if (!sold.isEmpty()) {
            this.recordSale(player, sold);
        }
        if (this.isUseMultipliers()) {
            double categorized = revCats.entrySet().stream().mapToDouble(e -> (Double)e.getValue() * this.getSellMultiplier(player.getUniqueId(), (String)e.getKey())).sum();
            double uncategorized = sold.entrySet().stream().filter(e -> this.categoryItems.values().stream().noneMatch(list -> list.contains(((String)e.getKey()).toUpperCase(Locale.ROOT)))).mapToDouble(e -> ((Stats)e.getValue()).revenue).sum();
            payout = categorized + uncategorized;
        } else {
            payout = sold.values().stream().mapToDouble(s -> s.revenue).sum();
        }
        if (payout > 0.0) {
            this.getEconomy().depositPlayer((OfflinePlayer)player, payout);
        }
        long itemsSold = orderItemsSold + Math.round(sold.values().stream().mapToDouble(s -> s.count).sum());
        double totalPayout = payout + orderPayout;
        if (totalPayout > 0.0 || itemsSold > 0L) {
            Sound sound = Sound.valueOf((String)this.getMenusConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            this.notifySale(player, totalPayout, itemsSold);
        }
        return new SellQuote(totalPayout, itemsSold);
    }

    public void returnActionSellItems(Player player, Inventory inv) {
        if (player == null || inv == null) {
            return;
        }
        int reservedSlot = this.getActionSellButtonSlot();
        for (int i = 0; i < inv.getSize(); ++i) {
            ItemStack item;
            if (i == reservedSlot || (item = inv.getItem(i)) == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(d -> player.getWorld().dropItemNaturally(player.getLocation(), d));
            inv.setItem(i, null);
        }
    }

    private double multiplierForItem(UUID playerId, ItemStack item) {
        if (!this.isUseMultipliers() || item == null) {
            return 1.0;
        }
        String category = this.categoryItems.entrySet().stream().filter(entry -> entry.getValue().contains(item.getType().name())).map(Map.Entry::getKey).findFirst().orElse(null);
        return category == null ? 1.0 : this.getSellMultiplier(playerId, category);
    }

    public Map<String, Double> getItemValues() {
        return Collections.unmodifiableMap(this.itemValues);
    }

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
        HashMap<String, Double> catDelta = new HashMap<String, Double>();
        for (Map.Entry<String, List<String>> entry : this.categoryItems.entrySet()) {
            String catName = entry.getKey();
            double catSum = sold.entrySet().stream().filter(e2 -> entry.getValue().contains(e2.getKey().toUpperCase(Locale.ROOT))).mapToDouble(e2 -> e2.getValue().revenue).sum();
            if (!(catSum > 0.0)) continue;
            cmap.merge(catName, catSum, Double::sum);
            catDelta.put(catName, catSum);
        }
        if (this.usingMySQL && this.mysql != null && sum > 0.0) {
            HashMap catDeltaCopy = new HashMap(catDelta);
            HashMap soldCopy = new HashMap();
            sold.forEach((k, st) -> soldCopy.put(k, new Stats(st.count, st.revenue)));
            this.runAsync(() -> this.mysql.applySaleDelta(id, sum, catDeltaCopy, soldCopy));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!this.isModuleActive()) return;
        double payout;
        boolean isShulkerBox;
        String mat;
        ItemStack item;
        int i;
        Player p = (Player)event.getPlayer();
        String openTitle = event.getView().getTitle();
        String classicTitle = Utils.formatColors(this.getMenusConfig().getString("sell-menu.title", "&aSell Items"));
        String newTitle = Utils.formatColors(this.getMenusConfig().getString("new-sell-menu.title", "&aSell Items"));
        if (this.isActionSellTitle(openTitle)) {
            int reservedSlot;
            if (this.consumeActionMenuTransfer(p.getUniqueId())) {
                return;
            }
            Inventory inv = event.getInventory();
            SellQuote quote = this.quoteSellInventory(p, inv, reservedSlot = this.getActionSellButtonSlot());
            if (quote.payout() <= 0.0 || quote.items() <= 0L) {
                this.returnActionSellItems(p, inv);
                return;
            }
            boolean confirmEnabled = this.sellConfig.getBoolean("sale-confirmation.enabled", true);
            double threshold = this.sellConfig.getDouble("sale-confirmation.threshold", Double.MAX_VALUE);
            if (confirmEnabled && quote.payout() >= threshold) {
                this.runAtPlayerLater(p, () -> SellConfirmationGui.open(this, p, inv, quote, false), 1L);
                return;
            }
            this.sellActionInventory(p, inv, reservedSlot);
            this.returnActionSellItems(p, inv);
            return;
        }
        if (openTitle.equals(newTitle)) {
            return;
        }
        if (!openTitle.equals(classicTitle)) {
            return;
        }
        Inventory inv = event.getInventory();
        boolean useNewFlag = this.isSellMultiMenuEnabled();
        boolean excludeBottomRow = !useNewFlag && this.isUseMultipliers();
        int sellableSlots = inv.getSize() - (excludeBottomRow ? 9 : 0);
        Set<String> disabledSet = this.disabledItemsUpper;
        Sound declineSound = Sound.valueOf((String)this.sellConfig.getString("sounds.declined", "ENTITY_VILLAGER_NO"));
        String declineMsg = Utils.formatColors(this.getMessagesConfig().getString("messages.cannot-sell", "&cYou cannot sell that item!"));
        HashMap<String, Stats> sold = new HashMap<String, Stats>();
        HashMap<String, Double> revCats = new HashMap<String, Double>();
        boolean notifiedDecline = false;
        double orderPayout = 0.0;
        long orderItemsSold = 0L;
        for (i = 0; i < sellableSlots; ++i) {
            boolean missingPriceBlocked;
            item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            mat = item.getType().name();
            isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
            missingPriceBlocked = this.sellConfig.getBoolean("missing-price-not-sellable", false) && !this.hasListedPrice(item);
            if (!disabledSet.contains(mat) && !missingPriceBlocked || isShulkerBox) continue;
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
            leftover.values().forEach(d -> p.getWorld().dropItemNaturally(p.getLocation(), d));
            inv.setItem(i, null);
            if (notifiedDecline) continue;
            p.playSound(p.getLocation(), declineSound, 1.0f, 1.0f);
            p.sendMessage(declineMsg);
            notifiedDecline = true;
        }
        for (i = 0; i < sellableSlots; ++i) {
            ShulkerBox boxState;
            item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            mat = item.getType().name();
            isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
            if (this.sellConfig.getBoolean("missing-price-not-sellable", false) && !this.hasListedPrice(item)) continue;
            if (isShulkerBox && disabledSet.contains(mat)) {
                BlockState blockState;
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof BlockStateMeta blockStateMeta) || !((blockState = blockStateMeta.getBlockState()) instanceof ShulkerBox)) {
                    inv.setItem(i, null);
                    continue;
                }
                boxState = (ShulkerBox)blockState;
                Inventory shulkerInventory = boxState.getInventory();
                for (ItemStack itemStack : shulkerInventory.getContents()) {
                    if (itemStack == null || itemStack.getType().isAir() || disabledSet.contains(itemStack.getType().name())) continue;
                    ItemMeta insideMeta = itemStack.getItemMeta();
                    if (itemStack.getType() == Material.ENCHANTED_BOOK && insideMeta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta innerESM = (EnchantmentStorageMeta)insideMeta;
                        for (Map.Entry<Enchantment, Integer> eEntry : innerESM.getStoredEnchants().entrySet()) {
                            Enchantment enc = eEntry.getKey();
                            int n = eEntry.getValue();
                            String keyName = enc.getKey().getKey().toLowerCase(Locale.ROOT) + n;
                            double singlePrice = this.getPrice(keyName + "-value");
                            double totalRev = singlePrice * (double)itemStack.getAmount();
                            sold.merge(keyName, new Stats(itemStack.getAmount(), totalRev), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                                if (!cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) continue;
                                revCats.merge(cat.getKey(), totalRev, Double::sum);
                            }
                        }
                        continue;
                    }
                    String insideKey = itemStack.getType().name().toLowerCase(Locale.ROOT);
                    double insideValue = this.calculateItemWorth(itemStack);
                    sold.merge(insideKey, new Stats(itemStack.getAmount(), insideValue), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(itemStack.getType().name())) continue;
                        revCats.merge(cat.getKey(), insideValue, Double::sum);
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
                BlockState blockState;
                if (!(meta instanceof BlockStateMeta blockStateMeta) || !((blockState = blockStateMeta.getBlockState()) instanceof ShulkerBox)) continue;
                boxState = (ShulkerBox)blockState;
                String boxKey = item.getType().name().toLowerCase(Locale.ROOT);
                double boxValue = this.getPrice(boxKey + "-value") * (double)item.getAmount();
                sold.merge(boxKey, new Stats(item.getAmount(), boxValue), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                for (Map.Entry<String, List<String>> entry : this.categoryItems.entrySet()) {
                    if (!entry.getValue().contains(item.getType().name())) continue;
                    revCats.merge(entry.getKey(), boxValue, Double::sum);
                }
                Inventory boxInv2 = boxState.getInventory();
                for (ItemStack inside : boxInv2.getContents()) {
                    if (inside == null || inside.getType().isAir()) continue;
                    ItemMeta insideMeta = inside.getItemMeta();
                    if (inside.getType() == Material.ENCHANTED_BOOK && insideMeta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta innerESM = (EnchantmentStorageMeta)insideMeta;
                        for (Map.Entry<Enchantment, Integer> eEntry : innerESM.getStoredEnchants().entrySet()) {
                            Enchantment enc = eEntry.getKey();
                            int lvl = eEntry.getValue();
                            String keyName = enc.getKey().getKey().toLowerCase(Locale.ROOT) + lvl;
                            double singlePrice = this.getPrice(keyName + "-value");
                            double totalRev = singlePrice * (double)inside.getAmount();
                            sold.merge(keyName, new Stats(inside.getAmount(), totalRev), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                            for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                                if (!cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) continue;
                                revCats.merge(cat.getKey(), totalRev, Double::sum);
                            }
                        }
                        continue;
                    }
                    String insideKey = inside.getType().name().toLowerCase(Locale.ROOT);
                    double d2 = this.calculateItemWorth(inside);
                    sold.merge(insideKey, new Stats(inside.getAmount(), d2), (a, b) -> new Stats(a.count + b.count, a.revenue + b.revenue));
                    for (Map.Entry<String, List<String>> cat : this.categoryItems.entrySet()) {
                        if (!cat.getValue().contains(inside.getType().name())) continue;
                        revCats.merge(cat.getKey(), d2, Double::sum);
                    }
                }
                continue;
            }
            if (disabledSet.contains(mat)) continue;
            double orderWorth = this.calculateItemWorth(item);
            DonutOrderBridge.Result orderResult = this.trySellToBetterOrder(p, item, orderWorth, false);
            if (orderResult.wasFilled()) {
                orderPayout += orderResult.totalPayout();
                orderItemsSold += (long)orderResult.filledAmount();
                if (orderResult.filledAmount() >= item.getAmount()) {
                    inv.setItem(i, null);
                    continue;
                }
                item.setAmount(item.getAmount() - orderResult.filledAmount());
                inv.setItem(i, item);
            }
            ItemMeta im = item.getItemMeta();
            if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta)im;
                for (Map.Entry eEntry : esm.getStoredEnchants().entrySet()) {
                    Enchantment enchantment = (Enchantment)eEntry.getKey();
                    int lvl = (Integer)eEntry.getValue();
                    String keyName = enchantment.getKey().getKey().toLowerCase(Locale.ROOT) + lvl;
                    double singlePrice = this.getPrice(keyName + "-value");
                    double totalRev = singlePrice * (double)item.getAmount();
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
        if (sold.isEmpty() && orderItemsSold <= 0L) {
            return;
        }
        if (!sold.isEmpty()) {
            this.recordSale(p, sold);
        }
        if (this.isUseMultipliers()) {
            double categorized = revCats.entrySet().stream().mapToDouble(e -> (Double)e.getValue() * this.getSellMultiplier(p.getUniqueId(), (String)e.getKey())).sum();
            double uncategorized = sold.entrySet().stream().filter(e -> this.categoryItems.values().stream().noneMatch(list -> list.contains(((String)e.getKey()).toUpperCase(Locale.ROOT)))).mapToDouble(e -> ((Stats)e.getValue()).revenue).sum();
            payout = categorized + uncategorized;
        } else {
            payout = sold.values().stream().mapToDouble(s -> s.revenue).sum();
        }
        if (payout > 0.0) {
            this.getEconomy().depositPlayer((OfflinePlayer)p, payout);
        }
        Sound soundOnClose = Sound.valueOf((String)this.getMenusConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        p.playSound(p.getLocation(), soundOnClose, 1.0f, 1.0f);
        long itemsSold = orderItemsSold + Math.round(sold.values().stream().mapToDouble(s -> s.count).sum());
        this.notifySale(p, payout + orderPayout, itemsSold);
    }

    private String getPotionKey(ItemStack item) {
        Material mat;
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof PotionMeta)) {
            return null;
        }
        PotionMeta pm = (PotionMeta)itemMeta;
        String base = pm.getBasePotionData().getType().name().toLowerCase(Locale.ROOT);
        if (pm.getBasePotionData().isExtended()) {
            base = "long_" + base;
        }
        if (pm.getBasePotionData().isUpgraded()) {
            base = "strong_" + base;
        }
        if ((mat = item.getType()) == Material.SPLASH_POTION) {
            base = "splash_" + base;
        } else if (mat == Material.LINGERING_POTION) {
            base = "lingering_" + base;
        }
        return base;
    }

    public double calculateItemWorth(ItemStack item) {
        double base;
        ItemMeta im = item.getItemMeta();
        boolean missingNotSellable = this.sellConfig.getBoolean("missing-price-not-sellable", false);
        if (item.getType() == Material.SPAWNER && im instanceof BlockStateMeta) {
            BlockStateMeta bsm2 = (BlockStateMeta)im;
            BlockState blockState = bsm2.getBlockState();
            if (blockState instanceof CreatureSpawner) {
                CreatureSpawner cs = (CreatureSpawner)blockState;
                String spawned = cs.getSpawnedType().name().toLowerCase(Locale.ROOT);
                String string = spawned + "_spawner-value";
                base = this.getPrice(string);
                if (missingNotSellable && !this.itemValues.containsKey(string.toLowerCase(Locale.ROOT))) {
                    return 0.0;
                }
            } else {
                base = this.getPrice("spawner-value");
                if (missingNotSellable && !this.itemValues.containsKey("spawner-value")) {
                    return 0.0;
                }
            }
        } else {
            String potionKey = this.getPotionKey(item);
            if (potionKey != null) {
                base = this.getPrice(potionKey + "-value");
                if (missingNotSellable && !this.itemValues.containsKey((potionKey + "-value").toLowerCase(Locale.ROOT))) {
                    return 0.0;
                }
            } else {
                String baseKey = item.getType().name().toLowerCase(Locale.ROOT) + "-value";
                base = this.getPrice(baseKey);
                if (missingNotSellable && !this.itemValues.containsKey(baseKey)) {
                    return 0.0;
                }
            }
        }
        double ench = 0.0;
        if (im instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta)im;
            for (Map.Entry entry : esm.getStoredEnchants().entrySet()) {
                String k = ((Enchantment)entry.getKey()).getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(entry.getValue()) + "-value";
                ench += this.getPrice(k);
            }
        }
        if (im != null) {
            for (Map.Entry entry : im.getEnchants().entrySet()) {
                String string = ((Enchantment)entry.getKey()).getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(entry.getValue()) + "-value";
                ench += this.getPrice(string);
            }
        }
        double total = (base + ench) * (double)item.getAmount();
        if (im instanceof BlockStateMeta) {
            BlockStateMeta bsm = (BlockStateMeta) im;
            BlockState blockState = bsm.getBlockState();
            if (blockState instanceof ShulkerBox) {
                ShulkerBox box = (ShulkerBox) blockState;
                for (ItemStack inside : box.getInventory().getContents()) {
                    if (inside == null || inside.getType() == Material.AIR) continue;
                    total += this.calculateItemWorth(inside);
                }
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
            try {
                this.saveConfig.save(this.saveFile);
            }
            catch (IOException e) {
                this.parent.getLogger().severe("Could not save resets for " + String.valueOf(uuid) + ": " + e.getMessage());
            }
        }
        if (this.usingMySQL && this.mysql != null) {
            this.mysql.resetPlayerData(uuid);
        }
    }

    public double getSellMultiplier(UUID u, String cat) {
        double multiplier = 1.0;
        ConfigurationSection levels = this.getMenusConfig().getConfigurationSection("progress-menu.levels");
        if (levels == null) {
            return 1.0;
        }
        double soldInCategory = this.soldByCategory.getOrDefault(u, Collections.emptyMap()).getOrDefault(cat, 0.0);
        for (String key : levels.getKeys(false)) {
            ConfigurationSection lvlSec = levels.getConfigurationSection(key);
            if (lvlSec == null) continue;
            double needed = lvlSec.getDouble("amountNeeded", Double.MAX_VALUE);
            double multi = lvlSec.getDouble("multi", 1.0);
            if (!(soldInCategory >= needed)) continue;
            multiplier = multi;
        }
        return multiplier;
    }

    public ViewTracker getViewTracker() {
        return this.viewTracker;
    }

    public ItemPricesMenu getItemPricesMenu() {
        return this.itemPricesMenu;
    }

    public AdminPriceEditorMenu getAdminPriceEditorMenu() {
        return this.adminPriceEditorMenu;
    }

    public SellGui getSellGui() {
        return this.sellGui;
    }

    public ProgressGui getProgressGui() {
        return this.progressGui;
    }

    public Economy getEconomy() {
        return this.econ;
    }

    public HistoryTracker getHistoryTracker() {
        return this.historyTracker;
    }

    public SellHistoryGui getSellHistoryGui() {
        return this.sellHistoryGui;
    }

    public Map<String, Stats> getHistory(UUID u) {
        return this.itemHistory.getOrDefault(u, Collections.emptyMap());
    }

    public String getFormattedTotalSold(UUID u) {
        return Utils.abbreviateNumber(this.totalSold.getOrDefault(u, 0.0));
    }

    public double getRawTotalSold(UUID u, String cat) {
        return this.soldByCategory.getOrDefault(u, Collections.emptyMap()).getOrDefault(cat, 0.0);
    }

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
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    public boolean isUseMultipliers() {
        return this.sellConfig.getBoolean("use-multipliers", true);
    }

    private void rebuildDisabledItemsCache() {
        this.disabledItemsUpper.clear();
        List<String> raw = this.sellConfig.getStringList("disabled-items");
        if (raw == null) {
            return;
        }
        for (String s : raw) {
            if (s == null) continue;
            this.disabledItemsUpper.add(s.toUpperCase(Locale.ROOT));
        }
    }

    private void setupPacketLoreHook() {
        try {
            this.packetListenerInstance = new SellPacketListener(this);
            this.parent.getServer().getPluginManager().registerEvents((Listener)new WorthListener(this.packetListenerInstance), this.parent);
            this.packetListenerInstance.injectOnlinePlayers();
            this.parent.getLogger().info("[DonutSell] Worth-lore packet hook enabled.");
        }
        catch (Throwable t) {
            this.packetListenerInstance = null;
            this.parent.getLogger().log(Level.WARNING, "[DonutSell] Failed to initialize worth-lore packet hook. Worth-lore via packets disabled. Error: " + t.getMessage(), t);
        }
    }

    private void teardownPacketLoreHook() {
        if (this.packetListenerInstance == null) {
            return;
        }
        try {
            this.packetListenerInstance.shutdown();
        }
        catch (Throwable t) {
            this.parent.getLogger().log(Level.WARNING, "[DonutSell] Failed to tear down worth-lore packet hook cleanly: " + t.getMessage(), t);
        }
        finally {
            this.packetListenerInstance = null;
        }
    }

    private void reloadPacketLoreConfig() {
        if (this.packetListenerInstance == null) {
            return;
        }
        try {
            this.packetListenerInstance.reloadConfigData();
        }
        catch (Throwable t) {
            this.parent.getLogger().log(Level.WARNING, "[DonutSell] Failed to reload packet lore listener config: " + t.getMessage(), t);
        }
    }

    @FunctionalInterface
    private static interface CancellableTask {
        public void cancel();
    }



    public static class Stats {
        public double count;
        public double revenue;

        public Stats(double count, double revenue) {
            this.count = count;
            this.revenue = revenue;
        }
    }

    public static class SellQuote {
        private final double payout;
        private final long items;

        public SellQuote(double payout, long items) {
            this.payout = Math.max(0.0, payout);
            this.items = Math.max(0L, items);
        }

        public double payout() {
            return this.payout;
        }

        public long items() {
            return this.items;
        }
    }
}

