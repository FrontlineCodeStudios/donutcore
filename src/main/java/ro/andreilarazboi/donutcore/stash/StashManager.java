package ro.andreilarazboi.donutcore.stash;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import ro.andreilarazboi.donutcore.DonutCore;

public class StashManager {

    private final NamespacedKey keyIsStash;
    private final NamespacedKey keyStashName;

    public StashManager(DonutCore plugin) {
        this.keyIsStash   = new NamespacedKey(plugin, "is_stash");
        this.keyStashName = new NamespacedKey(plugin, "stash_name");
    }

    public boolean isStash(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof PersistentDataHolder holder)) return false;
        return holder.getPersistentDataContainer().has(keyIsStash, PersistentDataType.BYTE);
    }

    public String getStashName(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof PersistentDataHolder holder)) return null;
        return holder.getPersistentDataContainer().get(keyStashName, PersistentDataType.STRING);
    }

    public boolean markAsStash(Block block, String name) {
        BlockState state = block.getState();
        if (!(state instanceof PersistentDataHolder holder)) return false;
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        pdc.set(keyIsStash,   PersistentDataType.BYTE,   (byte) 1);
        pdc.set(keyStashName, PersistentDataType.STRING, name);
        state.update();
        return true;
    }

    public boolean removeStash(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof PersistentDataHolder holder)) return false;
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        if (!pdc.has(keyIsStash, PersistentDataType.BYTE)) return false;
        pdc.remove(keyIsStash);
        pdc.remove(keyStashName);
        state.update();
        return true;
    }
}
