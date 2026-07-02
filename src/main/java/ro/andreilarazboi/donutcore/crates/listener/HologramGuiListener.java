
package ro.andreilarazboi.donutcore.crates.listener;

import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class HologramGuiListener
implements Listener {
    private final DonutCrates plugin;

    public HologramGuiListener(DonutCrates plugin) {
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
        if (!rawTitle.endsWith(" Hologram")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Hologram".length());
        int slot = e.getRawSlot();
        String base = "Crates." + crate + ".Hologram";
        ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
        if (h == null) {
            h = this.plugin.cfg.crates.createSection(base);
        }
        switch (slot) {
            case 27: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiCrateSettings.build(crate));
                break;
            }
            case 10: {
                GuiUtil.playClick(this.plugin, p);
                boolean en = h.getBoolean("enabled", false);
                boolean enableNow = !en;
                h.set("enabled", enableNow);
                if (enableNow) {
                    String activeTemplate;
                    String defaultTemplateId;
                    ConfigurationSection templates = this.plugin.cfg.config.getConfigurationSection("hologram-templates");
                    defaultTemplateId = templates != null && templates.isConfigurationSection("default") ? "default" : null;
                    if (templates != null && defaultTemplateId != null && ((activeTemplate = h.getString("template", null)) == null || activeTemplate.isBlank())) {
                        ConfigurationSection defaultTemplate = templates.getConfigurationSection(defaultTemplateId);
                        h.set("template", defaultTemplateId);
                        h.set("lines", defaultTemplate.getStringList("lines"));
                        h.set("offsetY", defaultTemplate.getDouble("offset-y", 1.5));
                    }
                }
                this.plugin.cfg.saveAll();
                this.plugin.holoMgr.refreshCrate(crate);
                p.openInventory(this.plugin.guiHologram.build(crate));
                break;
            }
            case 12: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiHoloTemplates.build(crate));
                break;
            }
            case 14: {
                GuiUtil.playClick(this.plugin, p);
                boolean shadow = h.getBoolean("shadow", true);
                h.set("shadow", (!shadow ? 1 : 0));
                this.plugin.cfg.saveAll();
                this.plugin.holoMgr.refreshCrate(crate);
                p.openInventory(this.plugin.guiHologram.build(crate));
                break;
            }
            case 16: {
                GuiUtil.playClick(this.plugin, p);
                double off = h.getDouble("offsetY", 1.5);
                h.set("offsetY", (off - 0.25));
                this.plugin.cfg.saveAll();
                this.plugin.holoMgr.refreshCrate(crate);
                p.openInventory(this.plugin.guiHologram.build(crate));
                break;
            }
            case 25: {
                GuiUtil.playClick(this.plugin, p);
                double off = h.getDouble("offsetY", 1.5);
                h.set("offsetY", (off + 0.25));
                this.plugin.cfg.saveAll();
                this.plugin.holoMgr.refreshCrate(crate);
                p.openInventory(this.plugin.guiHologram.build(crate));
                break;
            }
            case 13: {
                GuiUtil.playClick(this.plugin, p);
                p.openInventory(this.plugin.guiHoloBackground.build(crate));
            }
        }
    }
}

