package ro.andreilarazboi.donutcore.arenas;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ro.andreilarazboi.donutcore.sell.Utils;

import java.time.Duration;
import java.util.*;

public class ArenaTracker extends BukkitRunnable {

    private final ArenaManager manager;
    // player → set of arena names they are currently inside
    private final Map<UUID, Set<String>>    playerZones = new HashMap<>();
    // player → (arenaName → elapsed AFK seconds)
    private final Map<UUID, Map<String, Integer>> afkTimers = new HashMap<>();

    public ArenaTracker(ArenaManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        Collection<Arena> snapshot = new ArrayList<>(manager.getArenas());

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uid = player.getUniqueId();

            Set<String> prev = playerZones.getOrDefault(uid, new HashSet<>());
            Set<String> curr = new HashSet<>();

            for (Arena arena : snapshot) {
                if (manager.isPlayerInArena(player, arena)) {
                    curr.add(arena.getName());
                }
            }

            // Detect zone entries
            for (String zoneName : curr) {
                if (!prev.contains(zoneName)) {
                    Arena arena = manager.getArena(zoneName);
                    if (arena != null) handleEnter(player, arena);
                }
            }

            // Detect zone exits
            for (String zoneName : prev) {
                if (!curr.contains(zoneName)) {
                    Arena arena = manager.getArena(zoneName);
                    if (arena != null) handleExit(player, arena);
                    afkTimers.computeIfPresent(uid, (k, v) -> { v.remove(zoneName); return v; });
                }
            }

            // AFK ticks for zones the player is currently in
            for (String zoneName : curr) {
                Arena arena = manager.getArena(zoneName);
                if (arena != null && arena.isAfkEnabled()) {
                    tickAfk(player, arena);
                }
            }

            playerZones.put(uid, curr);
        }

        playerZones.keySet().removeIf(uid -> Bukkit.getPlayer(uid) == null);
        afkTimers.keySet().removeIf(uid -> Bukkit.getPlayer(uid) == null);
    }

    private void handleEnter(Player player, Arena arena) {
        if (arena.isEnterCommandEnabled() && !arena.getEnterCommand().isBlank()) {
            String cmd = replace(arena.getEnterCommand(), player, arena, null);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        String type = arena.getEnterMessageType();
        if (!type.equals("none")) {
            sendMessage(player, type,
                replace(arena.getEnterMessage(), player, arena, null),
                replace(arena.getEnterTitle(),   player, arena, null),
                replace(arena.getEnterSubtitle(),player, arena, null),
                3000);
        }
    }

    private void handleExit(Player player, Arena arena) {
        if (arena.isExitCommandEnabled() && !arena.getExitCommand().isBlank()) {
            String cmd = replace(arena.getExitCommand(), player, arena, null);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        String type = arena.getExitMessageType();
        if (!type.equals("none")) {
            sendMessage(player, type,
                replace(arena.getExitMessage(), player, arena, null),
                replace(arena.getExitTitle(),   player, arena, null),
                replace(arena.getExitSubtitle(),player, arena, null),
                3000);
        }
    }

    private void tickAfk(Player player, Arena arena) {
        UUID uid = player.getUniqueId();
        afkTimers.computeIfAbsent(uid, k -> new HashMap<>());
        int elapsed   = afkTimers.get(uid).getOrDefault(arena.getName(), 0) + 1;
        int remaining = Math.max(0, arena.getAfkIntervalSeconds() - elapsed);
        String time   = formatTime(remaining);

        // Always show countdown display every second
        String dtype = arena.getAfkDisplayType();
        boolean all  = dtype.equals("all");

        if (all || dtype.equals("actionbar")) {
            player.sendActionBar(Utils.toComponent(arena.getAfkActionbarText().replace("{time}", time)));
        }
        if (all || dtype.equals("title")) {
            player.showTitle(Title.title(
                Utils.toComponent(arena.getAfkTitleText().replace("{time}", time)),
                Utils.toComponent(arena.getAfkSubtitleText().replace("{time}", time)),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
            ));
        }
        if (all || dtype.equals("chat")) {
            if (isMilestone(remaining)) {
                player.sendMessage(Utils.toComponent(arena.getAfkChatText().replace("{time}", time)));
            }
        }

        if (elapsed >= arena.getAfkIntervalSeconds()) {
            if (!arena.getAfkCommand().isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    replace(arena.getAfkCommand(), player, arena, null));
            }
            afkTimers.get(uid).put(arena.getName(), 0);
        } else {
            afkTimers.get(uid).put(arena.getName(), elapsed);
        }
    }

    private void sendMessage(Player player, String type, String chat, String title, String subtitle, long stayMs) {
        switch (type) {
            case "chat"      -> player.sendMessage(Utils.toComponent(chat));
            case "actionbar" -> player.sendActionBar(Utils.toComponent(chat));
            case "title"     -> player.showTitle(Title.title(
                Utils.toComponent(title), Utils.toComponent(subtitle),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(stayMs), Duration.ofMillis(200))
            ));
            case "all"       -> {
                player.sendMessage(Utils.toComponent(chat));
                player.sendActionBar(Utils.toComponent(chat));
                player.showTitle(Title.title(
                    Utils.toComponent(title), Utils.toComponent(subtitle),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(stayMs), Duration.ofMillis(200))
                ));
            }
        }
    }

    private String replace(String text, Player player, Arena arena, String time) {
        if (text == null) return "";
        text = text.replace("{player}", player.getName());
        text = text.replace("{zone}", arena.getName());
        if (time != null) text = text.replace("{time}", time);
        return text;
    }

    private boolean isMilestone(int seconds) {
        return switch (seconds) {
            case 300, 120, 60, 30, 10, 5, 3, 2, 1, 0 -> true;
            default -> false;
        };
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "0s";
        int m = seconds / 60, s = seconds % 60;
        if (m > 0 && s > 0) return m + "m " + s + "s";
        return m > 0 ? m + "m" : s + "s";
    }

    public void clearPlayer(UUID uid) {
        playerZones.remove(uid);
        afkTimers.remove(uid);
    }
}
