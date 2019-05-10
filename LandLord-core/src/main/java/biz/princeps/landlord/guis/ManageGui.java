package biz.princeps.landlord.guis;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.lib.gui.MultiPagedGUI;
import biz.princeps.lib.gui.simple.AbstractGUI;
import com.google.common.collect.Lists;
import org.bukkit.entity.Player;

public class ManageGui extends AManage {

    public ManageGui(ILandLord pl, Player player, IOwnedLand land) {
        super(pl, player, pl.getLangManager().getRawString("Commands.Manage.header")
                .replace("%info%", land.getName()), Lists.newArrayList(land));
    }

    public ManageGui(ILandLord pl, Player player, MultiPagedGUI landGui, IOwnedLand land) {
        super(pl, player, landGui, pl.getLangManager().getRawString("Commands.Manage.header")
                .replace("%info%", land.getName()), Lists.newArrayList(land));
    }
}