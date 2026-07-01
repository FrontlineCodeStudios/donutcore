package ro.andreilarazboi.donutcore.hide;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ro.andreilarazboi.donutcore.DonutCore;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central manager for the Hide module.
 *
 * <h3>VIP Hide</h3>
 * <ul>
 * <li>Adds the player to a Scoreboard team with
 * {@code NAME_TAG_VISIBILITY = NEVER}
 * → no floating name tag above head.</li>
 * <li>Sets an empty tab-list name ({@code Component.empty()}).</li>
 * <li>Applies a <em>random fake skin</em> from the config list
 * asynchronously.</li>
 * </ul>
 *
 * <h3>Staff Hide (camouflage)</h3>
 * <ul>
 * <li>Same name-tag hiding as VIP hide.</li>
 * <li>Sets a custom fake name in the tab list.</li>
 * <li>Applies the skin of any Minecraft player by name or UUID (async Mojang
 * API).</li>
 * </ul>
 *
 * <p>
 * <strong>Priority design:</strong> the original {@link PlayerProfile} (skin)
 * is saved
 * <em>before</em> any change and fully restored on toggle-off or disconnect.
 * </p>
 */
public class HideManager {

    // -------------------------------------------------------------------------
    // Inner records
    // -------------------------------------------------------------------------

    record StaffHideData(String fakeName, PlayerProfile originalProfile) {
    }

    record SkinData(String value, String signature) {
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final DonutCore plugin;

    /** Players with active VIP hide. */
    private final Set<UUID> vipHidden = new HashSet<>();
    /** Original profiles of VIP-hidden players (needed to restore skin). */
    private final Map<UUID, PlayerProfile> vipOriginalProfiles = new HashMap<>();
    /** Players with active staff camouflage. */
    private final Map<UUID, StaffHideData> staffHidden = new HashMap<>();
    /**
     * Staff members (with donutcore.hide.staff) added to the scoreboard team
     * purely as "observers" so they can see hidden players' name tags.
     * Tracked separately so we don't accidentally remove them when unhiding a
     * player.
     */
    private final Set<UUID> staffObservers = new HashSet<>();

    /** Scoreboard team that suppresses the floating name tag. */
    private Team hideTeam;

    // -------------------------------------------------------------------------

    public HideManager(DonutCore plugin) {
        this.plugin = plugin;
        setupScoreboardTeam();
    }

    // -------------------------------------------------------------------------
    // Scoreboard team lifecycle
    // -------------------------------------------------------------------------

    private void setupScoreboardTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team existing = board.getTeam("donuthide");
        if (existing != null)
            existing.unregister();

        hideTeam = board.registerNewTeam("donuthide");
        // FOR_OWN_TEAM: name tags are visible ONLY to members of this team.
        // Staff observers are added to the team, so they can see hidden players.
        // Normal players are NOT in the team, so hidden players are invisible to them.
        hideTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
        hideTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    /** Unregisters the scoreboard team — call on module disable. */
    public void cleanup() {
        if (hideTeam != null) {
            try {
                hideTeam.unregister();
            } catch (Exception ignored) {
            }
            hideTeam = null;
        }
        staffObservers.clear();
    }

    void addToHideTeam(Player player) {
        if (hideTeam != null)
            hideTeam.addEntry(player.getName());
    }

    /**
     * Removes a player from the hide team UNLESS they are a staff observer
     * (staff observers must stay in the team to keep seeing hidden players).
     */
    void removeFromHideTeam(Player player) {
        if (hideTeam == null)
            return;
        if (staffObservers.contains(player.getUniqueId()))
            return; // keep staff observers in team
        hideTeam.removeEntry(player.getName());
    }

    /**
     * Adds a staff member to the scoreboard team as an observer so they can
     * see hidden players' floating name tags. Safe to call multiple times.
     */
    public void addStaffObserver(Player player) {
        if (!player.hasPermission("donutcore.hide.staff"))
            return;
        staffObservers.add(player.getUniqueId());
        addToHideTeam(player);
    }

