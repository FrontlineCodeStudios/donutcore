package ro.andreilarazboi.donutcore.sell;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;

@SuppressWarnings({"deprecation", "removal", "unchecked", "rawtypes"})
public final class SellPacketListener {
    private static final String HANDLER_PREFIX = "donutsell_lore_";
    private static final String PACKET_SET_SLOT = "ClientboundContainerSetSlotPacket";
    private static final String PACKET_SET_CONTENT = "ClientboundContainerSetContentPacket";
    private final DonutSell plugin;
    private final ReflectionBridge reflectionBridge;
    private final NamespacedKey noWorthLoreKey;
    private final Map<UUID, String> handlerNames = new ConcurrentHashMap<UUID, String>();
    private final Set<UUID> noWorthOpen = ConcurrentHashMap.newKeySet();
    private List<String> loreTemplate = List.of();
    private List<String> lorePlainPrefixes = List.of();
    private boolean displayWorthLore;
    private boolean worthLorePerItem;
    private boolean hideWorthLoreInNonWhitelistedGuis;
    private List<String> worthLoreGuiWhitelist = List.of();
    private Set<String> disabledItems = Set.of();

    public SellPacketListener(DonutSell plugin) {
        this.plugin = plugin;
        this.reflectionBridge = new ReflectionBridge(plugin);
        this.noWorthLoreKey = new NamespacedKey(plugin.getPlugin(), "no_worth_lore");
        this.loadConfigData();
    }

    public DonutSell plugin() {
        return this.plugin;
    }

