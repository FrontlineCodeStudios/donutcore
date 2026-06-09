
package ro.andreilarazboi.donutcore.crates;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ro.andreilarazboi.donutcore.crates.gui.ConfirmGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateDisplayNameGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateEditorGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateNameColorGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateRewardsGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateSettingsGUI;
import ro.andreilarazboi.donutcore.crates.gui.CrateStatsGUI;
import ro.andreilarazboi.donutcore.crates.gui.CreateModeGUI;
import ro.andreilarazboi.donutcore.crates.gui.DeleteConfirmGUI;
import ro.andreilarazboi.donutcore.crates.gui.EditCrateGUI;
import ro.andreilarazboi.donutcore.crates.gui.HologramBackgroundGUI;
import ro.andreilarazboi.donutcore.crates.gui.HologramGUI;
import ro.andreilarazboi.donutcore.crates.gui.HologramTemplateGUI;
import ro.andreilarazboi.donutcore.crates.gui.ItemEditorGUI;
import ro.andreilarazboi.donutcore.crates.gui.KeyEditorGUI;
import ro.andreilarazboi.donutcore.crates.gui.KeyListGUI;
import ro.andreilarazboi.donutcore.crates.gui.KeySelectGUI;
import ro.andreilarazboi.donutcore.crates.gui.MainEditorGUI;
import ro.andreilarazboi.donutcore.crates.gui.OpeningAnimationsGUI;
import ro.andreilarazboi.donutcore.crates.gui.RootEditorGUI;
import ro.andreilarazboi.donutcore.crates.gui.RowEditorGUI;
import ro.andreilarazboi.donutcore.crates.listener.ConfirmGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateDisplayNameGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateEditGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateManagerGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateNameColorGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateOpenService;
import ro.andreilarazboi.donutcore.crates.listener.CrateRewardsGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateSettingsGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CrateStatsGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.CreateModeGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.DeleteConfirmGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.HologramBackgroundGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.HologramGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.HologramTemplatesGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.InventoryProtectionListener;
import ro.andreilarazboi.donutcore.crates.listener.ItemEditorGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.KeyEditorGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.KeyManagerGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.KeySelectGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.KeyUtil;
import ro.andreilarazboi.donutcore.crates.listener.OpeningAnimationGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.OpeningAnimationsGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.PlayerCrateGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.RootEditorGuiListener;
import ro.andreilarazboi.donutcore.crates.listener.RowEditorGuiListener;

public final class DonutCrates {
    public static DonutCrates instance;
    private final JavaPlugin parent;
    public ConfigManager cfg;
    public CrateManager crateMgr;
    public DataManager dataMgr;
    public HologramManager holoMgr;
    public GUIItemUtil guiItemUtil;
    public CrateGUI guiCrate;
    public ConfirmGUI guiConfirm;
    public DeleteConfirmGUI guiDeleteConfirm;
    public RootEditorGUI guiRootEditor;
    public MainEditorGUI guiMainEditor;
    public CrateEditorGUI guiCrateEditor;
    public CrateSettingsGUI guiCrateSettings;
    public CreateModeGUI guiCreateMode;
    public EditCrateGUI guiCrateEdit;
    public CrateRewardsGUI guiCrateRewards;
    public RowEditorGUI guiRowEditor;
    public ItemEditorGUI guiItemEditor;
    public HologramGUI guiHologram;
    public HologramTemplateGUI guiHoloTemplates;
    public HologramBackgroundGUI guiHoloBackground;
    public KeyListGUI guiKeyList;
    public KeyEditorGUI guiKeyEditor;
    public KeySelectGUI guiKeySelect;
    public OpeningAnimationsGUI guiOpeningAnimations;
    public CrateDisplayNameGUI guiCrateDisplayName;
    public CrateNameColorGUI guiCrateNameColor;
    public CrateStatsGUI guiCrateStats;
    public final Map<UUID, String> pendingCrate = new HashMap<UUID, String>();
    public final Map<UUID, Integer> pendingSlot = new HashMap<UUID, Integer>();
    public final Map<UUID, String> pendingEditorCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingEditorItemKey = new HashMap<UUID, String>();
    public final Map<UUID, Boolean> pendingEditorIsLore = new HashMap<UUID, Boolean>();
    public final Map<UUID, Boolean> pendingEditorIsRename = new HashMap<UUID, Boolean>();
    public final Map<UUID, Boolean> pendingEditorIsChance = new HashMap<UUID, Boolean>();
    public final Set<UUID> pendingCreate = new HashSet<UUID>();
    public final Map<UUID, Boolean> pendingCreateRandom = new HashMap<UUID, Boolean>();
    public final Map<UUID, String> pendingMoveCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingRenameCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingHoloAddLine = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingCopyCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingKeyRename = new HashMap<UUID, String>();
    public final Set<UUID> pendingHoloTimerEdit = new HashSet<UUID>();
    public final Map<UUID, String> pendingDeleteType = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingDeleteCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingDeleteItemKey = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingDeleteKeyId = new HashMap<UUID, String>();
    public final Map<UUID, String> previewReturnCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingDisplayNameCrate = new HashMap<UUID, String>();
    public final Map<UUID, String> pendingDisplayNameColor = new HashMap<UUID, String>();
    private BukkitTask holoTask;
    public boolean hasPapi;
    private String prefix;
    private final Set<String> ensuredKeyConfigs = new HashSet<String>();

