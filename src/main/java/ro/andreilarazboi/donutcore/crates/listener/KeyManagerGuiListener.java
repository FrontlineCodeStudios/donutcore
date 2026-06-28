
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
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

public class KeyManagerGuiListener
implements Listener {
    private final DonutCrates plugin;
    private final NamespacedKey keyListIdTag;

    public KeyManagerGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
        this.keyListIdTag = new NamespacedKey((Plugin)plugin.getPlugin(), "key_id");
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
        String title = Utils.stripColor(e.getView().title());
        if (!title.equals("Key Manager")) {
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
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (slot == 45) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiRootEditor.build());
            return;
        }
        if (clicked.getType() != Material.BLACK_STAINED_GLASS_PANE && clicked.hasItemMeta()) {
            String name;
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) {
                return;
            }
            String keyId = meta.getPersistentDataContainer().get(this.keyListIdTag, PersistentDataType.STRING);
            if ((keyId == null || keyId.isBlank()) && meta.hasDisplayName() && (name = Utils.stripColor(meta.displayName())).startsWith("Key: ")) {
                keyId = name.substring("Key: ".length()).trim();
            }
            if (keyId != null && !keyId.isBlank()) {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiKeyEditor.build(keyId));
            }
        }
    }
}

