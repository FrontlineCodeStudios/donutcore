
package ro.andreilarazboi.donutcore.crates.listener;

import java.time.Duration;
import java.util.UUID;
import net.kyori.adventure.title.Title;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrateEditGuiListener
implements Listener {
    private final DonutCrates plugin;

    public CrateEditGuiListener(DonutCrates plugin) {
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
        String rawTitle = Utils.stripColor(e.getView().getTitle());
        if (!rawTitle.endsWith(" Edit")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Edit".length());
        int slot = e.getRawSlot();
        UUID uid = p.getUniqueId();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        switch (slot) {
            case 18: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiCrateSettings.build(crate));
                break;
            }
            case 10: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingCopyCrate.put(uid, crate);
                p.closeInventory();
                p.showTitle(Title.title(Utils.toComponent("\u00a7d[Copy crate]"), Utils.toComponent("\u00a77Left-click a block to clone this crate!"), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(3000L), Duration.ofMillis(500L))));
                break;
            }
            case 11: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingMoveCrate.put(uid, crate);
                p.closeInventory();
                p.showTitle(Title.title(Utils.toComponent("\u00a7b[Move crate]"), Utils.toComponent("\u00a77Left-click a block to set new location!"), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(3000L), Duration.ofMillis(500L))));
                break;
            }
            case 12: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingDeleteType.put(uid, "CRATE");
                this.plugin.pendingDeleteCrate.put(uid, crate);
                Material mat = this.plugin.crateMgr.crateBlocks.get(crate) != null ? this.plugin.crateMgr.crateBlocks.get(crate).getType() : Material.CHEST;
                ItemStack display = new ItemStack(mat);
                ItemMeta im = display.getItemMeta();
                if (im != null) {
                    im.displayName(Utils.toComponent("&#d61111" + crate));
                    display.setItemMeta(im);
                }
                p.openInventory(this.plugin.guiDeleteConfirm.build(p, display));
                break;
            }
            case 13: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingRenameCrate.put(uid, crate);
                p.closeInventory();
                this.plugin.msg(p, "&#0f99e3Enter a new (plain) crate ID for &f" + crate + "&#0f99e3:");
                break;
            }
            case 14: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiRowEditor.build(crate));
                break;
            }
            case 15: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
                break;
            }
            case 16: {
                GuiUtil.playClick(this.plugin, p);
                String base = "Crates." + crate + ".random.enabled";
                boolean current = this.plugin.cfg.crates.getBoolean(base, false);
                this.plugin.cfg.crates.set(base, (Object)(!current ? 1 : 0));
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, !current ? "&#0fe30fRandom rewards &fENABLED &7for crate &f" + crate : "&#d61111Random rewards &fDISABLED &7for crate &f" + crate);
                p.openInventory(this.plugin.guiCrateEdit.build(crate));
            }
        }
    }
}

