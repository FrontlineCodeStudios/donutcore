
package ro.andreilarazboi.donutcore.crates;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatInputListener
implements Listener {
    private final DonutCrates plugin;
    private static final Pattern COLOR_CODE = Pattern.compile("§.|&[0-9a-fk-orx]|&#[A-Fa-f0-9]{6}", 2);

    public ChatInputListener(DonutCrates plugin) {
        this.plugin = plugin;
    }

    private static boolean hasColorCodes(String s) {
        return COLOR_CODE.matcher(s).find();
    }

    private static boolean isValidCrateName(String s) {
        return s.matches("[A-Za-z0-9_\\-]+");
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        boolean awaitingItemInput;
        final Player p = e.getPlayer();
        final UUID uid = p.getUniqueId();
        final String msg = Utils.stripColor(e.message()).trim();
        if (this.plugin.pendingHoloTimerEdit.contains(uid)) {
            int seconds;
            e.setCancelled(true);
            try {
                seconds = Integer.parseInt(msg.replaceAll("[^0-9]", ""));
            }
            catch (Exception ex) {
                this.plugin.msg(p, "&#ff5555Enter a valid number like &f5&7 or &f10&7.");
                return;
            }
            if (seconds < 1) {
                seconds = 1;
            }
            if (seconds > 3600) {
                seconds = 3600;
            }
            this.plugin.cfg.config.set("hologram-update-seconds", seconds);
            this.plugin.cfg.saveAll();
            new BukkitRunnable(){
                public void run() {
                    ChatInputListener.this.plugin.restartHologramTask();
                    ChatInputListener.this.plugin.pendingHoloTimerEdit.remove(uid);
                    ChatInputListener.this.plugin.msg(p, "&#FF073AUpdated hologram timer");
                }
            }.runTask((Plugin)this.plugin.getPlugin());
            return;
        }
        if (this.plugin.pendingKeyRename.containsKey(uid)) {
            ItemMeta im;
            ItemStack it;
            e.setCancelled(true);
            final String keyId = this.plugin.pendingKeyRename.remove(uid);
            this.plugin.ensureKeyConfig(keyId);
            this.plugin.cfg.saves.set("keys." + keyId + ".displayname", (Object)msg);
            if (this.plugin.cfg.saves.isItemStack("keys." + keyId + ".item") && (it = this.plugin.cfg.saves.getItemStack("keys." + keyId + ".item")) != null && (im = it.getItemMeta()) != null) {
                im.displayName(Utils.toComponent(msg));
                it.setItemMeta(im);
                this.plugin.cfg.saves.set("keys." + keyId + ".item", (Object)it);
            }
            this.plugin.cfg.saveAll();
            new BukkitRunnable(){
                public void run() {
                    ChatInputListener.this.plugin.msg(p, "&#FF073ARenamed key &f" + keyId + " &#FF073Ato: &f" + msg);
                    p.openInventory(ChatInputListener.this.plugin.guiKeyEditor.build(keyId));
                }
            }.runTask((Plugin)this.plugin.getPlugin());
            return;
        }
        if (this.plugin.pendingHoloAddLine.containsKey(uid)) {
            e.setCancelled(true);
            final String crate = this.plugin.pendingHoloAddLine.remove(uid);
            String base = "Crates." + crate + ".Hologram";
            ConfigurationSection h = this.plugin.cfg.crates.getConfigurationSection(base);
            if (h == null) {
                h = this.plugin.cfg.crates.createSection(base);
            }
            ArrayList<String> lines = new ArrayList<String>(h.getStringList("lines"));
            lines.add(msg);
            h.set("lines", lines);
            this.plugin.cfg.saveAll();
            new BukkitRunnable(){
                public void run() {
                    ChatInputListener.this.plugin.holoMgr.refreshCrate(crate);
                    p.openInventory(ChatInputListener.this.plugin.guiHologram.build(crate));
                }
            }.runTask((Plugin)this.plugin.getPlugin());
            this.plugin.msg(p, "&#FF073AAdded hologram line: &f" + msg);
            return;
        }
        if (this.plugin.pendingRenameCrate.containsKey(uid)) {
            e.setCancelled(true);
            final String oldName = this.plugin.pendingRenameCrate.remove(uid);
            String raw = msg.replace(' ', '_');
            if (raw.isEmpty()) {
                this.plugin.msg(p, "&#ff5555Name cannot be empty.");
                this.reopen(oldName, p);
                return;
            }
            if (ChatInputListener.hasColorCodes(raw)) {
                this.plugin.msg(p, "&#ff5555Color codes are not allowed in crate names.");
                this.reopen(oldName, p);
                return;
            }
            if (!ChatInputListener.isValidCrateName(raw)) {
                this.plugin.msg(p, "&#ff5555Invalid name. Use only letters, numbers, dashes and underscores.");
                this.reopen(oldName, p);
                return;
            }
            final String newName = raw;
            if (this.plugin.crateMgr.crateExists(newName)) {
                this.plugin.msg(p, "&#ff5555A crate named &f" + newName + " &#ff5555already exists.");
                this.reopen(oldName, p);
                return;
            }
            ConfigurationSection oldSec = this.plugin.cfg.crates.getConfigurationSection("Crates." + oldName);
            if (oldSec == null) {
                this.plugin.msg(p, "&#ff5555Crate not found.");
                return;
            }
            this.plugin.cfg.crates.createSection("Crates." + newName, oldSec.getValues(true));
            this.plugin.cfg.crates.set("Crates." + oldName, null);
            String oldKeySel = this.plugin.cfg.crates.getString("Crates." + newName + ".key", oldName);
            if (oldKeySel != null && oldKeySel.equalsIgnoreCase(oldName)) {
                this.plugin.cfg.crates.set("Crates." + newName + ".key", (Object)newName);
                this.plugin.ensureKeyConfig(newName);
            }
            this.plugin.cfg.saveAll();
            Block b = this.plugin.crateMgr.crateBlocks.remove(oldName);
            if (b != null) {
                this.plugin.crateMgr.crateBlocks.put(newName, b);
            }
            this.plugin.crateMgr.saveBlocks();
            this.plugin.msg(p, "&#FF073ARenamed crate &f" + oldName + " &#FF073A→ &f" + newName);
            new BukkitRunnable(){
                public void run() {
                    ChatInputListener.this.plugin.holoMgr.removeCrate(oldName);
                    ChatInputListener.this.plugin.holoMgr.refreshCrate(newName);
                    p.openInventory(ChatInputListener.this.plugin.guiCrateSettings.build(newName));
                }
            }.runTask((Plugin)this.plugin.getPlugin());
            return;
        }
        awaitingItemInput = this.plugin.pendingEditorIsRename.containsKey(uid) || this.plugin.pendingEditorIsLore.containsKey(uid) || this.plugin.pendingEditorIsChance.containsKey(uid);
        if (!this.plugin.pendingEditorCrate.containsKey(uid) || !this.plugin.pendingEditorItemKey.containsKey(uid)) {
            return;
        }
        if (!awaitingItemInput) {
            return;
        }
        e.setCancelled(true);
        final String crate = this.plugin.pendingEditorCrate.get(uid);
        final String key = this.plugin.pendingEditorItemKey.get(uid);
        if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("exit")) {
            this.plugin.pendingEditorIsRename.remove(uid);
            this.plugin.pendingEditorIsLore.remove(uid);
            this.plugin.pendingEditorIsChance.remove(uid);
            this.plugin.msg(p, "&#d61111Cancelled.");
            new BukkitRunnable(){
                public void run() {
                    p.openInventory(ChatInputListener.this.plugin.guiItemEditor.build(crate, key));
                }
            }.runTask((Plugin)this.plugin.getPlugin());
            return;
        }
        Boolean renameFlag = this.plugin.pendingEditorIsRename.get(uid);
        Boolean loreFlag = this.plugin.pendingEditorIsLore.get(uid);
        Boolean chanceFlag = this.plugin.pendingEditorIsChance.get(uid);
        String base = "Crates." + crate + ".Items." + key;
        this.plugin.pendingEditorIsRename.remove(uid);
        this.plugin.pendingEditorIsLore.remove(uid);
        this.plugin.pendingEditorIsChance.remove(uid);
        if (Boolean.TRUE.equals(renameFlag)) {
            ItemMeta im;
            ItemStack it;
            this.plugin.cfg.crates.set(base + ".displayname", (Object)msg);
            if (this.plugin.cfg.crates.isItemStack(base + ".item") && (it = this.plugin.cfg.crates.getItemStack(base + ".item")) != null && (im = it.getItemMeta()) != null) {
                im.displayName(Utils.toComponent(msg));
                it.setItemMeta(im);
                this.plugin.cfg.crates.set(base + ".item", (Object)it);
            }
            this.plugin.cfg.saveAll();
            this.plugin.msg(p, "&#FF073ASet display name to: &f" + msg);
        } else if (Boolean.TRUE.equals(loreFlag)) {
            ItemMeta im;
            ItemStack it;
            ArrayList<String> lore = new ArrayList<String>(this.plugin.cfg.crates.getStringList(base + ".lore"));
            lore.add(msg);
            this.plugin.cfg.crates.set(base + ".lore", lore);
            if (this.plugin.cfg.crates.isItemStack(base + ".item") && (it = this.plugin.cfg.crates.getItemStack(base + ".item")) != null && (im = it.getItemMeta()) != null) {
                im.lore(Utils.toComponents(lore));
                it.setItemMeta(im);
                this.plugin.cfg.crates.set(base + ".item", (Object)it);
            }
            this.plugin.cfg.saveAll();
            this.plugin.msg(p, "&#FF073AAdded lore: &f" + msg);
        } else if (Boolean.TRUE.equals(chanceFlag)) {
            double val;
            String normalized = msg.replace(',', '.');
            try {
                val = Double.parseDouble(normalized);
            }
            catch (NumberFormatException ex) {
                this.plugin.pendingEditorIsChance.put(uid, true);
                this.plugin.msg(p, "&#ff5555Invalid number. Enter e.g. &f0.01&7, &f5&7 or &f25.5&7. Type &fcancel&7 to stop.");
                return;
            }
            if (val < 0.0) {
                val = 0.0;
            }
            if (val > 100000.0) {
                val = 100000.0;
            }
            this.plugin.cfg.crates.set(base + ".chance", (Object)val);
            this.plugin.cfg.saveAll();
            this.plugin.msg(p, "&#FF073ASet chance to &f" + val + "% &7for this reward.");
        } else if (loreFlag != null && !loreFlag.booleanValue()) {
            this.plugin.cfg.crates.set(base + ".command", (Object)msg);
            this.plugin.cfg.saveAll();
            this.plugin.msg(p, "&#FF073ASet command: &f" + msg);
        }
        new BukkitRunnable(){
            public void run() {
                p.openInventory(ChatInputListener.this.plugin.guiItemEditor.build(crate, key));
            }
        }.runTask((Plugin)this.plugin.getPlugin());
    }

    private void reopen(final String crate, final Player p) {
        new BukkitRunnable(){
            public void run() {
                p.openInventory(ChatInputListener.this.plugin.guiCrateSettings.build(crate));
            }
        }.runTask((Plugin)ChatInputListener.this.plugin.getPlugin());
    }
}
