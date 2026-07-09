package ro.andreilarazboi.donutcore.sell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HistoryTracker {
    private final Map<UUID, Integer> pages = new HashMap<UUID, Integer>();
    private final Map<UUID, SortOrder> orders = new HashMap<UUID, SortOrder>();
    private final Map<UUID, String> filters = new HashMap<UUID, String>();

    public void setPage(UUID player, int page) {
        this.pages.put(player, page);
    }

    public int getPage(UUID player) {
        return this.pages.getOrDefault(player, 1);
    }

    public void setOrder(UUID player, SortOrder order) {
        this.orders.put(player, order);
    }

    public SortOrder getOrder(UUID player) {
        return this.orders.getOrDefault(player, SortOrder.HIGH);
    }

    public void cycleOrder(UUID player) {
        SortOrder cur = this.getOrder(player);
        this.orders.put(player, switch (cur.ordinal()) {
            case 0 -> SortOrder.LOW;
            case 1 -> SortOrder.NAME;
            case 2 -> SortOrder.HIGH;
            default -> SortOrder.HIGH;
        });
    }

    public void setFilter(UUID player, String filter) {
        if (filter == null) {
            this.filters.remove(player);
        } else {
            this.filters.put(player, filter);
        }
    }

    public String getFilter(UUID player) {
        return this.filters.get(player);
    }

    public boolean isTracked(UUID player) {
        return this.pages.containsKey(player);
    }

    public void clear(UUID player) {
        this.pages.remove(player);
        this.orders.remove(player);
        this.filters.remove(player);
    }

    public static enum SortOrder {
        HIGH,
        LOW,
        NAME;

    }
}