    public DonutCrates(JavaPlugin parent) {
        this.parent = parent;
    }

    public JavaPlugin getPlugin() {
        return parent;
    }

    public Server getServer() {
        return parent.getServer();
    }

    public Logger getLogger() {
        return parent.getLogger();
    }

    public File getDataFolder() {
        return parent.getDataFolder();
    }

    public InputStream getResource(String filename) {
        return parent.getResource(filename);
    }

    public void enable() {
        instance = this;
        this.cfg = new ConfigManager(this);
        this.crateMgr = new CrateManager(this);
        this.dataMgr = new DataManager(this);
        this.holoMgr = new HologramManager(this);
        this.reloadPrefix();
        this.guiItemUtil = new GUIItemUtil(this);
        this.guiCrate = new CrateGUI(this);
        this.guiConfirm = new ConfirmGUI(this);
        this.guiDeleteConfirm = new DeleteConfirmGUI(this);
        this.guiRootEditor = new RootEditorGUI(this);
        this.guiMainEditor = new MainEditorGUI(this);
        this.guiCrateEditor = new CrateEditorGUI(this);
        this.guiCrateSettings = new CrateSettingsGUI(this);
        this.guiCreateMode = new CreateModeGUI(this);
        this.guiCrateEdit = new EditCrateGUI(this);
        this.guiCrateRewards = new CrateRewardsGUI(this);
        this.guiRowEditor = new RowEditorGUI(this);
        this.guiItemEditor = new ItemEditorGUI(this);
        this.guiHologram = new HologramGUI(this);
        this.guiHoloTemplates = new HologramTemplateGUI(this);
        this.guiHoloBackground = new HologramBackgroundGUI(this);
        this.guiKeyList = new KeyListGUI(this);
        this.guiKeyEditor = new KeyEditorGUI(this);
        this.guiKeySelect = new KeySelectGUI(this);
        this.guiOpeningAnimations = new OpeningAnimationsGUI(this);
        this.guiCrateDisplayName = new CrateDisplayNameGUI(this);
        this.guiCrateNameColor = new CrateNameColorGUI(this);
        this.guiCrateStats = new CrateStatsGUI(this);
        this.hasPapi = parent.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (this.hasPapi) {
            new DonutCratesPlaceholder().register();
        }
        DonutCrateCommand executor = new DonutCrateCommand(this);
        Objects.requireNonNull(parent.getCommand("donutcrate")).setExecutor((CommandExecutor)executor);
        Objects.requireNonNull(parent.getCommand("donutcrate")).setTabCompleter((TabCompleter)executor);
        parent.getServer().getPluginManager().registerEvents((Listener)new BlockListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new InventoryCloseListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new ChatInputListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new HologramListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new Utils.SignInputListener(), (Plugin)parent);
        KeyUtil keyUtil = new KeyUtil(this);
        CrateOpenService openService = new CrateOpenService(this, keyUtil);
        parent.getServer().getPluginManager().registerEvents((Listener)new InventoryProtectionListener(openService), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new OpeningAnimationGuiListener(), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new PlayerCrateGuiListener(openService), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new ConfirmGuiListener(this, openService), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new RootEditorGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateManagerGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CreateModeGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateSettingsGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new OpeningAnimationsGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new KeySelectGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateEditGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateRewardsGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new RowEditorGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new HologramGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new HologramTemplatesGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new HologramBackgroundGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new ItemEditorGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new DeleteConfirmGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new KeyManagerGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new KeyEditorGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateDisplayNameGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateNameColorGuiListener(this), (Plugin)parent);
        parent.getServer().getPluginManager().registerEvents((Listener)new CrateStatsGuiListener(this), (Plugin)parent);
        parent.getServer().getScheduler().runTask((Plugin)parent, () -> {
            try {
                this.holoMgr.startupVacuum();
            }
            catch (Throwable t) {
                parent.getLogger().warning("[DonutCrates] Startup hologram vacuum failed: " + t.getMessage());
            }
        });
        parent.getServer().getScheduler().runTaskLater((Plugin)parent, () -> {
            for (String crate : this.crateMgr.crateBlocks.keySet()) {
                this.holoMgr.refreshCrate(crate);
            }
        }, 2L);
        this.startHologramTask();
    }

