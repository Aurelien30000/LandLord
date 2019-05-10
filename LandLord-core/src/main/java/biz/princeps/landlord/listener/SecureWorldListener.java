package biz.princeps.landlord.listener;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.events.PlayerBrokeSecureWorldEvent;
import biz.princeps.lib.PrincepsLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 3/12/17
 * <p>
 * Structure wise it goes like this:
 * 1. BlockBreak, BlockPlace, BucketEmpty calls PlayerBrokeSecureWorldEvent
 * 2. PlaceBrokeSecureWorldEvent decides if the intrusion was allowed or not.
 */
public class SecureWorldListener extends BasicListener {

    private LandAlerter.LandMessageDisplay display;
    private int treshold;

    public SecureWorldListener(ILandLord pl) {
        super(pl);
        this.treshold = plugin.getConfig().getInt("SecureWorld.threshold");

        this.display = LandAlerter.LandMessageDisplay.valueOf(plugin.getConfig().getString("SecureWorld.displayWarning"));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        IOwnedLand land = plugin.getWGProxy().getRegion(e.getBlock().getLocation());

        if (land == null) {
            PlayerBrokeSecureWorldEvent event = new PlayerBrokeSecureWorldEvent(p, e.getBlock(), e);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        IOwnedLand land = plugin.getWGProxy().getRegion(e.getBlock().getLocation());

        if (land == null) {
            PlayerBrokeSecureWorldEvent event = new PlayerBrokeSecureWorldEvent(p, e.getBlockPlaced(), e);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        IOwnedLand land = plugin.getWGProxy().getRegion(e.getBlockClicked().getLocation());

        if (land == null) {
            PlayerBrokeSecureWorldEvent event = new PlayerBrokeSecureWorldEvent(p, e.getBlockClicked(), e);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @EventHandler
    public void onThresholdEvent(PlayerBrokeSecureWorldEvent e) {
        IOwnedLand land = plugin.getWGProxy().getRegion(e.getBlock().getLocation());
        if (!e.isCancelled()) {
            handleLand(e.getPlayer(), e.getBlock().getLocation(), land, e.getCancellable());
        }
    }

    private void handleLand(Player p, Location loc, IOwnedLand land, Cancellable e) {
        // is free land
        if (p.isOp() || p.hasPermission("landlord.admin.bypass"))
            return;

        if (!plugin.getConfig().getStringList("disabled-worlds").contains(loc.getWorld().getName())) {


            if (plugin.getWGProxy().isAllowedInOverlap(p, loc)) return;

            if (land == null) {
                int landcount = plugin.getWGProxy().getRegionCount(p.getUniqueId());

                if (landcount < treshold) {
                    String rawString = plugin.getLangManager().getRawString("Alerts.tresholdNotReached")
                            .replace("%x%", treshold + "");
                    if (display == LandAlerter.LandMessageDisplay.ActionBar) {
                        PrincepsLib.getStuffManager().sendActionBar(p, rawString);
                    } else if (display == LandAlerter.LandMessageDisplay.Chat) {
                        plugin.getLangManager().sendMessage(p, plugin.getLangManager().getString("Alerts.tresholdNotReached")
                                .replace("%x%", treshold + ""));
                    } else if (display == LandAlerter.LandMessageDisplay.Title) {
                        p.sendTitle(rawString, null);
                    }

                    e.setCancelled(true);
                }
            }
        }
    }
}