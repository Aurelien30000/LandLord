package biz.princeps.landlord;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.listener.BasicListener;
import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.bukkit.util.Materials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 05/05/19
 * <p>
 * This listener overwrites worldguard events in order to allow pistons to move blocks between regions
 * that are owned by the same person.
 */
public class PistonOverwriter extends BasicListener {

    public PistonOverwriter(ILandLord plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlaceBlock(final PlaceBlockEvent event) {
        Block block = event.getCause().getFirstBlock();
        handleEvent(event, block, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreakBlock(final BreakBlockEvent event) {
        Block block = event.getCause().getFirstBlock();
        handleEvent(event, block, event.getBlocks());
    }

    private void handleEvent(DelegateEvent event, Block block, List<Block> blocks) {
        if (block == null) {
            return;
        }
        if (Materials.isPistonBlock(block.getType()) || block.getType() == Material.PISTON_MOVING_PIECE) {
            if (sameOwner(block, blocks)) {
                event.setAllowed(true);
            }
        }
    }

    /**
     * Checks if all the blocks in the list are on the same land as the origin block
     *
     * @param origin the origin (the piston)
     * @param blocks all the pushed blocks
     * @return if all blocks have the same owner
     */
    private boolean sameOwner(Block origin, List<Block> blocks) {
        if (origin == null) {
            return false;
        }

        Set<IOwnedLand> lands = new HashSet<>();
        blocks.forEach(b -> lands.add(plugin.getWGProxy().getRegion(b.getChunk())));

        UUID onlyOwner = plugin.getWGProxy().getRegion(origin.getChunk()).getOwner();
        // System.out.println("original owner" + onlyOwner);
        for (IOwnedLand land : lands) {
            // System.out.println(land);
            if (land == null) {
                return false;
            }
            if (!onlyOwner.equals(land.getOwner())) {
                return false;
            }
        }
        return true;
    }
}
