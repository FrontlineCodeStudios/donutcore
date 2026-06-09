
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class CrateDisplayNameGuiListener
implements Listener {
    private static final Pattern HEX6 = Pattern.compile("^[0-9a-fA-F]{6}$");
    private final DonutCrates plugin;

    public CrateDisplayNameGuiListener(DonutCrates plugin) {
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
        String title = Utils.stripColor(e.getView().title());
        if (title == null || !title.endsWith(" Display Name")) {
            return;
        }
        int topSize = top.getSize();
        if (e.getRawSlot() >= topSize) {
            return;
        }
        e.setCancelled(true);
        String crate = title.substring(0, title.length() - " Display Name".length()).trim();
        int slot = e.getRawSlot();
        UUID uid = p.getUniqueId();
        if (slot == 18) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateEdit.build(crate));
            return;
        }
        if (slot == 10) {
            GuiUtil.playClick(this.plugin, p);
            this.plugin.pendingDisplayNameCrate.put(uid, crate);
            Utils.openSignInput(this.plugin.getPlugin(), p, List.of("", "", "Type name", "No colors"), 0, value -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> {
                if (value == null) {
                    this.plugin.msg(p, "&#d61111Display name change cancelled.");
                    p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
                    return;
                }
                String plain = value.trim();
                if (plain.isEmpty()) {
                    this.plugin.msg(p, "&#d61111Name cannot be empty.");
                    p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
                    return;
                }
                String color = this.plugin.pendingDisplayNameColor.get(uid);
                if (color == null) {
                    String raw = this.plugin.cfg.crates.getString("Crates." + crate + ".displayname", null);
                    color = this.extractLeadingColor(raw, "&7");
                }
                this.plugin.setCrateDisplayName(crate, color + plain);
                this.plugin.msg(p, "&#0fe30fDisplay name set to: &r" + this.plugin.getCrateDisplayNameFormatted(crate));
                p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
            }));
            return;
        }
        if (slot == 12) {
            GuiUtil.playClick(this.plugin, p);
            p.openInventory(this.plugin.guiCrateNameColor.build(crate));
            return;
        }
        if (slot == 14) {
            GuiUtil.playClick(this.plugin, p);
            this.plugin.pendingDisplayNameCrate.put(uid, crate);
            Utils.openSignInput(this.plugin.getPlugin(), p, List.of("", "", "Type HEX Color", "&#RRGGBB"), 0, value -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> {
                if (value == null) {
                    this.plugin.msg(p, "&#d61111Hex color change cancelled.");
                    p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
                    return;
                }
                String in = value.trim();
                if (in.startsWith("&#")) {
                    in = in.substring(2);
                }
                if (in.startsWith("#")) {
                    in = in.substring(1);
                }
                if (!HEX6.matcher(in).matches()) {
                    this.plugin.msg(p, "&#d61111Invalid hex. Example: &f&#444444");
                    p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
                    return;
                }
                String hexCode = "&#" + in.toLowerCase(Locale.ROOT);
                this.plugin.pendingDisplayNameColor.put(uid, hexCode);
                String currentPlain = this.plugin.getCrateDisplayNamePlain(crate);
                if (currentPlain == null || currentPlain.isBlank()) {
                    currentPlain = crate;
                }
                this.plugin.setCrateDisplayName(crate, hexCode + currentPlain);
                this.plugin.msg(p, "&#0fe30fHex applied. Preview: &r" + this.plugin.getCrateDisplayNameFormatted(crate));
                p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
            }));
            return;
        }
        if (slot == 16) {
            GuiUtil.playClick(this.plugin, p);
            this.plugin.setCrateDisplayName(crate, null);
            this.plugin.pendingDisplayNameColor.remove(uid);
            this.plugin.pendingDisplayNameCrate.remove(uid);
            this.plugin.msg(p, "&#0fe30fDisplay name reset to default.");
            p.openInventory(this.plugin.guiCrateDisplayName.build(crate));
        }
    }

    private String extractLeadingColor(String raw, String def) {
        String c;
        String maybe;
        if (raw == null || raw.isBlank()) {
            return def;
        }
        if ((raw = raw.trim()).startsWith("&#") && raw.length() >= 8 && (maybe = raw.substring(0, 8)).matches("^&#[0-9a-fA-F]{6}$")) {
            return maybe;
        }
        if (raw.startsWith("&") && raw.length() >= 2 && (c = raw.substring(0, 2)).matches("^&[0-9a-fA-FrR]$")) {
            return c;
        }
        return def;
    }
}

