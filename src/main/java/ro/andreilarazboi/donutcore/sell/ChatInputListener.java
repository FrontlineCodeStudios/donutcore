package ro.andreilarazboi.donutcore.sell;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener
implements Listener {
    private final DonutSell plugin;

    public ChatInputListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!this.plugin.isModuleActive()) return;
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (this.plugin.getAdminPriceEditorMenu().isAwaitingPriceInput(uuid)) {
            e.setCancelled(true);
            e.getRecipients().clear();
            String input = e.getMessage().trim();
            this.plugin.runAtPlayer(p, () -> this.plugin.getAdminPriceEditorMenu().handlePriceChat(p, input));
            return;
        }
        ViewTracker vt = this.plugin.getViewTracker();
        String worthFilter = vt.getFilter(uuid);
        if (worthFilter != null && worthFilter.isEmpty()) {
            e.setCancelled(true);
            e.getRecipients().clear();
            String input = e.getMessage().trim();
            vt.setFilter(uuid, input);
            this.plugin.runAtPlayer(p, () -> this.plugin.getItemPricesMenu().open(p, 1));
        }
    }
}

