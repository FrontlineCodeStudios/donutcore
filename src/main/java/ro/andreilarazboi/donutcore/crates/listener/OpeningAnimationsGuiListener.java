
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import ro.andreilarazboi.donutcore.crates.EditorHolder;
import ro.andreilarazboi.donutcore.crates.Utils;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationService;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class OpeningAnimationsGuiListener
implements Listener {
    private final DonutCrates plugin;
    private final OpeningAnimationService previewService;
    private final NamespacedKey animKey;

    public OpeningAnimationsGuiListener(DonutCrates plugin) {
        this.plugin = plugin;
        this.previewService = new OpeningAnimationService(plugin);
        this.animKey = new NamespacedKey((Plugin)plugin.getPlugin(), "opening_anim_id");
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
        if (!rawTitle.endsWith(" Opening Animations")) {
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
        String crate = rawTitle.substring(0, rawTitle.length() - " Opening Animations".length()).trim();
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
        if (slot == 49) {
            GuiUtil.playClick(this.plugin, p);
            return;
        }
        if (!clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        String guiId = meta.getPersistentDataContainer().get(this.animKey, PersistentDataType.STRING);
        if (guiId == null || guiId.isBlank()) {
            return;
        }
        guiId = guiId.trim().toUpperCase(Locale.ROOT);
        ClickType click = e.getClick();
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            ItemStack previewReward;
            if (guiId.equalsIgnoreCase("NONE")) {
                GuiUtil.playClick(this.plugin, p);
                this.plugin.msg(p, "&#d61111Disabled has no animation to preview.");
                return;
            }
            OpeningAnimationType type = OpeningAnimationType.byId(guiId);
            if (type == null) {
                return;
            }
            ArrayList<ItemStack> candidates = new ArrayList<ItemStack>();
            ConfigurationSection itemsSec = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate + ".Items");
            if (itemsSec != null) {
                for (String k : itemsSec.getKeys(false)) {
                    ItemStack it;
                    ConfigurationSection s = itemsSec.getConfigurationSection(k);
                    if (s == null || (it = this.plugin.guiItemUtil.buildItemFromSection(s)) == null || it.getType().isAir()) continue;
                    ItemStack one = it.clone();
                    one.setAmount(1);
                    candidates.add(one);
                }
            }
            if (!candidates.isEmpty()) {
                previewReward = candidates.get(new Random().nextInt(candidates.size())).clone();
                previewReward.setAmount(1);
            } else {
                previewReward = new ItemStack(Material.DIAMOND);
            }
            GuiUtil.playClick(this.plugin, p);
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.getPlugin(), () -> this.previewService.play(p, crate, type, candidates, previewReward, () -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> {
                if (p.isOnline()) {
                    p.openInventory(this.plugin.guiOpeningAnimations.build(crate));
                }
            })), 2L);
            return;
        }
        GuiUtil.playClick(this.plugin, p);
        if (guiId.equalsIgnoreCase("NONE")) {
            this.plugin.cfg.crates.set("Crates." + crate + ".opening-animation.enabled", (Object)false);
            this.plugin.cfg.crates.set("Crates." + crate + ".opening-animation.type", (Object)"ROW_SPIN");
            this.plugin.cfg.saveAll();
            this.plugin.msg(p, "&#0fe30fOpening animation &cdisabled &7for &f" + crate + "&#0fe30f.");
            p.openInventory(this.plugin.guiOpeningAnimations.build(crate));
            return;
        }
        OpeningAnimationType type = OpeningAnimationType.byId(guiId);
        if (type == null) {
            return;
        }
        this.plugin.cfg.crates.set("Crates." + crate + ".opening-animation.enabled", (Object)true);
        this.plugin.cfg.crates.set("Crates." + crate + ".opening-animation.type", (Object)type.id());
        this.plugin.cfg.saveAll();
        this.plugin.msg(p, "&#0fe30fSelected: &fAnimation &7for &f" + crate + "&#0fe30f.");
        p.openInventory(this.plugin.guiOpeningAnimations.build(crate));
    }
}

