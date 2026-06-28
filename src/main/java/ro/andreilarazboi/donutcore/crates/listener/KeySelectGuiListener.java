
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.Locale;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class KeySelectGuiListener
implements Listener {
    private final DonutCrates plugin;
    private final NamespacedKey keySelectIdTag;

    public KeySelectGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
        this.keySelectIdTag = new NamespacedKey((Plugin)plugin.getPlugin(), "select_key_id");
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder)) {
            return;
        }
        String rawTitle = Utils.stripColor(e.getView().title());
        if (!rawTitle.endsWith(" Select Key")) {
            return;
        }
        if (!p.hasPermission("donutcrate.admin")) {
            return;
        }
        int topSize = top.getSize();
        if (e.getRawSlot() >= topSize) {
            return;
        }
        e.setCancelled(true);
        String crate = rawTitle.substring(0, rawTitle.length() - " Select Key".length());
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateSettings.build(crate));
            return;
        }
        if (!clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String keyId = meta.getPersistentDataContainer().get(this.keySelectIdTag, PersistentDataType.STRING);
        if (keyId == null || keyId.isBlank()) {
            return;
        }
        GuiUtil.playClick(this.plugin, p);
        this.plugin.cfg.crates.set("Crates." + crate + ".key", (Object)keyId.toLowerCase(Locale.ROOT));
        this.plugin.cfg.saveAll();
        this.plugin.msg(p, "&#0fe30fSelected key &f" + keyId + " &#0fe30ffor crate &f" + crate + "&#0fe30f.");
        p.openInventory(this.plugin.guiCrateSettings.build(crate));
    }
}

