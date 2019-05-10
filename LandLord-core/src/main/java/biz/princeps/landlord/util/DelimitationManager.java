package biz.princeps.landlord.util;

import biz.princeps.landlord.api.IDelimitationManager;
import biz.princeps.landlord.api.ILandLord;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 18/11/18
 */
public class DelimitationManager implements IDelimitationManager {

    private ILandLord plugin;
    private Map<BlockVector, Material> pattern;

    public DelimitationManager(ILandLord plugin) {
        this.plugin = plugin;
        pattern = getPattern();
    }

    /**
     * Returns the delimitation pattern defined in the config in a way, the plugin can work with
     * x --------->
     * z mmmmmmmmmmmmmmmm
     * | m--------------m
     * | m--------------m
     * | ...
     * |
     * v
     *
     * @return a map of a vector and a material
     */
    @Override
    public Map<BlockVector, Material> getPattern() {
        if (pattern != null) {
            return pattern;
        }

        List<String> cfgString = plugin.getConfig().getStringList("CommandSettings.Claim.delimitation");
        Map<Character, Material> varToMaterial = new HashMap<>();
        Map<BlockVector, Material> delimitPattern = new HashMap<>();

        int x = 0;
        for (String s : cfgString) {
            // its a variable definition
            if (s.startsWith("define:")) {
                String a = s.split(":")[1].trim();
                char var = a.split("=")[0].charAt(0);
                Material mat = Material.getMaterial(a.split("=")[1]);
                if (mat == null) {
                    plugin.getLogger().warning("Invalid Material in delimitation!");
                    return null;
                }
                varToMaterial.put(var, mat);
            } else if (s.length() == 16) {
                // must be a String containing 16chars describing the pattern
                for (int z = 0; z < 16; z++) {
                    char varString = s.charAt(z);
                    Material material = varToMaterial.get(varString);
                    delimitPattern.put(new BlockVector(x, z), material);
                }
                x++;
            } else {
                plugin.getLogger().warning("Invalid line '" + s + "' detected!!");
                return null;
            }
        }
        return delimitPattern;
    }


    @Override
    public void delimit(Player player, Chunk chunk) {
        Map<BlockVector, Material> pattern = this.pattern;
        if (pattern == null) {
            plugin.getLogger().warning("Delimitation failed, because there was an error in the config!");
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Material mat = pattern.get(new BlockVector(x, z));

                if (mat != null) {
                    int highestY = chunk.getWorld().getHighestBlockYAt(chunk.getX() * 16 + x, chunk.getZ() * 16 + z);
                    Block b = chunk.getBlock(x, highestY, z);

                    while (b.getType() != Material.AIR) {
                        b = chunk.getBlock(x, ++highestY, z);
                    }

                    if (plugin.getConfig().getBoolean("CommandSettings.Claim.enablePhantomBlocks")) {
                        sendBlockChangePacket(player, b.getLocation(), mat);
                    } else {
                        b.setType(mat);
                    }
                }
            }
        }
    }

    private void sendBlockChangePacket(Player p, Location loc, Material mat) {
        plugin.getUtilsProxy().send_fake_block_packet(p, loc, mat);
    }

    public class BlockVector {

        private int x, z;

        public BlockVector(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public String toString() {
            return "BlockVector{" +
                    "x=" + x +
                    ", z=" + z +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockVector that = (BlockVector) o;
            return x == that.x &&
                    z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}