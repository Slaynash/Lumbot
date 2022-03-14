package slaynash.lum.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import slaynash.lum.bot.utils.ExceptionUtils;

public final class DBConnectionManagerLum {
    public static final String LOG_IDENTIFIER = "DBLUM";

    private DBConnectionManagerLum() {
    }

    private static final int DATABASE_TIMEOUT = 10; // in seconds

    private static Connection connection;
    private static final Map<ResultSet, PreparedStatement> requests = new HashMap<>();
    private static volatile int requestCount, updateCount, requestClosedCount, updateClosedCount;

    public static void init() {
        try {
            System.out.println("Connecting to Database...");
            DriverManager.setLoginTimeout(DATABASE_TIMEOUT);
            connection = DriverManager.getConnection("jdbc:mysql://" + ConfigManager.dbAddress + ":" + ConfigManager.dbPort + "/" + ConfigManager.dbDatabaseLum, ConfigManager.dbLogin, ConfigManager.dbPassword);
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

    public static ResultSet sendRequest(String statement, Object... args) throws SQLException {
        requestCount++;
        PreparedStatement ps = getConnection().prepareStatement(statement);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                throw new IllegalArgumentException("Trying to initialise request with null arg (arg number " + i + ")");
            else if (args[i].getClass() == String.class)
                ps.setString(i + 1, (String) args[i]);
            else if (args[i].getClass() == Integer.class)
                ps.setInt(i + 1, (int) args[i]);
            else if (args[i].getClass() == Boolean.class)
                ps.setBoolean(i + 1, (boolean) args[i]);
            else if (args[i].getClass() == Long.class)
                ps.setLong(i + 1, (long) args[i]);
            else throw new IllegalArgumentException("Trying to initialise request with unknown arg type " + args[0].getClass() + "(arg number " + i + ")");
        }
        ResultSet rs = ps.executeQuery();
        synchronized (requests) {
            requests.put(rs, ps);
        }
        return rs;
    }

    public static String getString(String table, String keycolumn, String valuecolumn, String keyrow) {
        String pString = "";
        ResultSet rs;
        try {
            rs = DBConnectionManagerLum.sendRequest("SELECT " + valuecolumn + " FROM `" + table + "` WHERE " + keycolumn + " = '" + keyrow + "'");
            if (rs.next());
                pString = rs.getString(valuecolumn);
            rs.close();
        } catch (SQLException e) {
            ExceptionUtils.reportException("Exception while fetching SQL string", e);
        }
        return pString;
    }

    public static int sendUpdate(String statement, Object... args) throws SQLException {
        updateCount++;
        PreparedStatement ps = getConnection().prepareStatement(statement);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                throw new IllegalArgumentException("Trying to initialise request with null arg (arg number " + i + ")");
            else if (args[i].getClass() == String.class)
                ps.setString(i + 1, (String) args[i]);
            else if (args[i].getClass() == Integer.class)
                ps.setInt(i + 1, (int) args[i]);
            else if (args[i].getClass() == Boolean.class)
                ps.setBoolean(i + 1, (boolean) args[i]);
            else if (args[i].getClass() == Long.class)
                ps.setLong(i + 1, (long) args[i]);
            else throw new IllegalArgumentException("Trying to initialise request with unknown arg type " + args[i].getClass() + "(arg number " + i + ")");
        }
        int r = ps.executeUpdate();
        ps.close();
        updateClosedCount++;
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
            requestClosedCount++;
        }
    }

    public static int getRequestCount() {
        return requestCount;
    }

    public static int getUpdateCount() {
        return updateCount;
    }

    public static int getRequestClosedCount() {
        return requestClosedCount;
    }

    public static int getUpdateClosedCount() {
        return updateClosedCount;
    }
}