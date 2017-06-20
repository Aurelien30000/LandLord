package com.jcdesimp.landlord.persistantData.db;

import biz.princeps.lib.storage.MySQL;
import com.jcdesimp.landlord.Landlord;
import com.jcdesimp.landlord.persistantData.Data;
import com.jcdesimp.landlord.persistantData.Friend;
import com.jcdesimp.landlord.persistantData.LandFlag;
import com.jcdesimp.landlord.persistantData.OwnedLand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Created by spatium on 10.06.17.
 */
public class MySQLDatabase extends MySQL {

    public MySQLDatabase(String hostname, int port, String database, String user, String password) {
        super(hostname, port, database, user, password);
        setupDatabase();
    }

    @Override
    public void setupDatabase() {
        /*
        String query1 = "CREATE TABLE IF NOT EXISTS ll_flagperm (landid INTEGER, identifier VARCHAR(20), canEveryone BOOLEAN, canFriends BOOLEAN, id INTEGER)";
        this.execute(query1);
        String query2 = "ALTER TABLE ll_flagperm ADD UNIQUE (id)";
        this.execute(query2);
        */
        String query3 = "CREATE TABLE IF NOT EXISTS ll_friend (landid INTEGER, frienduuid VARCHAR(36), id INTEGER)";
        this.execute(query3);
        String query4 = "ALTER TABLE ll_friend ADD UNIQUE (id)";
        this.execute(query4);
        String query5 = "CREATE TABLE IF NOT EXISTS ll_land (landid INTEGER, owneruuid VARCHAR(36), x INTEGER, z INTEGER, world VARCHAR(16), flags TEXT)";
        this.execute(query5);
        String query6 = "ALTER TABLE ll_land ADD UNIQUE (landid)";
        this.execute(query6);
        Landlord.getInstance().getLogger().info("Connected to MySQL and setted up tables!");
    }

