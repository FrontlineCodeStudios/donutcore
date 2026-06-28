
package ro.andreilarazboi.donutcore.crates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.time.Duration;
import net.kyori.adventure.title.Title;
import org.bukkit.plugin.Plugin;
import ro.andreilarazboi.donutcore.DonutCore;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationService;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationType;

public class BlockListener
implements Listener {
    private final DonutCrates plugin;
    private final NamespacedKey keyTag;
    private final OpeningAnimationService openingService;
    private final Map<UUID, PendingOpen> pendingOpens = new HashMap<UUID, PendingOpen>();

    public BlockListener(DonutCrates pl) {
        this.plugin = pl;
        this.keyTag = new NamespacedKey((Plugin)pl.getPlugin(), "crate_key");
        this.openingService = new OpeningAnimationService(pl);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!DonutCore.getInstance().getCratesModule().isActive()) return;
        Action action = e.getAction();
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (this.plugin.pendingCreate.contains(p.getUniqueId()) && action == Action.LEFT_CLICK_BLOCK && b != null) {
            String name;
            e.setCancelled(true);
            int i = 1;
            while (this.plugin.crateMgr.crateExists(name = "crate" + i++)) {
            }
            this.plugin.crateMgr.createCrate(name, b, p);
            this.plugin.cfg.crates.set("Crates." + name + ".key", (Object)name);
            this.plugin.ensureKeyConfig(name);
            boolean randomMode = this.plugin.pendingCreateRandom.getOrDefault(p.getUniqueId(), false);
            this.plugin.cfg.crates.set("Crates." + name + ".random.enabled", (Object)randomMode);
            this.plugin.cfg.saveAll();
            this.plugin.pendingCreate.remove(p.getUniqueId());
            this.plugin.pendingCreateRandom.remove(p.getUniqueId());
            p.openInventory(this.plugin.guiCrateSettings.build(name));
            p.showTitle(Title.title(Utils.toComponent("\u00a7a[Crate created]"), Utils.toComponent("\u00a77Opened settings for \u00a7e" + name), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(2000L), Duration.ofMillis(500L))));
            return;
        }
        if (action == Action.LEFT_CLICK_BLOCK && b != null && this.plugin.pendingMoveCrate.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            String crate = this.plugin.pendingMoveCrate.remove(p.getUniqueId());
            this.plugin.crateMgr.moveCrate(crate, b, p);
            p.openInventory(this.plugin.guiCrateSettings.build(crate));
            p.showTitle(Title.title(Utils.toComponent("\u00a7a[Crate moved]"), Utils.toComponent("\u00a77Location updated for \u00a7e" + crate), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(2000L), Duration.ofMillis(500L))));
            return;
        }
        if (action == Action.LEFT_CLICK_BLOCK && b != null && this.plugin.pendingCopyCrate.containsKey(p.getUniqueId())) {
            String baseName;
            e.setCancelled(true);
            UUID uid = p.getUniqueId();
            String source = this.plugin.pendingCopyCrate.remove(uid);
            if (source == null || !this.plugin.crateMgr.crateExists(source)) {
                this.plugin.msg(p, "&#ff5555The source crate no longer exists.");
                return;
            }
            String newName = baseName = source + "_copy";
            int index = 2;
            while (this.plugin.crateMgr.crateExists(newName)) {
                newName = baseName + index++;
            }
            this.plugin.crateMgr.createCrate(newName, b, p);
            String srcPath = "Crates." + source;
            String dstPath = "Crates." + newName;
            ConfigurationSection srcCrate = this.plugin.cfg.crates.getConfigurationSection(srcPath);
            ConfigurationSection dstCrate = this.plugin.cfg.crates.getConfigurationSection(dstPath);
            if (srcCrate != null && dstCrate != null) {
                if (srcCrate.isSet("rows")) {
                    dstCrate.set("rows", srcCrate.get("rows"));
                }
                if (srcCrate.isConfigurationSection("Items")) {
                    dstCrate.set("Items", null);
                    ConfigurationSection srcItems = srcCrate.getConfigurationSection("Items");
                    ConfigurationSection dstItems = dstCrate.createSection("Items");
                    this.deepCopySection(srcItems, dstItems);
                }
                if (srcCrate.isConfigurationSection("random")) {
                    dstCrate.set("random", null);
                    ConfigurationSection srcRandom = srcCrate.getConfigurationSection("random");
                    ConfigurationSection dstRandom = dstCrate.createSection("random");
                    this.deepCopySection(srcRandom, dstRandom);
                }
                if (srcCrate.isConfigurationSection("Hologram")) {
                    dstCrate.set("Hologram", null);
                    ConfigurationSection srcHolo = srcCrate.getConfigurationSection("Hologram");
                    ConfigurationSection dstHolo = dstCrate.createSection("Hologram");
                    this.deepCopySection(srcHolo, dstHolo);
                }
                if (srcCrate.isSet("key")) {
                    dstCrate.set("key", srcCrate.getString("key"));
                } else {
                    dstCrate.set("key", newName);
                }
                if (srcCrate.isSet("displayname")) {
                    dstCrate.set("displayname", srcCrate.getString("displayname"));
                }
            }
            this.plugin.ensureKeyConfig(this.plugin.getKeyIdForCrate(newName));
            this.plugin.cfg.saveAll();
            this.plugin.holoMgr.refreshCrate(newName);
            p.openInventory(this.plugin.guiCrateSettings.build(newName));
            p.showTitle(Title.title(Utils.toComponent("\u00a7a[Crate copied]"), Utils.toComponent("\u00a77Cloned \u00a7e" + source + " \u00a77to \u00a7e" + newName), Title.Times.times(Duration.ofMillis(500L), Duration.ofMillis(2000L), Duration.ofMillis(500L))));
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (b == null) {
            return;
        }
        if (!this.plugin.crateMgr.isCrateBlock(b)) {
            return;
        }
        String crateName = this.plugin.crateMgr.getCrateByBlock(b);
        if (crateName == null) {
            return;
        }
        if (this.plugin.crateMgr.isRandomMode(crateName)) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                e.setCancelled(true);
                p.openInventory(this.plugin.guiCrate.build(crateName, true));
                return;
            }
            if (action == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                if (!this.plugin.crateMgr.checkAndMarkCooldown(p, crateName)) {
                    return;
                }
                this.openRandomCrateWithAnimation(crateName, p);
                return;
            }
            return;
        }
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (!this.plugin.crateMgr.checkAndMarkCooldown(p, crateName)) {
                return;
            }
            p.openInventory(this.plugin.guiCrate.build(crateName, false));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (this.plugin.crateMgr.isCrateBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (this.plugin.crateMgr.isCrateBlock(e.getBlockPlaced())) {
            e.setCancelled(true);
            return;
        }
        if (this.isAnyKeyItem(e.getItemInHand())) {
            e.setCancelled(true);
            this.plugin.msg(e.getPlayer(), "&#ff5555You cannot place crate keys.");
        }
    }

    private void openRandomCrateWithAnimation(String crate, Player p) {
        ConfigurationSection crateRoot;
        String keyId = this.plugin.getKeyIdForCrate(crate);
        this.plugin.ensureKeyConfig(keyId);
        boolean virtual = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true);
        if (virtual) {
            int keys = this.plugin.dataMgr.getKeys(p, keyId);
            if (keys <= 0) {
                this.sendNoKeysMessage(p, crate);
                return;
            }
        } else if (!this.hasPhysicalKey(p, keyId)) {
            this.sendNoKeysMessage(p, crate);
            return;
        }
        if ((crateRoot = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate)) == null) {
            this.plugin.msg(p, "&#ff5555Crate config missing.");
            return;
        }
        ConfigurationSection itemsSec = crateRoot.getConfigurationSection("Items");
        if (itemsSec == null || itemsSec.getKeys(false).isEmpty()) {
            this.plugin.msg(p, "&#ff5555No rewards configured for this crate.");
            return;
        }
        double totalWeight = 0.0;
        LinkedHashMap<String, Double> weights = new LinkedHashMap<String, Double>();
        for (String k : itemsSec.getKeys(false)) {
            ConfigurationSection s = itemsSec.getConfigurationSection(k);
            if (s == null) continue;
            double chance = s.getDouble("chance", 0.0);
            if (chance <= 0.0) {
                chance = 1.0;
            }
            weights.put(k, chance);
            totalWeight += chance;
        }
        if (totalWeight <= 0.0 || weights.isEmpty()) {
            this.plugin.msg(p, "&#ff5555No valid rewards configured for this crate.");
            return;
        }
        String chosenKey = null;
        ConfigurationSection chosen = null;
        double r = Math.random() * totalWeight;
        for (Map.Entry<String, Double> en : weights.entrySet()) {
            if (!((r -= en.getValue().doubleValue()) <= 0.0)) continue;
            chosenKey = en.getKey();
            chosen = itemsSec.getConfigurationSection(chosenKey);
            break;
        }
        if (chosen == null) {
            chosenKey = weights.keySet().iterator().next();
            chosen = itemsSec.getConfigurationSection(chosenKey);
        }
        if (chosen == null) {
            this.plugin.msg(p, "&#ff5555Could not find a reward for this selection.");
            return;
        }
        boolean give = chosen.getBoolean("giveitem", true);
        String cmd = chosen.getString("command", "");
        ItemStack reward = this.plugin.guiItemUtil.buildItemFromSection(chosen);
        String rewardName = this.plugin.getRewardDisplayNameForChat(reward);
        if (give && reward != null && !reward.getType().isAir() && !Utils.canFit(p.getInventory(), reward)) {
            String raw = this.plugin.cfg.config.getString("messages.inventory-full", "&#ff5555Your inventory is full!");
            this.plugin.msg(p, raw);
            this.playNoKeySound(p);
            return;
        }
        if (virtual) {
            if (this.plugin.dataMgr.getKeys(p, keyId) <= 0) {
                this.sendNoKeysMessage(p, crate);
                return;
            }
            this.plugin.dataMgr.modifyKeys(p, keyId, -1);
        } else if (!this.consumePhysicalKey(p, keyId)) {
            this.sendNoKeysMessage(p, crate);
            return;
        }
        boolean animEnabled = crateRoot.getBoolean("opening-animation.enabled", false);
        String typeId = crateRoot.getString("opening-animation.type", "ROW_SPIN");
        OpeningAnimationType type = OpeningAnimationType.byId(typeId);
        PendingOpen po = new PendingOpen(p.getUniqueId(), crate, chosenKey, give, cmd, reward, rewardName);
        this.pendingOpens.put(p.getUniqueId(), po);
        if (animEnabled) {
            ArrayList<ItemStack> candidates = new ArrayList<ItemStack>();
            for (String k : itemsSec.getKeys(false)) {
                ItemStack it;
                ConfigurationSection s = itemsSec.getConfigurationSection(k);
                if (s == null || (it = this.plugin.guiItemUtil.buildItemFromSection(s)) == null || it.getType().isAir()) continue;
                ItemStack one = it.clone();
                one.setAmount(1);
                candidates.add(one);
            }
            this.openingService.play(p, crate, type, candidates, reward, () -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> this.finishReward(po)));
        } else {
            this.finishReward(po);
        }
    }

    private void finishReward(PendingOpen po) {
        if (po.completed) {
            return;
        }
        po.completed = true;
        Player p = Bukkit.getPlayer(po.player);
        if (p == null) {
            this.pendingOpens.remove(po.player);
            return;
        }
        if (po.give && po.reward != null && !po.reward.getType().isAir()) {
            p.getInventory().addItem(new ItemStack[]{po.reward.clone()});
        }
        if (po.cmd != null && !po.cmd.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), po.cmd.replace("%player%", p.getName()));
        }
        String crateDisplay = this.getCrateDisplayRaw(po.crate);
        String selfTpl = this.plugin.cfg.config.getString("messages.reward-received", "&#0fe30fYou received &f%reward% &7from the &f%crate% &7crate!");
        this.plugin.msg(p, selfTpl.replace("%crate%", crateDisplay).replace("%reward%", po.rewardName).replace("%player%", p.getName()));
        ConfigurationSection chosen = this.plugin.cfg.crates.getConfigurationSection("Crates." + po.crate + ".Items." + po.chosenKey);
        if (chosen != null && chosen.getBoolean("broadcast", false)) {
            ConfigurationSection itemsSec = this.plugin.cfg.crates.getConfigurationSection("Crates." + po.crate + ".Items");
            double chanceVal = chosen.getDouble("chance", 0.0);
            if (chanceVal <= 0.0 && itemsSec != null) {
                int autoCount = 0;
                double explicitTotal = 0.0;
                for (String k : itemsSec.getKeys(false)) {
                    ConfigurationSection itSec = itemsSec.getConfigurationSection(k);
                    if (itSec == null) continue;
                    double c = itSec.getDouble("chance", 0.0);
                    if (c <= 0.0) {
                        ++autoCount;
                        continue;
                    }
                    explicitTotal += c;
                }
                if (autoCount > 0) {
                    double remaining = Math.max(0.0, 100.0 - explicitTotal);
                    chanceVal = remaining / (double)autoCount;
                }
            }
            String chanceText = chanceVal > 0.0 ? String.format(Locale.US, "%.2f%%", chanceVal) : "Unknown";
            List<String> lines = this.plugin.cfg.config.getStringList("broadcast-message");
            if (lines == null || lines.isEmpty()) {
                lines = Collections.singletonList("{player} just won {item-name} from {crate}!");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                for (String line : lines) {
                    if (line == null) continue;
                    if (line.isEmpty()) {
                        online.sendMessage(Utils.toComponent(""));
                        continue;
                    }
                    String out = line.replace("{player}", p.getName()).replace("{crate}", crateDisplay).replace("{item-name}", po.rewardName).replace("{item-chance}", chanceText);
                    online.sendMessage(Utils.toComponent(out));
                }
            }
        }
        p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.claim", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP), 1.0f, 1.0f);
        this.pendingOpens.remove(po.player);
    }

    private String getCrateDisplayRaw(String crateId) {
        String raw = this.plugin.cfg.crates.getString("Crates." + crateId + ".displayname", null);
        if (raw == null || raw.isBlank()) {
            return "&7" + crateId;
        }
        return raw;
    }

    private void sendNoKeysMessage(Player p, String crate) {
        String crateDisplay = this.getCrateDisplayRaw(crate);
        String rawNo = this.plugin.cfg.config.getString("messages.no-keys", "&#ff5555You don't have any keys for this crate!").replace("%crate%", crateDisplay);
        this.plugin.msg(p, rawNo);
        this.playNoKeySound(p);
    }

    private void playNoKeySound(Player p) {
        p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.no-key", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
    }

    private boolean isAnyKeyItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.keyTag, PersistentDataType.STRING);
    }

    private boolean hasPhysicalKey(Player p, String keyId) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (!this.isKeyItem(item, keyId)) continue;
            return true;
        }
        return false;
    }

    private boolean consumePhysicalKey(Player p, String keyId) {
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

    private void deepCopySection(ConfigurationSection src, ConfigurationSection dst) {
        if (src == null || dst == null) {
            return;
        }
        for (String key : src.getKeys(false)) {
            Object val = src.get(key);
            if (val instanceof ConfigurationSection) {
                ConfigurationSection childSrc = src.getConfigurationSection(key);
                ConfigurationSection childDst = dst.createSection(key);
                this.deepCopySection(childSrc, childDst);
                continue;
            }
            dst.set(key, val);
        }
    }

    private static class PendingOpen {
        final UUID player;
        final String crate;
        final String chosenKey;
        final boolean give;
        final String cmd;
        final ItemStack reward;
        final String rewardName;
        boolean completed = false;

        PendingOpen(UUID player, String crate, String chosenKey, boolean give, String cmd, ItemStack reward, String rewardName) {
            this.player = player;
            this.crate = crate;
            this.chosenKey = chosenKey;
            this.give = give;
            this.cmd = cmd;
            this.reward = reward;
            this.rewardName = rewardName;
        }
    }
}

