package ro.andreilarazboi.donutcore.arenas.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.format.TextDecoration;
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
import ro.andreilarazboi.donutcore.sell.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

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
            Utils.toComponent(menus.getString("list-menu.title", "&8&lZone Manager")));

        ItemStack glass = glass();
        border(inv, glass);

        inv.setItem(4, configItem("list-menu.title-icon", null));

        List<Arena> sorted = new ArrayList<>(manager.getArenas());
        sorted.sort(Comparator.comparing(Arena::getName));

        int start = page * 28;
        for (int i = 0; i < 28 && start + i < sorted.size(); i++) {
            Arena a = sorted.get(start + i);
            List<String> lore = new ArrayList<>(List.of(
                "&7Region: &f" + a.getRegion(),
                "&7World:  &f" + a.getWorldName(),
                "",
                "&7AFK Timer: " + (a.isAfkEnabled()
                    ? "&aEnabled &8| &f" + formatTime(a.getAfkIntervalSeconds())
                    : "&cDisabled"),
                "&7On Enter:  " + (a.getEnterMessageType().equals("none") && !a.isEnterCommandEnabled()
                    ? "&8Off" : "&aOn"),
                "&7On Exit:   " + (a.getExitMessageType().equals("none") && !a.isExitCommandEnabled()
                    ? "&8Off" : "&aOn"),
                "&7Triggers:  &f" + a.getTriggers().size(),
                "",
                "&8Click to manage"
            ));
            Material mat = configMaterial("list-menu.zone-item", Material.GRASS_BLOCK);
            inv.setItem(GRID_SLOTS[i], item(mat, "&e" + a.getName(), lore.toArray(new String[0])));
        }

        inv.setItem(49, configItem("list-menu.create-button", null));
        if (page > 0)
            inv.setItem(45, configItem("list-menu.prev-button", null));
        if (start + 28 < sorted.size())
            inv.setItem(53, configItem("list-menu.next-button", null));

        return inv;
    }

    // ── Zone Detail GUI builder ───────────────────────────────────────────────

    private Inventory buildDetailInventory(Arena arena) {
        UnaryOperator<String> zoneRepl = s -> s.replace("%zone%", arena.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("detail", arena.getName()), 54,
            Utils.toComponent(configTitle("detail-menu.title", "&8Zone: &e%zone%", zoneRepl)));

        ItemStack glass = glass();

        // Row 0: navigation header
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        inv.setItem(0, configItem("detail-menu.back-button", null));
        inv.setItem(4, item(Material.EMERALD, "&e&l" + arena.getName(),
            "&7Region: &f" + arena.getRegion(),
            "&7World:  &f" + arena.getWorldName()));
        inv.setItem(8, configItem("detail-menu.delete-button", null));

        // Row 1: AFK Timer
        inv.setItem(9,  item(Material.CLOCK, "&6&lAFK Timer",
            "&7Reward players for staying in this zone",
            "&8(requires interval + command to be set)"));
        inv.setItem(10, toggle(arena.isAfkEnabled(), "AFK Timer"));
        inv.setItem(11, item(Material.GOLD_NUGGET, "&eInterval",
            "&7Current: &f" + formatTime(arena.getAfkIntervalSeconds()),
            "&8Click to change  &7|  Format: &f30s&7, &f1m&7, &f2m30s"));
        inv.setItem(12, item(Material.COMMAND_BLOCK, "&eReward Command",
            "&7Current: &f" + blank(arena.getAfkCommand()),
            "&8Click to change  &7|  Use &f{player}"));
        inv.setItem(13, item(Material.OAK_SIGN, "&eDisplay Type",
            "&7Current: &f" + arena.getAfkDisplayType(),
            "&8Click to cycle: actionbar → title → chat → all"));
        inv.setItem(14, item(Material.PAPER, "&eActionbar Text",
            "&7" + arena.getAfkActionbarText(),
            "&8Click to change  &7|  Use &f{time}"));
        inv.setItem(15, item(Material.ENCHANTED_BOOK, "&eTitle Text",
            "&7" + arena.getAfkTitleText(),
            "&8Click to change  &7|  Use &f{time}"));
        inv.setItem(16, item(Material.BOOK, "&eSubtitle Text",
            "&7" + arena.getAfkSubtitleText(),
            "&8Click to change  &7|  Use &f{time}"));
        inv.setItem(17, item(Material.FEATHER, "&eChat Text",
            "&7" + arena.getAfkChatText(),
            "&8Click to change  &7|  Use &f{time}",
            "&8Shown only at reward milestones"));

        // Row 2: separator + Triggers button
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);
        UnaryOperator<String> countRepl = s -> s.replace("%count%", String.valueOf(arena.getTriggers().size()));
        inv.setItem(22, configItem("detail-menu.triggers-button", countRepl));

        // Row 3: Enter action
        inv.setItem(27, item(Material.LIME_DYE, "&a&lEnter Action",
            "&7Triggers when a player enters this zone"));
        inv.setItem(28, item(Material.OAK_SIGN, "&aMessage Type",
            "&7Current: &f" + arena.getEnterMessageType(),
            "&8Click to cycle: none → chat → actionbar → title"));
        inv.setItem(29, item(Material.PAPER, "&aMessage Text",
            "&7" + arena.getEnterMessage(),
            "&8Click to change  &7|  Use &f{zone}"));
        if (arena.getEnterMessageType().equals("title")) {
            inv.setItem(30, item(Material.ENCHANTED_BOOK, "&aTitle Text",
                "&7" + arena.getEnterTitle(),
                "&8Click to change  &7|  Use &f{zone}"));
            inv.setItem(31, item(Material.BOOK, "&aSubtitle Text",
                "&7" + arena.getEnterSubtitle(),
                "&8Click to change  &7|  Use &f{zone}"));
        } else {
            inv.setItem(30, glass);
            inv.setItem(31, glass);
        }
        inv.setItem(32, glass);
        inv.setItem(33, toggle(arena.isEnterCommandEnabled(), "Enter Command"));
        inv.setItem(34, arena.isEnterCommandEnabled()
            ? item(Material.COMMAND_BLOCK, "&aEnter Command",
                "&7Current: &f" + blank(arena.getEnterCommand()),
                "&8Click to change  &7|  Use &f{player}&7, &f{zone}")
            : glass);
        inv.setItem(35, glass);

        // Row 4: Exit action
        inv.setItem(36, item(Material.RED_DYE, "&c&lExit Action",
            "&7Triggers when a player leaves this zone"));
        inv.setItem(37, item(Material.OAK_SIGN, "&cMessage Type",
            "&7Current: &f" + arena.getExitMessageType(),
            "&8Click to cycle: none → chat → actionbar → title"));
        inv.setItem(38, item(Material.PAPER, "&cMessage Text",
            "&7" + arena.getExitMessage(),
            "&8Click to change  &7|  Use &f{zone}"));
        if (arena.getExitMessageType().equals("title")) {
            inv.setItem(39, item(Material.ENCHANTED_BOOK, "&cTitle Text",
                "&7" + arena.getExitTitle(),
                "&8Click to change  &7|  Use &f{zone}"));
            inv.setItem(40, item(Material.BOOK, "&cSubtitle Text",
                "&7" + arena.getExitSubtitle(),
                "&8Click to change  &7|  Use &f{zone}"));
        } else {
            inv.setItem(39, glass);
            inv.setItem(40, glass);
        }
        inv.setItem(41, glass);
        inv.setItem(42, toggle(arena.isExitCommandEnabled(), "Exit Command"));
        inv.setItem(43, arena.isExitCommandEnabled()
            ? item(Material.COMMAND_BLOCK, "&cExit Command",
                "&7Current: &f" + blank(arena.getExitCommand()),
                "&8Click to change  &7|  Use &f{player}&7, &f{zone}")
            : glass);
        inv.setItem(44, glass);

        // Row 5: footer
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        inv.setItem(45, configItem("detail-menu.back-button", null));
        inv.setItem(49, configItem("detail-menu.autosave-note", null));
        inv.setItem(53, configItem("detail-menu.delete-button", null));

        return inv;
    }

    // ── Trigger List GUI builder ──────────────────────────────────────────────

    private Inventory buildTriggerListInventory(Arena arena, int page) {
        UnaryOperator<String> zoneRepl = s -> s.replace("%zone%", arena.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("trigger-list", arena.getName()), 54,
            Utils.toComponent(configTitle("trigger-list-menu.title", "&8Triggers: &e%zone%", zoneRepl)));

        ItemStack glass = glass();
        border(inv, glass);

        inv.setItem(4, configItem("trigger-list-menu.title-icon", zoneRepl));

        List<Trigger> sorted = new ArrayList<>(arena.getTriggers());
        sorted.sort(Comparator.comparing(Trigger::getName));

        int start = page * 28;
        for (int i = 0; i < 28 && start + i < sorted.size(); i++) {
            Trigger t = sorted.get(start + i);
            List<String> lore = List.of(
                "&7Region: &f" + t.getRegion(),
                "&7Status: " + (t.isEnabled() ? "&aEnabled" : "&cDisabled"),
                "",
                "&7Command:  " + (t.isCommandEnabled() ? "&aOn" : "&8Off"),
                "&7Teleport: " + (t.isTeleportEnabled() ? "&aOn" : "&8Off"),
                "&7Message:  " + (t.getMessageType().equals("none") ? "&8Off" : "&aOn"),
                "",
                "&8Click to manage"
            );
            Material mat = configMaterial("trigger-list-menu.trigger-item", Material.COMPASS);
            inv.setItem(GRID_SLOTS[i], item(mat, "&b" + t.getName(), lore.toArray(new String[0])));
        }

        inv.setItem(45, configItem("trigger-list-menu.back-button", null));
        inv.setItem(49, configItem("trigger-list-menu.create-button", null));
        if (page > 0)
            inv.setItem(47, item(Material.ARROW, "&7← Previous"));
        if (start + 28 < sorted.size())
            inv.setItem(51, item(Material.ARROW, "&7Next →"));

        return inv;
    }

    // ── Trigger Detail GUI builder ────────────────────────────────────────────

    private Inventory buildTriggerDetailInventory(Arena arena, Trigger trigger) {
        UnaryOperator<String> trigRepl = s -> s.replace("%trigger%", trigger.getName());

        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("trigger-detail", arena.getName(), trigger.getName()), 54,
            Utils.toComponent(configTitle("trigger-detail-menu.title", "&8Trigger: &e%trigger%", trigRepl)));

        ItemStack glass = glass();
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        inv.setItem(0, configItem("trigger-detail-menu.back-button", null));
        inv.setItem(4, item(Material.COMPASS, "&b&l" + trigger.getName(),
            "&7Region: &f" + trigger.getRegion(),
            "&7Status: " + (trigger.isEnabled() ? "&aEnabled" : "&cDisabled"),
            "&8Click to toggle"));
        inv.setItem(8, configItem("trigger-detail-menu.delete-button", null));

        // Row 1: Command action
        inv.setItem(9,  item(Material.COMMAND_BLOCK, "&6&lCommand Action",
            "&7Run a console command when entered"));
        inv.setItem(10, toggle(trigger.isCommandEnabled(), "Run Command"));
        inv.setItem(11, item(Material.PAPER, "&eCommand",
            "&7Current: &f" + blank(trigger.getCommand()),
            "&8Click to change  &7|  Use &f{player}&7, &f{zone}&7, &f{trigger}"));

        for (int i = 12; i < 18; i++) inv.setItem(i, glass);

        // Row 2: Teleport action
        inv.setItem(18, item(Material.ENDER_PEARL, "&d&lTeleport Action",
            "&7Teleport the player to a fixed location"));
        inv.setItem(19, toggle(trigger.isTeleportEnabled(), "Teleport"));
        inv.setItem(20, configItem("trigger-detail-menu.set-location-button", null));
        inv.setItem(21, item(Material.MAP, "&dCurrent Location",
            trigger.hasTeleportLocation()
                ? "&7" + trigger.getTeleportWorld() + " &8@ &7"
                    + Math.round(trigger.getTeleportX()) + ", "
                    + Math.round(trigger.getTeleportY()) + ", "
                    + Math.round(trigger.getTeleportZ())
                : "&8(not set)"));

        for (int i = 22; i < 27; i++) inv.setItem(i, glass);

        // Row 3: Message on enter
        inv.setItem(27, item(Material.OAK_SIGN, "&b&lMessage on Enter",
            "&7Shown to the player when they enter"));
        inv.setItem(28, item(Material.OAK_SIGN, "&bMessage Type",
            "&7Current: &f" + trigger.getMessageType(),
            "&8Click to cycle: none → chat → actionbar → title"));
        inv.setItem(29, item(Material.PAPER, "&bMessage Text",
            "&7" + blank(trigger.getMessage()),
            "&8Click to change  &7|  Use &f{zone}&7, &f{trigger}"));
        if (trigger.getMessageType().equals("title")) {
            inv.setItem(30, item(Material.ENCHANTED_BOOK, "&bTitle Text",
                "&7" + blank(trigger.getTitle()),
                "&8Click to change  &7|  Use &f{zone}&7, &f{trigger}"));
            inv.setItem(31, item(Material.BOOK, "&bSubtitle Text",
                "&7" + blank(trigger.getSubtitle()),
                "&8Click to change  &7|  Use &f{zone}&7, &f{trigger}"));
        } else {
            inv.setItem(30, glass);
            inv.setItem(31, glass);
        }
        for (int i = 32; i < 36; i++) inv.setItem(i, glass);

        for (int i = 36; i < 45; i++) inv.setItem(i, glass);

        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        inv.setItem(45, configItem("trigger-detail-menu.back-button", null));
        inv.setItem(53, configItem("trigger-detail-menu.delete-button", null));

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
        if (clicked.getType() == fillerMaterial()) return;

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
            case 0, 45 -> openListGUI(player);
            case 8, 53 -> promptDeleteZone(player, arena);
            case 22 -> { triggerPage.put(player.getUniqueId(), 0); openTriggerListGUI(player, arena); }

            // AFK Timer
            case 10 -> { arena.setAfkEnabled(!arena.isAfkEnabled()); saveZone(player, arena); }
            case 11 -> prompt(player, arenaName, InputType.AFk_INTERVAL,
                "&aType the AFK reward interval &7(e.g. &f30s&7, &f1m&7, &f2m30s&7):");
            case 12 -> prompt(player, arenaName, InputType.AFk_COMMAND,
                "&aType the reward command &7(use &f{player}&7):");
            case 13 -> { cycleAfkDisplay(arena); saveZone(player, arena); }
            case 14 -> prompt(player, arenaName, InputType.AFk_ACTIONBAR,
                "&aType the actionbar text &7(use &f{time}&7):");
            case 15 -> prompt(player, arenaName, InputType.AFk_TITLE,
                "&aType the title text &7(use &f{time}&7):");
            case 16 -> prompt(player, arenaName, InputType.AFk_SUBTITLE,
                "&aType the subtitle text &7(use &f{time}&7):");
            case 17 -> prompt(player, arenaName, InputType.AFk_CHAT,
                "&aType the chat text &7(use &f{time}&7):");

            // Enter action
            case 28 -> { cycleMessageType(arena, true); saveZone(player, arena); }
            case 29 -> prompt(player, arenaName, InputType.ENTER_MESSAGE,
                "&aType the enter message &7(use &f{zone}&7):");
            case 30 -> prompt(player, arenaName, InputType.ENTER_TITLE,
                "&aType the enter title &7(use &f{zone}&7):");
            case 31 -> prompt(player, arenaName, InputType.ENTER_SUBTITLE,
                "&aType the enter subtitle &7(use &f{zone}&7):");
            case 33 -> { arena.setEnterCommandEnabled(!arena.isEnterCommandEnabled()); saveZone(player, arena); }
            case 34 -> prompt(player, arenaName, InputType.ENTER_COMMAND,
                "&aType the enter command &7(use &f{player}&7, &f{zone}&7):");

            // Exit action
            case 37 -> { cycleMessageType(arena, false); saveZone(player, arena); }
            case 38 -> prompt(player, arenaName, InputType.EXIT_MESSAGE,
                "&aType the exit message &7(use &f{zone}&7):");
            case 39 -> prompt(player, arenaName, InputType.EXIT_TITLE,
                "&aType the exit title &7(use &f{zone}&7):");
            case 40 -> prompt(player, arenaName, InputType.EXIT_SUBTITLE,
                "&aType the exit subtitle &7(use &f{zone}&7):");
            case 42 -> { arena.setExitCommandEnabled(!arena.isExitCommandEnabled()); saveZone(player, arena); }
            case 43 -> prompt(player, arenaName, InputType.EXIT_COMMAND,
                "&aType the exit command &7(use &f{player}&7, &f{zone}&7):");
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
            case 0, 45 -> openTriggerListGUI(player, arena);
            case 8, 53 -> promptDeleteTrigger(player, arena, trigger);
            case 4 -> { trigger.setEnabled(!trigger.isEnabled()); saveTrigger(player, arena, trigger); }

            case 10 -> { trigger.setCommandEnabled(!trigger.isCommandEnabled()); saveTrigger(player, arena, trigger); }
            case 11 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_COMMAND,
                "&aType the trigger command &7(use &f{player}&7, &f{zone}&7, &f{trigger}&7):");

            case 19 -> { trigger.setTeleportEnabled(!trigger.isTeleportEnabled()); saveTrigger(player, arena, trigger); }
            case 20 -> {
                Location loc = player.getLocation();
                trigger.setTeleportWorld(loc.getWorld().getName());
                trigger.setTeleportX(loc.getX());
                trigger.setTeleportY(loc.getY());
                trigger.setTeleportZ(loc.getZ());
                trigger.setTeleportYaw(loc.getYaw());
                trigger.setTeleportPitch(loc.getPitch());
                tell(player, "&aTeleport location set to your current position.");
                saveTrigger(player, arena, trigger);
            }

            case 28 -> { cycleTriggerMessageType(trigger); saveTrigger(player, arena, trigger); }
            case 29 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_MESSAGE,
                "&aType the trigger message &7(use &f{zone}&7, &f{trigger}&7):");
            case 30 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_TITLE,
                "&aType the trigger title &7(use &f{zone}&7, &f{trigger}&7):");
            case 31 -> promptTrigger(player, arena, trigger, InputType.TRIGGER_SUBTITLE,
                "&aType the trigger subtitle &7(use &f{zone}&7, &f{trigger}&7):");
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
                    tell(player, "&cInvalid name. Type a zone name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
                    return;
                }
                if (manager.getArena(name) != null) {
                    tell(player, "&cZone '&4" + name + "&c' already exists. Choose a different name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
                    return;
                }
                tell(player, "&aZone name: &2" + name + "&a. Now type the WorldGuard region name:");
                tell(player, "&8(Region must already exist in your current world)");
                pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, null, name));
            }

            case CREATE_ZONE_REGION -> {
                String name   = p.extra();
                String region = text.toLowerCase();
                String world  = player.getWorld().getName();
                if (!manager.regionExists(world, region)) {
                    tell(player, "&cRegion '&4" + region + "&c' not found in world '&4" + world + "&c'. Try again:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, null, name));
                    return;
                }
                Arena arena = new Arena(name, region, world);
                manager.addArena(arena);
                tell(player, "&aZone '&2" + name + "&a' created!");
                openDetailGUI(player, arena);
            }

            case CONFIRM_DELETE_ZONE -> {
                if (text.equalsIgnoreCase("confirm")) {
                    manager.removeArena(p.arenaName());
                    tell(player, "&aZone '&2" + p.arenaName() + "&a' deleted.");
                    openListGUI(player);
                } else {
                    tell(player, "&cType &4confirm &cor &4cancel&c:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_ZONE, p.arenaName(), null, null));
                }
            }

            case CREATE_TRIGGER_NAME -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }
                String name = text.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                if (name.isBlank() || arena.getTrigger(name) != null) {
                    tell(player, "&cInvalid or duplicate name. Type a trigger name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_NAME, arena.getName(), null, null));
                    return;
                }
                tell(player, "&aTrigger name: &2" + name + "&a. Now type the WorldGuard region name:");
                tell(player, "&8(Region must already exist in this zone's world: " + arena.getWorldName() + ")");
                pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_REGION, arena.getName(), null, name));
            }

            case CREATE_TRIGGER_REGION -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }
                String name   = p.extra();
                String region = text.toLowerCase();
                if (!manager.regionExists(arena.getWorldName(), region)) {
                    tell(player, "&cRegion '&4" + region + "&c' not found in world '&4" + arena.getWorldName() + "&c'. Try again:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_REGION, arena.getName(), null, name));
                    return;
                }
                Trigger trigger = new Trigger(name, region);
                arena.addTrigger(trigger);
                manager.save();
                tell(player, "&aTrigger '&2" + name + "&a' created!");
                openTriggerDetailGUI(player, arena, trigger);
            }

            case CONFIRM_DELETE_TRIGGER -> {
                Arena arena = manager.getArena(p.arenaName());
                if (text.equalsIgnoreCase("confirm")) {
                    if (arena != null) {
                        arena.removeTrigger(p.triggerName());
                        manager.save();
                    }
                    tell(player, "&aTrigger '&2" + p.triggerName() + "&a' deleted.");
                    if (arena != null) openTriggerListGUI(player, arena); else openListGUI(player);
                } else {
                    tell(player, "&cType &4confirm &cor &4cancel&c:");
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
        tell(player, "&8Type &ccancel &8to discard changes.");
        pending.put(player.getUniqueId(), new PendingInput(type, arenaName, null, null));
    }

    private void promptTrigger(Player player, Arena arena, Trigger trigger, InputType type, String message) {
        player.closeInventory();
        tell(player, message);
        tell(player, "&8Type &ccancel &8to discard changes.");
        pending.put(player.getUniqueId(), new PendingInput(type, arena.getName(), trigger.getName(), null));
    }

    private void promptCreateZone(Player player) {
        player.closeInventory();
        tell(player, "&aType the zone name in chat:");
        tell(player, "&8Type &ccancel &8to discard.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null, null));
    }

    private void promptDeleteZone(Player player, Arena arena) {
        player.closeInventory();
        tell(player, "&cType &4confirm &cto permanently delete zone '&4" + arena.getName() + "&c'.");
        tell(player, "&8Type &ccancel &8to go back.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE_ZONE, arena.getName(), null, null));
    }

    private void promptCreateTrigger(Player player, Arena arena) {
        player.closeInventory();
        tell(player, "&aType the trigger name in chat:");
        tell(player, "&8Type &ccancel &8to discard.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_TRIGGER_NAME, arena.getName(), null, null));
    }

    private void promptDeleteTrigger(Player player, Arena arena, Trigger trigger) {
        player.closeInventory();
        tell(player, "&cType &4confirm &cto permanently delete trigger '&4" + trigger.getName() + "&c'.");
        tell(player, "&8Type &ccancel &8to go back.");
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
        player.sendMessage(Utils.toComponent(menus.getString("prefix", "&8&l[&aArena&8&l] &r") + msg));
    }

    private void border(Inventory inv, ItemStack glass) {
        for (int i = 0; i <  9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }
    }

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        stack.editMeta(meta -> {
            meta.displayName(Utils.toComponent(name).decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore)
                    .map(l -> Utils.toComponent(l).decoration(TextDecoration.ITALIC, false))
                    .toList());
            }
        });
        return stack;
    }

    private static ItemStack toggle(boolean enabled, String label) {
        Material mat  = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String   name = (enabled ? "&a" : "&c") + label + ": " + (enabled ? "&aEnabled" : "&cDisabled");
        return item(mat, name, "&8Click to toggle");
    }

    private ItemStack glass() {
        return item(fillerMaterial(), " ");
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
