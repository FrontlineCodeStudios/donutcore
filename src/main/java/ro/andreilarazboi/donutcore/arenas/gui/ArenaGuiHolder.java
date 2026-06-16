package ro.andreilarazboi.donutcore.arenas.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ArenaGuiHolder implements InventoryHolder {

    private final String type;      // "list" or "detail"
    private final String arenaName; // null for list GUI

    public ArenaGuiHolder(String type, String arenaName) {
        this.type      = type;
        this.arenaName = arenaName;
    }

    public String getType()      { return type; }
    public String getArenaName() { return arenaName; }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException();
    }
}
