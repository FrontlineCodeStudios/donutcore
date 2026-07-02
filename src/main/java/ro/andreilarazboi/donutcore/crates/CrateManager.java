
package ro.andreilarazboi.donutcore.crates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CrateManager {
    private final DonutCrates plugin;
    public final Map<String, Block> crateBlocks = new HashMap<String, Block>();
    private final Map<String, Long> openCooldowns = new HashMap<String, Long>();

    public CrateManager(DonutCrates pl) {
        this.plugin = pl;
        ConfigurationSection ss = this.plugin.cfg.saves.getConfigurationSection("crateBlocks");
        if (ss != null) {
            for (String name : ss.getKeys(false)) {
                String coord = ss.getString(name);
                if (coord == null) continue;
                String[] p = coord.split(",");
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.getPlugin(), () -> {
                    World w = Bukkit.getWorld(p[0]);
                    if (w == null) {
                        this.plugin.getLogger().warning("[DonutCrates] Skipping crate '" + name + "': world '" + p[0] + "' not found.");
                        return;
                    }
                    try {
                        int x = Integer.parseInt(p[1]);
                        int y = Integer.parseInt(p[2]);
                        int z = Integer.parseInt(p[3]);
                        Block b = w.getBlockAt(x, y, z);
                        this.crateBlocks.put(name, b);
                    }
                    catch (Exception ex) {
                        this.plugin.getLogger().warning("[DonutCrates] Invalid location for crate '" + name + "': " + coord);
                    }
                }, 1L);
            }
        }
    }

    public boolean crateExists(String n) {
        return this.crateBlocks.containsKey(n);
    }

    public void saveBlocks() {
        this.plugin.cfg.saves.set("crateBlocks", null);
        ConfigurationSection ss = this.plugin.cfg.saves.createSection("crateBlocks");
        this.crateBlocks.forEach((n, b) -> {
            if (b == null || b.getWorld() == null) {
                return;
            }
            ss.set(n, (Object)(b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ()));
        });
        this.plugin.cfg.saveAll();
    }

    public void createCrate(String name, Block b, Player p) {
        this.crateBlocks.put(name, b);
        String base = "Crates." + name;
        this.plugin.cfg.crates.createSection(base);
        this.plugin.cfg.crates.set(base + ".rows", (Object)3);
        this.plugin.cfg.crates.set(base + ".fillerEnabled", (Object)true);
        this.plugin.cfg.crates.set(base + ".fillerMaterial", (Object)this.plugin.cfg.config.getString("fillerMaterial", "GRAY_STAINED_GLASS_PANE"));
        this.plugin.cfg.crates.set(base + ".title", (Object)"&#444444\u1d04\u029c\u1d0f\u1d0f\u0455\u1d07 1 \u0280\u1d07\u1d21\u1d00\u0280\u1d05");
        this.plugin.cfg.crates.set(base + ".fillerLore", List.of("&7You can choose different rewards", "&7from this crate!"));
        this.plugin.cfg.crates.set(base + ".Hologram.enabled", (Object)false);
        this.plugin.cfg.crates.set(base + ".Hologram.offsetY", (Object)1.5);
        this.plugin.cfg.crates.set(base + ".Hologram.lines", new ArrayList<>());
        this.plugin.cfg.crates.set(base + ".random.enabled", (Object)false);
        ConfigurationSection items = this.plugin.cfg.crates.createSection(base + ".Items");
        this.addSample(items, "Item1", "DIAMOND_SWORD", 1, 10, "&7Example reward");
        this.addSample(items, "Item2", "DIAMOND_PICKAXE", 1, 11, "&7Example reward");
        this.addSample(items, "Item3", "DIAMOND_BOOTS", 1, 12, "&7Example reward");
        this.addSample(items, "Item4", "DIAMOND_LEGGINGS", 1, 13, "&7Example reward");
        this.addSample(items, "Item5", "DIAMOND_CHESTPLATE", 1, 14, "&7Example reward");
        this.addSample(items, "Item6", "DIAMOND_HELMET", 1, 15, "&7Example reward");
        this.addSample(items, "Item7", "GOLDEN_APPLE", 16, 16, "&7Example reward");
        this.plugin.cfg.saveAll();
        this.saveBlocks();
        this.plugin.holoMgr.refreshCrate(name);
        if (p != null) {
            this.plugin.msg(p, "&#0fe30fCreated crate &f" + name + " &#0fe30f at your target block.");
        }
    }

    private void addSample(ConfigurationSection items, String key, String material, int amount, int slot, String lore) {
        String path = key + ".";
        items.createSection(key);
        items.set(path + "material", (Object)material);
        items.set(path + "displayname", (Object)"&#0fe30fExample Reward");
        items.set(path + "command", (Object)"");
        items.set(path + "amount", (Object)amount);
        items.set(path + "slot", (Object)slot);
        items.set(path + "giveitem", (Object)true);
        items.set(path + "lore", List.of(lore));
        items.set(path + "enchantments", List.of("UNBREAKING;3"));
        items.set(path + "chance", (Object)0.0);
        items.set(path + "broadcast.enabled", (Object)false);
    }

    public void deleteCrate(String name, Player p) {
        this.crateBlocks.remove(name);
        this.plugin.cfg.crates.set("Crates." + name, null);
        this.plugin.cfg.saveAll();
        this.saveBlocks();
        this.plugin.holoMgr.removeCrate(name);
        if (p != null) {
            this.plugin.msg(p, "&#d61111Deleted crate &f" + name);
        }
    }

    public void moveCrate(String name, Block b, Player p) {
        this.crateBlocks.put(name, b);
        this.saveBlocks();
        this.plugin.holoMgr.refreshCrate(name);
        if (p != null) {
            this.plugin.msg(p, "&#0fe30fMoved crate &f" + name + " &#0fe30f to the new block.");
        }
    }

    public void removeItem(String crate, String key, Player p) {
        String path = "Crates." + crate + ".Items." + key;
        if (this.plugin.cfg.crates.getConfigurationSection(path) == null) {
            if (p != null) {
                this.plugin.msg(p, "&#d61111Item &f" + key + " &#d61111not found.");
            }
            return;
        }
        this.plugin.cfg.crates.set(path, null);
        this.plugin.cfg.saveAll();
        if (p != null) {
            this.plugin.msg(p, "&#d61111Removed item &f" + key + " &#d61111from crate &f" + crate);
        }
    }

    @SuppressWarnings("removal")
    public void addItemFromStack(String crate, ItemStack original, Player p) {
        if (original == null || original.getType().isAir()) {
            if (p != null) {
                this.plugin.msg(p, "&#d61111Hold an item or drag it onto the add-reward slot.");
            }
            return;
        }
        ConfigurationSection itemsSec = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate + ".Items");
        if (itemsSec == null) {
            if (p != null) {
                this.plugin.msg(p, "&#d61111No crate &f" + crate + " &#d61111found.");
            }
            return;
        }
        int idx = itemsSec.getKeys(false).stream().mapToInt(k -> Integer.parseInt(k.replaceAll("\\D", ""))).max().orElse(0) + 1;
        String key = "Item" + idx;
        String base = "Crates." + crate + ".Items." + key;
        int rows = this.plugin.cfg.crates.getInt("Crates." + crate + ".rows", 3);
        int size = Math.max(9, Math.min(54, rows * 9));
        HashSet<Integer> used = new HashSet<Integer>();
        for (String k2 : itemsSec.getKeys(false)) {
            ConfigurationSection it = itemsSec.getConfigurationSection(k2);
            if (it == null) continue;
            used.add(it.getInt("slot", 0));
        }
        int slot = 0;
        for (int i = 0; i < size; ++i) {
            if (used.contains(i)) continue;
            slot = i;
            break;
        }
        ItemStack item = original.clone();
        this.plugin.cfg.crates.set(base + ".item", (Object)item);
        this.plugin.cfg.crates.set(base + ".amount", (Object)item.getAmount());
        this.plugin.cfg.crates.set(base + ".slot", (Object)slot);
        this.plugin.cfg.crates.set(base + ".giveitem", (Object)true);
        this.plugin.cfg.crates.set(base + ".material", (Object)item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ArmorMeta am;
            if (meta.hasDisplayName()) {
                this.plugin.cfg.crates.set(base + ".displayname", (Object)LegacyComponentSerializer.legacySection().serialize(meta.displayName()));
            }
            if (meta.hasLore() && meta.lore() != null) {
                this.plugin.cfg.crates.set(base + ".lore", meta.lore().stream().map(c -> LegacyComponentSerializer.legacySection().serialize(c)).collect(Collectors.toList()));
            }
            if (meta.isUnbreakable()) {
                this.plugin.cfg.crates.set(base + ".unbreakable", (Object)true);
            }
            List<String> enchants = item.getEnchantments().entrySet().stream().map(e -> e.getKey().getKey().getKey() + ";" + String.valueOf(e.getValue())).toList();
            this.plugin.cfg.crates.set(base + ".enchantments", enchants);
            if (meta instanceof ArmorMeta && (am = (ArmorMeta)meta).hasTrim()) {
                ArmorTrim t = am.getTrim();
                String trimMat = t.getMaterial().getKey().toString();
                String trimPat = t.getPattern().getKey().toString();
                this.plugin.cfg.crates.set(base + ".trim.material", (Object)trimMat);
                this.plugin.cfg.crates.set(base + ".trim.pattern", (Object)trimPat);
            }
        }
        this.plugin.cfg.crates.set(base + ".chance", (Object)0.0);
        this.plugin.cfg.crates.set(base + ".broadcast.enabled", (Object)false);
        this.plugin.cfg.saveAll();
        if (p != null) {
            this.plugin.msg(p, "&#0fe30fAdded reward &f" + key + " &#0fe30f to crate &f" + crate);
        }
    }

    public void keyGiveAll(final String crate, final int amt, final CommandSender sender) {
        final ArrayList<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        final int batch = this.plugin.cfg.config.getInt("keygiveall.batch-size", 100);
        int period = this.plugin.cfg.config.getInt("keygiveall.ticks-per-batch", 1);
        final String perTpl = this.plugin.cfg.config.getString("messages.keyreceive", "&#0fe30fYou received %amount% %crate% keys!");
        final String allTpl = this.plugin.cfg.config.getString("messages.keyall", "&#0f99e3A %crate% keyall has been given to %players% players");
        new BukkitRunnable(){
            int idx = 0;

            public void run() {
                int end = Math.min(this.idx + batch, players.size());
                while (this.idx < end) {
                    Player p = players.get(this.idx);
                    CrateManager.this.plugin.dataMgr.modifyKeys(p, crate, amt);
                    String raw = perTpl.replace("%amount%", String.valueOf(amt)).replace("%crate%", crate);
                    CrateManager.this.plugin.msg(p, raw);
                    ++this.idx;
                }
                if (this.idx >= players.size()) {
                    String rawAll = allTpl.replace("%players%", String.valueOf(players.size())).replace("%amount%", String.valueOf(amt)).replace("%crate%", crate);
                    net.kyori.adventure.text.Component broadcastMsg = Utils.toComponent(CrateManager.this.plugin.getPrefix() + rawAll);
                    for (Player onlineP : Bukkit.getOnlinePlayers()) {
                        onlineP.sendMessage(broadcastMsg);
                    }
                    if (sender != null) {
                        CrateManager.this.plugin.msg(sender, "&#0fe30fKeyall complete. &7(Players: " + players.size() + ")");
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin.getPlugin(), 0L, Math.max(1L, (long)period));
    }

    public boolean isCrateBlock(Block b) {
        return this.crateBlocks.containsValue(b);
    }

    public String getCrateByBlock(Block b) {
        return this.crateBlocks.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().equals(b)).map(e -> e.getKey()).findFirst().orElse(null);
    }

    public boolean isRandomMode(String crate) {
        return this.plugin.cfg.crates.getBoolean("Crates." + crate + ".random.enabled", false);
    }

    public boolean checkAndMarkCooldown(Player p, String crate) {
        long elapsed;
        int seconds = this.plugin.cfg.config.getInt("crate-open-cooldown-seconds", 0);
        if (seconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        String key = p.getUniqueId().toString() + ":" + crate.toLowerCase(Locale.ROOT);
        long cdMillis = (long)seconds * 1000L;
        Long last = this.openCooldowns.get(key);
        if (last != null && (elapsed = now - last) < cdMillis) {
            long remainingSec = (cdMillis - elapsed + 999L) / 1000L;
            String raw = this.plugin.cfg.config.getString("messages.cooldown", "&#d61111You must wait &f%seconds%s &#d61111before opening this crate again.").replace("%seconds%", String.valueOf(remainingSec));
            this.plugin.msg(p, raw);
            return false;
        }
        this.openCooldowns.put(key, now);
        return true;
    }

    public void openRandom(String crate, Player p) {
        List<String> lines;
        boolean allZero;
        int keys = this.plugin.dataMgr.getKeys(p, crate);
        if (keys <= 0) {
            String raw = this.plugin.cfg.config.getString("messages.no-keys", "&#ff5555You don't have any keys for this crate!").replace("%crate%", crate);
            this.plugin.msg(p, raw);
            p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.no-key", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
            return;
        }
        ConfigurationSection sec = this.plugin.cfg.crates.getConfigurationSection("Crates." + crate);
        if (sec == null) {
            return;
        }
        ConfigurationSection items = sec.getConfigurationSection("Items");
        if (items == null || items.getKeys(false).isEmpty()) {
            return;
        }
        ArrayList<ConfigurationSection> sections = new ArrayList<ConfigurationSection>();
        ArrayList<Double> weights = new ArrayList<Double>();
        double total = 0.0;
        for (String key : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(key);
            if (it == null) continue;
            double chance = it.getDouble("chance", 0.0);
            if (chance < 0.0) {
                chance = 0.0;
            }
            sections.add(it);
            weights.add(chance);
            total += chance;
        }
        if (sections.isEmpty()) {
            return;
        }
        allZero = total <= 0.0;
        if (allZero) {
            total = sections.size();
            weights.clear();
            for (int i = 0; i < sections.size(); ++i) {
                weights.add(1.0);
            }
        }
        double r = Math.random() * total;
        ConfigurationSection chosen = null;
        for (int i = 0; i < sections.size(); ++i) {
            double w = weights.get(i);
            if (w <= 0.0) continue;
            if (r < w) {
                chosen = sections.get(i);
                break;
            }
            r -= w;
        }
        if (chosen == null) {
            chosen = sections.get(sections.size() - 1);
        }
        boolean give = chosen.getBoolean("giveitem", true);
        ItemStack reward = this.plugin.guiItemUtil.buildItemFromSection(chosen);
        String cmd = chosen.getString("command", "");
        if (give && !this.canFit((Inventory)p.getInventory(), reward)) {
            String raw = this.plugin.cfg.config.getString("messages.inventory-full", "&#ff5555Your inventory is full!");
            this.plugin.msg(p, raw);
            p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.no-key", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
            return;
        }
        this.plugin.dataMgr.modifyKeys(p, crate, -1);
        if (give) {
            p.getInventory().addItem(new ItemStack[]{reward.clone()});
        }
        if (cmd != null && !cmd.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }
        String itemName = this.plugin.getRewardDisplayNameForChat(reward);
        double chanceVal = chosen.getDouble("chance", 0.0);
        String chanceStr = chanceVal > 0.0 ? String.format(Locale.US, "%.2f%%", chanceVal) : String.format(Locale.US, "%.2f%%", 100.0 / (double)sections.size());
        String selfTpl = this.plugin.cfg.config.getString("messages.reward-self", "&#0fe30fYou opened &f{crate} &#0fe30fand received &f{item-name}&7!");
        String selfMsg = selfTpl.replace("{player}", p.getName()).replace("{crate}", crate).replace("{item-name}", itemName).replace("{item-chance}", chanceStr);
        this.plugin.msg(p, selfMsg);
        if (chosen.getBoolean("broadcast.enabled", false) && !(lines = this.plugin.cfg.config.getStringList("broadcast-message")).isEmpty()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                for (String rawLine : lines) {
                    if (rawLine == null) continue;
                    if (rawLine.isEmpty()) {
                        online.sendMessage("");
                        continue;
                    }
                    String line = rawLine.replace("{player}", p.getName()).replace("{crate}", crate).replace("{item-name}", itemName).replace("{item-chance}", chanceStr);
                    online.sendMessage(Utils.toComponent(this.plugin.getPrefix() + line));
                }
            }
        }
        p.playSound(p.getLocation(), Utils.resolveSound(this.plugin.cfg.config.getString("sounds.claim", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP), 1.0f, 1.0f);
    }

    private boolean canFit(Inventory inv, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return true;
        }
        if (inv.firstEmpty() != -1) {
            return true;
        }
        int toPlace = stack.getAmount();
        int max = stack.getMaxStackSize();
        for (ItemStack c : inv.getContents()) {
            int free;
            if (c == null || !c.isSimilar(stack) || (free = max - c.getAmount()) <= 0 || (toPlace -= free) > 0) continue;
            return true;
        }
        return false;
    }
}

