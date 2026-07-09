package ro.andreilarazboi.donutcore.sell;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

final class DonutOrderBridge {
    private DonutOrderBridge() {
    }

    static Result fillBestOrder(DonutSell plugin, Player seller, ItemStack item, double minimumPriceEach, boolean requireFullAmount) {
        if (plugin == null || seller == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return Result.empty();
        }
        if (!plugin.getConfig().getBoolean("integrations.donut-order.sell-to-orders", true)) {
            return Result.empty();
        }
        try {
            Class<?> apiClass = Class.forName("andreilarazboi.lifestealorder.api.DonutOrderApi");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) {
                return Result.empty();
            }
            Object api = registration.getProvider();
            Method method = apiClass.getMethod("fillBestOrder", UUID.class, ItemStack.class, Double.TYPE, Boolean.TYPE, Boolean.TYPE);
            Object result = method.invoke(api, seller.getUniqueId(), item.clone(), minimumPriceEach, false, requireFullAmount);
            return Result.from(result);
        }
        catch (ClassNotFoundException ignored) {
            return Result.empty();
        }
        catch (Throwable t) {
            plugin.getLogger().warning("DonutOrder integration failed: " + t.getMessage());
            return Result.empty();
        }
    }

    static Result quoteBestOrder(DonutSell plugin, UUID seller, ItemStack item, double minimumPriceEach) {
        if (plugin == null || seller == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return Result.empty();
        }
        if (!plugin.getConfig().getBoolean("integrations.donut-order.use-order-prices-for-worth-lore", true) && !plugin.getConfig().getBoolean("integrations.donut-order.sell-to-orders", true)) {
            return Result.empty();
        }
        try {
            Class<?> apiClass = Class.forName("andreilarazboi.lifestealorder.api.DonutOrderApi");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) {
                return Result.empty();
            }
            Method method = apiClass.getMethod("quoteBestOrder", UUID.class, ItemStack.class, Double.TYPE, Boolean.TYPE);
            return Result.from(method.invoke(registration.getProvider(), seller, item.clone(), minimumPriceEach, false));
        }
        catch (Throwable ignored) {
            return Result.empty();
        }
    }

    static double getHighestActivePrice(DonutSell plugin, ItemStack item) {
        if (plugin == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return 0.0;
        }
        if (!plugin.getConfig().getBoolean("integrations.donut-order.use-order-prices-for-worth-lore", true)) {
            return 0.0;
        }
        try {
            double d;
            Class<?> apiClass = Class.forName("andreilarazboi.lifestealorder.api.DonutOrderApi");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) {
                return 0.0;
            }
            Method method = apiClass.getMethod("getHighestActivePrice", ItemStack.class);
            Object value = method.invoke(registration.getProvider(), item.clone());
            if (value instanceof Number) {
                Number number = (Number)value;
                d = Math.max(0.0, number.doubleValue());
            } else {
                d = 0.0;
            }
            return d;
        }
        catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return 0.0;
        }
        catch (Throwable t) {
            plugin.getLogger().warning("DonutOrder worth-lore integration failed: " + t.getMessage());
            return 0.0;
        }
    }

    static final class Result {
        private final int filledAmount;
        private final double totalPayout;

        private Result(int filledAmount, double totalPayout) {
            this.filledAmount = Math.max(0, filledAmount);
            this.totalPayout = Math.max(0.0, totalPayout);
        }

        static Result empty() {
            return new Result(0, 0.0);
        }

        static Result from(Object result) {
            if (result == null) {
                return Result.empty();
            }
            try {
                int amount = ((Number)result.getClass().getMethod("getFilledAmount", new Class[0]).invoke(result, new Object[0])).intValue();
                double payout = ((Number)result.getClass().getMethod("getTotalPayout", new Class[0]).invoke(result, new Object[0])).doubleValue();
                return new Result(amount, payout);
            }
            catch (Throwable ignored) {
                return Result.empty();
            }
        }

        boolean wasFilled() {
            return this.filledAmount > 0;
        }

        int filledAmount() {
            return this.filledAmount;
        }

        double totalPayout() {
            return this.totalPayout;
        }
    }
}

