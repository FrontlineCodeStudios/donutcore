
package ro.andreilarazboi.donutcore.crates;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DataManager {
    private static final String KEY_SUFFIX = "_Key";
    private final DonutCrates plugin;
    public final Map<String, Map<String, Integer>> playerKeys = new ConcurrentHashMap<String, Map<String, Integer>>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private boolean sqliteEnabled;
    private Connection sqliteConnection;

    public DataManager(DonutCrates pl) {
        this.plugin = pl;
        this.loadSettings();
        this.loadYamlData();
        if (this.sqliteEnabled) {
            try {
                this.setupSqlite();
                this.createTablesIfMissing();
                this.mergeYamlIntoSqlite();
                this.loadSqliteSnapshotIntoCache();
                this.migrateYamlStatsIntoSqlite();
                this.plugin.getLogger().info("[DonutCrates] SQLite storage initialized.");
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "[DonutCrates] SQLite initialization failed. Key/stat persistence will be unavailable until SQLite is fixed.", e);
                this.closeSqlite();
                this.sqliteEnabled = false;
            }
        } else {
            this.plugin.getLogger().warning("[DonutCrates] SQLite storage is disabled in config. Key/stat persistence is disabled because YAML saving is no longer used.");
        }
    }

    private void loadSettings() {
        this.sqliteEnabled = this.plugin.cfg.config.getBoolean("storage.sqlite.enabled", true);
    }

    private void setupSqlite() throws SQLException {
        String path = this.plugin.cfg.config.getString("storage.sqlite.path", "donutcrates.db");
        if (path == null || path.isBlank()) {
            path = "donutcrates.db";
        }
        String jdbcUrl = path.startsWith("/") || path.contains(":") ? "jdbc:sqlite:" + path : "jdbc:sqlite:" + String.valueOf(this.plugin.getDataFolder().toPath().resolve(path));
        this.sqliteConnection = DriverManager.getConnection(jdbcUrl);
        this.sqliteConnection.setAutoCommit(true);
    }

    private void createTablesIfMissing() throws SQLException {
        try (Statement st = this.sqliteConnection.createStatement();){
            st.executeUpdate("CREATE TABLE IF NOT EXISTS donutcrate_keys (uuid TEXT NOT NULL, crate TEXT NOT NULL, amount INTEGER NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY (uuid, crate));");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS donutcrate_stats (uuid TEXT NOT NULL, crate TEXT NOT NULL, total_opened INTEGER NOT NULL, recent_rewards TEXT NOT NULL, recent_items TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY (uuid, crate));");
        }
    }

    private void loadYamlData() {
        this.playerKeys.clear();
        ConfigurationSection ps = this.plugin.cfg.saves.getConfigurationSection("playerKeys");
        if (ps == null) {
            return;
        }
        for (String uuid : ps.getKeys(false)) {
            ConfigurationSection sub = ps.getConfigurationSection(uuid);
            if (sub == null) continue;
            ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
            for (String key : sub.getKeys(false)) {
                int amount = Math.max(0, sub.getInt(key, 0));
                map.put(this.normalizeKeyName(key), amount);
            }
            if (map.isEmpty()) continue;
            this.playerKeys.put(uuid, map);
        }
    }

    private synchronized void loadSqliteSnapshotIntoCache() {
        if (this.sqliteConnection == null) {
            return;
        }
        this.playerKeys.clear();
        String sql = "SELECT uuid, crate, amount FROM donutcrate_keys";
        try (PreparedStatement ps = this.sqliteConnection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();){
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String crate = this.normalizeKeyName(rs.getString("crate"));
                int amount = Math.max(0, rs.getInt("amount"));
                this.playerKeys.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(crate, amount);
            }
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to load SQLite keys snapshot", e);
        }
    }

    private void mergeYamlIntoSqlite() {
        if (this.sqliteConnection == null) {
            return;
        }
        for (Map.Entry<String, Map<String, Integer>> playerEntry : this.playerKeys.entrySet()) {
            String uuid = playerEntry.getKey();
            for (Map.Entry<String, Integer> keyEntry : playerEntry.getValue().entrySet()) {
                this.upsertKey(uuid, this.normalizeKeyName(keyEntry.getKey()), Math.max(0, keyEntry.getValue()));
            }
        }
    }

    private void migrateYamlStatsIntoSqlite() {
        if (this.sqliteConnection == null) {
            return;
        }
        ConfigurationSection statsRoot = this.plugin.cfg.saves.getConfigurationSection("crateStats");
        if (statsRoot == null) {
            return;
        }
        for (String uuid : statsRoot.getKeys(false)) {
            ConfigurationSection perPlayer = statsRoot.getConfigurationSection(uuid);
            if (perPlayer == null) continue;
            for (String crate : perPlayer.getKeys(false)) {
                ConfigurationSection sec = perPlayer.getConfigurationSection(crate);
                if (sec == null) continue;
                int total = Math.max(0, sec.getInt("totalOpened", 0));
                List<String> rewards = sec.getStringList("recentRewards");
                ArrayList<ItemStack> items = new ArrayList<ItemStack>();
                for (Object o : sec.getList("recentRewardItems", List.of())) {
                    if (!(o instanceof ItemStack)) continue;
                    ItemStack is = (ItemStack)o;
                    items.add(is.clone());
                }
                if (total <= 0 && rewards.isEmpty() && items.isEmpty()) continue;
                this.upsertStatsRow(uuid, this.normalizeStatsCrate(crate), total, rewards, items);
            }
        }
    }

    public boolean isDirty() {
        return this.dirty.get();
    }

    public void clearDirty() {
        this.dirty.set(false);
    }

    public synchronized void saveAll() {
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            this.writeSqliteSnapshot(this.playerKeys);
        }
        this.clearDirty();
    }

    public int getKeys(Player p, String crate) {
        String uid = p.getUniqueId().toString();
        Map<String, Integer> map = this.playerKeys.computeIfAbsent(uid, ignored -> new ConcurrentHashMap<>());
        return Math.max(0, map.getOrDefault(this.normalizeKeyName(crate), 0));
    }

    public void modifyKeys(Player p, String crate, int delta) {
        String uid = p.getUniqueId().toString();
        Map<String, Integer> map = this.playerKeys.computeIfAbsent(uid, ignored -> new ConcurrentHashMap<>());
        String keyName = this.normalizeKeyName(crate);
        int newVal = Math.max(0, map.getOrDefault(keyName, 0) + delta);
        map.put(keyName, newVal);
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            this.upsertKey(uid, keyName, newVal);
        }
        this.dirty.set(true);
    }

    public synchronized void recordCrateOpen(UUID playerId, String crate, String rewardName, ItemStack rewardItem) {
        if (playerId == null || crate == null || crate.isBlank()) {
            return;
        }
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            this.recordCrateOpenSqlite(playerId, crate, rewardName, rewardItem);
        }
    }

    public synchronized int getCrateOpenCount(UUID playerId, String crate) {
        if (playerId == null || crate == null || crate.isBlank()) {
            return 0;
        }
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            StatsRow row = this.getStatsRowSqlite(playerId.toString(), this.normalizeStatsCrate(crate));
            return row == null ? 0 : Math.max(0, row.totalOpened);
        }
        return 0;
    }

    public synchronized List<String> getRecentRewards(UUID playerId, String crate, int limit) {
        if (playerId == null || crate == null || crate.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, limit);
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            StatsRow row = this.getStatsRowSqlite(playerId.toString(), this.normalizeStatsCrate(crate));
            if (row == null) {
                return List.of();
            }
            return row.recentRewards.size() <= safeLimit ? new ArrayList<String>(row.recentRewards) : new ArrayList<String>(row.recentRewards.subList(0, safeLimit));
        }
        return List.of();
    }

    public synchronized List<ItemStack> getRecentRewardItems(UUID playerId, String crate, int limit) {
        if (playerId == null || crate == null || crate.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, limit);
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            StatsRow row = this.getStatsRowSqlite(playerId.toString(), this.normalizeStatsCrate(crate));
            if (row == null) {
                return List.of();
            }
            return row.recentItems.size() <= safeLimit ? new ArrayList<ItemStack>(row.recentItems) : new ArrayList<ItemStack>(row.recentItems.subList(0, safeLimit));
        }
        return List.of();
    }

    public void resetKeysForPlayer(UUID uuid, String crate) {
        String uid = uuid.toString();
        String keyName = this.normalizeKeyName(crate);
        Map<String, Integer> map = this.playerKeys.computeIfAbsent(uid, ignored -> new ConcurrentHashMap<>());
        map.put(keyName, 0);
        if (this.sqliteEnabled && this.sqliteConnection != null) {
            this.upsertKey(uid, keyName, 0);
        }
        this.dirty.set(true);
    }

    public void resetKeysForAll(String crate) {
        String keyName = this.normalizeKeyName(crate);
        for (Map.Entry<String, Map<String, Integer>> entry : this.playerKeys.entrySet()) {
            entry.getValue().put(keyName, 0);
            if (!this.sqliteEnabled || this.sqliteConnection == null) continue;
            this.upsertKey(entry.getKey(), keyName, 0);
        }
        this.dirty.set(true);
    }

    private void writeSqliteSnapshot(Map<String, ? extends Map<String, Integer>> snapshot) {
        for (Map.Entry<String, ? extends Map<String, Integer>> player : snapshot.entrySet()) {
            for (Map.Entry<String, Integer> key : player.getValue().entrySet()) {
                this.upsertKey(player.getKey(), this.normalizeKeyName(key.getKey()), Math.max(0, key.getValue()));
            }
        }
    }

    private void upsertKey(String uuid, String crate, int amount) {
        if (this.sqliteConnection == null) {
            return;
        }
        String sql = "INSERT INTO donutcrate_keys (uuid, crate, amount, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(uuid, crate) DO UPDATE SET amount=excluded.amount, updated_at=excluded.updated_at";
        try (PreparedStatement ps = this.sqliteConnection.prepareStatement(sql);){
            ps.setString(1, uuid);
            ps.setString(2, this.normalizeKeyName(crate));
            ps.setInt(3, Math.max(0, amount));
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to upsert SQLite key", e);
        }
    }

    private void recordCrateOpenSqlite(UUID playerId, String crate, String rewardName, ItemStack rewardItem) {
        String crateKey;
        String uid = playerId.toString();
        StatsRow existing = this.getStatsRowSqlite(uid, crateKey = this.normalizeStatsCrate(crate));
        int total = existing == null ? 0 : existing.totalOpened;
        ArrayList<String> rewards = existing == null ? new ArrayList<String>() : new ArrayList<String>(existing.recentRewards);
        ArrayList<ItemStack> items = existing == null ? new ArrayList<ItemStack>() : new ArrayList<ItemStack>(existing.recentItems);
        rewards.add(0, rewardName == null || rewardName.isBlank() ? "Unknown Reward" : rewardName);
        items.add(0, rewardItem == null ? new ItemStack(Material.PAPER) : rewardItem.clone());
        while (rewards.size() > 45) {
            rewards.remove(rewards.size() - 1);
        }
        while (items.size() > 45) {
            items.remove(items.size() - 1);
        }
        this.upsertStatsRow(uid, crateKey, total + 1, rewards, items);
    }

    private StatsRow getStatsRowSqlite(String uuid, String crate) {
        if (this.sqliteConnection == null) {
            return null;
        }
        String sql = "SELECT total_opened, recent_rewards, recent_items FROM donutcrate_stats WHERE uuid=? AND crate=?";
        try (PreparedStatement ps = this.sqliteConnection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, crate);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int total = Math.max(0, rs.getInt("total_opened"));
                List<String> rewards = this.decodeStringList(rs.getString("recent_rewards"));
                List<ItemStack> items = this.decodeItemList(rs.getString("recent_items"));
                return new StatsRow(total, rewards, items);
            }
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to read SQLite stats row", e);
            return null;
        }
    }

    private void upsertStatsRow(String uuid, String crate, int totalOpened, List<String> rewards, List<ItemStack> items) {
        if (this.sqliteConnection == null) {
            return;
        }
        String sql = "INSERT INTO donutcrate_stats (uuid, crate, total_opened, recent_rewards, recent_items, updated_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid, crate) DO UPDATE SET total_opened=excluded.total_opened, recent_rewards=excluded.recent_rewards, recent_items=excluded.recent_items, updated_at=excluded.updated_at";
        try (PreparedStatement ps = this.sqliteConnection.prepareStatement(sql);){
            ps.setString(1, uuid);
            ps.setString(2, crate);
            ps.setInt(3, Math.max(0, totalOpened));
            ps.setString(4, this.encodeStringList(rewards));
            ps.setString(5, this.encodeItemList(items));
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to write SQLite stats row", e);
        }
    }

    private String encodeStringList(List<String> input) {
        return String.join((CharSequence)"\n", input == null ? List.of() : input);
    }

    private List<String> decodeStringList(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(List.of(encoded.split("\\n")));
    }

    private String encodeItemList(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        ArrayList<String> out = new ArrayList<String>();
        for (ItemStack item : items) {
            try {
                out.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to serialize reward item", e);
            }
        }
        return String.join("\n", out);
    }

    private List<ItemStack> decodeItemList(String encoded) {
        ArrayList<ItemStack> out = new ArrayList<ItemStack>();
        if (encoded == null || encoded.isBlank()) {
            return out;
        }
        for (String line : encoded.split("\\n")) {
            if (line.isBlank()) continue;
            try {
                out.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(line)));
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "[DonutCrates] Failed to deserialize reward item", e);
            }
        }
        return out;
    }

    private String normalizeKeyName(String crateOrKeyName) {
        if (crateOrKeyName.endsWith(KEY_SUFFIX)) {
            return crateOrKeyName;
        }
        return crateOrKeyName + KEY_SUFFIX;
    }

    private String normalizeStatsCrate(String crate) {
        return crate.replace(".", "_");
    }

    public void shutdown() {
        this.saveAll();
        this.closeSqlite();
    }

    private void closeSqlite() {
        if (this.sqliteConnection != null) {
            try {
                this.sqliteConnection.close();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            this.sqliteConnection = null;
        }
    }

    private record StatsRow(int totalOpened, List<String> recentRewards, List<ItemStack> recentItems) {
    }
}

