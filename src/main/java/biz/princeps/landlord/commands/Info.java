package biz.princeps.landlord.commands;

import biz.princeps.landlord.util.OwnedLand;
import org.bukkit.Chunk;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.List;

/**
 * Created by spatium on 17.07.17.
 */
public class Info extends LandlordCommand {

    private String owned, free;

    public Info() {
        List<String> ownedList = plugin.getLangManager().getStringList("Commands.Info.owned");
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = ownedList.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext())
                sb.append("\n");
        }
        owned = sb.toString();


        List<String> freeList = plugin.getLangManager().getStringList("Commands.Info.free");
        StringBuilder sb2 = new StringBuilder();
        Iterator<String> it2 = freeList.iterator();
        while (it2.hasNext()) {
            sb2.append(it2.next());
            if (it2.hasNext())
                sb2.append("\n");
        }
        free = sb2.toString();
    }

    public void onInfo(Player player) {
        if (this.worldDisabled(player)) {
            player.sendMessage(lm.getString("Disabled-World"));
            return;
        }
        Chunk chunk = player.getWorld().getChunkAt(player.getLocation());

        OwnedLand land = plugin.getWgHandler().getRegion(chunk);

        // claimed
        if (land != null) {
            player.sendMessage(owned
                    .replace("%landid%", land.getLandName())
                    .replace("%owner%", land.printOwners())
                    .replace("%member%", land.printMembers().isEmpty() ? "-" : land.printMembers()));
            OwnedLand.highlightLand(player, Particle.DRIP_WATER);

        } else {
            // unclaimed
            player.sendMessage(free
                    .replace("%landid%", OwnedLand.getLandName(chunk))
                    .replace("%price%", plugin.getVaultHandler().format(OwnedLand.calculateCost(player))));
            OwnedLand.highlightLand(player, Particle.DRIP_LAVA);
        }

    }
}