package slaynash.lum.bot;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import slaynash.lum.bot.discord.GuildConfiguration;
import slaynash.lum.bot.utils.ExceptionUtils;

public final class DBConnectionManagerLum {
    public static final String LOG_IDENTIFIER = "DBLUM";

    private DBConnectionManagerLum() {
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
            if (ConfigManager.dbAddress.isBlank() || ConfigManager.dbPort.isBlank() || ConfigManager.dbDatabaseLum.isBlank() || ConfigManager.dbLogin.isBlank() || ConfigManager.dbPassword.isBlank()) {
                System.err.println("Database configuration is missing!");
                return;
            }
            DriverManager.setLoginTimeout(DATABASE_TIMEOUT);
            connection = DriverManager.getConnection("jdbc:mysql://" + ConfigManager.dbAddress + ":" + ConfigManager.dbPort + "/" + ConfigManager.dbDatabaseLum + "?useUnicode=true&characterEncoding=UTF-8", ConfigManager.dbLogin, ConfigManager.dbPassword);
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

    public static PreparedStatement formatStatement(String statement, Object... args) throws SQLException {
        PreparedStatement ps = Objects.requireNonNull(getConnection()).prepareStatement(statement);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                ps.setNull(i + 1, Types.VARCHAR);
            else if (args[i].getClass() == String.class)
                ps.setString(i + 1, new String(args[i].toString().getBytes(), StandardCharsets.UTF_8));
            else if (args[i].getClass() == Integer.class)
                ps.setInt(i + 1, (int) args[i]);
            else if (args[i].getClass() == Boolean.class)
                ps.setBoolean(i + 1, (boolean) args[i]);
            else if (args[i].getClass() == Long.class)
                ps.setLong(i + 1, (long) args[i]);
            else if (args[i].getClass() == Timestamp.class) {
                Timestamp ts = (Timestamp) args[i];
                if (ts.getTime() / 1000 > Integer.MAX_VALUE)
                    throw new IllegalArgumentException("Timestamp too big to be stored in an int " + ts.getTime() / 1000 + " > " + Integer.MAX_VALUE);
                ps.setTimestamp(i + 1, ts);
            }
            else throw new IllegalArgumentException("Trying to initialise request with unknown arg type " + args[i].getClass() + "(arg number " + i + ")");
        }
        return ps;
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

    public static String getString(String table, String keycolumn, String valuecolumn, String keyrow) {
        String pString = "";
        ResultSet rs;
        try {
            rs = DBConnectionManagerLum.sendRequest("SELECT " + valuecolumn + " FROM `" + table + "` WHERE " + keycolumn + " = '" + keyrow + "' LIMIT 1");
            if (rs.next())
                pString = rs.getString(valuecolumn);
            closeRequest(rs);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Exception while fetching SQL string", e);
        }
        return pString;
    }

    public static GuildConfiguration getGuildConfig(Long guildID) {
        return getGuildConfig(guildID.toString());
    }

    public static GuildConfiguration getGuildConfig(String guildID) {
        Timestamp ts;
        boolean scamShield, scamShieldBan, scamShieldCross, scamShieldDm, mLLogScan, mLLogReaction, mLReplies,
            mLPartialRemover, mLGeneralRemover, dLLRemover, lumReplies, dadJokes;
        ResultSet rs;
        try {
            rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `GuildConfigurations` WHERE GuildID = '" + guildID + "' LIMIT 1");
            if (rs.next()) {
                ts = rs.getTimestamp(GuildConfiguration.Setting.TS.string);
                scamShield = rs.getBoolean(GuildConfiguration.Setting.SCAMSHIELD.string);
                scamShieldBan = rs.getBoolean(GuildConfiguration.Setting.SSBAN.string);
                scamShieldCross = rs.getBoolean(GuildConfiguration.Setting.SSCROSS.string);
                scamShieldDm = rs.getBoolean(GuildConfiguration.Setting.SSCROSS.string);
                mLLogScan = rs.getBoolean(GuildConfiguration.Setting.LOGSCAN.string);
                mLLogReaction = rs.getBoolean(GuildConfiguration.Setting.LOGREACTION.string);
                mLReplies = rs.getBoolean(GuildConfiguration.Setting.MLREPLIES.string);
                mLPartialRemover = rs.getBoolean(GuildConfiguration.Setting.PARTIALLOGREMOVER.string);
                mLGeneralRemover = rs.getBoolean(GuildConfiguration.Setting.GENERALLOGREMOVER.string);
                dLLRemover = rs.getBoolean(GuildConfiguration.Setting.DLLREMOVER.string);
                lumReplies = rs.getBoolean(GuildConfiguration.Setting.LUMREPLIES.string);
                dadJokes = rs.getBoolean(GuildConfiguration.Setting.DADJOKES.string);
            }
            else {
                System.out.println("No guild configuration found for guild and creating " + guildID);
                PreparedStatement ps = getConnection().prepareStatement("INSERT INTO `GuildConfigurations` (`GuildID`) VALUES ('" + guildID + "');");
                ps.executeUpdate();
                ps.close();
                return getGuildConfig(guildID);
            }
            closeRequest(rs);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Exception while fetching Guild Config", e);
            return null;
        }
        return new GuildConfiguration(guildID, ts, scamShield, scamShieldBan, scamShieldCross, scamShieldDm, mLLogScan, mLLogReaction, mLReplies, mLPartialRemover, mLGeneralRemover, dLLRemover, lumReplies, dadJokes);
    }

    public static void setGuildSetting(Long guildID, String setting, boolean value) {
        setGuildSetting(guildID.toString(), setting, value);
    }

    public static void setGuildSetting(String guildID, String setting, boolean value) {
        try {
            PreparedStatement ps = getConnection().prepareStatement("UPDATE `GuildConfigurations` SET `" + setting + "` = '" + (value ? "1" : "0") + "' WHERE `GuildID` = '" + guildID + "'");
            int rowsedited = ps.executeUpdate();
            ps.close();
            if (rowsedited == 0) {
                PreparedStatement ps2 = getConnection().prepareStatement("INSERT INTO `GuildConfigurations` (`GuildID`, `" + setting + "`) VALUES ('" + guildID + "', '" + (value ? "1" : "0") + "');");
                ps2.executeUpdate();
                ps2.close();
            }
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Exception while setting Guild Config", e);
        }
    }

    /** INSERT, UPDATE or DELETE
     *
     * @param statement The SQL statement to execute
     * @param args Replacements for the ? in the statement
     * @return Number of Rows affected
     */
    public static int sendUpdate(String statement, Object... args) throws SQLException {
        if (!ConfigManager.mainBot) {
            throw new SQLException("Backup bot cannot send updates");
        }

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
