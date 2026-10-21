package ir.gharri.vkauth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MysqlStorage implements Storage {

    private static boolean driverLoaded;

    private final String url;
    private final String user;
    private final String pass;
    private final String table;

    public MysqlStorage(String host, int port, String database, String user, String pass, String table) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&autoReconnect=true"
                + "&connectTimeout=5000";
        this.user = user;
        this.pass = pass;
        String safe = table == null ? "" : table.replaceAll("[^A-Za-z0-9_]", "");
        this.table = safe.isEmpty() ? "vkauth_accounts" : safe;
    }

    @Override
    public String name() {
        return "mysql";
    }

    private static synchronized void loadDriver() throws SQLException {
        if (driverLoaded) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on this server", e);
        }
    }

    private Connection connect() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(url, user, pass);
    }

    private boolean tableExists(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SHOW TABLES LIKE ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Makes sure registered / last_login exist and are BIGINT (epoch millis), fixing old tables. */
    private void ensureTimeColumns(Connection c) throws SQLException {
        ensureTimeColumn(c, "registered");
        ensureTimeColumn(c, "last_login");
    }

    private void ensureTimeColumn(Connection c, String column) throws SQLException {
        String type = null;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SHOW COLUMNS FROM `" + table + "` LIKE '" + column + "'")) {
            if (rs.next()) type = rs.getString("Type");
        }
        try (Statement st = c.createStatement()) {
            if (type == null) {
                st.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + column
                        + "` BIGINT NOT NULL DEFAULT 0");
                return;
            }
            String t = type.toLowerCase(Locale.ROOT);
            if (t.startsWith("bigint")) return;

            if (t.startsWith("date") || t.startsWith("timestamp")) {
                String tmp = column + "_ms";
                st.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + tmp + "` BIGINT NOT NULL DEFAULT 0");
                st.executeUpdate("UPDATE `" + table + "` SET `" + tmp + "` = IFNULL(UNIX_TIMESTAMP(`" + column
                        + "`), 0) * 1000");
                st.executeUpdate("ALTER TABLE `" + table + "` DROP COLUMN `" + column + "`");
                st.executeUpdate("ALTER TABLE `" + table + "` CHANGE COLUMN `" + tmp + "` `" + column
                        + "` BIGINT NOT NULL DEFAULT 0");
            } else {
                st.executeUpdate("ALTER TABLE `" + table + "` MODIFY COLUMN `" + column
                        + "` BIGINT NOT NULL DEFAULT 0");
            }
        }
    }

    @Override
    public void init() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(16) NOT NULL, "
                    + "hash VARCHAR(255) NOT NULL, "
                    + "registered BIGINT NOT NULL DEFAULT 0, "
                    + "last_login BIGINT NOT NULL DEFAULT 0"
                    + ") DEFAULT CHARSET=utf8mb4");
            ensureTimeColumns(c);
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
                             + "registered = IF(registered > 0, registered, VALUES(registered)), "
                             + "last_login = VALUES(last_login)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name == null ? "unknown" : name);
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
    public List<StoredAccount> getAll() throws Exception {
        List<StoredAccount> out = new ArrayList<>();
        try (Connection c = connect()) {
            if (!tableExists(c)) return out;
            ensureTimeColumns(c);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT uuid, name, hash, registered, last_login FROM `" + table + "`")) {
                while (rs.next()) {
                    UUID id;
                    try {
                        id = UUID.fromString(rs.getString("uuid"));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    out.add(new StoredAccount(id, rs.getString("name"),
                            new Account(rs.getString("hash"), rs.getLong("registered"), rs.getLong("last_login"))));
                }
            }
        }
        return out;
    }

    @Override
    public void deleteAll() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS `" + table + "`");
        }
    }

    @Override
    public void close() {
    }
}
