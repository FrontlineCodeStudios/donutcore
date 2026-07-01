package ro.andreilarazboi.donutcore.hide;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /staffhide [FakeName] [SkinName|UUID] — staff camouflage.
 *
 * <ul>
 *   <li>{@code /staffhide FakePlayer Notch} — disguise as "FakePlayer" with Notch's skin</li>
 *   <li>{@code /staffhide FakePlayer} — disguise with custom name, no skin change</li>
 *   <li>{@code /staffhide} (no args) — deactivate camouflage and restore identity</li>
 * </ul>
 *
 * <p>Requires permission {@code donutcore.hide.staff}.</p>
 */
public class StaffHideCommand implements CommandExecutor, TabCompleter {

    private final HideManager manager;

    public StaffHideCommand(HideManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("donutcore.hide.staff")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // No arguments → toggle off (or show usage if not hidden)
        if (args.length == 0) {
            if (manager.isStaffHidden(player.getUniqueId())) {
                manager.staffShowSilent(player);
                player.sendMessage("§aStaff-hide §ldeactivated§r§a. Your true identity has been restored.");
            } else {
                player.sendMessage("§eUsage: §f/staffhide <FakeName> [SkinName|UUID]");
                player.sendMessage("§7Run §f/staffhide §7again (no args) to deactivate.");
            }
            return true;
        }

        String fakeName = args[0];

        // Validate fake name
        if (fakeName.length() > 16 || !fakeName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage("§cInvalid name: max §e16 §ccharacters, letters/numbers/underscores only.");
            return true;
        }

        String skinName = (args.length >= 2) ? args[1] : null;

        manager.staffHide(player, fakeName, skinName);

        player.sendMessage("§aStaff-hide §lactivated§r§a. Disguised as §e" + fakeName + "§a.");
        if (skinName != null) {
            player.sendMessage("§7→ Fetching skin from §f" + skinName + "§7...");
        } else {
            player.sendMessage("§7→ No skin specified — your current skin is kept.");
        }
        player.sendMessage("§7Run §f/staffhide §7(no args) to deactivate.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("donutcore.hide.staff")) return null;
        return switch (args.length) {
            case 1  -> List.of("FakePlayer", "RandomPlayer", "NormalUser");
            default -> null; // arg2 is a free-text skin name, no suggestions
        };
    }
}
