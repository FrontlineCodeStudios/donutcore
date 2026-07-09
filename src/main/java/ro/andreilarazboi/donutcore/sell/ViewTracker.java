package ro.andreilarazboi.donutcore.sell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViewTracker {
    private final Map<UUID, Integer> pages = new HashMap<UUID, Integer>();
    private final Map<UUID, SortOrder> order = new HashMap<UUID, SortOrder>();
    private final Map<UUID, String> filters = new HashMap<UUID, String>();

    public void setPage(UUID player, int page) {
        this.pages.put(player, page);
    }

    public int getPage(UUID player) {
        return this.pages.getOrDefault(player, 1);
    }

    public SortOrder getOrder(UUID player) {
        return this.order.getOrDefault(player, SortOrder.HIGH_TO_LOW);
    }

    public void setOrder(UUID player, SortOrder sortOrder) {
        this.order.put(player, sortOrder);
    }

    public void cycleOrder(UUID player) {
        SortOrder cur = this.getOrder(player);
        SortOrder next = switch (cur.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> SortOrder.LOW_TO_HIGH;
            case 1 -> SortOrder.A_TO_Z;
            case 3 -> SortOrder.Z_TO_A;
            case 4 -> SortOrder.HIGH_TO_LOW;
            case 2 -> SortOrder.HIGH_TO_LOW;
        };
        this.order.put(player, next);
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

    public void clear(UUID player) {
        this.pages.remove(player);
        this.order.remove(player);
        this.filters.remove(player);
    }

    public boolean isTracked(UUID player) {
        return this.pages.containsKey(player);
    }

    public static enum SortOrder {
        HIGH_TO_LOW,
        LOW_TO_HIGH,
        NAME,
        A_TO_Z,
        Z_TO_A;

    }
}