    public OwnedLand getLand(Data data) {
        try {
            return pool.submit(() -> {
                OwnedLand land = null;
                String query = "SELECT * FROM ll_land WHERE world = ? AND x = ? AND z = ?";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                    st.setString(1, data.getWorld());
                    st.setInt(2, data.getX());
                    st.setInt(3, data.getZ());

                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        land = new OwnedLand(data);
                        land.setOwner(UUID.fromString(res.getString("owneruuid")));
                        land.setLandId(res.getInt("landid"));
                        land.setFriends(getFriends(land.getLandId()));
                        land.setFlags(stringToFlags(res.getString("flags")));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return land;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    private List<LandFlag> stringToFlags(String flags) {
        String[] splitted = flags.split("_");
        List<LandFlag> list = new ArrayList<>();
        for (String flag : splitted) {
            String[] params = flag.split(":");
            LandFlag flagy = new LandFlag(Integer.parseInt(params[0]), params[1], Boolean.parseBoolean(params[2]), Boolean.parseBoolean(params[3]));
            list.add(flagy);
        }
        return list;
    }

    private String flagsToString(List<LandFlag> list) {
        StringBuilder sb = new StringBuilder(list.get(0).toString());
        for (int i = 1; i < list.size(); i++) {
            sb.append("_");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    protected List<Friend> getFriends(int id) {
        ArrayList<Friend> list = new ArrayList<>();
        String query = "SELECT * FROM ll_friend WHERE landid = ?";
        try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
            st.setInt(1, id);

            ResultSet res = st.executeQuery();
            while (res.next()) {
                Friend f = new Friend(UUID.fromString(res.getString("frienduuid")));
                f.setId(res.getInt("id"));
                list.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void removeFriend(UUID f) {
        pool.submit(() -> {
            String query = "DELETE FROM ll_friend WHERE frienduuid = ?";
            try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                st.setString(1, f.toString());
                st.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeLand(int landid) {
        pool.submit(() -> {
            String query = "DELETE FROM ll_land WHERE landid = ?";
            String query3 = "DELETE FROM ll_friend WHERE landid = ?";

            try (Connection con = getConnection();
                 PreparedStatement st = con.prepareStatement(query);
                 PreparedStatement st3 = con.prepareStatement(query3)) {
                st.setInt(1, landid);
                st.executeUpdate();
                st3.setInt(1, landid);
                st3.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void save(OwnedLand land) {
        String query2 = "REPLACE INTO ll_friend (landid, frienduuid, id) VALUES (?,?,?)";
        String query3 = "REPLACE INTO ll_land (landid, owneruuid, x, z, world, flags) VALUES (?,?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement st2 = con.prepareStatement(query2);
             PreparedStatement st3 = con.prepareStatement(query3)) {

            for (Friend f : land.getFriends()) {
                st2.setInt(1, land.getLandId());
                st2.setString(2, f.getUuid().toString());
                st2.setInt(3, f.getId());
                st2.execute();

            }

            st3.setInt(1, land.getLandId());
            st3.setString(2, land.getOwner().toString());
            st3.setInt(3, land.getData().getX());
            st3.setInt(4, land.getData().getZ());
            st3.setString(5, land.getData().getWorld());
            st3.setString(6, flagsToString(land.getFlags()));
            st3.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public List<OwnedLand> getLands(UUID owner) {
        try {
            return pool.submit(() -> {
                ArrayList<OwnedLand> list = new ArrayList<>();
                String query = "SELECT * FROM ll_land WHERE owneruuid = ?";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                    st.setString(1, owner.toString());

                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        Data data = new Data(res.getString("world"), res.getInt("x"), res.getInt("z"));
                        OwnedLand ownedLand = new OwnedLand(data);
                        ownedLand.setOwner(owner);
                        ownedLand.setLandId(res.getInt("landid"));
                        ownedLand.setFriends(getFriends(ownedLand.getLandId()));
                        ownedLand.setFlags(stringToFlags(res.getString("flags")));
                        list.add(ownedLand);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return list;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public List<OwnedLand> getLands(UUID owner, String world) {
        try {
            return pool.submit(() -> {
                ArrayList<OwnedLand> list = new ArrayList<>();
                String query = "SELECT * FROM ll_land WHERE owneruuid = ? AND world = ?";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                    st.setString(1, owner.toString());
                    st.setString(2, world);
                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        Data data = new Data(world, res.getInt("x"), res.getInt("z"));
                        OwnedLand ownedLand = new OwnedLand(data);
                        ownedLand.setOwner(owner);
                        ownedLand.setLandId(res.getInt("landid"));
                        ownedLand.setFriends(getFriends(ownedLand.getLandId()));
                        ownedLand.setFlags(stringToFlags(res.getString("flags")));
                        list.add(ownedLand);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return list;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public List<OwnedLand> getLands(String world) {
        try {
            return pool.submit(() -> {
                ArrayList<OwnedLand> list = new ArrayList<>();
                String query = "SELECT * FROM ll_land WHERE world = ?";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                    st.setString(1, world);
                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        Data data = new Data(world, res.getInt("x"), res.getInt("z"));
                        OwnedLand ownedLand = new OwnedLand(data);
                        ownedLand.setOwner(UUID.fromString(res.getString("owneruuid")));
                        ownedLand.setLandId(res.getInt("landid"));
                        ownedLand.setFriends(getFriends(ownedLand.getLandId()));
                        ownedLand.setFlags(stringToFlags(res.getString("flags")));
                        list.add(ownedLand);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return list;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    /*
    public List<OwnedLand> getNearbyLands(Location location, int offsetX, int offsetZ) {
        try {
            return pool.submit(() -> {
                ArrayList<OwnedLand> list = new ArrayList<>();
                String query = "SELECT * FROM ll_land WHERE world = ?";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {

                    st.setString(1, location.getWorld().getName());
                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        Data data = new Data(location.getWorld().getName(), res.getInt("x"), res.getInt("z"));

                        if (data.getX() <= location.getChunk().getX() + offsetX &&
                                data.getX() >= location.getChunk().getX() - offsetX &&
                                data.getZ() <= location.getChunk().getZ() + offsetZ &&
                                data.getZ() >= location.getChunk().getZ() - offsetZ) {

                            OwnedLand ownedLand = new OwnedLand(data);
                            ownedLand.setOwner(UUID.fromString(res.getString("owneruuid")));
                            ownedLand.setLandId(res.getInt("landid"));
                            ownedLand.setFriends(getFriends(ownedLand.getLandId()));
                            ownedLand.setFlags(stringToFlags(res.getString("flags")));
                            list.add(ownedLand);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return list;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
    */

    public int getFirstFreeLandID() {
        try {
            return pool.submit(() -> {
                String query = "SELECT COUNT(*) FROM ll_land";
                try (Connection con = getConnection(); PreparedStatement st = con.prepareStatement(query)) {
                    ResultSet res = st.executeQuery();
                    while (res.next()) {
                        return res.getInt(1);
                    }
                }
                return 0;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return -1;
        }
    }

}
