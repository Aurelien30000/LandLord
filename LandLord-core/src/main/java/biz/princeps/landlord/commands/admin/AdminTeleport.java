package biz.princeps.landlord.commands.admin;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.commands.LandlordCommand;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.Properties;
import biz.princeps.lib.exception.ArgumentsOutOfBoundsException;
import biz.princeps.lib.gui.MultiPagedGUI;
import biz.princeps.lib.gui.simple.Icon;
import com.google.common.collect.Sets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: Unknown
 */
public class AdminTeleport extends LandlordCommand {

    public AdminTeleport(ILandLord pl) {
        super(pl, pl.getConfig().getString("CommandSettings.AdminTP.name"),
                pl.getConfig().getString("CommandSettings.AdminTP.usage"),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.AdminTP.permissions")),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.AdminTP.aliases")));
    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {
        if (properties.isConsole()) {
            return;
        }

        Player sender = properties.getPlayer();
        String target;
        try {
            target = arguments.get(0);
        } catch (ArgumentsOutOfBoundsException e) {
            properties.sendUsage();
            return;
        }


        plugin.getPlayerManager().getOfflinePlayerAsync(target, lplayer -> {

            if (lplayer == null) {
                // Failure
                lm.sendMessage(sender, lm.getString("Commands.AdminTp.noPlayer").replace("%player%", target));
            } else {
                // Success
                Set<IOwnedLand> lands = plugin.getWGProxy().getRegions(lplayer.getUuid());
                if (lands.size() > 0) {
                    MultiPagedGUI landGui = new MultiPagedGUI(sender, 5,
                            lm.getRawString("Commands.AdminTp.guiHeader").replace("%player%", target));

                    lands.forEach(land -> landGui.addIcon(new Icon(new ItemStack(plugin.getMatProxy().getGrass()))
                            .setName(land.getName())
                            .addClickAction((p) -> {
                                        Location toTp = land.getALocation();
                                        sender.teleport(toTp);
                                        land.highlightLand(sender, Particle.VILLAGER_HAPPY);
                                    }
                            )
                    ));
                    landGui.display();
                } else {
                    lm.sendMessage(sender, lm.getString("Commands.AdminTp.noLands").replace("%player%", target));
                }
            }
        });
    }
}