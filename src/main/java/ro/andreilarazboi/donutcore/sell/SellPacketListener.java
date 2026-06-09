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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import org.bukkit.potion.PotionType;

public final class SellPacketListener {
    private static final String HANDLER_PREFIX = "donutsell_lore_";
    private static final String PACKET_SET_SLOT = "ClientboundContainerSetSlotPacket";
    private static final String PACKET_SET_CONTENT = "ClientboundContainerSetContentPacket";
    private final DonutSell plugin;
    private final ReflectionBridge reflectionBridge;
    private final Map<UUID, String> handlerNames = new ConcurrentHashMap<>();
    private final Set<UUID> noWorthOpen = ConcurrentHashMap.newKeySet();
    private List<String> loreTemplate = List.of();
    private List<String> lorePlainPrefixes = List.of();
    private boolean displayWorthLore;
    private boolean worthLorePerItem;
    private List<String> worthLoreGuiWhitelist = List.of();
    private Set<String> disabledItems = Set.of();

    public SellPacketListener(DonutSell plugin) {
        this.plugin = plugin;
        this.reflectionBridge = new ReflectionBridge(plugin);
        this.loadConfigData();
    }

    public DonutSell plugin() {
        return this.plugin;
    }

    public void loadConfigData() {
        this.loreTemplate = this.plugin.getConfig().getStringList("lore");
        this.lorePlainPrefixes = this.loreTemplate.stream()
                .map(line -> Utils.stripColor(Utils.formatColors(line.replace("%amount%", "")))
                        .toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());
        this.displayWorthLore = this.plugin.getConfig().getBoolean("display-worth-lore", true);
        this.worthLorePerItem = this.plugin.getConfig().getBoolean("worth-lore-per-item", false);
        this.worthLoreGuiWhitelist = this.plugin.getConfig().getStringList("worth-lore-whitelist-gui-names")
                .stream().map(String::toLowerCase).collect(Collectors.toList());
        this.disabledItems = this.plugin.getConfig().getStringList("disabled-items")
                .stream().map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
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
        String handlerName = HANDLER_PREFIX + player.getUniqueId();
        this.handlerNames.put(player.getUniqueId(), handlerName);
        Channel channel = this.reflectionBridge.channelOf(player);
        if (channel == null || channel.pipeline().get(handlerName) != null) return;
        ChannelDuplexHandler handler = new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                Object rewritten = SellPacketListener.this.rewritePacket(player, msg);
                super.write(ctx, rewritten, promise);
            }
        };
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) == null) {
                    channel.pipeline().addBefore("packet_handler", handlerName, (ChannelHandler) handler);
                }
            } catch (Throwable throwable) {
                SellPacketListener.this.plugin.getPlugin().getLogger().warning(
                        "Failed to inject packet handler for " + player.getName() + ": " + throwable.getMessage());
            }
        });
    }

    public void uninject(Player player) {
        String handlerName = this.handlerNames.remove(player.getUniqueId());
        this.noWorthOpen.remove(player.getUniqueId());
        if (handlerName == null) return;
        Channel channel = this.reflectionBridge.channelOf(player);
        if (channel == null) return;
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
            } catch (Throwable ignored) {}
        });
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.uninject(player);
        }
    }

    public void setNoWorthOpen(Player player, boolean disabled) {
        if (disabled) this.noWorthOpen.add(player.getUniqueId());
        else this.noWorthOpen.remove(player.getUniqueId());
    }

    public boolean isNoWorthInventory(InventoryView view) {
        if (view == null) return false;
        if (view.getTopInventory() == null || view.getTopInventory().getType() == InventoryType.PLAYER) return false;
        String title = Utils.stripColor(view.getTitle());
        if (title == null) return false;
        String lower = title.toLowerCase(Locale.ROOT);
        return this.worthLoreGuiWhitelist.stream().noneMatch(lower::contains);
    }

    private Object rewritePacket(Player player, Object packet) {
        if (packet == null) return packet;
        if (!this.displayWorthLore || !this.plugin.isWorthEnabled(player.getUniqueId())
                || player.getGameMode() == GameMode.CREATIVE || this.hasCursorItem(player)) {
            return this.stripPacket(packet);
        }
        String simple = packet.getClass().getSimpleName();
        if (PACKET_SET_SLOT.equals(simple)) return this.rewriteSetSlotPacket(packet, player.getUniqueId());
        if (PACKET_SET_CONTENT.equals(simple)) return this.rewriteSetContentPacket(packet, player.getUniqueId(), this.noWorthOpen.contains(player.getUniqueId()));
        return packet;
    }

    private Object stripPacket(Object packet) {
        String simple = packet.getClass().getSimpleName();
        if (PACKET_SET_SLOT.equals(simple)) return this.rewriteSetSlotPacket(packet, null);
        if (PACKET_SET_CONTENT.equals(simple)) return this.rewriteSetContentPacket(packet, null, true);
        return packet;
    }

    private boolean hasCursorItem(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        return cursor != null && cursor.getType() != Material.AIR && cursor.getAmount() > 0;
    }

    private Object rewriteSetSlotPacket(Object packet, UUID playerId) {
        try {
            Object originalItem = this.reflectionBridge.findNmsItemField(packet);
            if (originalItem == null) return packet;
            Object replaced = playerId == null ? this.stripWorthLore(originalItem) : this.withWorthLore(originalItem, playerId);
            Object cloned = this.reflectionBridge.cloneSetSlotPacket(packet, replaced);
            return cloned == null ? packet : cloned;
        } catch (Throwable ignored) {
            return packet;
        }
    }

    private Object rewriteSetContentPacket(Object packet, UUID playerId, boolean disableTopContainerLore) {
        try {
            List<Object> nmsItems = this.reflectionBridge.findNmsItemListField(packet);
            if (nmsItems == null) return packet;
            int contSlots = this.reflectionBridge.extractContainerSlots(packet, nmsItems.size());
            ArrayList<Object> replacedItems = new ArrayList<>(nmsItems.size());
            for (int i = 0; i < nmsItems.size(); ++i) {
                Object nmsItem = nmsItems.get(i);
                boolean inTop = contSlots > 0 && i < contSlots;
                if (playerId == null || (disableTopContainerLore && inTop)) {
                    replacedItems.add(this.stripWorthLore(nmsItem));
                } else {
                    replacedItems.add(this.withWorthLore(nmsItem, playerId));
                }
            }
            Object carried = this.reflectionBridge.findNmsItemField(packet);
            if (carried == null) return packet;
            Object replacedCarried = playerId == null ? this.stripWorthLore(carried) : this.withWorthLore(carried, playerId);
            Object cloned = this.reflectionBridge.cloneSetContentPacket(packet, replacedItems, replacedCarried);
            return cloned == null ? packet : cloned;
        } catch (Throwable ignored) {
            return packet;
        }
    }

    private Object stripWorthLore(Object nmsItem) {
        ItemStack bukkit = this.reflectionBridge.toBukkitCopy(nmsItem);
        if (bukkit == null || bukkit.getType() == Material.AIR || bukkit.getAmount() <= 0) return nmsItem;
        ItemMeta meta = bukkit.getItemMeta();
        if (meta == null || !meta.hasLore()) return nmsItem;
        List<Component> origLore = meta.lore() != null ? meta.lore() : new ArrayList<>();
        ArrayList<Component> filtered = new ArrayList<>();
        for (Component comp : origLore) {
            String plain = Utils.stripColor(comp).toLowerCase(Locale.ROOT).trim();
            if (this.lorePlainPrefixes.stream().noneMatch(plain::startsWith)) {
                filtered.add(comp);
            }
        }
        meta.lore(filtered.isEmpty() ? null : filtered);
        bukkit.setItemMeta(meta);
        Object rebuilt = this.reflectionBridge.toNmsCopy(bukkit);
        return rebuilt == null ? nmsItem : rebuilt;
    }

    private Object withWorthLore(Object nmsItem, UUID playerId) {
        ItemStack bukkit = this.reflectionBridge.toBukkitCopy(nmsItem);
        if (bukkit == null || bukkit.getType() == Material.AIR || bukkit.getAmount() <= 0) return nmsItem;
        ItemMeta meta = bukkit.getItemMeta();
        if (meta == null || this.disabledItems.contains(bukkit.getType().name())) return nmsItem;
        double valueToShow;
        if (meta instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox box) {
            String boxKey = bukkit.getType().name().toLowerCase(Locale.ROOT) + "-value";
            double boxUnitPrice = this.plugin.getPrice(boxKey);
            String boxCat = this.plugin.categoryItems.entrySet().stream()
                    .filter(e -> e.getValue().contains(bukkit.getType().name()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            double boxMult = boxCat != null ? this.plugin.getSellMultiplier(playerId, boxCat) : 1.0;
            double perBoxTotal = boxUnitPrice * boxMult;
            for (ItemStack inside : box.getInventory().getContents()) {
                if (inside == null || inside.getType() == Material.AIR || this.disabledItems.contains(inside.getType().name())) continue;
                double insideRaw = this.plugin.calculateItemWorth(inside);
                String insideCat = this.plugin.categoryItems.entrySet().stream()
                        .filter(e -> e.getValue().contains(inside.getType().name()))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                double insideMult = insideCat != null ? this.plugin.getSellMultiplier(playerId, insideCat) : 1.0;
                perBoxTotal += insideRaw * insideMult;
            }
            valueToShow = this.worthLorePerItem ? perBoxTotal : perBoxTotal * bukkit.getAmount();
        } else {
            double baseVal;
            if (bukkit.getType() == Material.SPAWNER && meta instanceof BlockStateMeta bsm2) {
                BlockState blockState = bsm2.getBlockState();
                if (blockState instanceof CreatureSpawner cs && cs.getSpawnedType() != null) {
                    String spawnerKey = cs.getSpawnedType().name().toLowerCase(Locale.ROOT) + "_spawner-value";
                    baseVal = this.plugin.getPrice(spawnerKey);
                } else {
                    baseVal = this.plugin.getPrice("spawner-value");
                }
            } else {
                String pKey = this.getPotionKey(bukkit);
                baseVal = pKey != null
                        ? this.plugin.getPrice(pKey + "-value")
                        : this.plugin.getPrice(bukkit.getType().name().toLowerCase(Locale.ROOT) + "-value");
            }
            double enchVal = 0.0;
            if (meta instanceof EnchantmentStorageMeta esm) {
                for (Map.Entry<Enchantment, Integer> e : esm.getStoredEnchants().entrySet()) {
                    enchVal += this.plugin.getPrice(e.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + e.getValue() + "-value");
                }
            }
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchVal += this.plugin.getPrice(entry.getKey().getKey().getKey().toLowerCase(Locale.ROOT) + entry.getValue() + "-value");
            }
            double unitRaw = baseVal + enchVal;
            String cat = this.plugin.categoryItems.entrySet().stream()
                    .filter(e -> e.getValue().contains(bukkit.getType().name()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            double mult = cat != null ? this.plugin.getSellMultiplier(playerId, cat) : 1.0;
            valueToShow = this.worthLorePerItem ? unitRaw * mult : unitRaw * bukkit.getAmount() * mult;
        }
        String display = Utils.abbreviateNumber(valueToShow);
        List<Component> newComponents = this.loreTemplate.stream()
                .map(line -> Utils.toComponent(line.replace("%amount%", display)))
                .collect(Collectors.toList());
        List<Component> existing = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        existing.removeIf(comp -> {
            String plain = Utils.stripColor(comp).toLowerCase(Locale.ROOT).trim();
            return this.lorePlainPrefixes.stream().anyMatch(plain::startsWith);
        });
        for (Component nc : newComponents) {
            String ncSer = LegacyComponentSerializer.legacySection().serialize(nc);
            if (existing.stream().noneMatch(c -> LegacyComponentSerializer.legacySection().serialize(c).equals(ncSer))) {
                existing.add(nc);
            }
        }
        meta.lore(existing.isEmpty() ? null : existing);
        bukkit.setItemMeta(meta);
        Object rebuilt = this.reflectionBridge.toNmsCopy(bukkit);
        return rebuilt == null ? nmsItem : rebuilt;
    }

    private String getPotionKey(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta pm)) return null;
        PotionType potionType = pm.getBasePotionType();
        if (potionType == null) return null;
        String base = potionType.name().toLowerCase(Locale.ROOT);
        if (item.getType() == Material.SPLASH_POTION) base = "splash_" + base;
        else if (item.getType() == Material.LINGERING_POTION) base = "lingering_" + base;
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
                this.getHandleMethod = this.craftPlayerClass.getMethod("getHandle");
                this.asBukkitCopyMethod = this.craftItemStackClass.getMethod("asBukkitCopy",
                        Class.forName("net.minecraft.world.item.ItemStack"));
                this.asNmsCopyMethod = this.craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to initialize reflection bridge", throwable);
            }
        }

        Channel channelOf(Player player) {
            try {
                Object craftPlayer = this.craftPlayerClass.cast(player);
                Object serverPlayer = this.getHandleMethod.invoke(craftPlayer);
                Field connectionField = serverPlayer.getClass().getField("connection");
                Object connection = connectionField.get(serverPlayer);
                Field connectionInnerField = connection.getClass().getField("connection");
                Object networkManager = connectionInnerField.get(connection);
                for (Field field : networkManager.getClass().getDeclaredFields()) {
                    if (!Channel.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object ch = field.get(networkManager);
                    if (ch instanceof Channel channel) return channel;
                }
            } catch (Throwable throwable) {
                this.plugin.getPlugin().getLogger().warning(
                        "Could not find channel for player " + player.getName() + ": " + throwable.getMessage());
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
                if (!(value instanceof List<?> list)) continue;
                if (list.isEmpty()) return new ArrayList<>();
                Object first = list.get(0);
                if (first == null || !first.getClass().getName().equals("net.minecraft.world.item.ItemStack")) continue;
                @SuppressWarnings("unchecked")
                List<Object> result = (List<Object>) list;
                return result;
            }
            return null;
        }

        int extractContainerSlots(Object packet, int totalSlots) {
            try {
                int windowId = this.readIntByOrder(packet, 0);
                if (windowId == 0) return 0;
                return Math.max(0, totalSlots - 36);
            } catch (Throwable ignored) {
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
                    if (types.length != 4 || types[0] != Integer.TYPE || types[1] != Integer.TYPE
                            || types[2] != Integer.TYPE
                            || !types[3].getName().equals("net.minecraft.world.item.ItemStack")) continue;
                    constructor.setAccessible(true);
                    return constructor.newInstance(a, b, c, newItem);
                }
            } catch (Throwable ignored) {}
            return null;
        }

        Object cloneSetContentPacket(Object packet, List<Object> newItems, Object carried) {
            try {
                int a = this.readIntByOrder(packet, 0);
                int b = this.readIntByOrder(packet, 1);
                for (Constructor<?> constructor : packet.getClass().getDeclaredConstructors()) {
                    Class<?>[] types = constructor.getParameterTypes();
                    if (types.length != 4 || types[0] != Integer.TYPE || types[1] != Integer.TYPE
                            || !List.class.isAssignableFrom(types[2])
                            || !types[3].getName().equals("net.minecraft.world.item.ItemStack")) continue;
                    constructor.setAccessible(true);
                    return constructor.newInstance(a, b, newItems, carried);
                }
            } catch (Throwable ignored) {}
            return null;
        }

        private int readIntByOrder(Object packet, int index) throws IllegalAccessException {
            int current = 0;
            for (Field field : packet.getClass().getDeclaredFields()) {
                if (field.getType() != Integer.TYPE) continue;
                field.setAccessible(true);
                if (current == index) return field.getInt(packet);
                ++current;
            }
            return 0;
        }

        ItemStack toBukkitCopy(Object nmsItem) {
            try {
                Object result = this.asBukkitCopyMethod.invoke(null, nmsItem);
                return result instanceof ItemStack stack ? stack : null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        Object toNmsCopy(ItemStack bukkitItem) {
            try {
                return this.asNmsCopyMethod.invoke(null, bukkitItem);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
