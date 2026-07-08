
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class ItemEditorGuiListener
implements Listener {
    private final DonutCrates plugin;

    public ItemEditorGuiListener(DonutCrates plugin) {
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
        if (!rawTitle.endsWith(" Item Editor")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Item Editor".length());
        UUID uid = p.getUniqueId();
        String key = this.plugin.pendingEditorItemKey.get(uid);
        if (key == null) {
            return;
        }
        switch (e.getRawSlot()) {
            case 26: {
                ItemMeta im;
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingDeleteType.put(uid, "ITEM");
                this.plugin.pendingDeleteCrate.put(uid, crate);
                this.plugin.pendingDeleteItemKey.put(uid, key);
                String base = "Crates." + crate + ".Items." + key;
                ItemStack display = this.plugin.guiItemUtil.buildItemFromSection(this.plugin.cfg.crates.getConfigurationSection(base));
                if (display != null && display.hasItemMeta() && (im = display.getItemMeta()) != null && !im.hasDisplayName()) {
                    im.displayName(Utils.toComponent("&#d61111" + key));
                    display.setItemMeta(im);
                }
                p.openInventory(this.plugin.guiDeleteConfirm.build(p, display));
                break;
            }
            case 18: {
                GuiUtil.playClick(this.plugin, p);
                this.clearPendings(uid);
                p.openInventory(this.plugin.guiCrateRewards.build(crate));
                break;
            }
            case 13: {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingEditorCrate.put(uid, crate);
                this.plugin.pendingEditorItemKey.put(uid, key);
                this.plugin.pendingEditorIsRename.put(uid, true);
                this.plugin.pendingEditorIsLore.remove(uid);
                this.plugin.pendingEditorIsChance.remove(uid);
                p.closeInventory();
                this.plugin.msg(p, "&#0f99e3Enter new display name for this item (&ftype 'cancel' to stop&#0f99e3):");
                break;
            }
            case 14: {
                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    ItemStack it;
                    GuiUtil.playClick(this.plugin, p);
                    this.plugin.cfg.crates.set("Crates." + crate + ".Items." + key + ".lore", new ArrayList<>());
                    String base = "Crates." + crate + ".Items." + key;
                    if (this.plugin.cfg.crates.isItemStack(base + ".item") && (it = this.plugin.cfg.crates.getItemStack(base + ".item")) != null && it.hasItemMeta()) {
                        ItemMeta im = it.getItemMeta();
                        im.lore(new ArrayList<>());
                        it.setItemMeta(im);
                        this.plugin.cfg.crates.set(base + ".item", (Object)it);
                    }
                    this.plugin.cfg.saveAll();
                    this.plugin.msg(p, "&#FF073ACleared all lore for &f" + key);
                    p.openInventory(this.plugin.guiItemEditor.build(crate, key));
                    break;
                }
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingEditorCrate.put(uid, crate);
                this.plugin.pendingEditorItemKey.put(uid, key);
                this.plugin.pendingEditorIsLore.put(uid, true);
                this.plugin.pendingEditorIsRename.remove(uid);
                this.plugin.pendingEditorIsChance.remove(uid);
                p.closeInventory();
                this.plugin.msg(p, "&#0f99e3Enter a lore line for this item (&ftype 'cancel' to stop&#0f99e3):");
                break;
            }
            case 15: {
                GuiUtil.playClick(this.plugin, p);
                String path = "Crates." + crate + ".Items." + key + ".giveitem";
                boolean give = this.plugin.cfg.crates.getBoolean(path, true);
                this.plugin.cfg.crates.set(path, (Object)(!give ? 1 : 0));
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, !give ? "&#FF073AGive Item: &fENABLED" : "&#d61111Give Item: &fDISABLED");
                p.openInventory(this.plugin.guiItemEditor.build(crate, key));
                break;
            }
            case 16: {
                if (this.plugin.cfg.crates.getBoolean("Crates." + crate + ".Items." + key + ".giveitem", true)) {
                    return;
                }
                GuiUtil.playClick(this.plugin, p);
                this.plugin.pendingEditorCrate.put(uid, crate);
                this.plugin.pendingEditorItemKey.put(uid, key);
                this.plugin.pendingEditorIsLore.put(uid, false);
                this.plugin.pendingEditorIsRename.remove(uid);
                this.plugin.pendingEditorIsChance.remove(uid);
                p.closeInventory();
                this.plugin.msg(p, "&#0f99e3Enter a command to run when this reward is chosen (&ftype 'cancel' to stop&#0f99e3):");
                break;
            }
            case 4: {
                if (!this.plugin.cfg.crates.getBoolean("Crates." + crate + ".random.enabled", false)) {
                    return;
                }
                GuiUtil.playClick(this.plugin, p);
                if (e.getClick().isLeftClick()) {
                    String crateName = crate;
                    String itemKey = key;
                    Player playerRef = p;
                    Utils.openSignInput(this.plugin.getPlugin(), p, List.of("", "\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191\u2191", "Chance (%)", "Right-click = AUTO"), 1, value -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> {
                        double chance;
                        if (value == null || value.trim().isEmpty()) {
                            this.plugin.msg(playerRef, "&#d61111Chance change cancelled.");
                            playerRef.openInventory(this.plugin.guiItemEditor.build(crateName, itemKey));
                            return;
                        }
                        try {
                            chance = Double.parseDouble(value.trim().replace(",", "."));
                        }
                        catch (NumberFormatException ex) {
                            this.plugin.msg(playerRef, "&#d61111Invalid chance: &f" + value);
                            playerRef.openInventory(this.plugin.guiItemEditor.build(crateName, itemKey));
                            return;
                        }
                        if (chance < 0.0) {
                            chance = 0.0;
                        }
                        if (chance > 100.0) {
                            chance = 100.0;
                        }
                        String path = "Crates." + crateName + ".Items." + itemKey + ".chance";
                        this.plugin.cfg.crates.set(path, (Object)chance);
                        this.plugin.cfg.saveAll();
                        this.plugin.msg(playerRef, "&#FF073AChance set to &f" + chance + "% &#FF073Afor &f" + itemKey + "&#FF073A.");
                        playerRef.openInventory(this.plugin.guiItemEditor.build(crateName, itemKey));
                    }));
                    break;
                }
                if (!e.getClick().isRightClick()) break;
                String base = "Crates." + crate + ".Items." + key + ".chance";
                this.plugin.cfg.crates.set(base, (Object)0.0);
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, "&#FF073AChance mode set to &fAUTO &7for this reward.");
                p.openInventory(this.plugin.guiItemEditor.build(crate, key));
                break;
            }
            case 22: {
                if (!this.plugin.cfg.crates.getBoolean("Crates." + crate + ".random.enabled", false)) {
                    return;
                }
                GuiUtil.playClick(this.plugin, p);
                String path = "Crates." + crate + ".Items." + key + ".broadcast";
                boolean current = this.plugin.cfg.crates.getBoolean(path, false);
                this.plugin.cfg.crates.set(path, (Object)(!current ? 1 : 0));
                this.plugin.cfg.saveAll();
                this.plugin.msg(p, !current ? "&#FF073ABroadcast &fENABLED &7for this reward." : "&#d61111Broadcast &fDISABLED &7for this reward.");
                p.openInventory(this.plugin.guiItemEditor.build(crate, key));
            }
        }
    }

    private void clearPendings(UUID uid) {
        this.plugin.pendingEditorCrate.remove(uid);
        this.plugin.pendingEditorItemKey.remove(uid);
        this.plugin.pendingEditorIsLore.remove(uid);
        this.plugin.pendingEditorIsRename.remove(uid);
        this.plugin.pendingEditorIsChance.remove(uid);
    }
}

