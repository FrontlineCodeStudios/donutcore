
package ro.andreilarazboi.donutcore.crates;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class Utils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#");
    private static final Map<UUID, Location> SIGN_LOCATIONS = new ConcurrentHashMap<UUID, Location>();
    private static final Map<UUID, BlockData> SIGN_OLD_DATA = new ConcurrentHashMap<UUID, BlockData>();
    private static final Map<UUID, Integer> SIGN_INPUT_LINE = new ConcurrentHashMap<UUID, Integer>();
    private static final Map<UUID, Consumer<String>> SIGN_CALLBACKS = new ConcurrentHashMap<UUID, Consumer<String>>();
    private static final Map<UUID, BukkitTask> SIGN_HIDE_TASKS = new ConcurrentHashMap<UUID, BukkitTask>();
    private static final Map<UUID, Boolean> SIGN_HIDE_FROM_OTHERS = new ConcurrentHashMap<UUID, Boolean>();

    private Utils() {
    }

    public static String formatColors(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer(input.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder repl = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                repl.append('\u00a7').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(repl.toString()));
        }
        matcher.appendTail(buffer);
        return translateAmpersandCodes(buffer.toString());
    }

    private static String translateAmpersandCodes(String input) {
        char[] chars = input.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                if ("0123456789abcdefklmnorx".indexOf(code) >= 0) {
                    sb.append('\u00a7').append(code);
                    i++;
                    continue;
                }
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static String stripColor(String s) {
        if (s == null) return null;
        return s.replaceAll("\u00a7[0-9a-fA-Fk-oK-OrRxX]", "");
    }

    public static String stripColor(Component component) {
        if (component == null) return "";
        return stripColor(LegacyComponentSerializer.legacySection().serialize(component));
    }

    public static Component toComponent(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(formatColors(text));
    }

    public static List<Component> toComponents(List<String> lines) {
        return lines.stream().map(Utils::toComponent).collect(Collectors.toList());
    }

    public static List<String> formatColors(List<String> lines) {
        return lines.stream().map(Utils::formatColors).collect(Collectors.toList());
    }

    public static Sound resolveSound(String name, Sound fallback) {
        if (name == null || name.isEmpty()) return fallback;
        String lower = name.toLowerCase(Locale.ROOT);
        Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(lower));
        if (s != null) return s;
        String dotted = lower.replace('_', '.');
        s = Registry.SOUNDS.get(NamespacedKey.minecraft(dotted));
        if (s != null) return s;
        return fallback;
    }

    public static boolean canFit(PlayerInventory inv, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return true;
        }
        if (inv.firstEmpty() != -1) {
            return true;
        }
        int toPlace = stack.getAmount();
        int max = stack.getMaxStackSize();
        for (ItemStack c : inv.getContents()) {
            int free;
            if (c == null || !c.isSimilar(stack) || (free = max - c.getAmount()) <= 0 || (toPlace -= free) > 0) continue;
            return true;
        }
        return false;
    }

    public static void openSignInput(JavaPlugin plugin, Player player, List<String> rawLines, int inputLine, Consumer<String> callback) {
        Utils.openSignInput(plugin, player, rawLines, inputLine, true, callback);
    }

    public static void openSignInput(JavaPlugin plugin, Player player, List<String> rawLines, int inputLine, boolean hideFromOthers, Consumer<String> callback) {
        Utils.cancelSignInput(player);
        UUID uuid = player.getUniqueId();
        int lineIndex = inputLine - 1;
        if (lineIndex < 0) {
            lineIndex = 0;
        }
        if (lineIndex > 3) {
            lineIndex = 3;
        }
        Location base = player.getLocation();
        ArrayList<Location> candidates = new ArrayList<Location>();
        candidates.add(base.clone().add(0.0, 2.0, 0.0));
        Vector dir = base.getDirection();
        dir.setY(0);
        if (dir.lengthSquared() > 1.0E-4) {
            dir.normalize();
            candidates.add(base.clone().add(dir).add(0.0, 1.0, 0.0));
            candidates.add(base.clone().add(dir.multiply(2)).add(0.0, 1.0, 0.0));
        }
        candidates.add(base.clone().add(0.0, 1.0, 0.0));
        Location signLoc = null;
        BlockData oldData = null;
        for (Location cand : candidates) {
            Block b = cand.getBlock();
            Material t = b.getType();
            if (t != Material.AIR && t != Material.CAVE_AIR && t != Material.VOID_AIR) continue;
            signLoc = b.getLocation();
            oldData = b.getBlockData();
            break;
        }
        if (signLoc == null) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        List lines = new ArrayList<String>();
        if (rawLines != null) {
            lines.addAll(rawLines);
        }
        while (lines.size() < 4) {
            lines.add("");
        }
        if (lines.size() > 4) {
            lines = lines.subList(0, 4);
        }
        String[] plainLines = new String[4];
        for (int i = 0; i < 4; ++i) {
            String raw = (String)lines.get(i);
            String colored = Utils.formatColors(raw);
            String stripped = Utils.stripColor(colored);
            plainLines[i] = stripped == null ? "" : stripped;
        }
        SIGN_LOCATIONS.put(uuid, signLoc);
        SIGN_OLD_DATA.put(uuid, oldData);
        SIGN_INPUT_LINE.put(uuid, lineIndex);
        SIGN_HIDE_FROM_OTHERS.put(uuid, hideFromOthers);
        if (callback != null) {
            SIGN_CALLBACKS.put(uuid, callback);
        }
        Location finalLoc = signLoc.clone();
        String[] finalLines = plainLines;
        BlockData finalOldData = oldData;
        boolean finalHideFromOthers = hideFromOthers;
        Bukkit.getScheduler().runTask((Plugin)plugin, () -> {
            Block block = finalLoc.getBlock();
            block.setType(Material.OAK_SIGN, false);
            if (!(block.getState() instanceof Sign)) {
                Utils.finishSignInput(player, null);
                return;
            }
            Sign sign = (Sign)block.getState();
            boolean usedSideApi = false;
            try {
                Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
                Method getSide = sign.getClass().getMethod("getSide", sideClass);
                Object[] enumConstants = sideClass.getEnumConstants();
                Object frontSideEnum = enumConstants[0];
                Object signSide = getSide.invoke((Object)sign, frontSideEnum);
                Method setLine = signSide.getClass().getMethod("setLine", Integer.TYPE, String.class);
                for (int i = 0; i < 4; ++i) {
                    setLine.invoke(signSide, i, finalLines[i]);
                }
                usedSideApi = true;
            }
            catch (Throwable sideClass) {
                // empty catch block
            }
            if (!usedSideApi) {
                for (int i = 0; i < 4; ++i) {
                    try {
                        sign.setLine(i, finalLines[i]);
                        continue;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
            try {
                Method wax = sign.getClass().getMethod("setWaxed", Boolean.TYPE);
                wax.invoke((Object)sign, false);
            }
            catch (Throwable wax) {
                // empty catch block
            }
            try {
                Method editable = sign.getClass().getMethod("setEditable", Boolean.TYPE);
                editable.invoke((Object)sign, true);
            }
            catch (Throwable editable) {
                // empty catch block
            }
            try {
                Method allowed = sign.getClass().getMethod("setAllowedEditorUniqueId", UUID.class);
                allowed.invoke((Object)sign, uuid);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            sign.update(true, false);
            player.sendBlockChange(finalLoc, sign.getBlockData());
            if (finalHideFromOthers) {
                Utils.startHideFromOthers(plugin, player, finalLoc, finalOldData);
            }
            Bukkit.getScheduler().runTaskLater((Plugin)plugin, () -> {
                Block current = finalLoc.getBlock();
                if (!(current.getState() instanceof Sign)) {
                    Utils.finishSignInput(player, null);
                    return;
                }
                try {
                    player.openSign((Sign)current.getState());
                }
                catch (Throwable t) {
                    Utils.finishSignInput(player, null);
                }
            }, 1L);
        });
    }

    public static void cancelSignInput(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        BukkitTask task = SIGN_HIDE_TASKS.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        Location loc = SIGN_LOCATIONS.remove(uuid);
        BlockData old = SIGN_OLD_DATA.remove(uuid);
        SIGN_INPUT_LINE.remove(uuid);
        SIGN_CALLBACKS.remove(uuid);
        Boolean hide = SIGN_HIDE_FROM_OTHERS.remove(uuid);
        if (loc != null && old != null) {
            Block b = loc.getBlock();
            b.setBlockData(old, false);
            if (player.isOnline()) {
                player.sendBlockChange(loc, old);
            }
            if (Boolean.TRUE.equals(hide)) {
                Utils.sendOriginalToOthers(player, loc, old);
            }
        }
    }

    private static void finishSignInput(Player player, String value) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        BukkitTask task = SIGN_HIDE_TASKS.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        Location loc = SIGN_LOCATIONS.remove(uuid);
        BlockData old = SIGN_OLD_DATA.remove(uuid);
        SIGN_INPUT_LINE.remove(uuid);
        Boolean hide = SIGN_HIDE_FROM_OTHERS.remove(uuid);
        Consumer<String> callback = SIGN_CALLBACKS.remove(uuid);
        if (loc != null && old != null) {
            Block b = loc.getBlock();
            b.setBlockData(old, false);
            if (player.isOnline()) {
                player.sendBlockChange(loc, old);
            }
            if (Boolean.TRUE.equals(hide)) {
                Utils.sendOriginalToOthers(player, loc, old);
            }
        }
        if (callback != null) {
            callback.accept(value);
        }
    }

    private static void startHideFromOthers(JavaPlugin plugin, Player owner, Location loc, BlockData originalData) {
        UUID uuid = owner.getUniqueId();
        Utils.sendOriginalToOthers(owner, loc, originalData);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, () -> {
            Location active = SIGN_LOCATIONS.get(uuid);
            if (active == null || !Utils.sameBlock(active, loc) || !owner.isOnline()) {
                BukkitTask t = SIGN_HIDE_TASKS.remove(uuid);
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            Utils.sendOriginalToOthers(owner, loc, originalData);
        }, 1L, 1L);
        SIGN_HIDE_TASKS.put(uuid, task);
    }

    private static void sendOriginalToOthers(Player owner, Location loc, BlockData originalData) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals((Object)owner) || !other.getWorld().equals((Object)loc.getWorld()) || other.getLocation().distanceSquared(loc) > 9216.0) continue;
            other.sendBlockChange(loc, originalData);
        }
    }

    private static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        if (!a.getWorld().equals((Object)b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    public static class SignInputListener
    implements Listener {
        @EventHandler
        public void onSignChange(SignChangeEvent event) {
            String raw;
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            Location expected = SIGN_LOCATIONS.get(uuid);
            if (expected == null) {
                return;
            }
            if (!Utils.sameBlock(event.getBlock().getLocation(), expected)) {
                return;
            }
            int lineIndex = SIGN_INPUT_LINE.getOrDefault(uuid, 0);
            try {
                net.kyori.adventure.text.Component lineComp = event.line(lineIndex);
                raw = lineComp != null ? LegacyComponentSerializer.legacySection().serialize(lineComp) : "";
            }
            catch (Throwable ex) {
                raw = "";
            }
            if (raw == null) {
                raw = "";
            }
            String value = Utils.stripColor(raw).trim();
            Utils.finishSignInput(player, value);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Utils.cancelSignInput(event.getPlayer());
        }
    }
}

