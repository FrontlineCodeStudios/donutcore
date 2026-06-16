package ro.andreilarazboi.donutcore.arenas.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import ro.andreilarazboi.donutcore.sell.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaGUIListener implements Listener {

    // ── Zone slot layout ──────────────────────────────────────────────────────
    // 6-row inventory: border on top/bottom rows and col 0 / col 8
    // Inner area (rows 1-4, cols 1-7) = 28 zone slots per page
    private static final int[] ZONE_SLOTS;
    static {
        ZONE_SLOTS = new int[28];
        int i = 0;
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                ZONE_SLOTS[i++] = row * 9 + col;
    }

    enum InputType {
        CREATE_ZONE_NAME, CREATE_ZONE_REGION, CONFIRM_DELETE,
        AFk_INTERVAL, AFk_COMMAND, AFk_ACTIONBAR, AFk_TITLE, AFk_SUBTITLE, AFk_CHAT,
        ENTER_MESSAGE, ENTER_TITLE, ENTER_SUBTITLE, ENTER_COMMAND,
        EXIT_MESSAGE,  EXIT_TITLE,  EXIT_SUBTITLE,  EXIT_COMMAND,
    }

    record PendingInput(InputType type, String arenaName, String extra) {}

    private final DonutCore   plugin;
    private final ArenaManager manager;
    private final Map<UUID, PendingInput> pending    = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>      playerPage = new ConcurrentHashMap<>();

    public ArenaGUIListener(DonutCore plugin, ArenaManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // ── Public open methods ───────────────────────────────────────────────────

    public void openListGUI(Player player) {
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        player.openInventory(buildListInventory(page));
    }

    public void openDetailGUI(Player player, Arena arena) {
        player.openInventory(buildDetailInventory(arena));
    }

    // ── List GUI builder ──────────────────────────────────────────────────────

    private Inventory buildListInventory(int page) {
        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("list", null), 54,
            Utils.toComponent("&8&lZone Manager"));

        ItemStack glass = glass();

        // Border: top row, bottom row, left/right columns
        for (int i = 0; i <  9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }

        inv.setItem(4, item(Material.BEACON, "&8&lZone Manager",
            "&7Manage all WorldGuard-based arena zones",
            "&8Click a zone to edit its settings"));

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
                "",
                "&8Click to manage"
            ));
            inv.setItem(ZONE_SLOTS[i], item(Material.GRASS_BLOCK, "&e" + a.getName(),
                lore.toArray(new String[0])));
        }

        // Create Zone button
        inv.setItem(49, item(Material.LIME_DYE, "&a&l+ Create Zone",
            "&7Click to create a new arena zone",
            "&7You will need an existing WorldGuard region"));

        // Page navigation
        if (page > 0)
            inv.setItem(45, item(Material.ARROW, "&7← Previous", "&8Page " + page));
        if (start + 28 < sorted.size())
            inv.setItem(53, item(Material.ARROW, "&7Next →", "&8Page " + (page + 2)));

        return inv;
    }

    // ── Detail GUI builder ────────────────────────────────────────────────────

    private Inventory buildDetailInventory(Arena arena) {
        Inventory inv = Bukkit.createInventory(
            new ArenaGuiHolder("detail", arena.getName()), 54,
            Utils.toComponent("&8Zone: &e" + arena.getName()));

        ItemStack glass = glass();

        // Row 0: navigation header
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        inv.setItem(0, item(Material.ARROW, "&7← Back", "&8Return to zone list"));
        inv.setItem(4, item(Material.EMERALD, "&e&l" + arena.getName(),
            "&7Region: &f" + arena.getRegion(),
            "&7World:  &f" + arena.getWorldName()));
        inv.setItem(8, item(Material.RED_WOOL, "&c&lDelete Zone",
            "&7Click to permanently delete this zone"));

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

        // Row 2: separator
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);

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
        inv.setItem(45, item(Material.ARROW, "&7← Back", "&8Return to zone list"));
        inv.setItem(49, item(Material.GREEN_DYE, "&aAuto-Save",
            "&7All changes are saved automatically"));
        inv.setItem(53, item(Material.RED_WOOL, "&c&lDelete Zone",
            "&7Click to permanently delete this zone"));

        return inv;
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArenaGuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Ignore clicks in the player's own inventory row at the bottom
        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (holder.getType().equals("list")) {
            handleListClick(player, holder, event.getRawSlot());
        } else {
            handleDetailClick(player, holder, event.getRawSlot());
        }
    }

    private void handleListClick(Player player, ArenaGuiHolder holder, int slot) {
        int page  = playerPage.getOrDefault(player.getUniqueId(), 0);
        List<Arena> sorted = new ArrayList<>(manager.getArenas());
        sorted.sort(Comparator.comparing(Arena::getName));

        // Zone item?
        for (int i = 0; i < ZONE_SLOTS.length; i++) {
            if (slot == ZONE_SLOTS[i]) {
                int idx = page * 28 + i;
                if (idx < sorted.size()) openDetailGUI(player, sorted.get(idx));
                return;
            }
        }

        switch (slot) {
            case 49 -> promptCreate(player);
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
        Arena  arena     = manager.getArena(arenaName);
        if (arena == null) { openListGUI(player); return; }

        switch (slot) {
            // Navigation
            case 0, 45 -> openListGUI(player);
            case 8, 53 -> promptDelete(player, arena);

            // AFK Timer
            case 10 -> { arena.setAfkEnabled(!arena.isAfkEnabled()); save(player, arena); }
            case 11 -> prompt(player, arenaName, InputType.AFk_INTERVAL,
                "&aType the AFK reward interval &7(e.g. &f30s&7, &f1m&7, &f2m30s&7):");
            case 12 -> prompt(player, arenaName, InputType.AFk_COMMAND,
                "&aType the reward command &7(use &f{player}&7):");
            case 13 -> { cycleAfkDisplay(arena); save(player, arena); }
            case 14 -> prompt(player, arenaName, InputType.AFk_ACTIONBAR,
                "&aType the actionbar text &7(use &f{time}&7):");
            case 15 -> prompt(player, arenaName, InputType.AFk_TITLE,
                "&aType the title text &7(use &f{time}&7):");
            case 16 -> prompt(player, arenaName, InputType.AFk_SUBTITLE,
                "&aType the subtitle text &7(use &f{time}&7):");
            case 17 -> prompt(player, arenaName, InputType.AFk_CHAT,
                "&aType the chat text &7(use &f{time}&7):");

            // Enter action
            case 28 -> { cycleMessageType(arena, true); save(player, arena); }
            case 29 -> prompt(player, arenaName, InputType.ENTER_MESSAGE,
                "&aType the enter message &7(use &f{zone}&7):");
            case 30 -> prompt(player, arenaName, InputType.ENTER_TITLE,
                "&aType the enter title &7(use &f{zone}&7):");
            case 31 -> prompt(player, arenaName, InputType.ENTER_SUBTITLE,
                "&aType the enter subtitle &7(use &f{zone}&7):");
            case 33 -> { arena.setEnterCommandEnabled(!arena.isEnterCommandEnabled()); save(player, arena); }
            case 34 -> prompt(player, arenaName, InputType.ENTER_COMMAND,
                "&aType the enter command &7(use &f{player}&7, &f{zone}&7):");

            // Exit action
            case 37 -> { cycleMessageType(arena, false); save(player, arena); }
            case 38 -> prompt(player, arenaName, InputType.EXIT_MESSAGE,
                "&aType the exit message &7(use &f{zone}&7):");
            case 39 -> prompt(player, arenaName, InputType.EXIT_TITLE,
                "&aType the exit title &7(use &f{zone}&7):");
            case 40 -> prompt(player, arenaName, InputType.EXIT_SUBTITLE,
                "&aType the exit subtitle &7(use &f{zone}&7):");
            case 42 -> { arena.setExitCommandEnabled(!arena.isExitCommandEnabled()); save(player, arena); }
            case 43 -> prompt(player, arenaName, InputType.EXIT_COMMAND,
                "&aType the exit command &7(use &f{player}&7, &f{zone}&7):");
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
    }

    private void handleInput(Player player, PendingInput p, String text) {
        switch (p.type()) {

            case CREATE_ZONE_NAME -> {
                String name = text.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                if (name.isBlank()) {
                    tell(player, "&cInvalid name. Type a zone name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null));
                    return;
                }
                if (manager.getArena(name) != null) {
                    tell(player, "&cZone '&4" + name + "&c' already exists. Choose a different name:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null));
                    return;
                }
                tell(player, "&aZone name: &2" + name + "&a. Now type the WorldGuard region name:");
                tell(player, "&8(Region must already exist in your current world)");
                pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, name));
            }

            case CREATE_ZONE_REGION -> {
                String name   = p.extra();
                String region = text.toLowerCase();
                String world  = player.getWorld().getName();
                if (!manager.regionExists(world, region)) {
                    tell(player, "&cRegion '&4" + region + "&c' not found in world '&4" + world + "&c'. Try again:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_REGION, null, name));
                    return;
                }
                Arena arena = new Arena(name, region, world);
                manager.addArena(arena);
                tell(player, "&aZone '&2" + name + "&a' created!");
                openDetailGUI(player, arena);
            }

            case CONFIRM_DELETE -> {
                if (text.equalsIgnoreCase("confirm")) {
                    manager.removeArena(p.arenaName());
                    tell(player, "&aZone '&2" + p.arenaName() + "&a' deleted.");
                    openListGUI(player);
                } else {
                    tell(player, "&cType &4confirm &cor &4cancel&c:");
                    pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE, p.arenaName(), null));
                }
            }

            default -> {
                Arena arena = manager.getArena(p.arenaName());
                if (arena == null) { openListGUI(player); return; }
                applyInput(arena, p.type(), text);
                manager.save();
                openDetailGUI(player, arena);
            }
        }
    }

    private void cancelInput(Player player, PendingInput p) {
        if (p.type() == InputType.CREATE_ZONE_NAME || p.type() == InputType.CREATE_ZONE_REGION) {
            openListGUI(player); return;
        }
        Arena arena = manager.getArena(p.arenaName());
        if (arena != null) openDetailGUI(player, arena);
        else openListGUI(player);
    }

    private void applyInput(Arena arena, InputType type, String text) {
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void save(Player player, Arena arena) {
        manager.save();
        Bukkit.getScheduler().runTask(plugin, () -> openDetailGUI(player, arena));
    }

    private void prompt(Player player, String arenaName, InputType type, String message) {
        player.closeInventory();
        tell(player, message);
        tell(player, "&8Type &ccancel &8to discard changes.");
        pending.put(player.getUniqueId(), new PendingInput(type, arenaName, null));
    }

    private void promptCreate(Player player) {
        player.closeInventory();
        tell(player, "&aType the zone name in chat:");
        tell(player, "&8Type &ccancel &8to discard.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CREATE_ZONE_NAME, null, null));
    }

    private void promptDelete(Player player, Arena arena) {
        player.closeInventory();
        tell(player, "&cType &4confirm &cto permanently delete zone '&4" + arena.getName() + "&c'.");
        tell(player, "&8Type &ccancel &8to go back.");
        pending.put(player.getUniqueId(), new PendingInput(InputType.CONFIRM_DELETE, arena.getName(), null));
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

    private void tell(Player player, String msg) {
        player.sendMessage(Utils.toComponent("&8&l[&aArena&8&l] &r" + msg));
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

    private static ItemStack glass() {
        return item(Material.GRAY_STAINED_GLASS_PANE, " ");
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
