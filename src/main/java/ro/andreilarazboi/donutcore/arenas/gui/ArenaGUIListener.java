package ro.andreilarazboi.donutcore.arenas.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ro.andreilarazboi.donutcore.DonutCore;
import ro.andreilarazboi.donutcore.arenas.Arena;
import ro.andreilarazboi.donutcore.arenas.ArenaManager;
import ro.andreilarazboi.donutcore.arenas.Trigger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaGUIListener implements Listener {

    // 6-row inventory: border on top/bottom rows and col 0 / col 8.
    // Inner area (rows 1-4, cols 1-7) = 28 item slots per page.
    // Shared by the zone list and the trigger list.
    private static final int[] GRID_SLOTS;
    static {
        GRID_SLOTS = new int[28];
        int i = 0;
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                GRID_SLOTS[i++] = row * 9 + col;
    }

    enum InputType {
        CREATE_ZONE_NAME, CREATE_ZONE_REGION, CONFIRM_DELETE_ZONE,
        AFk_INTERVAL, AFk_COMMAND, AFk_ACTIONBAR, AFk_TITLE, AFk_SUBTITLE, AFk_CHAT,
        ENTER_MESSAGE, ENTER_TITLE, ENTER_SUBTITLE, ENTER_COMMAND,
        EXIT_MESSAGE,  EXIT_TITLE,  EXIT_SUBTITLE,  EXIT_COMMAND,
        CREATE_TRIGGER_NAME, CREATE_TRIGGER_REGION, CONFIRM_DELETE_TRIGGER,
        TRIGGER_COMMAND, TRIGGER_MESSAGE, TRIGGER_TITLE, TRIGGER_SUBTITLE,
    }

    record PendingInput(InputType type, String arenaName, String triggerName, String extra) {}

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX_CODE = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final DonutCore         plugin;
    private final ArenaManager      manager;
    private final FileConfiguration menus;
    private final Map<UUID, PendingInput> pending      = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>      playerPage   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>      triggerPage  = new ConcurrentHashMap<>();

    public ArenaGUIListener(DonutCore plugin, ArenaManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
        this.menus   = manager.getMenusConfig();
    }

    // ── Public open methods ───────────────────────────────────────────────────

    public void openListGUI(Player player) {
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        player.openInventory(buildListInventory(page));
    }

    public void openDetailGUI(Player player, Arena arena) {
        player.openInventory(buildDetailInventory(arena));
    }

    public void openTriggerListGUI(Player player, Arena arena) {
        int page = triggerPage.getOrDefault(player.getUniqueId(), 0);
        player.openInventory(buildTriggerListInventory(arena, page));
    }

    public void openTriggerDetailGUI(Player player, Arena arena, Trigger trigger) {
        player.openInventory(buildTriggerDetailInventory(arena, trigger));
    }

    // ── Zone List GUI builder ─────────────────────────────────────────────────

    private Inventory buildListInventory(int page) {
        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("list", null), 54,
            render(menus.getString("list-menu.title", "<dark_gray><bold>Zone Manager")));

        ItemStack glassItem = glass();
        border(inv, glassItem);

        inv.setItem(4, configItem("list-menu.title-icon", null));

        List<Arena> sorted = new ArrayList<>(manager.getArenas());
        sorted.sort(Comparator.comparing(Arena::getName));

        int start = page * 28;
        for (int i = 0; i < 28 && start + i < sorted.size(); i++) {
            Arena a = sorted.get(start + i);
            List<String> lore = new ArrayList<>(List.of(
                "<gray>Region: <white>" + a.getRegion(),
                "<gray>World: <white>" + a.getWorldName(),
                "",
                "<gray>AFK Timer: " + (a.isAfkEnabled()
                    ? "<green>Enabled <dark_gray>| <white>" + formatTime(a.getAfkIntervalSeconds())
                    : "<red>Disabled"),
                "<gray>On Enter: " + (a.getEnterMessageType().equals("none") && !a.isEnterCommandEnabled()
                    ? "<dark_gray>Off" : "<green>On"),
                "<gray>On Exit: " + (a.getExitMessageType().equals("none") && !a.isExitCommandEnabled()
                    ? "<dark_gray>Off" : "<green>On"),
                "<gray>Triggers: <white>" + a.getTriggers().size(),
                "",
                "<dark_gray>Click to manage"
            ));
            Material mat = configMaterial("list-menu.zone-item", Material.GRASS_BLOCK);
            inv.setItem(GRID_SLOTS[i], item(mat, "<yellow>" + a.getName(), lore.toArray(new String[0])));
        }

        inv.setItem(49, configItem("list-menu.create-button", null));
        if (page > 0)
            inv.setItem(45, configItem("list-menu.prev-button", null));
        if (start + 28 < sorted.size())
            inv.setItem(53, configItem("list-menu.next-button", null));

        return inv;
    }

    // ── Zone Detail GUI builder (5 rows, color-coded sections) ───────────────

    private Inventory buildDetailInventory(Arena arena) {
        UnaryOperator<String> zoneRepl = s -> s.replace("%zone%", arena.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("detail", arena.getName()), 45,
            render(configTitle("detail-menu.title", "<dark_gray>Zone: <yellow>%zone%", zoneRepl)));

        ItemStack navGlass = glass();

        // Row 0 (0-8): navigation header
        for (int i = 0; i < 9; i++) inv.setItem(i, navGlass);
        inv.setItem(0, configItem("detail-menu.back-button", null));
        UnaryOperator<String> countRepl = s -> s.replace("%count%", String.valueOf(arena.getTriggers().size()));
        inv.setItem(2, configItem("detail-menu.triggers-button", countRepl));
        inv.setItem(4, item(Material.EMERALD, "<yellow><bold>" + arena.getName(),
            "<gray>Region: <white>" + arena.getRegion(),
            "<gray>World: <white>" + arena.getWorldName()));
        inv.setItem(8, configItem("detail-menu.delete-button", null));

        // Row 1 (9-17): AFK Timer — fully packed, no spacers needed
        inv.setItem(9,  item(Material.CLOCK, "<gold><bold>AFK Timer",
            "<gray>Reward players for staying in this zone",
            "<dark_gray>(requires interval + command to be set)"));
        inv.setItem(10, toggle(arena.isAfkEnabled(), "AFK Timer"));
        inv.setItem(11, item(Material.GOLD_NUGGET, "<yellow>Interval",
            "<gray>Current: <white>" + formatTime(arena.getAfkIntervalSeconds()),
            "<dark_gray>Click to change  |  Format: <white>30s<dark_gray>, <white>1m<dark_gray>, <white>2m30s"));
        inv.setItem(12, item(Material.COMMAND_BLOCK, "<yellow>Reward Command",
            "<gray>Current: <white>" + blank(arena.getAfkCommand()),
            "<dark_gray>Click to change  |  Use <white>{player}"));
        inv.setItem(13, item(Material.OAK_SIGN, "<yellow>Display Type",
            "<gray>Current: <white>" + arena.getAfkDisplayType(),
            "<dark_gray>Click to cycle: actionbar → title → chat → all"));
        inv.setItem(14, item(Material.PAPER, "<yellow>Actionbar Text",
            "<gray>" + arena.getAfkActionbarText(),
            "<dark_gray>Click to change  |  Use <white>{time}"));
        inv.setItem(15, item(Material.ENCHANTED_BOOK, "<yellow>Title Text",
            "<gray>" + arena.getAfkTitleText(),
            "<dark_gray>Click to change  |  Use <white>{time}"));
        inv.setItem(16, item(Material.BOOK, "<yellow>Subtitle Text",
            "<gray>" + arena.getAfkSubtitleText(),
            "<dark_gray>Click to change  |  Use <white>{time}"));
        inv.setItem(17, item(Material.FEATHER, "<yellow>Chat Text",
            "<gray>" + arena.getAfkChatText(),
            "<dark_gray>Click to change  |  Use <white>{time}",
            "<dark_gray>Shown only at reward milestones"));

        // Row 2 (18-26): Enter action — lime-tinted spacers
        ItemStack enterGlass = coloredGlass(Material.LIME_STAINED_GLASS_PANE);
        inv.setItem(18, item(Material.LIME_DYE, "<green><bold>Enter Action",
            "<gray>Runs when a player enters this zone"));
        inv.setItem(19, item(Material.OAK_SIGN, "<green>Message Type",
            "<gray>Current: <white>" + arena.getEnterMessageType(),
            "<dark_gray>Click to cycle: none → chat → actionbar → title"));
        inv.setItem(20, item(Material.PAPER, "<green>Message Text",
            "<gray>" + arena.getEnterMessage(),
            "<dark_gray>Click to change  |  Use <white>{zone}"));
        if (arena.getEnterMessageType().equals("title")) {
            inv.setItem(21, item(Material.ENCHANTED_BOOK, "<green>Title Text",
                "<gray>" + arena.getEnterTitle(), "<dark_gray>Click to change  |  Use <white>{zone}"));
            inv.setItem(22, item(Material.BOOK, "<green>Subtitle Text",
                "<gray>" + arena.getEnterSubtitle(), "<dark_gray>Click to change  |  Use <white>{zone}"));
        } else {
            inv.setItem(21, enterGlass);
            inv.setItem(22, enterGlass);
        }
        inv.setItem(23, enterGlass);
        inv.setItem(24, toggle(arena.isEnterCommandEnabled(), "Enter Command"));
        inv.setItem(25, arena.isEnterCommandEnabled()
            ? item(Material.COMMAND_BLOCK, "<green>Enter Command",
                "<gray>Current: <white>" + blank(arena.getEnterCommand()),
                "<dark_gray>Click to change  |  Use <white>{player}<dark_gray>, <white>{zone}")
            : enterGlass);
        inv.setItem(26, enterGlass);

        // Row 3 (27-35): Exit action — red-tinted spacers
        ItemStack exitGlass = coloredGlass(Material.RED_STAINED_GLASS_PANE);
        inv.setItem(27, item(Material.RED_DYE, "<red><bold>Exit Action",
            "<gray>Runs when a player leaves this zone"));
        inv.setItem(28, item(Material.OAK_SIGN, "<red>Message Type",
            "<gray>Current: <white>" + arena.getExitMessageType(),
            "<dark_gray>Click to cycle: none → chat → actionbar → title"));
        inv.setItem(29, item(Material.PAPER, "<red>Message Text",
            "<gray>" + arena.getExitMessage(),
            "<dark_gray>Click to change  |  Use <white>{zone}"));
        if (arena.getExitMessageType().equals("title")) {
            inv.setItem(30, item(Material.ENCHANTED_BOOK, "<red>Title Text",
                "<gray>" + arena.getExitTitle(), "<dark_gray>Click to change  |  Use <white>{zone}"));
            inv.setItem(31, item(Material.BOOK, "<red>Subtitle Text",
                "<gray>" + arena.getExitSubtitle(), "<dark_gray>Click to change  |  Use <white>{zone}"));
        } else {
            inv.setItem(30, exitGlass);
            inv.setItem(31, exitGlass);
        }
        inv.setItem(32, exitGlass);
        inv.setItem(33, toggle(arena.isExitCommandEnabled(), "Exit Command"));
        inv.setItem(34, arena.isExitCommandEnabled()
            ? item(Material.COMMAND_BLOCK, "<red>Exit Command",
                "<gray>Current: <white>" + blank(arena.getExitCommand()),
                "<dark_gray>Click to change  |  Use <white>{player}<dark_gray>, <white>{zone}")
            : exitGlass);
        inv.setItem(35, exitGlass);

        // Row 4 (36-44): footer
        for (int i = 36; i < 45; i++) inv.setItem(i, navGlass);
        inv.setItem(36, configItem("detail-menu.back-button", null));
        inv.setItem(40, configItem("detail-menu.autosave-note", null));
        inv.setItem(44, configItem("detail-menu.delete-button", null));

        return inv;
    }

    // ── Trigger List GUI builder ──────────────────────────────────────────────

    private Inventory buildTriggerListInventory(Arena arena, int page) {
        UnaryOperator<String> zoneRepl = s -> s.replace("%zone%", arena.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("trigger-list", arena.getName()), 54,
            render(configTitle("trigger-list-menu.title", "<dark_gray>Triggers: <yellow>%zone%", zoneRepl)));

        ItemStack glassItem = glass();
        border(inv, glassItem);

        inv.setItem(4, configItem("trigger-list-menu.title-icon", zoneRepl));

        List<Trigger> sorted = new ArrayList<>(arena.getTriggers());
        sorted.sort(Comparator.comparing(Trigger::getName));

        int start = page * 28;
        for (int i = 0; i < 28 && start + i < sorted.size(); i++) {
            Trigger t = sorted.get(start + i);
            List<String> lore = List.of(
                "<gray>Region: <white>" + t.getRegion(),
                "<gray>Status: " + (t.isEnabled() ? "<green>Enabled" : "<red>Disabled"),
                "",
                "<gray>Command: " + (t.isCommandEnabled() ? "<green>On" : "<dark_gray>Off"),
                "<gray>Teleport: " + (t.isTeleportEnabled() ? "<green>On" : "<dark_gray>Off"),
                "<gray>Message: " + (t.getMessageType().equals("none") ? "<dark_gray>Off" : "<green>On"),
                "",
                "<dark_gray>Click to manage"
            );
            Material mat = configMaterial("trigger-list-menu.trigger-item", Material.COMPASS);
            inv.setItem(GRID_SLOTS[i], item(mat, "<aqua>" + t.getName(), lore.toArray(new String[0])));
        }

        inv.setItem(45, configItem("trigger-list-menu.back-button", null));
        inv.setItem(49, configItem("trigger-list-menu.create-button", null));
        if (page > 0)
            inv.setItem(47, item(Material.ARROW, "<gray>← Previous"));
        if (start + 28 < sorted.size())
            inv.setItem(51, item(Material.ARROW, "<gray>Next →"));

        return inv;
    }

    // ── Trigger Detail GUI builder (3 rows, no wasted space) ─────────────────

    private Inventory buildTriggerDetailInventory(Arena arena, Trigger trigger) {
        UnaryOperator<String> trigRepl = s -> s.replace("%trigger%", trigger.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("trigger-detail", arena.getName(), trigger.getName()), 27,
            render(configTitle("trigger-detail-menu.title", "<dark_gray>Trigger: <yellow>%trigger%", trigRepl)));

        ItemStack navGlass = glass();
        for (int i = 0; i < 9; i++) inv.setItem(i, navGlass);
        inv.setItem(0, configItem("trigger-detail-menu.back-button", null));
        inv.setItem(4, item(Material.COMPASS, "<aqua><bold>" + trigger.getName(),
            "<gray>Region: <white>" + trigger.getRegion(),
            "<gray>Status: " + (trigger.isEnabled() ? "<green>Enabled" : "<red>Disabled"),
            "<dark_gray>Click to toggle"));
        inv.setItem(8, configItem("trigger-detail-menu.delete-button", null));

        // Row 1 (9-17): Command action (9-11) + Teleport action (13-16)
        ItemStack spacer = glass();
        inv.setItem(9,  item(Material.COMMAND_BLOCK, "<gold><bold>Command Action",
            "<gray>Run a console command when entered"));
        inv.setItem(10, toggle(trigger.isCommandEnabled(), "Run Command"));
        inv.setItem(11, item(Material.PAPER, "<yellow>Command",
            "<gray>Current: <white>" + blank(trigger.getCommand()),
            "<dark_gray>Click to change  |  Use <white>{player}<dark_gray>, <white>{zone}<dark_gray>, <white>{trigger}"));
        inv.setItem(12, spacer);
        inv.setItem(13, item(Material.ENDER_PEARL, "<light_purple><bold>Teleport Action",
            "<gray>Teleport the player to a fixed location"));
        inv.setItem(14, toggle(trigger.isTeleportEnabled(), "Teleport"));
        inv.setItem(15, configItem("trigger-detail-menu.set-location-button", null));
        inv.setItem(16, item(Material.MAP, "<light_purple>Current Location",
            trigger.hasTeleportLocation()
                ? "<gray>" + trigger.getTeleportWorld() + " <dark_gray>@ <gray>"
                    + Math.round(trigger.getTeleportX()) + ", "
                    + Math.round(trigger.getTeleportY()) + ", "
                    + Math.round(trigger.getTeleportZ())
                : "<dark_gray>(not set)"));
        inv.setItem(17, spacer);

        // Row 2 (18-26): Message on enter
        inv.setItem(18, item(Material.OAK_SIGN, "<aqua><bold>Message on Enter",
            "<gray>Shown to the player when they enter"));
        inv.setItem(19, item(Material.OAK_SIGN, "<aqua>Message Type",
            "<gray>Current: <white>" + trigger.getMessageType(),
            "<dark_gray>Click to cycle: none → chat → actionbar → title"));
        inv.setItem(20, item(Material.PAPER, "<aqua>Message Text",
            "<gray>" + blank(trigger.getMessage()),
            "<dark_gray>Click to change  |  Use <white>{zone}<dark_gray>, <white>{trigger}"));
        if (trigger.getMessageType().equals("title")) {
            inv.setItem(21, item(Material.ENCHANTED_BOOK, "<aqua>Title Text",
                "<gray>" + blank(trigger.getTitle()),
                "<dark_gray>Click to change  |  Use <white>{zone}<dark_gray>, <white>{trigger}"));
            inv.setItem(22, item(Material.BOOK, "<aqua>Subtitle Text",
                "<gray>" + blank(trigger.getSubtitle()),
                "<dark_gray>Click to change  |  Use <white>{zone}<dark_gray>, <white>{trigger}"));
        } else {
            inv.setItem(21, spacer);
            inv.setItem(22, spacer);
        }
        for (int i = 23; i < 27; i++) inv.setItem(i, spacer);

        return inv;
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArenaGuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == fillerMaterial()
            || clicked.getType() == Material.LIME_STAINED_GLASS_PANE
            || clicked.getType() == Material.RED_STAINED_GLASS_PANE) return;

        switch (holder.getType()) {
            case "list"           -> handleListClick(player, event.getRawSlot());
            case "detail"         -> handleDetailClick(player, holder, event.getRawSlot());
            case "trigger-list"   -> handleTriggerListClick(player, holder, event.getRawSlot());
            case "trigger-detail" -> handleTriggerDetailClick(player, holder, event.getRawSlot());
            default -> {}
        }
    }

    private void handleListClick(Player player, int slot) {
        int page  = playerPage.getOrDefault(player.getUniqueId(), 0);
        List<Arena> sorted = new ArrayList<>(manager.getArenas());
        sorted.sort(Comparator.comparing(Arena::getName));

        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (slot == GRID_SLOTS[i]) {
                int idx = page * 28 + i;
                if (idx < sorted.size()) openDetailGUI(player, sorted.get(idx));
                return;
            }
        }

        switch (slot) {
            case 49 -> promptCreateZone(player);
            case 45 -> {
                if (page > 0) {
                    playerPage.put(player.getUniqueId(), page - 1);
                    openListGUI(player);
                }
            }
            case 53 -> {
                if ((page + 1) * 28 < sorted.size()) {
                    playerPage.put(player.getUniqueId(), page + 1);
                    openListGUI(player);
                }
            }
        }
    }

    private void handleDetailClick(Player player, ArenaGuiHolder holder, int slot) {
        String arenaName = holder.getArenaName();
        Arena  arena      = manager.getArena(arenaName);
        if (arena == null) { openListGUI(player); return; }

        switch (slot) {
            case 0, 36 -> openListGUI(player);
            case 8, 44 -> promptDeleteZone(player, arena);
            case 2 -> { triggerPage.put(player.getUniqueId(), 0); openTriggerListGUI(player, arena); }

            // AFK Timer
            case 10 -> { arena.setAfkEnabled(!arena.isAfkEnabled()); saveZone(player, arena); }
            case 11 -> prompt(player, arenaName, InputType.AFk_INTERVAL,
                "<green>Type the AFK reward interval <gray>(e.g. <white>30s<gray>, <white>1m<gray>, <white>2m30s<gray>):");
            case 12 -> prompt(player, arenaName, InputType.AFk_COMMAND,
                "<green>Type the reward command <gray>(use <white>{player}<gray>):");
            case 13 -> { cycleAfkDisplay(arena); saveZone(player, arena); }
            case 14 -> prompt(player, arenaName, InputType.AFk_ACTIONBAR,
                "<green>Type the actionbar text <gray>(use <white>{time}<gray>):");
            case 15 -> prompt(player, arenaName, InputType.AFk_TITLE,
                "<green>Type the title text <gray>(use <white>{time}<gray>):");
            case 16 -> prompt(player, arenaName, InputType.AFk_SUBTITLE,
                "<green>Type the subtitle text <gray>(use <white>{time}<gray>):");
            case 17 -> prompt(player, arenaName, InputType.AFk_CHAT,
                "<green>Type the chat text <gray>(use <white>{time}<gray>):");

            // Enter action
            case 19 -> { cycleMessageType(arena, true); saveZone(player, arena); }
            case 20 -> prompt(player, arenaName, InputType.ENTER_MESSAGE,
                "<green>Type the enter message <gray>(use <white>{zone}<gray>):");
            case 21 -> prompt(player, arenaName, InputType.ENTER_TITLE,
                "<green>Type the enter title <gray>(use <white>{zone}<gray>):");
            case 22 -> prompt(player, arenaName, InputType.ENTER_SUBTITLE,
                "<green>Type the enter subtitle <gray>(use <white>{zone}<gray>):");
            case 24 -> { arena.setEnterCommandEnabled(!arena.isEnterCommandEnabled()); saveZone(player, arena); }
            case 25 -> prompt(player, arenaName, InputType.ENTER_COMMAND,
                "<green>Type the enter command <gray>(use <white>{player}<gray>, <white>{zone}<gray>):");

            // Exit action
            case 28 -> { cycleMessageType(arena, false); saveZone(player, arena); }
            case 29 -> prompt(player, arenaName, InputType.EXIT_MESSAGE,
                "<green>Type the exit message <gray>(use <white>{zone}<gray>):");
            case 30 -> prompt(player, arenaName, InputType.EXIT_TITLE,
                "<green>Type the exit title <gray>(use <white>{zone}<gray>):");
            case 31 -> prompt(player, arenaName, InputType.EXIT_SUBTITLE,
                "<green>Type the exit subtitle <gray>(use <white>{zone}<gray>):");
            case 33 -> { arena.setExitCommandEnabled(!arena.isExitCommandEnabled()); saveZone(player, arena); }
            case 34 -> prompt(player, arenaName, InputType.EXIT_COMMAND,
                "<green>Type the exit command <gray>(use <white>{player}<gray>, <white>{zone}<gray>):");
        }
    }

    private void handleTriggerListClick(Player player, ArenaGuiHolder holder, int slot) {
        Arena arena = manager.getArena(holder.getArenaName());
        if (arena == null) { openListGUI(player); return; }

        int page = triggerPage.getOrDefault(player.getUniqueId(), 0);
        List<Trigger> sorted = new ArrayList<>(arena.getTriggers());
        sorted.sort(Comparator.comparing(Trigger::getName));

        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (slot == GRID_SLOTS[i]) {
                int idx = page * 28 + i;
                if (idx < sorted.size()) openTriggerDetailGUI(player, arena, sorted.get(idx));
                return;
            }
        }

        switch (slot) {
            case 45 -> openDetailGUI(player, arena);
            case 49 -> promptCreateTrigger(player, arena);
            case 47 -> {
                if (page > 0) {
                    triggerPage.put(player.getUniqueId(), page - 1);
                    openTriggerListGUI(player, arena);
                }
            }
            case 51 -> {
                if ((page + 1) * 28 < sorted.size()) {
                    triggerPage.put(player.getUniqueId(), page + 1);
                    openTriggerListGUI(player, arena);
                }
            }
        }
    }

    private void handleTriggerDetailClick(Player player, ArenaGuiHolder holder, int slot) {
        Arena   arena   = manager.getArena(holder.getArenaName());
        Trigger trigger = arena == null ? null : arena.getTrigger(holder.getExtra());
        if (arena == null || trigger == null) { openListGUI(player); return; }

        switch (slot) {
            case 0 -> openTriggerListGUI(player, arena);
            case 8 -> promptDeleteTrigger(player, arena, trigger);
            case 4 -> { trigger.setEnabled(!trigger.isEnabled()); saveTrigger(player, arena, trigger); }

            case 10 -> { trigger.setCommandEnabled(!trigger.isCommandEnabled()); saveTrigger(player, arena, trigger); }
            case 11 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_COMMAND,
                "<green>Type the trigger command <gray>(use <white>{player}<gray>, <white>{zone}<gray>, <white>{trigger}<gray>):");

            case 14 -> { trigger.setTeleportEnabled(!trigger.isTeleportEnabled()); saveTrigger(player, arena, trigger); }
            case 15 -> {
                Location loc = player.getLocation();
                trigger.setTeleportWorld(loc.getWorld().getName());
                trigger.setTeleportX(loc.getX());
                trigger.setTeleportY(loc.getY());
                trigger.setTeleportZ(loc.getZ());
                trigger.setTeleportYaw(loc.getYaw());
                trigger.setTeleportPitch(loc.getPitch());
                tell(player, "<green>Teleport location set to your current position.");
                saveTrigger(player, arena, trigger);
            }

            case 19 -> { cycleTriggerMessageType(trigger); saveTrigger(player, arena, trigger); }
            case 20 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_MESSAGE,
                "<green>Type the trigger message <gray>(use <white>{zone}<gray>, <white>{trigger}<gray>):");
            case 21 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_TITLE,
                "<green>Type the trigger title <gray>(use <white>{zone}<gray>, <white>{trigger}<gray>):");
            case 22 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_SUBTITLE,
                "<green>Type the trigger subtitle <gray>(use <white>{zone}<gray>, <white>{trigger}<gray>):");
        }
    }

    // ── Chat input ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        PendingInput p = pending.remove(uid);
        if (p == null) return;

        event.setCancelled(true);
        String text   = PlainTextComponentSerializer.plainText().serialize(event.message());
        Player player = event.getPlayer();

        if (text.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> cancelInput(player, p));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, p, text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        pending.remove(uid);
        playerPage.remove(uid);
        triggerPage.remove(uid);
    }

    private void handleInput(Player player, PendingInput p, String text) {
        switch (p.type()) {

            case CREATE_ZONE_NAME -> {
                String name = text.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                if (name.isBlank()) {
                    tell(player, "<red>Invalid name. Type a zone name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
                    return;
                }
                if (manager.getArena(name) != null) {
                    tell(player, "<red>Zone '<dark_red>" + name + "<red>' already exists. Choose a different name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
                    return;
                }
                tell(player, "<green>Zone name: <dark_green>" + name + "<green>. Now type the WorldGuard region name:");
                tell(player, "<dark_gray>(Region must already exist in your current world)");
                pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, null, name));
            }

            case CREATE_ZONE_REGION -> {
                String name   = p.extra();
                String region = text.toLowerCase();
                String world  = player.getWorld().getName();
                if (!manager.regionExists(world, region)) {
                    tell(player, "<red>Region '<dark_red>" + region + "<red>' not found in world '<dark_red>" + world + "<red>'. Try again:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, null, name));
                    return;
                }
                Arena arena = new Arena(name, region, world);
                manager.addArena(arena);
                tell(player, "<green>Zone '<dark_green>" + name + "<green>' created!");
                openDetailGUI(player, arena);
            }

            case CONFIRM_DELETE_ZONE -> {
                if (text.equalsIgnoreCase("confirm")) {
                    manager.removeArena(p.arenaName());
                    tell(player, "<green>Zone '<dark_green>" + p.arenaName() + "<green>' deleted.");
                    openListGUI(player);
                } else {
                    tell(player, "<red>Type <dark_red>confirm <red>or <dark_red>cancel<red>:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_ZONE, p.arenaName(), null, null));
                }
            }

            case CREATE_TRIGGER_NAME -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }
                String name = text.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                if (name.isBlank() || arena.getTrigger(name) != null) {
                    tell(player, "<red>Invalid or duplicate name. Type a trigger name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_NAME, arena.getName(), null, null));
                    return;
                }
                tell(player, "<green>Trigger name: <dark_green>" + name + "<green>. Now type the WorldGuard region name:");
                tell(player, "<dark_gray>(Region must already exist in this zone's world: " + arena.getWorldName() + ")");
                pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_REGION, arena.getName(), null, name));
            }

            case CREATE_TRIGGER_REGION -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }
                String name   = p.extra();
                String region = text.toLowerCase();
                if (!manager.regionExists(arena.getWorldName(), region)) {
                    tell(player, "<red>Region '<dark_red>" + region + "<red>' not found in world '<dark_red>" + arena.getWorldName() + "<red>'. Try again:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_REGION, arena.getName(), null, name));
                    return;
                }
                Trigger trigger = new Trigger(name, region);
                arena.addTrigger(trigger);
                manager.save();
                tell(player, "<green>Trigger '<dark_green>" + name + "<green>' created!");
                openTriggerDetailGUI(player, arena, trigger);
            }

            case CONFIRM_DELETE_TRIGGER -> {
                Arena arena = manager.getArena(p.arenaName());
                if (text.equalsIgnoreCase("confirm")) {
                    if (arena != null) {
                        arena.removeTrigger(p.triggerName());
                        manager.save();
                    }
                    tell(player, "<green>Trigger '<dark_green>" + p.triggerName() + "<green>' deleted.");
                    if (arena != null) openTriggerListGUI(player, arena); else openListGUI(player);
                } else {
                    tell(player, "<red>Type <dark_red>confirm <red>or <dark_red>cancel<red>:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_TRIGGER, p.arenaName(), p.triggerName(), null));
                }
            }

            default -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }

                if (isTriggerInput(p.type())) {
                    Trigger trigger = arena.getTrigger(p.triggerName());
                    if (trigger == null) { openTriggerListGUI(player, arena); return; }
                    applyTriggerInput(trigger, p.type(), text);
                    manager.save();
                    openTriggerDetailGUI(player, arena, trigger);
                } else {
                    applyZoneInput(arena, p.type(), text);
                    manager.save();
                    openDetailGUI(player, arena);
                }
            }
        }
    }

    private boolean isTriggerInput(InputType type) {
        return type == InputType.TRIGGER_COMMAND || type == InputType.TRIGGER_MESSAGE
            || type == InputType.TRIGGER_TITLE   || type == InputType.TRIGGER_SUBTITLE;
    }

    private void cancelInput(Player player, PendingInput p) {
        if (p.type() == InputType.CREATE_ZONE_NAME || p.type() == InputType.CREATE_ZONE_REGION) {
            openListGUI(player); return;
        }
        Arena arena = manager.getArena(p.arenaName());
        if (arena == null) { openListGUI(player); return; }

        if (p.type() == InputType.CREATE_TRIGGER_NAME || p.type() == InputType.CREATE_TRIGGER_REGION) {
            openTriggerListGUI(player, arena); return;
        }
        if (isTriggerInput(p.type()) || p.type() == InputType.CONFIRM_DELETE_TRIGGER) {
            Trigger trigger = arena.getTrigger(p.triggerName());
            if (trigger != null) openTriggerDetailGUI(player, arena, trigger);
            else openTriggerListGUI(player, arena);
            return;
        }
        openDetailGUI(player, arena);
    }

    private void applyZoneInput(Arena arena, InputType type, String text) {
        switch (type) {
            case AFk_INTERVAL  -> { int s = parseTime(text); if (s > 0) arena.setAfkIntervalSeconds(s); }
            case AFk_COMMAND   -> arena.setAfkCommand(text);
            case AFk_ACTIONBAR -> arena.setAfkActionbarText(text);
            case AFk_TITLE     -> arena.setAfkTitleText(text);
            case AFk_SUBTITLE  -> arena.setAfkSubtitleText(text);
            case AFk_CHAT      -> arena.setAfkChatText(text);
            case ENTER_MESSAGE -> arena.setEnterMessage(text);
            case ENTER_TITLE   -> arena.setEnterTitle(text);
            case ENTER_SUBTITLE-> arena.setEnterSubtitle(text);
            case ENTER_COMMAND -> arena.setEnterCommand(text);
            case EXIT_MESSAGE  -> arena.setExitMessage(text);
            case EXIT_TITLE    -> arena.setExitTitle(text);
            case EXIT_SUBTITLE -> arena.setExitSubtitle(text);
            case EXIT_COMMAND  -> arena.setExitCommand(text);
            default -> {}
        }
    }

    private void applyTriggerInput(Trigger trigger, InputType type, String text) {
        switch (type) {
            case TRIGGER_COMMAND  -> trigger.setCommand(text);
            case TRIGGER_MESSAGE  -> trigger.setMessage(text);
            case TRIGGER_TITLE    -> trigger.setTitle(text);
            case TRIGGER_SUBTITLE -> trigger.setSubtitle(text);
            default -> {}
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveZone(Player player, Arena arena) {
        manager.save();
        Bukkit.getScheduler().runTask(plugin, () -> openDetailGUI(player, arena));
    }

    private void saveTrigger(Player player, Arena arena, Trigger trigger) {
        manager.save();
        Bukkit.getScheduler().runTask(plugin, () -> openTriggerDetailGUI(player, arena, trigger));
    }

    private void prompt(Player player, String arenaName, InputType type, String message) {
        player.closeInventory();
        tell(player, message);
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to discard changes.");
        pending.put(player.getUniqueId(), new PendingInput(type, arenaName, null, null));
    }

    private void promptTrigger(Player player, Arena arena, Trigger trigger, InputType type, String message) {
        player.closeInventory();
        tell(player, message);
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to discard changes.");
        pending.put(player.getUniqueId(), new PendingInput(type, arena.getName(), trigger.getName(), null));
    }

    private void promptCreateZone(Player player) {
        player.closeInventory();
        tell(player, "<green>Type the zone name in chat:");
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to discard.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
    }

    private void promptDeleteZone(Player player, Arena arena) {
        player.closeInventory();
        tell(player, "<red>Type <dark_red>confirm <red>to permanently delete zone '<dark_red>" + arena.getName() + "<red>'.");
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to go back.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_ZONE, arena.getName(), null, null));
    }

    private void promptCreateTrigger(Player player, Arena arena) {
        player.closeInventory();
        tell(player, "<green>Type the trigger name in chat:");
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to discard.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_NAME, arena.getName(), null, null));
    }

    private void promptDeleteTrigger(Player player, Arena arena, Trigger trigger) {
        player.closeInventory();
        tell(player, "<red>Type <dark_red>confirm <red>to permanently delete trigger '<dark_red>" + trigger.getName() + "<red>'.");
        tell(player, "<dark_gray>Type <red>cancel <dark_gray>to go back.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_TRIGGER, arena.getName(), trigger.getName(), null));
    }

    private void cycleAfkDisplay(Arena arena) {
        arena.setAfkDisplayType(switch (arena.getAfkDisplayType()) {
            case "actionbar" -> "title";
            case "title"     -> "chat";
            case "chat"      -> "all";
            default          -> "actionbar";
        });
    }

    private void cycleMessageType(Arena arena, boolean enter) {
        String current = enter ? arena.getEnterMessageType() : arena.getExitMessageType();
        String next    = switch (current) {
            case "none"      -> "chat";
            case "chat"      -> "actionbar";
            case "actionbar" -> "title";
            default          -> "none";
        };
        if (enter) arena.setEnterMessageType(next);
        else       arena.setExitMessageType(next);
    }

    private void cycleTriggerMessageType(Trigger trigger) {
        trigger.setMessageType(switch (trigger.getMessageType()) {
            case "none"      -> "chat";
            case "chat"      -> "actionbar";
            case "actionbar" -> "title";
            default          -> "none";
        });
    }

    private void tell(Player player, String msg) {
        player.sendMessage(render(menus.getString("prefix", "<dark_gray><bold>[<green>Arena<dark_gray>]<reset> ") + msg));
    }

    private void border(Inventory inv, ItemStack glass) {
        for (int i = 0; i <  9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }
    }

    // ── MiniMessage rendering (with legacy & code interop) ────────────────────

    /**
     * Renders chrome text and/or admin-authored text as one Component. Accepts
     * native MiniMessage {@code <tags>} and legacy {@code &} codes (including
     * {@code &#rrggbb} hex) in the same string — legacy codes are translated to
     * MiniMessage tags before parsing, so both styles compose safely. Falls back
     * to plain text if an admin-typed value contains a stray '<' that MiniMessage
     * can't parse, so a typo can never break the menu.
     */
    private static Component render(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        try {
            return MM.deserialize(toMiniMessageTags(raw));
        } catch (Exception ex) {
            return Component.text(raw);
        }
    }

    private static String toMiniMessageTags(String input) {
        Matcher hex = HEX_CODE.matcher(input);
        StringBuilder withHex = new StringBuilder();
        int last = 0;
        while (hex.find()) {
            withHex.append(input, last, hex.start()).append("<#").append(hex.group(1)).append('>');
            last = hex.end();
        }
        withHex.append(input.substring(last));

        StringBuilder out = new StringBuilder(withHex.length());
        for (int i = 0; i < withHex.length(); i++) {
            char c = withHex.charAt(i);
            if (c == '&' && i + 1 < withHex.length()) {
                String tag = legacyTag(Character.toLowerCase(withHex.charAt(i + 1)));
                if (tag != null) { out.append(tag); i++; continue; }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String legacyTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underlined>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        stack.editMeta(meta -> {
            meta.displayName(render(name).decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore)
                    .map(l -> render(l).decoration(TextDecoration.ITALIC, false))
                    .toList());
            }
        });
        return stack;
    }

    private static ItemStack toggle(boolean enabled, String label) {
        Material mat  = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String   name = (enabled ? "<green>" : "<red>") + label + ": " + (enabled ? "<green>Enabled" : "<red>Disabled");
        return item(mat, name, "<dark_gray>Click to toggle");
    }

    private ItemStack glass() {
        return item(fillerMaterial(), " ");
    }

    private static ItemStack coloredGlass(Material mat) {
        return item(mat, " ");
    }

    private Material fillerMaterial() {
        Material m = Material.matchMaterial(menus.getString("filler", "GRAY_STAINED_GLASS_PANE"));
        return m != null ? m : Material.GRAY_STAINED_GLASS_PANE;
    }

    private String configTitle(String path, String fallback, UnaryOperator<String> replacer) {
        String raw = menus.getString(path, fallback);
        return replacer != null ? replacer.apply(raw) : raw;
    }

    private Material configMaterial(String path, Material fallback) {
        ConfigurationSection sec = menus.getConfigurationSection(path);
        if (sec == null) return fallback;
        Material m = Material.matchMaterial(sec.getString("material", fallback.name()));
        return m != null ? m : fallback;
    }

    private ItemStack configItem(String path, UnaryOperator<String> replacer) {
        ConfigurationSection sec = menus.getConfigurationSection(path);
        Material mat = Material.STONE;
        String name = " ";
        List<String> lore = new ArrayList<>();
        if (sec != null) {
            Material m = Material.matchMaterial(sec.getString("material", "STONE"));
            if (m != null) mat = m;
            name = sec.getString("displayname", " ");
            lore = new ArrayList<>(sec.getStringList("lore"));
        }
        if (replacer != null) {
            name = replacer.apply(name);
            lore.replaceAll(replacer);
        }
        return item(mat, name, lore.toArray(new String[0]));
    }

    private static String blank(String s) {
        return (s == null || s.isBlank()) ? "(none)" : s;
    }

    private static String formatTime(int seconds) {
        if (seconds <= 0) return "0s";
        int m = seconds / 60, s = seconds % 60;
        if (m > 0 && s > 0) return m + "m " + s + "s";
        return m > 0 ? m + "m" : s + "s";
    }

    private static int parseTime(String raw) {
        raw = raw.toLowerCase().trim();
        int total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) { num.append(c); }
            else if (c == 'm' && !num.isEmpty()) { total += Integer.parseInt(num.toString()) * 60; num.setLength(0); }
            else if (c == 's' && !num.isEmpty()) { total += Integer.parseInt(num.toString());      num.setLength(0); }
        }
        if (!num.isEmpty()) total += Integer.parseInt(num.toString());
        return total;
    }
}
