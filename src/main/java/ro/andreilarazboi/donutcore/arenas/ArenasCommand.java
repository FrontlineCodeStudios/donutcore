package ro.andreilarazboi.donutcore.arenas;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ro.andreilarazboi.donutcore.arenas.gui.ArenaGUIListener;
import ro.andreilarazboi.donutcore.sell.Utils;

public class ArenasCommand implements CommandExecutor {

    private static final String PERM = "donutcore.arenas.admin";

    private final ArenaGUIListener gui;

    public ArenasCommand(ArenaGUIListener gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission(PERM) && !player.isOp()) {
            player.sendMessage(Utils.toComponent("&cNo permission."));
            return true;
        }
        gui.openListGUI(player);
        return true;
    }
}