    public void disable() {
        if (this.holoTask != null) {
            this.holoTask.cancel();
        }
        if (this.holoMgr != null) {
            this.holoMgr.despawnAll();
        }
        if (this.dataMgr != null) {
            this.dataMgr.shutdown();
        }
        if (this.cfg != null) {
            this.cfg.saveAll();
        }
    }

    private void startHologramTask() {
        if (this.holoTask != null) {
            this.holoTask.cancel();
        }
        int seconds = Math.max(1, this.cfg.config.getInt("hologram-update-seconds", this.cfg.config.getInt("hologram-refresh-seconds", 5)));
        long interval = (long)seconds * 20L;
        this.holoTask = new BukkitRunnable(){
            public void run() {
                for (String crate : DonutCrates.this.crateMgr.crateBlocks.keySet()) {
                    ConfigurationSection h = DonutCrates.this.cfg.crates.getConfigurationSection("Crates." + crate + ".Hologram");
                    if (h == null || !h.getBoolean("enabled", false)) continue;
                    DonutCrates.this.holoMgr.refreshCrate(crate);
                }
            }
        }.runTaskTimer((Plugin)parent, interval, interval);
    }

    public void restartHologramTask() {
        this.startHologramTask();
    }

    public String getPrefix() {
        return this.prefix != null ? this.prefix : "";
    }

    public void reloadPrefix() {
        String raw = this.cfg != null && this.cfg.config != null ? this.cfg.config.getString("prefix", "&#0fe30f[DonutCrates]&r ") : "&#0fe30f[DonutCrates]&r ";
        this.prefix = Utils.formatColors(raw);
    }

    public void msg(CommandSender sender, String raw) {
        if (sender == null || raw == null) {
            return;
        }
        String colored = Utils.formatColors(raw);
        String full = this.getPrefix() + colored;
        sender.sendMessage(Utils.toComponent(full));
        if (sender instanceof Player) {
            Player p = (Player)sender;
            boolean useActionbar = this.cfg != null && this.cfg.config != null && this.cfg.config.getBoolean("messages.actionbar-enabled", false);
            if (useActionbar) {
                p.sendActionBar(Utils.toComponent(full));
            }
        }
    }

    public void msg(Player player, String raw) {
        this.msg((CommandSender)player, raw);
    }

    public String getCrateDisplayNameRaw(String crateId) {
        if (crateId == null || crateId.isBlank()) {
            return "&7Unknown";
        }
        String raw = this.cfg.crates.getString("Crates." + crateId + ".displayname", null);
        if (raw == null || raw.isBlank()) {
            return "&7" + crateId;
        }
        return raw;
    }

    public String getCrateDisplayNameFormatted(String crateId) {
        return Utils.formatColors(this.getCrateDisplayNameRaw(crateId));
    }

    public String getCrateDisplayNamePlain(String crateId) {
        String raw = this.getCrateDisplayNameRaw(crateId);
        return Utils.stripColor(Utils.formatColors(raw));
    }

