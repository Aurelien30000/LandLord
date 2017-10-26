package biz.princeps.landlord.guis;

import biz.princeps.landlord.Landlord;
import biz.princeps.landlord.flags.LLFlag;
import biz.princeps.landlord.manager.LangManager;
import biz.princeps.landlord.persistent.LPlayer;
import biz.princeps.landlord.util.OwnedLand;
import biz.princeps.lib.gui.ConfirmationGUI;
import biz.princeps.lib.gui.MultiPagedGUI;
import biz.princeps.lib.gui.simple.AbstractGUI;
import biz.princeps.lib.gui.simple.Icon;
import biz.princeps.lib.storage.requests.Conditions;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.material.SpawnEgg;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by spatium on 21.07.17.
 */
public abstract class AbstractManage extends AbstractGUI {


    private static int SIZE;

    static {
        ConfigurationSection section = Landlord.getInstance().getConfig().getConfigurationSection("Manage");

        Set<String> keys = section.getKeys(true);

        int trues = 0;
        for (String key : keys) {
            if (section.getBoolean(key))
                trues++;
        }

        SIZE = (trues / 9 + (trues % 9 == 0 ? 0 : 1)) * 9;
    }

    private List<OwnedLand> regions;
    private LangManager lm;
    private Landlord plugin;

    public AbstractManage(Player player, String header, List<OwnedLand> land) {
        super(player, SIZE, header);
        this.regions = new ArrayList<>();
        regions.addAll(land);
        plugin = Landlord.getInstance();
        lm = plugin.getLangManager();
    }

    public AbstractManage(Player player, MultiPagedGUI landGui, String header, List<OwnedLand> land) {
        super(player, SIZE + 9, header, landGui);
        this.regions = new ArrayList<>();
        regions.addAll(land);
        plugin = Landlord.getInstance();
        lm = plugin.getLangManager();
    }


    @Override
    public Inventory display() {
        create();
        this.player.openInventory(this.getInventory());
        return this.getInventory();
    }

