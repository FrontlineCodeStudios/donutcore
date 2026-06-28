
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DeleteConfirmGuiListener
implements Listener {
    private final DonutCrates plugin;

    public DeleteConfirmGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onClick(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        String title = Utils.stripColor(e.getView().title());
        if (!GuiUtil.isDeleteConfirmTitle(this.plugin, title)) {
            return;
        }
        int topSize = e.getView().getTopInventory().getSize();
        if (e.getRawSlot() >= topSize) {
            return;
        }
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ConfigurationSection delC = this.plugin.cfg.config.getConfigurationSection("delete-confirm-menu");
        String confMat = delC != null ? delC.getString("Confirm.material", "LIME_STAINED_GLASS_PANE") : "LIME_STAINED_GLASS_PANE";
        String decMat = delC != null ? delC.getString("Decline.material", "RED_STAINED_GLASS_PANE") : "RED_STAINED_GLASS_PANE";
        UUID uid = p.getUniqueId();
        String type = this.plugin.pendingDeleteType.get(uid);
        String crate = this.plugin.pendingDeleteCrate.get(uid);
        String key = this.plugin.pendingDeleteItemKey.get(uid);
        if (clicked.getType().name().equalsIgnoreCase(decMat)) {
            GuiUtil.playClick(this.plugin, p);
            if ("ITEM".equals(type)) {
                p.openInventory(this.plugin.guiItemEditor.build(crate, key));
            } else if ("KEY".equals(type)) {
                p.openInventory(this.plugin.guiKeyEditor.build(crate));
            } else {
                p.openInventory(this.plugin.guiCrateSettings.build(crate));
            }
            this.clearDelete(uid);
            return;
        }
        if (clicked.getType().name().equalsIgnoreCase(confMat)) {
            GuiUtil.playClick(this.plugin, p);
            if ("ITEM".equals(type)) {
                this.plugin.crateMgr.removeItem(crate, key, p);
                this.clearDelete(uid);
                p.openInventory(this.plugin.guiCrateRewards.build(crate));
                return;
            }
            if ("KEY".equals(type)) {
                String keyId = crate;
                if (keyId != null && !keyId.isBlank()) {
                    ConfigurationSection cratesRoot = this.plugin.cfg.crates.getConfigurationSection("Crates");
                    if (cratesRoot != null) {
                        for (String cName : cratesRoot.getKeys(false)) {
                            String sel = this.plugin.getKeyIdForCrate(cName);
                            if (sel == null || !sel.equalsIgnoreCase(keyId)) continue;
                            this.plugin.cfg.crates.set("Crates." + cName + ".key", null);
                        }
                    }
                    this.plugin.cfg.saves.set("keys." + keyId, null);
                    this.plugin.cfg.saveAll();
                    try {
                        this.plugin.dataMgr.resetKeysForAll(keyId);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                    this.plugin.msg(p, "&#0fe30fDeleted key &f" + keyId + "&#0fe30f.");
                }
                this.clearDelete(uid);
                p.openInventory(this.plugin.guiKeyList.build());
                return;
            }
            this.plugin.crateMgr.deleteCrate(crate, p);
            this.plugin.holoMgr.removeCrate(crate);
            this.clearDelete(uid);
            p.openInventory(this.plugin.guiMainEditor.build());
        }
    }

    private void clearDelete(UUID uid) {
        this.plugin.pendingDeleteType.remove(uid);
        this.plugin.pendingDeleteCrate.remove(uid);
        this.plugin.pendingDeleteItemKey.remove(uid);
    }
}