    public void setCrateDisplayName(String crateId, String rawDisplayName) {
        if (crateId == null || crateId.isBlank()) {
            return;
        }
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            this.cfg.crates.set("Crates." + crateId + ".displayname", null);
        } else {
            this.cfg.crates.set("Crates." + crateId + ".displayname", (Object)rawDisplayName);
        }
        this.cfg.saveAll();
        try {
            this.holoMgr.refreshCrate(crateId);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public String getKeyIdForCrate(String crate) {
        if (crate == null || crate.isEmpty()) {
            return crate;
        }
        return this.cfg.crates.getString("Crates." + crate + ".key", crate);
    }

    public boolean keyExists(String keyId) {
        if (keyId == null || keyId.isEmpty()) {
            return false;
        }
        return this.cfg.saves.isConfigurationSection("keys." + keyId);
    }

    public int countCratesUsingKey(String keyId) {
        if (keyId == null) {
            return 0;
        }
        ConfigurationSection cratesRoot = this.cfg.crates.getConfigurationSection("Crates");
        if (cratesRoot == null) {
            return 0;
        }
        int count = 0;
        for (String crate : cratesRoot.getKeys(false)) {
            String sel = this.getKeyIdForCrate(crate);
            if (sel == null || !sel.equalsIgnoreCase(keyId)) continue;
            ++count;
        }
        return count;
    }

    public void ensureKeyConfig(String keyId) {
        if (keyId == null || keyId.isEmpty()) {
            return;
        }
        if (this.ensuredKeyConfigs.contains(keyId)) {
            return;
        }
        boolean changed = false;
        String savesPath = "keys." + keyId;
        ConfigurationSection sSec = this.cfg.saves.getConfigurationSection(savesPath);
        if (sSec == null) {
            sSec = this.cfg.saves.createSection(savesPath);
            changed = true;
        }
        if (!sSec.isSet("displayname")) {
            sSec.set("displayname", ("&#0fe30f" + keyId + " Key"));
            changed = true;
        }
        if (!sSec.isSet("virtual")) {
            sSec.set("virtual", true);
            changed = true;
        }
        if (!sSec.isSet("material")) {
            sSec.set("material", "TRIPWIRE_HOOK");
            changed = true;
        }
        if (changed) {
            this.cfg.saveAll();
        }
        this.ensuredKeyConfigs.add(keyId);
    }

    public ItemStack buildKeyItemById(String keyId, int amount) {
        ItemStack keyItem;
        this.ensureKeyConfig(keyId);
        String base = "keys." + keyId;
        if (this.cfg.saves.isItemStack(base + ".item")) {
            keyItem = Objects.requireNonNull(this.cfg.saves.getItemStack(base + ".item")).clone();
        } else {
            Material mat;
            String matName = this.cfg.saves.getString(base + ".material", "TRIPWIRE_HOOK");
            try {
                mat = Material.valueOf((String)matName);
            }
            catch (Exception ex) {
                mat = Material.TRIPWIRE_HOOK;
            }
            keyItem = new ItemStack(mat);
        }
        keyItem.setAmount(Math.max(1, amount));
        ItemMeta meta = keyItem.getItemMeta();
        if (meta != null) {
            if (!meta.hasDisplayName()) {
                meta.displayName(Utils.toComponent(this.cfg.saves.getString(base + ".displayname", "&#0fe30f" + keyId + " Key")));
            }
            if (meta.hasEnchants()) {
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }
            NamespacedKey tag = new NamespacedKey((Plugin)parent, "crate_key");
            meta.getPersistentDataContainer().set(tag, PersistentDataType.STRING, keyId);
            keyItem.setItemMeta(meta);
        }
        return keyItem;
    }

    public ItemStack buildKeyItem(String crate, int amount) {
        String keyId = this.getKeyIdForCrate(crate);
        return this.buildKeyItemById(keyId, amount);
    }

    public String getRewardDisplayNameForChat(ItemStack item) {
        String nice;
        if (item == null || item.getType().isAir()) {
            return Utils.formatColors("&7Unknown");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                if (meta.hasDisplayName()) {
                    Component dn = meta.displayName();
                    if (dn != null) {
                        String legacy = LegacyComponentSerializer.legacySection().serialize(dn);
                        if (!legacy.isEmpty()) {
                            return legacy;
                        }
                    }
                }
            }
            catch (Throwable t) {
                // empty catch block
            }
        }
        String titled = (nice = item.getType().name().toLowerCase(Locale.ROOT).replace("_", " ")).isEmpty() ? "Item" : Character.toUpperCase(nice.charAt(0)) + nice.substring(1);
        return Utils.formatColors("&f" + titled);
    }
}
