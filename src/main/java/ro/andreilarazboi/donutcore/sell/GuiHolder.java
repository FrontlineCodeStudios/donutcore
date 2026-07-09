package ro.andreilarazboi.donutcore.sell;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder
implements InventoryHolder {
    private final String categoryKey;
    private final int page;

    public GuiHolder(String categoryKey, int page) {
        this.categoryKey = categoryKey;
        this.page = page;
    }

    public String getCategoryKey() {
        return this.categoryKey;
    }

    public int getPage() {
        return this.page;
    }

    public Inventory getInventory() {
        return null;
    }
}

