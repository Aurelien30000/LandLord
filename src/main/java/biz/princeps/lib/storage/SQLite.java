package biz.princeps.lib.storage;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by spatium on 11.06.17.
 */
public abstract class SQLite extends AbstractDatabase {

    private String dbpath;
    private Connection sqlConnection;


    public SQLite(String dbpath) {
        super();
        this.dbpath = dbpath;
        this.initialize();
    }

    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            getLogger().warning("The JBDC library for your database type was not found. Please read the plugin's support for more information.");
        }
        Connection conn = getSQLConnection();
        if (conn == null) {
            getLogger().warning("Could not establish SQLite Connection");
        }
    }

    public Connection getSQLConnection() {
        // Check if Connection was not previously closed.
        try {
            if (sqlConnection == null || sqlConnection.isClosed()) {
                sqlConnection = this.createSQLiteConnection();
            }
        } catch (SQLException e) {
            getLogger().warning("Error while attempting to retrieve connection to database: " + e);
        }
        return sqlConnection;
    }

    private Connection createSQLiteConnection() throws SQLException {

        File dbfile = new File(dbpath);
        try {
            if (dbfile.createNewFile()) {
                getLogger().warning("Successfully created database file.");
            }
        } catch (IOException e) {
            getLogger().warning("Error while creating database file: " + e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbfile);
    }

    @Override
    protected void setupDatabase() {

    }

    @Override
    public void close() {
        try {
            this.sqlConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void executeUpdate(String query) {
        pool.submit(() -> {
            try (PreparedStatement st = sqlConnection.prepareStatement(query)) {

                st.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public ResultSet executeQuery(String query) {
        try {
            return pool.submit(() -> {
                try (PreparedStatement st = sqlConnection.prepareStatement(query)) {

                    return st.executeQuery();

                } catch (SQLException e) {
                    return null;
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void execute(String query) {
        pool.submit(() -> {
            try (PreparedStatement st = sqlConnection.prepareStatement(query)) {

                st.execute();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


}