    /**
     * Removes a staff member from the observer role.
     * Should be called on PlayerQuitEvent.
     */
    public void removeStaffObserver(Player player) {
        staffObservers.remove(player.getUniqueId());
        // Only actually remove from scoreboard team if they are not currently hidden
        if (!vipHidden.contains(player.getUniqueId()) &&
                !staffHidden.containsKey(player.getUniqueId())) {
            if (hideTeam != null)
                hideTeam.removeEntry(player.getName());
        }
    }

    // -------------------------------------------------------------------------
    // VIP / Streamer hide
    // -------------------------------------------------------------------------

    /**
     * Toggles VIP hide for the player.
     *
     * @return {@code true} if the player is now hidden, {@code false} if restored.
     */
    public boolean toggleVipHide(Player player) {
        if (vipHidden.contains(player.getUniqueId())) {
            vipShowSilent(player);
            return false;
        }
        applyVipHide(player);
        return true;
    }

    private void applyVipHide(Player player) {
        if (staffHidden.containsKey(player.getUniqueId()))
            return; // already staff-hidden

        vipHidden.add(player.getUniqueId());
        addToHideTeam(player);
        player.playerListName(net.kyori.adventure.text.Component.empty()); // clears the display name, tab shows blank

        // Apply a random fake skin from config (async)
        List<String> fakeSkins = plugin.getConfig().getStringList("hide.vip-fake-skins");
        if (!fakeSkins.isEmpty()) {
            String randomSkin = fakeSkins.get(ThreadLocalRandom.current().nextInt(fakeSkins.size()));
            // Save original profile BEFORE applying the fake skin
            vipOriginalProfiles.put(player.getUniqueId(), player.getPlayerProfile());
            applySkinAsync(player, randomSkin, null); // null = VIP mode: keep player's own UUID
        }
    }

    /**
     * Restores the player's VIP hide state without sending a chat message.
     * Used on disconnect or internal toggle-off.
     */
    public void vipShowSilent(Player player) {
        vipHidden.remove(player.getUniqueId());
        removeFromHideTeam(player);
        player.playerListName(null); // restore original tab name (null resets to default)

        PlayerProfile original = vipOriginalProfiles.remove(player.getUniqueId());
        if (original != null && player.isOnline()) {
            player.setPlayerProfile(original);
        }
    }

    public boolean isVipHidden(UUID uuid) {
        return vipHidden.contains(uuid);
    }

    // -------------------------------------------------------------------------
    // Staff hide (camouflage)
    // -------------------------------------------------------------------------

    /**
     * Activates staff camouflage: hides the real name tag, sets a fake tab name,
     * and optionally applies a skin fetched from Mojang.
     *
     * @param fakeName       the name to display in the tab list
     * @param skinNameOrUUID Minecraft username or UUID string for the skin;
     *                       {@code null} to skip
     */
    public void staffHide(Player player, String fakeName, String skinNameOrUUID) {
        // Deactivate VIP hide first if stacked
        if (vipHidden.contains(player.getUniqueId())) {
            vipShowSilent(player);
        }

        // Save original profile BEFORE any modification
        PlayerProfile originalProfile = player.getPlayerProfile();
        staffHidden.put(player.getUniqueId(), new StaffHideData(fakeName, originalProfile));

        addToHideTeam(player);
        player.playerListName(
                net.kyori.adventure.text.Component.text(fakeName, net.kyori.adventure.text.format.NamedTextColor.GRAY));

        if (skinNameOrUUID != null && !skinNameOrUUID.isBlank()) {
            applySkinAsync(player, skinNameOrUUID, fakeName); // non-null fakeName = staff mode
        }
    }

    /**
     * Restores the player's original identity after staff hide, without sending a
     * message.
     * Used on disconnect or explicit deactivation.
     */
    public void staffShowSilent(Player player) {
        StaffHideData data = staffHidden.remove(player.getUniqueId());
        if (data == null)
            return;
        removeFromHideTeam(player);
        player.playerListName(null); // reset to default tab name
        if (data.originalProfile() != null) {
            player.setPlayerProfile(data.originalProfile());
        }
    }

    public boolean isStaffHidden(UUID uuid) {
        return staffHidden.containsKey(uuid);
    }

    public StaffHideData getStaffHideData(UUID uuid) {
        return staffHidden.get(uuid);
    }

