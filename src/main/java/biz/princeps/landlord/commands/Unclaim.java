package biz.princeps.landlord.commands;

import biz.princeps.landlord.util.OwnedLand;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * Created by spatium on 17.07.17.
 */
public class Unclaim extends LandlordCommand {

    public void onUnclaim(Player player) {

        if (this.worldDisabled(player)) {
            player.sendMessage(lm.getString("Disabled-World"));
            return;
        }
        Chunk chunk = player.getWorld().getChunkAt(player.getLocation());
        OwnedLand pr = plugin.getWgHandler().getRegion(chunk);
        if (pr == null) {
            player.sendMessage(lm.getString("Commands.Unclaim.notOwnFreeLand"));
            return;
        }

        if (!pr.isOwner(player.getUniqueId())) {
            player.sendMessage(lm.getString("Commands.Unclaim.notOwn")
                    .replaceAll("%owner%", pr.printOwners()));
            return;
        }

        int regionCount = plugin.getWgHandler().getWG().getRegionManager(player.getWorld()).getRegionCountOfPlayer(plugin.getWgHandler().getWG().wrapPlayer(player));
        int freeLands = plugin.getConfig().getInt("Freelands");

        double payback;
        if (regionCount <= freeLands)
            payback = 0;
        else
            payback = OwnedLand.calculateCost(player) * plugin.getConfig().getDouble("Payback");

        plugin.getVaultHandler().give(player.getUniqueId(), payback);
        plugin.getWgHandler().unclaim(chunk, pr.getLandName());

        player.sendMessage(lm.getString("Commands.Unclaim.success")
                .replaceAll("%chunk%", OwnedLand.getLandName(chunk))
                .replaceAll("%world%", chunk.getWorld().getName())
                .replaceAll("%money%", plugin.getVaultHandler().format(payback)));


        plugin.getMapManager().updateAll();

    }

}
