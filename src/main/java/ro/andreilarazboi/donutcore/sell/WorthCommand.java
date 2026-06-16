package ro.andreilarazboi.donutcore.sell;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ro.andreilarazboi.donutcore.DonutCore;

public class WorthCommand implements CommandExecutor, TabCompleter {
    private final DonutSell plugin;
    private final List<String> materialKeys;

    public WorthCommand(DonutSell plugin) {
        this.plugin = plugin;
        this.materialKeys = Arrays.stream(Material.values()).filter(Material::isItem).map(m -> m.name().toLowerCase()).sorted().collect(Collectors.toList());
        plugin.getCommand("worth").setExecutor(this);
        plugin.getCommand("worth").setTabCompleter(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!DonutCore.getInstance().getSellModule().isActive()) {
            if (sender.hasPermission("donutcore.admin") || sender.isOp()) {
                sender.sendMessage(Utils.toComponent("&cThe Sell module is currently disabled."));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.toComponent("&cOnly players can use /worth."));
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            ViewTracker vt = this.plugin.getViewTracker();
            vt.setFilter(p.getUniqueId(), null);
            this.plugin.getItemPricesMenu().open(p, 1);
            return true;
        }
        String joined = String.join("_", args).toUpperCase();
        Material mat = Material.matchMaterial(joined);
        if (mat == null || !mat.isItem()) {
            p.sendMessage(Utils.toComponent(this.plugin.getMessagesConfig().getString("messages.worth-invalid", "&cUnknown item: %input%").replace("%input%", String.join(" ", args))));
            return true;
        }
        ItemStack lookup = new ItemStack(mat);
        double baseValue = this.plugin.calculateItemWorth(lookup);
        double multiplier = 1.0;
        for (Map.Entry<String, List<String>> entry : this.plugin.categoryItems.entrySet()) {
            if (!entry.getValue().contains(mat.name())) continue;
            multiplier = this.plugin.getSellMultiplier(p.getUniqueId(), entry.getKey());
            break;
        }
        double finalValue = baseValue * multiplier;
        String pretty = this.prettify(mat);
        String amount = Utils.abbreviateNumber(finalValue);
        String template = this.plugin.getMessagesConfig().getString("messages.worth", "&e1 %item% is worth %amount%");
        String msg = Utils.formatColors(template.replace("%item%", pretty).replace("%amount%", amount).replace("%mult%", String.valueOf(multiplier)));
        p.sendActionBar(Utils.toComponent(msg));
        p.sendMessage(Utils.toComponent(msg));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length >= 1) {
            String pre = String.join("_", args).toLowerCase().replace(' ', '_');
            return this.materialKeys.stream().filter(key -> key.startsWith(pre)).map(key -> {
                Material m = Material.matchMaterial(key.toUpperCase());
                return m != null && m.isItem() ? this.prettify(m) : key.replace('_', ' ');
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String prettify(Material m) {
        return this.prettify(m.name());
    }

    private String prettify(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
