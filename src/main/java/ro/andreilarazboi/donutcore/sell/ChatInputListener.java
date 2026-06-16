package ro.andreilarazboi.donutcore.sell;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatInputListener implements Listener {
    private final DonutSell plugin;

    public ChatInputListener(DonutSell plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (this.plugin.getAdminPriceEditorMenu().isAwaitingPriceInput(uuid)) {
            e.setCancelled(true);
            String input = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
            this.plugin.runAtPlayer(p, () -> this.plugin.getAdminPriceEditorMenu().handlePriceChat(p, input));
            return;
        }
        ViewTracker vt = this.plugin.getViewTracker();
        String worthFilter = vt.getFilter(uuid);
        if (worthFilter != null && worthFilter.isEmpty()) {
            e.setCancelled(true);
            String input = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
            vt.setFilter(uuid, input);
            this.plugin.runAtPlayer(p, () -> this.plugin.getItemPricesMenu().open(p, 1));
        }
    }
}
