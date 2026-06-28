
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class KeyUtil {
    private final DonutCrates plugin;
    private final NamespacedKey keyTag;

    public KeyUtil(DonutCrates plugin) {
        this.plugin = plugin;
        this.keyTag = new NamespacedKey((Plugin)plugin.getPlugin(), "crate_key");
    }

    public boolean hasPhysicalKey(Player p, String keyId) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (!this.isKeyItem(item, keyId)) continue;
            return true;
        }
        return false;
    }

    public boolean consumePhysicalKey(Player p, String keyId) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); ++i) {
            ItemStack item = inv.getItem(i);
            if (!this.isKeyItem(item, keyId)) continue;
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                inv.setItem(i, null);
            }
            return true;
        }
        return false;
    }

    private boolean isKeyItem(ItemStack item, String keyId) {
        ItemStack template;
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        String tagged = meta.getPersistentDataContainer().get(this.keyTag, PersistentDataType.STRING);
        if (tagged != null) {
            return tagged.equalsIgnoreCase(keyId);
        }
        String base = "keys." + keyId;
        if (this.plugin.cfg.saves.isItemStack(base + ".item") && (template = this.plugin.cfg.saves.getItemStack(base + ".item")) != null) {
            ItemStack a = item.clone();
            a.setAmount(1);
            ItemStack b = template.clone();
            b.setAmount(1);
            return a.isSimilar(b);
        }
        return false;
    }
}

