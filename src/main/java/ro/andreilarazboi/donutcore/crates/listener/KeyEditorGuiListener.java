
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class KeyEditorGuiListener
implements Listener {
    private final DonutCrates plugin;

    public KeyEditorGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
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
        if (!rawTitle.endsWith(" Key Editor")) {
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
        String keyId = rawTitle.substring(0, rawTitle.length() - " Key Editor".length()).trim();
        int slot = e.getRawSlot();
        String base = "keys." + keyId;
        switch (slot) {
            case 18: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiKeyList.build());
                break;
            }
            case 10: {
                ItemStack cursor = e.getCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    return;
                }
                GuiUtil.playClick(this.plugin, p);
                ItemStack newItem = cursor.clone();
                newItem.setAmount(1);
                this.plugin.cfg.saves.set(base + ".item", (Object)newItem);
                this.plugin.cfg.saves.set(base + ".material", (Object)newItem.getType().name());
                if (newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName()) {
                    this.plugin.cfg.saves.set(base + ".displayname", (Object)Utils.stripColor(newItem.getItemMeta().displayName()));
                }
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, "&#FF073AUpdated key item for key &f" + keyId);
                p.openInventory(this.plugin.guiKeyEditor.build(keyId));
                break;
            }
            case 12: {
                GuiUtil.playClick(this.plugin, p);
                String current = this.plugin.cfg.saves.getString(base + ".displayname", keyId + " Key");
                String keyRef = keyId;
                Player playerRef = p;
                Utils.openSignInput(this.plugin.getPlugin(), p, List.of(Utils.stripColor(current), "\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191", "Key Name", ""), 1, value -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> {
                    if (value == null || value.trim().isEmpty()) {
                        this.plugin.msg(playerRef, "&#d61111Name change cancelled.");
                        playerRef.openInventory(this.plugin.guiKeyEditor.build(keyRef));
                        return;
                    }
                    this.plugin.cfg.saves.set("keys." + keyRef + ".displayname", (Object)value.trim());
                    this.plugin.cfg.saveAll();
                    this.plugin.msg(playerRef, "&#FF073AUpdated key name.");
                    playerRef.openInventory(this.plugin.guiKeyEditor.build(keyRef));
                }));
                break;
            }
            case 14: {
                GuiUtil.playClick(this.plugin, p);
                boolean virt = this.plugin.cfg.saves.getBoolean(base + ".virtual", true);
                this.plugin.cfg.saves.set(base + ".virtual", (Object)(!virt ? 1 : 0));
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, !virt ? "&#FF073AVirtual keys &fENABLED &7for key &f" + keyId : "&#d61111Virtual keys &fDISABLED &7for key &f" + keyId);
                p.openInventory(this.plugin.guiKeyEditor.build(keyId));
                break;
            }
            case 16: {
                GuiUtil.playClick(this.plugin, p);
                ItemStack keyItem = this.plugin.buildKeyItemById(keyId, 1);
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[]{keyItem});
                if (!left.isEmpty()) {
                    left.values().forEach(is -> p.getWorld().dropItemNaturally(p.getLocation(), is));
                }
                this.plugin.msg(p, "&#FF073AYou received one &f" + keyId + " &#FF073Akey item.");
                break;
            }
            case 26: {
                GuiUtil.playClick(this.plugin, p);
                UUID uid = p.getUniqueId();
                this.plugin.pendingDeleteType.put(uid, "KEY");
                this.plugin.pendingDeleteCrate.put(uid, keyId);
                this.plugin.pendingDeleteItemKey.remove(uid);
                ItemStack display = this.plugin.cfg.saves.isItemStack(base + ".item") ? this.plugin.cfg.saves.getItemStack(base + ".item").clone() : new ItemStack(Material.TRIPWIRE_HOOK);
                display.setAmount(1);
                ItemMeta im = display.getItemMeta();
                if (im != null) {
                    im.displayName(Utils.toComponent("&#d61111" + keyId));
                    display.setItemMeta(im);
                }
                p.openInventory(this.plugin.guiDeleteConfirm.build(p, display));
            }
        }
    }
}

