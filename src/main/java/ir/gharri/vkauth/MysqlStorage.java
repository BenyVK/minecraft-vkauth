package ir.gharri.vkauth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class MysqlStorage implements Storage {

    private final String url;
    private final String user;
    private final String pass;
    private final String table;

    public MysqlStorage(String host, int port, String database, String user, String pass, String table) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&autoReconnect=true";
        this.user = user;
        this.pass = pass;
        String safe = table.replaceAll("[^A-Za-z0-9_]", "");
        this.table = safe.isEmpty() ? "vkauth_accounts" : safe;
    }

    @Override
    public String name() {
        return "mysql";
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    @Override
    public void init() throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on this server", e);
        }
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(16) NOT NULL, "
                    + "hash VARCHAR(255) NOT NULL, "
                    + "registered BIGINT NOT NULL, "
                    + "last_login BIGINT NOT NULL DEFAULT 0"
                    + ") DEFAULT CHARSET=utf8mb4");

            boolean hasColumn;
            try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM `" + table + "` LIKE 'last_login'")) {
                hasColumn = rs.next();
            }
            if (!hasColumn) {
                st.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN last_login BIGINT NOT NULL DEFAULT 0");
            }
        }
    }

    @Override
    public Account getAccount(UUID uuid) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT hash, registered, last_login FROM `" + table + "` WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Account(rs.getString("hash"), rs.getLong("registered"), rs.getLong("last_login"));
            }
        }
    }

    @Override
    public void saveAccount(UUID uuid, String name, Account account) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO `" + table + "` (uuid, name, hash, registered, last_login) VALUES (?, ?, ?, ?, ?) "
                             + "ON DUPLICATE KEY UPDATE name = VALUES(name), hash = VALUES(hash), "
                             + "last_login = VALUES(last_login)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, account.hash());
            ps.setLong(4, account.registered() > 0 ? account.registered() : System.currentTimeMillis());
            ps.setLong(5, account.lastLogin());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateLastLogin(UUID uuid, long time) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE `" + table + "` SET last_login = ? WHERE uuid = ?")) {
            ps.setLong(1, time);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
    }
}
