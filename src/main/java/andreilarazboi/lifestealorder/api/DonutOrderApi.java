package andreilarazboi.lifestealorder.api;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import me.serbob.donutorder.commons.manager.e;
import me.serbob.donutorder.api.util.Order;
import me.serbob.donutorder.commons.m;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DonutOrderApi {
    private Economy econ;

    public DonutOrderApi() {
        setupEconomy();
        if (Bukkit.getPluginManager().getPlugin("DonutOrder") == null) {
            Bukkit.getLogger().severe("[DonutCore] API did NOT detect DonutOrder! Integration will NOT work.");
        } else {
            Bukkit.getLogger().info("[DonutCore] API successfully detected DonutOrder! Integration is active and working.");
        }
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Order> getActiveOrdersMap() {
        try {
            Field field = e.class.getDeclaredField("C");
            field.setAccessible(true);
            return (Map<UUID, Order>) field.get(e.getInstance());
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.SEVERE, "[DonutCore-OrdersBridge] Failed to get active orders map via reflection", t);
            return Collections.emptyMap();
        }
    }

    public Result fillBestOrder(UUID seller, ItemStack item, double minimumPriceEach, boolean flag, boolean requireFullAmount) {
        if (item == null || item.getAmount() <= 0) {
            return new Result(0, 0.0);
        }

        Map<UUID, Order> activeOrders = getActiveOrdersMap();
        if (activeOrders.isEmpty()) {
            return new Result(0, 0.0);
        }

        List<Order> matches = new ArrayList<>();
        for (Order order : activeOrders.values()) {
            if (order.isCancelled() || order.getAvailableAmount() <= 0) continue;
            if (order.getCreatorId().equals(seller)) continue;
            if (order.getPricePerItem() < minimumPriceEach) continue;
            if (!order.getItem().isSimilar(item)) continue;
            matches.add(order);
        }

        matches.sort((o1, o2) -> Double.compare(o2.getPricePerItem(), o1.getPricePerItem()));

        if (matches.isEmpty()) {
            return new Result(0, 0.0);
        }

        if (requireFullAmount) {
            long firstAvailable = matches.get(0).getAvailableAmount();
            if (item.getAmount() < firstAvailable) {
                return new Result(0, 0.0);
            }
        }

        int remainingToFill = item.getAmount();
        int totalFilled = 0;
        double totalPayout = 0.0;

        for (Order order : matches) {
            int toTake = (int) Math.min(remainingToFill, order.getAvailableAmount());

            // Fetch a fresh object instance from the database/storage to prevent reference duplication in memory caches
            Order dbOrder = m.getDatabase().getOrder(order.getOrderId());
            if (dbOrder == null) {
                continue;
            }

            long newDelivered = dbOrder.getDeliveredAmount() + toTake;
            double price = dbOrder.getPricePerItem();
            double newTotalPaid = newDelivered * price;

            dbOrder.setDeliveredAmount(newDelivered);
            dbOrder.setTotalPaid(newTotalPaid);

            // Update memory caches
            e.getInstance().updateOrder(dbOrder);
            // Force save to database directly
            m.getDatabase().updateOrder(dbOrder);

            totalFilled += toTake;
            totalPayout += toTake * price;
            remainingToFill -= toTake;

            if (remainingToFill <= 0) break;
        }

        if (econ != null && totalPayout > 0.0) {
            econ.depositPlayer(Bukkit.getOfflinePlayer(seller), totalPayout);
        }

        return new Result(totalFilled, totalPayout);
    }

    public Result quoteBestOrder(UUID seller, ItemStack item, double minimumPriceEach, boolean flag) {
        if (item == null || item.getAmount() <= 0) {
            return new Result(0, 0.0);
        }

        Map<UUID, Order> activeOrders = getActiveOrdersMap();
        if (activeOrders.isEmpty()) {
            return new Result(0, 0.0);
        }

        List<Order> matches = new ArrayList<>();
        for (Order order : activeOrders.values()) {
            if (order.isCancelled() || order.getAvailableAmount() <= 0) continue;
            if (order.getCreatorId().equals(seller)) continue;
            if (order.getPricePerItem() < minimumPriceEach) continue;
            if (!order.getItem().isSimilar(item)) continue;
            matches.add(order);
        }

        matches.sort((o1, o2) -> Double.compare(o2.getPricePerItem(), o1.getPricePerItem()));

        if (matches.isEmpty()) {
            return new Result(0, 0.0);
        }

        int remainingToFill = item.getAmount();
        int totalFilled = 0;
        double totalPayout = 0.0;

        for (Order order : matches) {
            int toTake = (int) Math.min(remainingToFill, order.getAvailableAmount());
            totalFilled += toTake;
            totalPayout += toTake * order.getPricePerItem();
            remainingToFill -= toTake;

            if (remainingToFill <= 0) break;
        }

        return new Result(totalFilled, totalPayout);
    }

    public double getHighestActivePrice(ItemStack item) {
        if (item == null || item.getAmount() <= 0) {
            return 0.0;
        }

        Map<UUID, Order> activeOrders = getActiveOrdersMap();
        if (activeOrders.isEmpty()) {
            return 0.0;
        }

        double max = 0.0;
        for (Order order : activeOrders.values()) {
            if (order.isCancelled() || order.getAvailableAmount() <= 0) continue;
            if (!order.getItem().isSimilar(item)) continue;
            if (order.getPricePerItem() > max) {
                max = order.getPricePerItem();
            }
        }
        return max;
    }

    public static class Result {
        private final int filledAmount;
        private final double totalPayout;

        public Result(int filledAmount, double totalPayout) {
            this.filledAmount = filledAmount;
            this.totalPayout = totalPayout;
        }

        public int getFilledAmount() {
            return this.filledAmount;
        }

        public double getTotalPayout() {
            return this.totalPayout;
        }
    }
}
