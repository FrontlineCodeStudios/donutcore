package ro.andreilarazboi.donutcore.stash;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class StashSchematicManager {

    private final File schematicsDir;

    public StashSchematicManager(Plugin plugin) {
        this.schematicsDir = new File(plugin.getDataFolder(), "stash/schematics");
        schematicsDir.mkdirs();
    }

    /**
     * Captures the player's current WorldEdit selection (skipping air blocks)
     * and writes it to a .schem file. Returns an error message, or null on success.
     */
    public String save(Player player, String name) {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        World weWorld = session.getSelectionWorld();
        if (weWorld == null) {
            return "&cMake a WorldEdit selection first (//pos1 and //pos2, or the wand).";
        }

        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            return "&cMake a WorldEdit selection first (//pos1 and //pos2, or the wand).";
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BukkitAdapter.asBlockVector(player.getLocation()));

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            copy.setSourceMask(new ExistingBlockMask(editSession)); // skip air — only solid blocks are copied
            copy.setCopyingEntities(false);
            Operations.complete(copy);
        } catch (Exception e) {
            return "&cFailed to copy selection: " + e.getMessage();
        }

        File file = new File(schematicsDir, name + ".schem");
        ClipboardFormat format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;
        try (OutputStream os = new FileOutputStream(file);
             ClipboardWriter writer = format.getWriter(os)) {
            writer.write(clipboard);
        } catch (IOException e) {
            return "&cFailed to write schematic: " + e.getMessage();
        }

        return null;
    }

    public record PasteResult(BlockVector3 min, BlockVector3 max, org.bukkit.World world) {}

    /**
     * Pastes a saved schematic at the player's current location (air blocks in
     * the schematic are skipped so existing terrain underneath isn't cleared).
     * Returns the world-space bounding box of the pasted structure, or null if
     * the schematic doesn't exist.
     */
    public PasteResult spawn(Player player, String name) throws IOException {
        File file = new File(schematicsDir, name + ".schem");
        if (!file.exists()) return null;

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;

        Clipboard clipboard;
        try (InputStream is = new FileInputStream(file);
             ClipboardReader reader = format.getReader(is)) {
            clipboard = reader.read();
        }

        World weWorld = BukkitAdapter.adapt(player.getWorld());
        BlockVector3 to = BukkitAdapter.asBlockVector(player.getLocation());

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation operation = holder.createPaste(editSession)
                .to(to)
                .ignoreAirBlocks(true)
                .copyEntities(false)
                .build();
            Operations.complete(operation);
        } catch (Exception e) {
            throw new IOException(e);
        }

        BlockVector3 min = to.add(clipboard.getMinimumPoint().subtract(clipboard.getOrigin()));
        BlockVector3 max = to.add(clipboard.getMaximumPoint().subtract(clipboard.getOrigin()));
        return new PasteResult(min, max, player.getWorld());
    }

    public boolean exists(String name) {
        return new File(schematicsDir, name + ".schem").exists();
    }

    public boolean delete(String name) {
        File file = new File(schematicsDir, name + ".schem");
        return file.exists() && file.delete();
    }

    public List<String> list() {
        List<String> names = new ArrayList<>();
        File[] files = schematicsDir.listFiles((dir, fname) -> fname.endsWith(".schem"));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().substring(0, f.getName().length() - ".schem".length()));
            }
        }
        names.sort(String::compareTo);
        return names;
    }
}