    @Override
    protected void create() {
        List<String> regenerateDesc = lm.getStringList("Commands.Manage.Regenerate.description");
        List<String> greetDesc = lm.getStringList("Commands.Manage.SetGreet.description");
        List<String> farewellDesc = lm.getStringList("Commands.Manage.SetFarewell.description");


        int position = 0;

        if (regions.size() < 1)
            return;

        OwnedLand land = regions.get(0);


        for (LLFlag iFlag : land.getFlags()) {

            // For every IFlag of the land we wanna display an icon in the gui IF the flag is enabled for change
            String flagName = iFlag.getWGFlag().getName();
            String title = lm.getRawString("Commands.Manage.Allow" + flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ".title");
            List<String> description = lm.getStringList("Commands.Manage.Allow" + flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ".description");

            if (plugin.getConfig().getBoolean("Manage." + flagName + ".enable")) {

                int finalPosition = position;
                this.setIcon(position, new Icon(createItem(iFlag.getMaterial(), 1,
                        title, formatList(description, iFlag.getStatus())))
                        .addClickAction((p) -> {
                            for (OwnedLand region : regions) {
                                for (LLFlag llFlag : region.getFlags()) {
                                    if (llFlag.getWGFlag().equals(iFlag.getWGFlag())) {
                                        llFlag.toggle();
                                    }
                                }
                            }
                            updateLore(finalPosition, formatList(description, iFlag.getStatus()));
                        })
                );
                position++;
            }
        }

        // Reminder: Regenerate is not implemented in Manageall, cos it might cos some trouble. Calculating costs might be a bit tedious
        //TODO Readd regenerate into abstractManage, but dont execute it on manageall

        // Set greet icon
        if (plugin.getConfig().getBoolean("Manage.setgreet.enable")) {
            String currentGreet = land.getWGLand().getFlag(DefaultFlag.GREET_MESSAGE);
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.setgreet.item")), 1,
                    lm.getRawString("Commands.Manage.SetGreet.title"), formatList(greetDesc, currentGreet)))
                    .addClickAction((p -> {
                        p.closeInventory();
                        ComponentBuilder builder = new ComponentBuilder(lm.getString("Commands.Manage.SetGreet.clickMsg"));
                        if (regions.size() > 1)
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setgreetall "));
                        else
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setgreet "));

                        p.spigot().sendMessage(builder.create());
                    }))
            );
            position++;
        }

        if (plugin.getConfig().getBoolean("Manage.mob-spawning.enable")) {
            String title = lm.getRawString("Commands.Manage.AllowMob-spawning.title");
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.mob-spawning.item")), 1,
                    title, lm.getStringList("Commands.Manage.AllowMob-spawning.description")))
                    .addClickAction(p -> {
                        // Open a new gui with spawneggs where you can manage the spawns by clicking on them

                        AbstractGUI gui = new AbstractGUI(p, 27, title, this) {
                            @Override
                            protected void create() {
                                EntityType[] types = EntityType.values();
                                for (int i = 0; i < 27 ; i++) {
                                    ItemStack spawnEgg = new ItemStack(Material.MONSTER_EGG);
                                    SpawnEggMeta meta = (SpawnEggMeta) spawnEgg.getItemMeta();
                                    meta.setSpawnedType(types[i]);
                                    spawnEgg.setItemMeta(meta);

                                    //TODO add click actions
                                    AbstractManage.this.setIcon(i, new Icon(spawnEgg));
                                }
                            }
                        };
                        gui.display();


                    }));

            position++;
        }

        // set farewell icon
        if (plugin.getConfig().getBoolean("Manage.setfarewell.enable")) {
            String currentFarewell = land.getWGLand().getFlag(DefaultFlag.FAREWELL_MESSAGE);
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.setfarewell.item")), 1,
                    lm.getRawString("Commands.Manage.SetFarewell.title"), formatList(farewellDesc, currentFarewell)))
                    .addClickAction((p -> {
                        p.closeInventory();
                        ComponentBuilder builder = new ComponentBuilder(lm.getString("Commands.Manage.SetFarewell.clickMsg"));
                        if (regions.size() > 1)
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setfarewellall "));
                        else
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setfarewell "));

                        p.spigot().sendMessage(builder.create());
                    }))
            );
            position++;
        }

        // set friends icon
        if (plugin.getConfig().getBoolean("Manage.friends.enable")) {
            ItemStack skull = createSkull(player.getName(), lm.getRawString("Commands.Manage.ManageFriends.title"), lm.getStringList("Commands.Manage.ManageFriends.description"));
            Set<UUID> friends = land.getWGLand().getMembers().getUniqueIds();
            MultiPagedGUI friendsGui = new MultiPagedGUI(player, (int) Math.ceil((double) friends.size() / 9.0), lm.getRawString("Commands.Manage.ManageFriends.title"), new ArrayList<>(), this) {

            };
            friends.forEach(id -> friendsGui.addIcon(new Icon(createSkull(Bukkit.getOfflinePlayer(id).getName(),
                    Bukkit.getOfflinePlayer(id).getName(), formatFriendsSegment(id)))
                    .addClickAction(player -> {
                        ConfirmationGUI confirmationGUI = new ConfirmationGUI(player, lm.getRawString("Commands.Manage.ManageFriends.unfriend")
                                .replace("%player%", Bukkit.getOfflinePlayer(id).getName()),
                                p -> {
                                    friendsGui.removeIcon(friendsGui.filter(Bukkit.getOfflinePlayer(id).getName()).get(0));
                                    if (regions.size() > 1)
                                        Bukkit.dispatchCommand(player, "land unfriendall " + Bukkit.getOfflinePlayer(id).getName());
                                    else
                                        Bukkit.dispatchCommand(player, "land unfriend " + Bukkit.getOfflinePlayer(id).getName());

                                    player.closeInventory();
                                    friendsGui.display();
                                },
                                p -> {
                                    player.closeInventory();
                                    friendsGui.display();
                                }, friendsGui);
                        confirmationGUI.setConfirm(lm.getRawString("Confirmation.accept"));
                        confirmationGUI.setDecline(lm.getRawString("Confirmation.decline"));
                        confirmationGUI.display();
                    })));


            this.setIcon(position, new Icon(skull)
                    .setName(lm.getRawString("Commands.Manage.ManageFriends.title"))
                    .addClickAction(p -> friendsGui.display())
            );
            position++;
        }

        if (plugin.getConfig().getBoolean("Manage.unclaim.enable")) {
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.unclaim.item")),
                    1, lm.getRawString("Commands.Manage.Unclaim.title"), lm.getStringList("Commands.Manage.Unclaim.description")))
                    .addClickAction((player1 -> {
                        ConfirmationGUI gui = new ConfirmationGUI(player1, lm.getRawString("Commands.Manage.Unclaim.confirmationTitle").replace("%land%", land.getName()),
                                p -> {
                                    if (regions.size() > 1)
                                        Bukkit.dispatchCommand(p, "ll unclaimall");
                                    else
                                        Bukkit.dispatchCommand(p, "ll unclaim");

                                    p.closeInventory();
                                },
                                (p) -> {
                                    p.closeInventory();
                                    display();
                                }, this);
                        gui.setConfirm(lm.getRawString("Confirmation.accept"));
                        gui.setDecline(lm.getRawString("Confirmation.decline"));
                        gui.display();
                    })));
            position++;
        }

    }

    private List<String> formatFriendsSegment(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);

        CompletableFuture<List<Object>> future = new CompletableFuture<>();
        plugin.getExecutorService().submit(() -> {
            List<Object> listi = plugin.getDatabaseAPI().retrieveObjects(LPlayer.class, new Conditions.Builder().addCondition("uuid", op.getUniqueId().toString()).create());
            future.complete(listi);
        });
        List<String> stringList = lm.getStringList("Commands.Manage.ManageFriends.friendSegment");
        List<String> newlist = new ArrayList<>();
        try {
            final String lastseen;
            if (op.isOnline()) {
                lastseen = lm.getRawString("Commands.Info.online");
            } else {
                lastseen = ((LPlayer) future.get().get(0)).getLastSeenAsString();
            }
            stringList.forEach(s -> {
                String ss = s.replace("%seen%", lastseen);
                newlist.add(ss);
            });

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            return newlist;
        }
    }

    private void updateLore(int index, List<String> lore) {
        ItemStack item = this.getIcon(index).itemStack;
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        refresh();
    }

    private List<String> formatList(List<String> allowDesc, String flag) {
        List<String> newList = new ArrayList<>();
        allowDesc.forEach(s -> newList.add(s.replace("%var%", flag)));
        return newList;
    }

    private ItemStack createItem(Material mat, int amount, String title, List<String> desc) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(title);
        itemMeta.setLore(desc);
        item.setItemMeta(itemMeta);
        return item;
    }

    private ItemStack createSkull(String owner, String displayname, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(owner);
        skullMeta.setDisplayName(displayname);
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }
}
