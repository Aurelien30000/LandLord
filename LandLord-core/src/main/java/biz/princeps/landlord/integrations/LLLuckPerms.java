package biz.princeps.landlord.integrations;

import biz.princeps.landlord.ALandLord;
import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IOwnedLand;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LLLuckPerms {

    private LuckPerms api;
    private ILandLord pl;


    public LLLuckPerms(ALandLord aLandLord) {
        pl = aLandLord;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            api = provider.getProvider();
        }

        api.getContextManager().registerCalculator(new CustomCalculator());
    }


    class CustomCalculator implements ContextCalculator<Player> {

        @Override
        public void calculate(Player p, ContextConsumer contextConsumer) {
            IOwnedLand region = pl.getWGManager().getRegion(p.getLocation());
            if (region == null) {
                contextConsumer.accept("land", "wilderness");
            } else {
                contextConsumer.accept("land", region.getName());
                if (region.isOwner(p.getUniqueId())) {
                    contextConsumer.accept("land_own", "yes");
                } else if (region.isFriend(p.getUniqueId())) {
                    contextConsumer.accept("land_befriended", "yes");
                }
            }
        }

        @Override
        public ContextSet estimatePotentialContexts() {
            ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
            builder.add("land", "wilderness");
            builder.add("land_own", "yes");
            builder.add("land_befriended", "yes");

            return builder.build();
        }
    }
}