    // -------------------------------------------------------------------------
    // Async skin fetching — Mojang API
    // -------------------------------------------------------------------------

    /**
     * Fetches a skin asynchronously and applies it to the player.
     *
     * @param skinNameOrUUID  name or UUID of the target skin owner
     * @param fakeProfileName if {@code null} → VIP mode: keep player's own UUID
     *                        &amp; name,
     *                        just swap the texture. If non-null → staff mode: use
     *                        skin
     *                        owner's UUID with the fake name.
     */
    void applySkinAsync(Player player, String skinNameOrUUID, String fakeProfileName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID targetUUID = resolveUUID(skinNameOrUUID);
                if (targetUUID == null) {
                    plugin.getLogger().warning("[Hide] Could not resolve UUID for skin: " + skinNameOrUUID);
                    sendSync(player, "§cCould not find player: §e" + skinNameOrUUID + "§c — skin not applied.");
                    return;
                }

                SkinData skin = fetchSkinData(targetUUID);
                if (skin == null) {
                    plugin.getLogger().warning("[Hide] Could not fetch skin for UUID: " + targetUUID);
                    sendSync(player, "§cCould not fetch skin data for §e" + skinNameOrUUID + "§c.");
                    return;
                }

                // Apply on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline())
                        return;

                    PlayerProfile fakeProfile;
                    if (fakeProfileName == null) {
                        // VIP mode: keep player's UUID + name, replace only the skin texture
                        fakeProfile = Bukkit.createProfile(player.getUniqueId(), player.getName());
                    } else {
                        // Staff mode: use skin owner's UUID with the fake name
                        fakeProfile = Bukkit.createProfile(targetUUID, fakeProfileName);
                    }
                    fakeProfile.setProperty(new ProfileProperty("textures", skin.value(), skin.signature()));
                    player.setPlayerProfile(fakeProfile);

                    sendSync(player, "§aSkin applied! §7(" + skinNameOrUUID + ")");
                });

            } catch (Exception e) {
                plugin.getLogger().warning("[Hide] Skin fetch error (" + skinNameOrUUID + "): " + e.getMessage());
                sendSync(player, "§cFailed to fetch skin: §e" + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Mojang API helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a Minecraft player name or UUID string to a {@link UUID}.
     * Returns {@code null} if the player could not be found.
     */
    private UUID resolveUUID(String nameOrUUID) throws Exception {
        if (isUUIDFormat(nameOrUUID)) {
            String clean = nameOrUUID.replace("-", "");
            return UUID.fromString(
                    clean.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        }
        // Fetch from Mojang by username
        URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + nameOrUUID).toURL();
        HttpURLConnection conn = openConn(url);
        if (conn.getResponseCode() != 200)
            return null;
        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            String id = obj.get("id").getAsString();
            return UUID.fromString(
                    id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        }
    }

    /**
     * Fetches the signed textures property from the session server for the given
     * UUID.
     */
    private SkinData fetchSkinData(UUID uuid) throws Exception {
        String noHyphen = uuid.toString().replace("-", "");
        URL url = URI
                .create("https://sessionserver.mojang.com/session/minecraft/profile/" + noHyphen + "?unsigned=false")
                .toURL();
        HttpURLConnection conn = openConn(url);
        if (conn.getResponseCode() != 200)
            return null;

        try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray props = obj.getAsJsonArray("properties");
            for (var elem : props) {
                JsonObject p = elem.getAsJsonObject();
                if ("textures".equals(p.get("name").getAsString())) {
                    String value = p.get("value").getAsString();
                    String sig = p.has("signature") ? p.get("signature").getAsString() : "";
                    return new SkinData(value, sig);
                }
            }
        }
        return null;
    }

    private HttpURLConnection openConn(URL url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(5_000);
        c.setReadTimeout(5_000);
        c.setRequestProperty("User-Agent", "DonutCore-HideModule/1.0");
        return c;
    }

    private boolean isUUIDFormat(String s) {
        return s.matches("[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}");
    }

    private void sendSync(Player player, String msg) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline())
                player.sendMessage(msg);
        });
    }
}