    public void loadConfigData() {
        this.loreTemplate = this.plugin.getConfig().getStringList("lore");
        this.lorePlainPrefixes = this.loreTemplate.stream().map(line -> ChatColor.stripColor((String)Utils.formatColors(line.replace("%amount%", ""))).toLowerCase(Locale.ROOT).trim()).collect(Collectors.toList());
        this.displayWorthLore = this.plugin.getConfig().getBoolean("display-worth-lore", true);
        this.worthLorePerItem = this.plugin.getConfig().getBoolean("worth-lore-per-item", false);
        this.hideWorthLoreInNonWhitelistedGuis = this.plugin.getConfig().getBoolean("worth-lore-hide-in-non-whitelisted-guis", false);
        this.worthLoreGuiWhitelist = this.plugin.getConfig().getStringList("worth-lore-whitelist-gui-names").stream().map(String::toLowerCase).collect(Collectors.toList());
        this.disabledItems = this.plugin.getConfig().getStringList("disabled-items").stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(HashSet::new));
    }

    public void reloadConfigData() {
        this.loadConfigData();
    }

    public void injectOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.inject(player);
        }
    }

    public void inject(final Player player) {
        String handlerName = HANDLER_PREFIX + String.valueOf(player.getUniqueId());
        this.handlerNames.put(player.getUniqueId(), handlerName);
        Channel channel = this.reflectionBridge.channelOf(player);
        if (channel == null || channel.pipeline().get(handlerName) != null) {
            return;
        }
        ChannelDuplexHandler handler = new ChannelDuplexHandler(){
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                Object rewritten = SellPacketListener.this.rewritePacket(player, msg);
                super.write(ctx, rewritten, promise);
            }
        };
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) == null) {
                    channel.pipeline().addBefore("packet_handler", handlerName, (ChannelHandler)handler);
                }
            }
            catch (Throwable throwable) {
                this.plugin.getLogger().warning("Failed to inject packet handler for " + player.getName() + ": " + throwable.getMessage());
            }
        });
    }

    public void uninject(Player player) {
        String handlerName = this.handlerNames.remove(player.getUniqueId());
        this.noWorthOpen.remove(player.getUniqueId());
        if (handlerName == null) {
            return;
        }
        Channel channel = this.reflectionBridge.channelOf(player);
        if (channel == null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.uninject(player);
        }
    }

    public void setNoWorthOpen(Player player, boolean disabled) {
        if (disabled) {
            this.noWorthOpen.add(player.getUniqueId());
        } else {
            this.noWorthOpen.remove(player.getUniqueId());
        }
    }

    public boolean isNoWorthInventory(InventoryView view) {
        if (!this.hideWorthLoreInNonWhitelistedGuis) {
            return false;
        }
        if (view == null) {
            return false;
        }
        if (view.getTopInventory() == null || view.getTopInventory().getType() == InventoryType.PLAYER) {
            return false;
        }
        String title = ChatColor.stripColor((String)view.getTitle());
        if (title == null) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        boolean whitelisted = this.worthLoreGuiWhitelist.stream().anyMatch(lower::contains);
        return !whitelisted;
    }

    private Object rewritePacket(Player player, Object packet) {
        if (packet == null) {
            return packet;
        }
        if (!this.displayWorthLore || !this.plugin.isWorthEnabled(player.getUniqueId()) || player.getGameMode() == GameMode.CREATIVE || this.hasCursorItem(player)) {
            return this.stripPacket(packet);
        }
        String simple = packet.getClass().getSimpleName();
        if (PACKET_SET_SLOT.equals(simple)) {
            return this.rewriteSetSlotPacket(packet, player.getUniqueId());
        }
        if (PACKET_SET_CONTENT.equals(simple)) {
            return this.rewriteSetContentPacket(packet, player.getUniqueId(), this.noWorthOpen.contains(player.getUniqueId()));
        }
        return packet;
    }

    private Object stripPacket(Object packet) {
        String simple = packet.getClass().getSimpleName();
        if (PACKET_SET_SLOT.equals(simple)) {
            return this.rewriteSetSlotPacket(packet, null);
        }
        if (PACKET_SET_CONTENT.equals(simple)) {
            return this.rewriteSetContentPacket(packet, null, true);
        }
        return packet;
    }

    private boolean hasCursorItem(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        return cursor != null && cursor.getType() != Material.AIR && cursor.getAmount() > 0;
    }

    private Object rewriteSetSlotPacket(Object packet, UUID playerId) {
        try {
            Object originalItem = this.reflectionBridge.findNmsItemField(packet);
            if (originalItem == null) {
                return packet;
            }
            Object replaced = playerId == null ? this.stripWorthLore(originalItem) : this.withWorthLore(originalItem, playerId);
            Object cloned = this.reflectionBridge.cloneSetSlotPacket(packet, replaced);
            return cloned == null ? packet : cloned;
        }
        catch (Throwable throwable) {
            return packet;
        }
    }

    private Object rewriteSetContentPacket(Object packet, UUID playerId, boolean disableTopContainerLore) {
        try {
            List<Object> nmsItems = this.reflectionBridge.findNmsItemListField(packet);
            if (nmsItems == null) {
                return packet;
            }
            int contSlots = this.reflectionBridge.extractContainerSlots(packet, nmsItems.size());
            ArrayList<Object> replacedItems = new ArrayList<Object>(nmsItems.size());
            for (int i = 0; i < nmsItems.size(); ++i) {
                boolean inTop;
                Object nmsItem = nmsItems.get(i);
                boolean bl = inTop = contSlots > 0 && i < contSlots;
                if (playerId == null || disableTopContainerLore && inTop) {
                    replacedItems.add(this.stripWorthLore(nmsItem));
                    continue;
                }
                replacedItems.add(this.withWorthLore(nmsItem, playerId));
            }
            Object carried = this.reflectionBridge.findNmsItemField(packet);
            if (carried == null) {
                return packet;
            }
            Object replacedCarried = playerId == null ? this.stripWorthLore(carried) : this.withWorthLore(carried, playerId);
            Object cloned = this.reflectionBridge.cloneSetContentPacket(packet, replacedItems, replacedCarried);
            return cloned == null ? packet : cloned;
        }
        catch (Throwable throwable) {
            return packet;
        }
    }

    private Object stripWorthLore(Object nmsItem) {
        ItemStack bukkit = this.reflectionBridge.toBukkitCopy(nmsItem);
        if (bukkit == null || bukkit.getType() == Material.AIR || bukkit.getAmount() <= 0) {
            return nmsItem;
        }
        ItemMeta meta = bukkit.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return nmsItem;
        }
        ArrayList<String> filtered = new ArrayList<String>();
        for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor((String)line).toLowerCase(Locale.ROOT).trim();
            if (!this.lorePlainPrefixes.stream().noneMatch(plain::startsWith)) continue;
            filtered.add(line);
        }
        meta.setLore(filtered.isEmpty() ? null : filtered);
        bukkit.setItemMeta(meta);
        Object rebuilt = this.reflectionBridge.toNmsCopy(bukkit);
        return rebuilt == null ? nmsItem : rebuilt;
    }

    private Object withWorthLore(Object nmsItem, UUID playerId) {
        double valueToShow;
        BlockStateMeta bsm;
        BlockState blockState;
        ItemStack bukkit = this.reflectionBridge.toBukkitCopy(nmsItem);
        if (bukkit == null || bukkit.getType() == Material.AIR || bukkit.getAmount() <= 0) {
            return nmsItem;
        }
        ItemMeta meta = bukkit.getItemMeta();
        if (meta == null || this.disabledItems.contains(bukkit.getType().name())) {
            return nmsItem;
        }
        Byte noWorthLore = (Byte)meta.getPersistentDataContainer().get(this.noWorthLoreKey, PersistentDataType.BYTE);
        if (noWorthLore != null && noWorthLore == 1) {
            return this.stripWorthLore(nmsItem);
        }
        if (meta instanceof BlockStateMeta && (blockState = (bsm = (BlockStateMeta)meta).getBlockState()) instanceof ShulkerBox) {
            ShulkerBox box = (ShulkerBox)blockState;
            String boxKey = bukkit.getType().name().toLowerCase(Locale.ROOT) + "-value";
            double boxUnitPrice = this.plugin.getPrice(boxKey);
            String boxCat = this.plugin.categoryItems.entrySet().stream().filter(e -> ((List)e.getValue()).contains(bukkit.getType().name())).map(Map.Entry::getKey).findFirst().orElse(null);
            double boxMult = boxCat != null ? this.plugin.getSellMultiplier(playerId, boxCat) : 1.0;
            double worthYamlPerBox = boxUnitPrice;
            double perBoxTotal = boxUnitPrice * boxMult;
            for (ItemStack inside : box.getInventory().getContents()) {
                if (inside == null || inside.getType() == Material.AIR || this.disabledItems.contains(inside.getType().name())) continue;
                double insideRaw = this.plugin.calculateItemWorth(inside);
                worthYamlPerBox += insideRaw;
                String insideCat = this.plugin.categoryItems.entrySet().stream().filter(e -> ((List)e.getValue()).contains(inside.getType().name())).map(Map.Entry::getKey).findFirst().orElse(null);
                double insideMult = insideCat != null ? this.plugin.getSellMultiplier(playerId, insideCat) : 1.0;
                perBoxTotal += insideRaw * insideMult;
            }
            double normalUnitValue = perBoxTotal;
            valueToShow = this.plugin.getWorthLoreValue(playerId, bukkit, normalUnitValue, this.worthLorePerItem);
        } else {
            double baseVal;
            if (bukkit.getType() == Material.SPAWNER && meta instanceof BlockStateMeta) {
                CreatureSpawner cs;
                BlockStateMeta bsm2 = (BlockStateMeta)meta;
                BlockState boxMult = bsm2.getBlockState();
                if (boxMult instanceof CreatureSpawner && (cs = (CreatureSpawner)boxMult).getSpawnedType() != null) {
                    String spawnerKey = cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
                    baseVal = this.plugin.getPrice(spawnerKey);
                } else {
                    baseVal = this.plugin.getPrice("spawner-value");
                }
            } else {
                String pKey = this.getPotionKey(bukkit);
                baseVal = pKey != null ? this.plugin.getPrice(pKey + "-value") : this.plugin.getPrice(bukkit.getType().name().toLowerCase(Locale.ROOT) + "-value");
            }
            double enchVal = 0.0;
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta)meta;
                for (Map.Entry e2 : esm.getStoredEnchants().entrySet()) {
                    enchVal += this.plugin.getPrice(((Enchantment)e2.getKey()).getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(e2.getValue()) + "-value");
                }
            }
            for (Map.Entry entry : meta.getEnchants().entrySet()) {
                enchVal += this.plugin.getPrice(((Enchantment)entry.getKey()).getKey().getKey().toLowerCase(Locale.ROOT) + String.valueOf(entry.getValue()) + "-value");
            }
            double unitRaw = baseVal + enchVal;
            String cat = this.plugin.categoryItems.entrySet().stream().filter(e -> ((List)e.getValue()).contains(bukkit.getType().name())).map(Map.Entry::getKey).findFirst().orElse(null);
            double mult = cat != null ? this.plugin.getSellMultiplier(playerId, cat) : 1.0;
            double normalUnitValue = unitRaw * mult;
            valueToShow = this.plugin.getWorthLoreValue(playerId, bukkit, normalUnitValue, this.worthLorePerItem);
        }
        String display = Utils.abbreviateNumber(valueToShow);
        List<String> newLines = this.loreTemplate.stream().map(line -> Utils.formatColors(line.replace("%amount%", display))).collect(Collectors.toList());
        ArrayList<String> existing = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList();
        existing.removeIf(line -> {
            String plain = ChatColor.stripColor((String)line).toLowerCase(Locale.ROOT).trim();
            return this.lorePlainPrefixes.stream().anyMatch(plain::startsWith);
        });
        for (String nl : newLines) {
            if (existing.contains(nl)) continue;
            existing.add(nl);
        }
        meta.setLore(existing.isEmpty() ? null : existing);
        bukkit.setItemMeta(meta);
        Object rebuilt = this.reflectionBridge.toNmsCopy(bukkit);
        return rebuilt == null ? nmsItem : rebuilt;
    }

    private String getPotionKey(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof PotionMeta)) {
            return null;
        }
        PotionMeta pm = (PotionMeta)itemMeta;
        PotionData data = pm.getBasePotionData();
        if (data == null) {
            return null;
        }
        String base = data.getType().name().toLowerCase(Locale.ROOT);
        if (data.isExtended()) {
            base = "long_" + base;
        }
        if (data.isUpgraded()) {
            base = "strong_" + base;
        }
        if (item.getType() == Material.SPLASH_POTION) {
            base = "splash_" + base;
        } else if (item.getType() == Material.LINGERING_POTION) {
            base = "lingering_" + base;
        }
        return base;
    }

    private static final class ReflectionBridge {
        private final DonutSell plugin;
        private final Class<?> craftPlayerClass;
        private final Class<?> craftItemStackClass;
        private final Method getHandleMethod;
        private final Method asBukkitCopyMethod;
        private final Method asNmsCopyMethod;

        ReflectionBridge(DonutSell plugin) {
            this.plugin = plugin;
            try {
                this.craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
                this.craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                this.getHandleMethod = this.craftPlayerClass.getMethod("getHandle", new Class[0]);
                this.asBukkitCopyMethod = this.craftItemStackClass.getMethod("asBukkitCopy", Class.forName("net.minecraft.world.item.ItemStack"));
                this.asNmsCopyMethod = this.craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            }
            catch (Throwable throwable) {
                throw new IllegalStateException("Failed to initialize reflection bridge", throwable);
            }
        }

        Channel channelOf(Player player) {
            try {
                Object craftPlayer = this.craftPlayerClass.cast(player);
                Object serverPlayer = this.getHandleMethod.invoke(craftPlayer, new Object[0]);
                Field connectionField = serverPlayer.getClass().getField("connection");
                Object connection = connectionField.get(serverPlayer);
                Field connectionInnerField = connection.getClass().getField("connection");
                Object networkManager = connectionInnerField.get(connection);
                for (Field field : networkManager.getClass().getDeclaredFields()) {
                    if (!Channel.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object channel = field.get(networkManager);
                    if (!(channel instanceof Channel)) continue;
                    Channel ch = (Channel)channel;
                    return ch;
                }
            }
            catch (Throwable throwable) {
                this.plugin.getLogger().warning("Could not find channel for player " + player.getName() + ": " + throwable.getMessage());
            }
            return null;
        }

        Object findNmsItemField(Object packet) throws IllegalAccessException {
            for (Field field : packet.getClass().getDeclaredFields()) {
                if (!field.getType().getName().equals("net.minecraft.world.item.ItemStack")) continue;
                field.setAccessible(true);
                return field.get(packet);
            }
            return null;
        }

        List<Object> findNmsItemListField(Object packet) throws IllegalAccessException {
            for (Field field : packet.getClass().getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(packet);
                if (!(value instanceof List)) continue;
                List list = (List)value;
                if (list.isEmpty()) {
                    return list;
                }
                Object first = list.get(0);
                if (first == null || !first.getClass().getName().equals("net.minecraft.world.item.ItemStack")) continue;
                return list;
            }
            return null;
        }

        int extractContainerSlots(Object packet, int totalSlots) {
            try {
                int windowId = this.readIntByOrder(packet, 0);
                if (windowId == 0) {
                    return 0;
                }
                int invSlots = 36;
                return Math.max(0, totalSlots - invSlots);
            }
            catch (Throwable ignored) {
                return 0;
            }
        }

        Object cloneSetSlotPacket(Object packet, Object newItem) {
            try {
                int a = this.readIntByOrder(packet, 0);
                int b = this.readIntByOrder(packet, 1);
                int c = this.readIntByOrder(packet, 2);
                for (Constructor<?> constructor : packet.getClass().getDeclaredConstructors()) {
                    Class<?>[] types = constructor.getParameterTypes();
                    if (types.length != 4 || types[0] != Integer.TYPE || types[1] != Integer.TYPE || types[2] != Integer.TYPE || !types[3].getName().equals("net.minecraft.world.item.ItemStack")) continue;
                    constructor.setAccessible(true);
                    return constructor.newInstance(a, b, c, newItem);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return null;
        }

        Object cloneSetContentPacket(Object packet, List<Object> newItems, Object carried) {
            try {
                int a = this.readIntByOrder(packet, 0);
                int b = this.readIntByOrder(packet, 1);
                for (Constructor<?> constructor : packet.getClass().getDeclaredConstructors()) {
                    Class<?>[] types = constructor.getParameterTypes();
                    if (types.length != 4 || types[0] != Integer.TYPE || types[1] != Integer.TYPE || !List.class.isAssignableFrom(types[2]) || !types[3].getName().equals("net.minecraft.world.item.ItemStack")) continue;
                    constructor.setAccessible(true);
                    return constructor.newInstance(a, b, newItems, carried);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return null;
        }

        private int readIntByOrder(Object packet, int index) throws IllegalAccessException {
            int current = 0;
            for (Field field : packet.getClass().getDeclaredFields()) {
                if (field.getType() != Integer.TYPE) continue;
                field.setAccessible(true);
                int value = field.getInt(packet);
                if (current == index) {
                    return value;
                }
                ++current;
            }
            return 0;
        }

        ItemStack toBukkitCopy(Object nmsItem) {
            try {
                ItemStack stack;
                Object result = this.asBukkitCopyMethod.invoke(null, nmsItem);
                return result instanceof ItemStack ? (stack = (ItemStack)result) : null;
            }
            catch (Throwable ignored) {
                return null;
            }
        }

        Object toNmsCopy(ItemStack bukkitItem) {
            try {
                return this.asNmsCopyMethod.invoke(null, bukkitItem);
            }
            catch (Throwable ignored) {
                return null;
            }
        }
    }
}

