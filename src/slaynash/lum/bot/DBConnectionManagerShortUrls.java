package slaynash.lum.bot;

import static slaynash.lum.bot.DBConnectionManagerLum.formatStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import slaynash.lum.bot.utils.ExceptionUtils;

public final class DBConnectionManagerShortUrls {
    public static final String LOG_IDENTIFIER = "DBSHORT";

    private DBConnectionManagerShortUrls() {
    }

    private static final int DATABASE_TIMEOUT = 10; // in seconds

    private static Connection connection;
    private static final Map<ResultSet, PreparedStatement> requests = new HashMap<>();
    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private static final AtomicInteger updateCount = new AtomicInteger(0);
    private static final AtomicInteger requestClosedCount = new AtomicInteger(0);
    private static final AtomicInteger updateClosedCount = new AtomicInteger(0);

    public static void init() {
        try {
            System.out.println("Connecting to Database...");
            if (ConfigManager.dbAddress.isBlank() || ConfigManager.dbPort.isBlank() || ConfigManager.dbDatabaseShortURL.isBlank() || ConfigManager.dbLogin.isBlank() || ConfigManager.dbPassword.isBlank()) {
                System.err.println("Database configuration is missing!");
                return;
            }
            DriverManager.setLoginTimeout(DATABASE_TIMEOUT);
            connection = DriverManager.getConnection("jdbc:mysql://" + ConfigManager.dbAddress + ":" + ConfigManager.dbPort + "/" + ConfigManager.dbDatabaseShortURL, ConfigManager.dbLogin, ConfigManager.dbPassword);
            System.out.println("Connection to Database initialised");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to contact database", e);
        }
    }

    private static Connection getConnection() {
        try {
            if (connection == null || !connection.isValid(DATABASE_TIMEOUT)) {
                if (connection != null && !connection.isClosed()) connection.close();
                init();
            }
            return connection;
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to get database connection", e);
        }
        return null;
    }

    /** SELECT, CALL
     *
     * @param statement The SQL statement to execute
     * @param args Replacements for the ? in the statement
     * @return ResultSet
     */
    public static ResultSet sendRequest(String statement, Object... args) throws SQLException {
        requestCount.incrementAndGet();
        PreparedStatement ps = formatStatement(statement, args);
        ResultSet rs = ps.executeQuery();
        synchronized (requests) {
            requests.put(rs, ps);
        }
        return rs;
    }

    /** INSERT, UPDATE or DELETE
     *
     * @param statement The SQL statement to execute
     * @param args Replacements for the ? in the statement
     * @return Number of Rows affected
     */
    public static int sendUpdate(String statement, Object... args) throws SQLException {
        updateCount.incrementAndGet();
        PreparedStatement ps = formatStatement(statement, args);
        int r = ps.executeUpdate();
        ps.close();
        updateClosedCount.incrementAndGet();
        return r;
    }

    public static void closeRequest(ResultSet resultSet) throws SQLException {
        PreparedStatement ps;
        synchronized (requests) {
            ps = requests.remove(resultSet);
        }
        resultSet.close();
        if (ps != null) {
            ps.close();
            requestClosedCount.incrementAndGet();
        }
    }

    public static int getRequestCount() {
        return requestCount.get();
    }

    public static int getUpdateCount() {
        return updateCount.get();
    }

    public static int getRequestClosedCount() {
        return requestClosedCount.get();
    }

    public static int getUpdateClosedCount() {
        return updateClosedCount.get();
    }
}
