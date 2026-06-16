
package ro.andreilarazboi.donutcore.crates.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import ro.andreilarazboi.donutcore.crates.CrateHolder;
import ro.andreilarazboi.donutcore.crates.DonutCrates;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ro.andreilarazboi.donutcore.crates.Utils;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationService;
import ro.andreilarazboi.donutcore.crates.opening.OpeningAnimationType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class CrateOpenService {
    private final DonutCrates plugin;
    private final KeyUtil keyUtil;
    private final OpeningAnimationService openingService;
    private final Map<UUID, PendingOpen> pendingOpens = new ConcurrentHashMap<UUID, PendingOpen>();

    public CrateOpenService(DonutCrates plugin, KeyUtil keyUtil) {
        this.plugin = plugin;
        this.keyUtil = keyUtil;
        this.openingService = new OpeningAnimationService(plugin);
    }

    public void onAnimationInventoryClosed(Player p) {
        PendingOpen po = this.pendingOpens.get(p.getUniqueId());
        if (po == null || po.completed) {
            return;
        }
        this.openingService.stop(p);
        this.finishReward(po);
    }

    public void handleCrateGuiClick(Player p, CrateHolder ch, ItemStack clicked, int rawSlot) {
        boolean randomEnabled;
        boolean virtual;
        String keyId;
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        if (ch.isPreview()) {
            return;
        }
        String crate = ch.getCrateName();
        ConfigurationSection crateRoot = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate);
        if (crateRoot == null) {
            this.plugin.msg(p, "&#ff5555Crate config missing.");
            return;
        }
        if (crateRoot.getBoolean("fillerEnabled", false)) {
            String matName = crateRoot.getString("fillerMaterial", "BLACK_STAINED_GLASS_PANE");
            if (clicked.getType().name().equalsIgnoreCase(matName)) {
                return;
            }
        }
        if ((keyId = this.plugin.getKeyIdForCrate(crate)) == null || keyId.isBlank()) {
            keyId = crate;
        }
        if (virtual = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true)) {
            if (this.plugin.dataMgr.getKeys(p, keyId) <= 0) {
                GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
                return;
            }
        } else if (!this.keyUtil.hasPhysicalKey(p, keyId)) {
            GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
            return;
        }
        randomEnabled = crateRoot.getConfigurationSection("random") != null && crateRoot.getConfigurationSection("random").getBoolean("enabled", false);
        if (randomEnabled) {
            ConfigurationSection itemsSec = crateRoot.getConfigurationSection("Items");
            if (itemsSec == null || itemsSec.getKeys(false).isEmpty()) {
                this.plugin.msg(p, "&#ff5555No rewards configured for this crate.");
                return;
            }
            ChosenReward chosen = this.pickWeightedReward(itemsSec);
            if (chosen == null || chosen.section == null) {
                this.plugin.msg(p, "&#ff5555No valid rewards configured for this crate.");
                return;
            }
            boolean give = chosen.section.getBoolean("giveitem", true);
            String cmd = chosen.section.getString("command", "");
            ItemStack reward = this.plugin.guiItemUtil.buildItemFromSection(chosen.section);
            String rewardName = this.getRewardNameForChat(reward, chosen.key);
            if (give && reward != null && !Utils.canFit(p.getInventory(), reward)) {
                this.plugin.msg(p, this.plugin.cfg.config.getString("messages.inventory-full", "&#ff5555Your inventory is full!"));
                GuiUtil.playNoKeySound(this.plugin, p);
                return;
            }
            if (virtual) {
                if (this.plugin.dataMgr.getKeys(p, keyId) <= 0) {
                    GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
                    return;
                }
                this.plugin.dataMgr.modifyKeys(p, keyId, -1);
            } else if (!this.keyUtil.consumePhysicalKey(p, keyId)) {
                GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
                return;
            }
            boolean animEnabled = crateRoot.getBoolean("opening-animation.enabled", false);
            String typeId = crateRoot.getString("opening-animation.type", "ROW_SPIN");
            OpeningAnimationType type = OpeningAnimationType.byId(typeId);
            GuiUtil.playClick(this.plugin, p);
            PendingOpen po = new PendingOpen(p.getUniqueId(), crate, keyId, virtual, chosen.key, give, cmd, reward, rewardName);
            this.pendingOpens.put(p.getUniqueId(), po);
            p.closeInventory();
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
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.getPlugin(), () -> this.openingService.play(p, crate, type, candidates, reward, () -> Bukkit.getScheduler().runTask((Plugin)this.plugin.getPlugin(), () -> this.finishReward(po))), 2L);
                return;
            }
            this.finishReward(po);
            return;
        }
        GuiUtil.playClick(this.plugin, p);
        this.plugin.pendingCrate.put(p.getUniqueId(), crate);
        this.plugin.pendingSlot.put(p.getUniqueId(), rawSlot);
        p.openInventory(this.plugin.guiConfirm.build(p, clicked, crate));
    }

    public void handleConfirmAccept(Player p) {
        UUID uid = p.getUniqueId();
        String crate = this.plugin.pendingCrate.get(uid);
        Integer clickedSlot = this.plugin.pendingSlot.get(uid);
        if (crate == null || clickedSlot == null) {
            p.closeInventory();
            return;
        }
        String keyId = this.plugin.getKeyIdForCrate(crate);
        if (keyId == null || keyId.isBlank()) {
            keyId = crate;
        }
        boolean virtual = this.plugin.cfg.saves.getBoolean("keys." + keyId + ".virtual", true);
        ConfigurationSection crateRoot = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate);
        if (crateRoot == null) {
            this.plugin.msg(p, "&#ff5555Crate config missing.");
            p.closeInventory();
            return;
        }
        ConfigurationSection itemsSec = crateRoot.getConfigurationSection("Items");
        if (itemsSec == null || itemsSec.getKeys(false).isEmpty()) {
            this.plugin.msg(p, "&#ff5555No rewards configured for this crate.");
            p.closeInventory();
            return;
        }
        String chosenKey = null;
        ConfigurationSection chosen = null;
        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection s = itemsSec.getConfigurationSection(key);
            if (s == null || s.getInt("slot", -1) != clickedSlot.intValue()) continue;
            chosenKey = key;
            chosen = s;
            break;
        }
        if (chosen == null || chosenKey == null) {
            this.plugin.msg(p, "&#ff5555Could not find a reward for this selection.");
            p.closeInventory();
            return;
        }
        boolean give = chosen.getBoolean("giveitem", true);
        String cmd = chosen.getString("command", "");
        ItemStack reward = this.plugin.guiItemUtil.buildItemFromSection(chosen);
        String rewardName = this.getRewardNameForChat(reward, chosenKey);
        if (give && reward != null && !Utils.canFit(p.getInventory(), reward)) {
            this.plugin.msg(p, this.plugin.cfg.config.getString("messages.inventory-full", "&#ff5555Your inventory is full!"));
            GuiUtil.playNoKeySound(this.plugin, p);
            return;
        }
        if (virtual) {
            if (this.plugin.dataMgr.getKeys(p, keyId) <= 0) {
                GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
                return;
            }
            this.plugin.dataMgr.modifyKeys(p, keyId, -1);
        } else if (!this.keyUtil.consumePhysicalKey(p, keyId)) {
            GuiUtil.sendNoKeysMessage(this.plugin, p, crate);
            return;
        }
        GuiUtil.playClick(this.plugin, p);
        PendingOpen po = new PendingOpen(uid, crate, keyId, virtual, chosenKey, give, cmd, reward, rewardName);
        this.pendingOpens.put(uid, po);
        this.openingService.stop(p);
        this.finishReward(po);
    }

    private ChosenReward pickWeightedReward(ConfigurationSection itemsSec) {
        double total = 0.0;
        LinkedHashMap<String, Double> weights = new LinkedHashMap<String, Double>();
        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection s = itemsSec.getConfigurationSection(key);
            if (s == null) continue;
            double d = s.getDouble("chance", 0.0);
            if (d <= 0.0) {
                d = 1.0;
            }
            weights.put(key, d);
            total += d;
        }
        if (total <= 0.0 || weights.isEmpty()) {
            return null;
        }
        double r = Math.random() * total;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (!((r -= entry.getValue()) <= 0.0)) continue;
            String k = entry.getKey();
            return new ChosenReward(k, itemsSec.getConfigurationSection(k));
        }
        String first = weights.keySet().iterator().next();
        return new ChosenReward(first, itemsSec.getConfigurationSection(first));
    }

    private void finishReward(PendingOpen po) {
        if (po.completed) {
            return;
        }
        po.completed = true;
        Player p = Bukkit.getPlayer((UUID)po.player);
        if (p == null) {
            this.pendingOpens.remove(po.player);
            return;
        }
        this.plugin.pendingCrate.remove(po.player);
        this.plugin.pendingSlot.remove(po.player);
        if (po.give && po.reward != null && !po.reward.getType().isAir()) {
            p.getInventory().addItem(new ItemStack[]{po.reward.clone()});
        }
        if (po.cmd != null && !po.cmd.isBlank()) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)po.cmd.replace("%player%", p.getName()));
        }
        this.plugin.dataMgr.recordCrateOpen(p.getUniqueId(), po.crate, po.rewardName, po.reward);
        String crateDisplayRaw = this.getCrateDisplayRaw(po.crate);
        String selfTpl = this.plugin.cfg.config.getString("messages.reward-received", "&#0fe30fYou received &f%reward% &7from the &f%crate% &7crate!");
        String rewardInsert = "&r" + po.rewardName;
        String crateInsert = "&r" + crateDisplayRaw;
        selfTpl = selfTpl.replace("%player%", p.getName()).replace("%crate%", crateInsert).replace("%reward%", rewardInsert);
        this.plugin.msg(p, selfTpl);
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
                    String out = line.replace("{player}", p.getName()).replace("{crate}", "&r" + crateDisplayRaw).replace("{item-name}", "&r" + po.rewardName).replace("{item-chance}", chanceText);
                    online.sendMessage(Utils.toComponent(out));
                }
            }
        }
        p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.claim", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP), 1.0f, 1.0f);
        String behavior = this.plugin.cfg.config.getString("after-reward-collect", "CLOSE").toUpperCase(Locale.ROOT);
        if ("BACK_TO_CRATE_UI".equals(behavior)) {
            p.openInventory(this.plugin.guiCrate.build(po.crate, false));
        } else if ("STAY_OPEN".equals(behavior)) {
            boolean stillHasKey = po.virtual ? this.plugin.dataMgr.getKeys(p, po.keyId) > 0 : this.keyUtil.hasPhysicalKey(p, po.keyId);
            if (stillHasKey) {
                p.openInventory(this.plugin.guiCrate.build(po.crate, false));
            } else {
                p.closeInventory();
            }
        } else {
            p.closeInventory();
        }
        this.pendingOpens.remove(po.player);
    }

    private String getRewardNameForChat(ItemStack reward, String fallbackKey) {
        if (reward != null) {
            ItemMeta meta = reward.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
                return LegacyComponentSerializer.legacySection().serialize(meta.displayName());
            }
            Object pretty = reward.getType().name().toLowerCase(Locale.ROOT).replace("_", " ");
            pretty = !((String)pretty).isEmpty() ? Character.toUpperCase(((String)pretty).charAt(0)) + ((String)pretty).substring(1) : "Item";
            return Utils.formatColors("&f" + (String)pretty);
        }
        return Utils.formatColors("&f" + (fallbackKey == null ? "Reward" : fallbackKey));
    }

    private String getCrateDisplayRaw(String crateId) {
        String raw2;
        try {
            raw2 = this.plugin.getCrateDisplayNameRaw(crateId);
            if (raw2 != null && !raw2.isBlank()) {
                return raw2;
            }
        }
        catch (Throwable ignored) {
            // empty catch block
        }
        raw2 = this.plugin.cfg.crates.getString("Crates." + crateId + ".displayname", null);
        if (raw2 == null || raw2.isBlank()) {
            return "&7" + crateId;
        }
        return raw2;
    }

    private static class PendingOpen {
        final UUID player;
        final String crate;
        final String keyId;
        final boolean virtual;
        final String chosenKey;
        final boolean give;
        final String cmd;
        final ItemStack reward;
        final String rewardName;
        boolean completed = false;

        PendingOpen(UUID player, String crate, String keyId, boolean virtual, String chosenKey, boolean give, String cmd, ItemStack reward, String rewardName) {
            this.player = player;
            this.crate = crate;
            this.keyId = keyId;
            this.virtual = virtual;
            this.chosenKey = chosenKey;
            this.give = give;
            this.cmd = cmd;
            this.reward = reward;
            this.rewardName = rewardName;
        }
    }

    private static class ChosenReward {
        final String key;
        final ConfigurationSection section;

        ChosenReward(String key, ConfigurationSection section) {
            this.key = key;
            this.section = section;
        }
    }
}

