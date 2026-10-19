package slaynash.lum.bot.timers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public final class TemporaryBans {
    private record BanKey(long guildId, long userId) {
    }

    private static final Set<BanKey> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private TemporaryBans() {
    }

    public static void start() {
        try {
            DBConnectionManagerLum.sendUpdate("""
                CREATE TABLE IF NOT EXISTS `TemporaryBans` (
                  `GuildID` bigint UNSIGNED NOT NULL,
                  `UserID` bigint UNSIGNED NOT NULL,
                  `ExpiresAt` datetime NOT NULL,
                  PRIMARY KEY (`GuildID`, `UserID`),
                  KEY `ExpiresAt` (`ExpiresAt`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to initialize temporary bans", e);
            return;
        }
        Main.SCHEDULER.scheduleWithFixedDelay(TemporaryBans::processExpired, 15, 15, TimeUnit.SECONDS);
    }

    private static void processExpired() {
        List<BanKey> expiredBans = new ArrayList<>();
        try {
            ResultSet results = DBConnectionManagerLum.sendRequest("SELECT `GuildID`, `UserID` FROM `TemporaryBans` WHERE `ExpiresAt` <= CURRENT_TIMESTAMP");
            while (results.next())
                expiredBans.add(new BanKey(results.getLong("GuildID"), results.getLong("UserID")));
            DBConnectionManagerLum.closeRequest(results);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to find expired temporary bans", e);
            return;
        }

        for (BanKey ban : expiredBans)
            expireBan(ban);
    }

    private static void expireBan(BanKey ban) {
        if (!IN_FLIGHT.add(ban))
            return;
        Guild guild = JDAManager.getJDA().getGuildById(ban.guildId());
        if (guild == null) {
            IN_FLIGHT.remove(ban);
            return;
        }
        guild.unban(UserSnowflake.fromId(ban.userId())).reason("Temporary ban expired").queue(
            ignored -> deleteExpiredBan(ban),
            error -> {
                if (error instanceof ErrorResponseException responseError
                    && responseError.getErrorCode() == ErrorResponse.UNKNOWN_BAN.getCode()) {
                    deleteExpiredBan(ban);
                    return;
                }
                IN_FLIGHT.remove(ban);
                ExceptionUtils.reportException("Failed to expire temporary ban", error);
            });
    }

    private static void deleteExpiredBan(BanKey ban) {
        try {
            DBConnectionManagerLum.sendUpdate("DELETE FROM `TemporaryBans` WHERE `GuildID` = ? AND `UserID` = ?", ban.guildId(), ban.userId());
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to remove expired temporary ban record", e);
        }
        finally {
            IN_FLIGHT.remove(ban);
        }
    }
}
