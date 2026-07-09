package ro.andreilarazboi.donutcore.enderchest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestHolder implements InventoryHolder {
    private final UUID ownerUUID;
    private final UUID viewerUUID;
    private final boolean readOnly;
    private final int rows;
    private final Map<Integer, ItemStack> fullItemsMap;
    private final org.bukkit.block.Block clickedBlock;
    private Inventory inventory;

    public EnderChestHolder(UUID ownerUUID, UUID viewerUUID, boolean readOnly, int rows, Map<Integer, ItemStack> fullItemsMap) {
        this(ownerUUID, viewerUUID, readOnly, rows, fullItemsMap, null);
    }

    public EnderChestHolder(UUID ownerUUID, UUID viewerUUID, boolean readOnly, int rows, Map<Integer, ItemStack> fullItemsMap, org.bukkit.block.Block clickedBlock) {
        this.ownerUUID = ownerUUID;
        this.viewerUUID = viewerUUID;
        this.readOnly = readOnly;
        this.rows = rows;
        this.fullItemsMap = new HashMap<>(fullItemsMap);
        this.clickedBlock = clickedBlock;
    }

    @Override
    public Inventory getInventory() { return this.inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public UUID getOwnerUUID() { return this.ownerUUID; }
    public UUID getViewerUUID() { return this.viewerUUID; }
    public boolean isReadOnly() { return this.readOnly; }
    public int getRows() { return this.rows; }
    public Map<Integer, ItemStack> getFullItemsMap() { return this.fullItemsMap; }
    public org.bukkit.block.Block getClickedBlock() { return this.clickedBlock; }
}
