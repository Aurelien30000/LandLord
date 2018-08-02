package biz.princeps.landlord.commands.friends;

import biz.princeps.landlord.commands.LandlordCommand;
import biz.princeps.landlord.util.OwnedLand;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 17/7/17
 */
public class Addfriend extends LandlordCommand {

    public void onAddfriend(Player player, String[] names) {

        Chunk chunk = player.getWorld().getChunkAt(player.getLocation());

        OwnedLand land = plugin.getWgHandler().getRegion(chunk);
        if (land != null) {
            if (!land.isOwner(player.getUniqueId()) && !player.hasPermission("landlord.admin.modifyfriends")) {
                player.sendMessage(lm.getString("Commands.Addfriend.notOwn")
                        .replace("%owner%", land.printOwners()));
                return;
            }

            if (names.length == 0) {
                player.sendMessage(lm.getString("Commands.Addfriend.noPlayer")
                        .replace("%players%", Arrays.asList(names).toString()));
                return;
            }

            for (String target : names) {

                plugin.getPlayerManager().getOfflinePlayerAsync(target, lPlayer -> {
                    if (lPlayer == null) {
                        player.sendMessage(lm.getString("Commands.Addfriend.noPlayer")
                                .replace("%players%", Arrays.asList(names).toString()));
                    } else {
                        // Success
                        if (!land.getWGLand().getOwners().getUniqueIds().contains(lPlayer.getUuid())) {
                            land.getWGLand().getMembers().addPlayer(lPlayer.getUuid());
                            player.sendMessage(lm.getString("Commands.Addfriend.success")
                                    .replace("%players%", Arrays.asList(names).toString()));
                        } else {
                            player.sendMessage(lm.getString("Commands.Addfriend.alreadyOwn"));
                        }
                    }
                });

                // lets delay it, because we cant be sure, that the requests are done when executing this piece of code
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getMapManager().updateAll();
                    }
                }.runTaskLater(plugin, 60L);
            }
        }
    }
}